package edu.uci.ics.amber.core.workflow

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout
import com.mxgraph.model.mxICell
import com.mxgraph.util.{mxCellRenderer, mxConstants}
import com.mxgraph.view.mxStylesheet
import edu.uci.ics.amber.core.executor.OpExecWithCode
import edu.uci.ics.amber.core.virtualidentity.{
  ExecutionIdentity,
  OperatorIdentity,
  PhysicalOpIdentity,
  WorkflowIdentity
}
import io.circe.{Json, ParsingFailure, parser}
import org.jgrapht.alg.connectivity.ConnectivityInspector
import org.jgrapht.ext.JGraphXAdapter
import org.jgrapht.graph.DirectedAcyclicGraph
import org.jgrapht.util.SupplierUtil

import java.awt._
import java.awt.image.BufferedImage
import java.io.File
import java.text.DecimalFormat
import java.util.{Map => JMap}
import javax.imageio.ImageIO
import javax.swing._
import scala.collection.mutable
import scala.io.Source
import scala.jdk.CollectionConverters.CollectionHasAsScala

object WorkflowParser {

  def parseWorkflowFile(filePath: String): PhysicalPlan = {
    val fileContent =
      try {
        Source.fromFile(filePath).getLines.mkString
      } catch {
        case e: Exception =>
          e.printStackTrace()
          ""
      } finally {
        Source.fromFile(filePath).close()
      }
    val json: Either[ParsingFailure, Json] = parser.parse(fileContent)
    // Handle the parsing result
    json match {
      case Right(doc) =>
        val portMatSizeMap: mutable.Map[(PhysicalOpIdentity, PortIdentity), Double] =
          mutable.Map.empty
        val operators = doc.hcursor.downField("operators").values match {
          case Some(operatorsJson) =>
            operatorsJson.toList.flatMap { operatorJson =>
              for {
                operatorIdStr <- operatorJson.hcursor.get[String]("operatorID").toOption
                operatorDNStr <- operatorJson.hcursor.get[String]("customDisplayName").toOption
                operatorID = PhysicalOpIdentity.apply(OperatorIdentity.apply(operatorIdStr), "main")
                inputPorts =
                  operatorJson.hcursor
                    .downField("inputPorts")
                    .values
                    .getOrElse(Vector())
                    .map(portJson => {
                      val portIdStr = portJson.hcursor.get[String]("portID").getOrElse("unknown")
                      val portId = PortIdentity(extractId(portIdStr))
                      val displayName = portJson.hcursor.get[String]("portID").getOrElse("")
                      InputPort(portId, displayName)
                    })

                outputPorts =
                  operatorJson.hcursor
                    .downField("outputPorts")
                    .values
                    .getOrElse(Vector())
                    .map(portJson => {
                      val portIdStr = portJson.hcursor.get[String]("portID").getOrElse("unknown")
                      val portId = PortIdentity(extractId(portIdStr))
                      val displayName = portJson.hcursor.get[String]("portID").getOrElse("")
                      val blocking: Boolean =
                        portJson.hcursor.get[Boolean]("isBlocking").getOrElse(false)
                      val matSize: Double =
                        portJson.hcursor.get[Double]("materializationSize").getOrElse(1.0)
                      portMatSizeMap += (operatorID, portId) -> matSize
                      OutputPort(portId, displayName, blocking)
                    })
              } yield PhysicalOp(
                id = operatorID,
                workflowId = WorkflowIdentity.defaultInstance,
                executionId = ExecutionIdentity.defaultInstance,
                opExecInitInfo = OpExecWithCode()
              ).withInputPorts(inputPorts.toList).withOutputPorts(outputPorts.toList)
            }.toSet
          case None => Set[PhysicalOp]()
        }
        val opIdentityMap = operators.map(op => op.id.logicalOpId.id -> op.id).toMap

        var links = doc.hcursor.downField("links").values match {
          case Some(linksJson) =>
            linksJson.toList.flatMap { linkJson =>
              for {
                fromOpId <- opIdentityMap.get(
                  linkJson.hcursor.downField("source").get[String]("operatorID").getOrElse("")
                )
                toOpId <- opIdentityMap.get(
                  linkJson.hcursor.downField("target").get[String]("operatorID").getOrElse("")
                )
              } yield {
                val fromPortId = PortIdentity(
                  extractId(
                    linkJson.hcursor.downField("source").get[String]("portID").getOrElse("unknown")
                  )
                )
                val toPortId = PortIdentity(
                  extractId(
                    linkJson.hcursor.downField("target").get[String]("portID").getOrElse("unknown")
                  )
                )
                PhysicalLink(fromOpId, fromPortId, toOpId, toPortId)
              }
            }.toSet
          case None => Set[PhysicalLink]()

        }

        val jgraphtDag = new DirectedAcyclicGraph[PhysicalOpIdentity, PhysicalLink](
          null, // vertexSupplier
          SupplierUtil.createSupplier(classOf[PhysicalLink]), // edgeSupplier
          true, // weighted
          true // allowMultipleEdges
        )
        operators.foreach(op => jgraphtDag.addVertex(op.id))
        val operatorsMap = operators.map(op => op.id -> op).toMap
        links.foreach(l => {
          try {
            val fromOp = operatorsMap(l.fromOpId)
            val toOp = operatorsMap(l.toOpId)
            if (
              !fromOp.outputPorts.contains(l.fromPortId) || !toOp.inputPorts.contains(l.toPortId)
            ) {
              links = links - l
              println(s"Removed invalid link $l as it connects to a non-existing port.")
            } else jgraphtDag.addEdge(l.fromOpId, l.toOpId, l)
          } catch {
            case _: Exception => {
              links = links - l
              println(s"Removed link $l to keep the input physical plan acyclic.")
            }
          }
        })

        val inspector = new ConnectivityInspector[PhysicalOpIdentity, PhysicalLink](jgraphtDag)
        val components = inspector.connectedSets()

        var largestComponent: Set[PhysicalOpIdentity] = Set.empty
        var maxSize = 0

        components.forEach { component =>
          if (component.size() > maxSize) {
            maxSize = component.size()
            largestComponent = component.asScala.toSet
          }
        }

        val operatorsToAdd = largestComponent.map(opId => operatorsMap(opId))
        val linksToAdd = jgraphtDag
          .edgeSet()
          .asScala
          .filter { edge =>
            val source = jgraphtDag.getEdgeSource(edge)
            val target = jgraphtDag.getEdgeTarget(edge)
            largestComponent.contains(source) && largestComponent.contains(target)
          }
          .toSet

        val physicalPlan = PhysicalPlan(operators = operatorsToAdd, links = linksToAdd)

        physicalPlan.links.foreach(link => {
          val matSize = portMatSizeMap((link.fromOpId, link.fromPortId))
          physicalPlan.dag.setEdgeWeight(link, matSize)
        })

        //        println(largestComponent)
        physicalPlan
      case Left(error) =>
        println("Failed to parse JSON:")
        throw error
    }
  }

  private def extractId(portIdStr: String): Int = {
    // Using regular expression to extract numbers
    "\\d+".r.findFirstIn(portIdStr).getOrElse("-1").toInt
  }

  def renderInputPhysicalPlanToFile(physicalPlan: PhysicalPlan, imageOutputPath: String): Unit = {
    val graphAdapter = getInputPhysicalPlanGraphAdapter(physicalPlan = physicalPlan)
    renderDAGToFile(imageOutputPath, graphAdapter)
  }

  def renderRegionPlanToFile(
      physicalPlan: PhysicalPlan,
      matEdges: Set[PhysicalLink],
      imageOutputPath: String
  ): Unit = {
    val graphAdapter = getRegionPlanGraphAdapter(physicalPlan = physicalPlan, matEdges = matEdges)
    renderDAGToFile(imageOutputPath, graphAdapter)
  }

  private def renderDAGToFile(
      outputPath: String,
      graphAdapter: JGraphXAdapter[PhysicalOpIdentity, PhysicalLink]
  ): Unit = {
    try {
      val originalImage: BufferedImage =
        mxCellRenderer.createBufferedImage(graphAdapter, null, 1, Color.WHITE, true, null)
      val maxWidth = 15000
      val maxHeight = 10000

      val width = originalImage.getWidth
      val height = originalImage.getHeight

      val aspectRatio = width.toDouble / height.toDouble
      var newWidth = 0
      var newHeight = 0

      if (width > height) {
        newWidth = Math.min(width, maxWidth)
        newHeight = (newWidth / aspectRatio).toInt
      } else {
        newHeight = Math.min(height, maxHeight)
        newWidth = (newHeight * aspectRatio).toInt
      }

      val resizedImage = new BufferedImage(newWidth, newHeight, originalImage.getType)
      val g: Graphics2D = resizedImage.createGraphics()

      g.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BILINEAR
      )
      g.drawImage(originalImage, 0, 0, newWidth, newHeight, null)
      g.dispose()

      val imgFile = new File(outputPath)
      ImageIO.write(resizedImage, "PNG", imgFile)
    } catch {
      case e: Exception => println(e)
    }
  }

  private def getRegionPlanGraphAdapter(
      physicalPlan: PhysicalPlan,
      matEdges: Set[PhysicalLink]
  ): JGraphXAdapter[PhysicalOpIdentity, PhysicalLink] = {
    def getMxStylesheet(
        graphAdapter: JGraphXAdapter[PhysicalOpIdentity, PhysicalLink]
    ): mxStylesheet = {
      val stylesheet = graphAdapter.getStylesheet
      val edgeStyle: JMap[String, Object] = stylesheet.getDefaultEdgeStyle
      val vertexStyle: JMap[String, Object] = stylesheet.getDefaultVertexStyle
      edgeStyle.put(mxConstants.STYLE_FONTSIZE, java.lang.Integer.valueOf(7))
      edgeStyle.put(mxConstants.STYLE_FONTFAMILY, "Arial")
      edgeStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_BLOCK)
      edgeStyle.put(mxConstants.STYLE_ENDSIZE, java.lang.Integer.valueOf(2))
      edgeStyle.put(mxConstants.STYLE_STROKEWIDTH, java.lang.Integer.valueOf(1))
      edgeStyle.put(mxConstants.STYLE_STROKE_OPACITY, java.lang.Integer.valueOf(50))
      vertexStyle.put(mxConstants.STYLE_FONTFAMILY, "Arial")
      vertexStyle.put(mxConstants.STYLE_ROUNDED, java.lang.Boolean.TRUE)
      stylesheet.setDefaultVertexStyle(vertexStyle) // Set the default style for vertices
      stylesheet.setDefaultEdgeStyle(edgeStyle) // Set the default style for edges
      stylesheet
    }

    val blockingEdgeColor = "strokeColor=#b22812"
    val matEdgeColor = "strokeColor=#cccc00"
    val graphAdapter = new JGraphXAdapter[PhysicalOpIdentity, PhysicalLink](physicalPlan.dag)

    val vertexDiameter = 50 // Set the diameter for the vertex
    graphAdapter.getModel.beginUpdate()
    try {
      for (vertex <- graphAdapter.getChildVertices(graphAdapter.getDefaultParent)) {
        val geometry = graphAdapter.getModel.getGeometry(vertex)
        geometry.setWidth(vertexDiameter)
        geometry.setHeight(vertexDiameter)
      }
    } finally {
      graphAdapter.getModel.endUpdate()
    }

    for (vertex <- physicalPlan.dag.vertexSet.asScala) {
      graphAdapter.getVertexToCellMap.get(vertex).setValue(vertex.logicalOpId.id.substring(15, 22))
    }

    val layout = new mxHierarchicalLayout(graphAdapter, SwingConstants.WEST)
    layout.execute(graphAdapter.getDefaultParent)
    val edgeToCellMap: JMap[PhysicalLink, mxICell] = graphAdapter.getEdgeToCellMap
    val stylesheet = getMxStylesheet(graphAdapter)

    graphAdapter.setStylesheet(stylesheet)

    val df = new DecimalFormat("#.#")

    for (edge <- physicalPlan.dag.edgeSet.asScala) {
      if (matEdges.contains(edge)) {
        edgeToCellMap.get(edge).setStyle(matEdgeColor)
      }
      if (physicalPlan.getBlockingAndDependeeLinks.contains(edge)) {
        edgeToCellMap.get(edge).setStyle(blockingEdgeColor)
      }
      edgeToCellMap.get(edge).setValue(df.format(physicalPlan.dag.getEdgeWeight(edge)))
    }
    graphAdapter
  }

  private def getInputPhysicalPlanGraphAdapter(
      physicalPlan: PhysicalPlan
  ): JGraphXAdapter[PhysicalOpIdentity, PhysicalLink] = {
    def getMxStylesheet(
        graphAdapter: JGraphXAdapter[PhysicalOpIdentity, PhysicalLink]
    ): mxStylesheet = {
      val stylesheet = graphAdapter.getStylesheet
      val edgeStyle: JMap[String, Object] = stylesheet.getDefaultEdgeStyle
      val vertexStyle: JMap[String, Object] = stylesheet.getDefaultVertexStyle
      edgeStyle.put(mxConstants.STYLE_FONTSIZE, java.lang.Integer.valueOf(7))
      edgeStyle.put(mxConstants.STYLE_FONTFAMILY, "Arial")
      edgeStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC)
      edgeStyle.put(mxConstants.STYLE_DASHED, java.lang.Boolean.FALSE)
      edgeStyle.put(mxConstants.STYLE_ENDSIZE, java.lang.Integer.valueOf(50))
      edgeStyle.put(mxConstants.STYLE_STROKEWIDTH, java.lang.Integer.valueOf(50))
      edgeStyle.put(mxConstants.STYLE_STROKE_OPACITY, java.lang.Integer.valueOf(50))
      edgeStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000")
      edgeStyle.put(mxConstants.STYLE_EDGE, mxConstants.EDGESTYLE_ELBOW)
      edgeStyle.put(mxConstants.STYLE_STARTARROW, mxConstants.NONE)
      edgeStyle.put(mxConstants.SHAPE_CURVE, java.lang.Boolean.TRUE)
      vertexStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_ELLIPSE)
      vertexStyle.put(mxConstants.STYLE_PERIMETER, mxConstants.PERIMETER_ELLIPSE)
      vertexStyle.put(mxConstants.STYLE_FONTSIZE, java.lang.Integer.valueOf(7))
      vertexStyle.put(mxConstants.STYLE_FONTFAMILY, "Arial")
      vertexStyle.put(mxConstants.STYLE_ROUNDED, java.lang.Boolean.TRUE)
      vertexStyle.put(mxConstants.STYLE_FILLCOLOR, "#FFFFFF")
      vertexStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000") // Black color for vertex borders
      vertexStyle.put(
        mxConstants.STYLE_STROKEWIDTH,
        java.lang.Integer.valueOf(50)
      ) // Increase the line width as needed
      stylesheet.setDefaultVertexStyle(vertexStyle) // Set the default style for vertices
      stylesheet.setDefaultEdgeStyle(edgeStyle) // Set the default style for edges
      stylesheet
    }

    val strokeColor = "strokeColor=#b22812"
    val graphAdapter = new JGraphXAdapter[PhysicalOpIdentity, PhysicalLink](physicalPlan.dag)
    val layout = new mxHierarchicalLayout(graphAdapter, SwingConstants.WEST)

    val edgeToCellMap: JMap[PhysicalLink, mxICell] = graphAdapter.getEdgeToCellMap
    val stylesheet = getMxStylesheet(graphAdapter)

    val vertexDiameter = 500 // Set the diameter for the vertex
    graphAdapter.getModel.beginUpdate()
    try {
      for (vertex <- graphAdapter.getChildVertices(graphAdapter.getDefaultParent)) {
        val geometry = graphAdapter.getModel.getGeometry(vertex)
        geometry.setWidth(vertexDiameter)
        geometry.setHeight(vertexDiameter)
      }
    } finally {
      graphAdapter.getModel.endUpdate()
    }

    for (vertex <- physicalPlan.dag.vertexSet.asScala) {
      graphAdapter.getVertexToCellMap.get(vertex).setValue("")
    }

    for (edge <- physicalPlan.dag.edgeSet.asScala) {
      if (physicalPlan.getBlockingAndDependeeLinks.contains(edge)) {
        edgeToCellMap.get(edge).setStyle(strokeColor)
      }
      edgeToCellMap.get(edge).setValue("")
    }

    layout.setIntraCellSpacing(500) // Decreases the space between cells at the same level
    layout.setInterRankCellSpacing(300) // Decreases the vertical space between ranks
    layout.setParallelEdgeSpacing(500)
    layout.execute(graphAdapter.getDefaultParent)

    graphAdapter.setStylesheet(stylesheet)

    graphAdapter
  }
}
