package edu.uci.ics.texera.workflow

import edu.uci.ics.amber.core.workflow.WorkflowContext
import edu.uci.ics.amber.core.workflow.WorkflowParser.{parseWorkflowFile, renderRegionPlanToFile}
import edu.uci.ics.amber.engine.architecture.scheduling.CostBasedScheduleGenerator
import edu.uci.ics.amber.engine.common.virtualidentity.util.CONTROLLER

import java.io.BufferedWriter
import java.nio.file.{Files, Path, Paths, StandardOpenOption}

case class ExperimentResult(
    cost: Double,
    searchTime: Double,
    searchFinished: Boolean,
    numStatesExplored: Int
)

object PastaMatSizeOptimizationExperimentRunner extends App {

  if (args.length != 3) {
    println("Usage: WorkflowExperimentApp <input_file> <output_directory> <results_file>")
    System.exit(1)
  }

  val inputFilePath = args(0)
  val outputPath = args(1)
  val resultsFilePath = args(2)

  val inputFile = Paths.get(inputFilePath)
  if (Files.exists(inputFile) && Files.isRegularFile(inputFile)) {
    val resultsFile = Paths.get(resultsFilePath)
    val bufferedWriter =
      Files.newBufferedWriter(resultsFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    try {
      runExperimentsOnSingleFile(
        inputFile,
        Paths.get(outputPath),
        bufferedWriter
      )
    } catch {
      case e: Exception => throw e
    } finally {
      bufferedWriter.close()
    }
  } else {
    println(s"Input file $inputFilePath does not exist or is not a regular file.")
  }

  def runExperimentsOnSingleFile(
      inputPath: Path,
      planOutputDirectory: Path,
      resultCSVWriter: BufferedWriter
  ): Unit = {
    try {

      if (Files.isRegularFile(inputPath)) {

        println(s"Starting experiments on $inputPath")
        val parts = inputPath.getFileName.toString.split("\\.")
        val workflowName =
          if (parts.length > 1) parts.dropRight(1).mkString(".") else inputPath.getFileName.toString
        val physicalPlan = parseWorkflowFile(filePath = inputPath.toString)
        val numOperators = physicalPlan.dag.vertexSet().size()
        val numLinks = physicalPlan.dag.edgeSet().size()
        val numBlockingLinks = physicalPlan.getBlockingAndDependeeLinks.size
        val numNonBlockingLinks = numLinks - numBlockingLinks
        val pasta = new CostBasedScheduleGenerator(
          new WorkflowContext(),
          physicalPlan,
          actorId = CONTROLLER,
          costFunction = "MATERIALIZATION_SIZES"
        )
        val bottomUpSeedSchedulability = pasta.getNaiveSchedulability()
        val hasMatSizeOnPorts =
          !physicalPlan.links.forall(link => physicalPlan.dag.getEdgeWeight(link) == 1.0)
        val mustMaterializeSize = physicalPlan.getBlockingAndDependeeLinks
          .map(link => physicalPlan.dag.getEdgeWeight(link))
          .sum
        val statsList = List(
          "workflowName" -> workflowName,
          "numOperators" -> numOperators,
          "numLinks" -> numLinks,
          "numBlockingLinks" -> numBlockingLinks,
          "numNonBlockingLinks" -> numNonBlockingLinks,
          "bottomUpSeedSchedulability" -> bottomUpSeedSchedulability,
          "hasMatSizeOnPorts" -> hasMatSizeOnPorts,
          "mustMaterializeSize" -> mustMaterializeSize
        )
        val stats = statsList
          .map { case (_, result) => s""""${result.toString.replace("\"", "\"\"")}"""" }
          .mkString(",")
        if (!bottomUpSeedSchedulability && hasMatSizeOnPorts) {
          println(s"Running experiments on $inputPath")
          val baseline = pasta.baselineMethod
          println(s"$workflowName: baseline finished")

          val topDownGreedy = pasta.topDownSearch(globalSearch = false)
          println(s"$workflowName: topDownGreedy finished")

          val bottomUpGreedy = pasta.bottomUpSearch(globalSearch = false)
          println(s"$workflowName: bottomUpGreedy finished")

          val topDownGlobal = pasta.topDownSearch(globalSearch = true)
          println(s"$workflowName: topDownGlobal finished")

          val bottomUpGlobal = pasta.bottomUpSearch(globalSearch = true)
          println(s"$workflowName: bottomUpGlobal finished")

          val pastaBest =
            Set(topDownGreedy, bottomUpGreedy, topDownGlobal, bottomUpGlobal).minBy(res => res.cost)
          println(s"$workflowName: pastaBest finished with cost ${pastaBest.cost}")

          val topDownGreedyNoOptimization = pasta.topDownSearch(
            globalSearch = false,
            oChains = false,
            oCleanEdges = false,
            oEarlyStop = false
          )
          println(s"$workflowName: topDownGreedyNoOptimization finished")

          val topDownGreedyOChains =
            pasta.topDownSearch(globalSearch = false, oCleanEdges = false, oEarlyStop = false)
          println(s"$workflowName: topDownGreedyOChains finished")

          val topDownGreedyOCleanEdges =
            pasta.topDownSearch(globalSearch = false, oChains = false, oEarlyStop = false)
          println(s"$workflowName: topDownGreedyOCleanEdges finished")

          val topDownGreedyOEarlyStop =
            pasta.topDownSearch(globalSearch = false, oChains = false, oCleanEdges = false)
          println(s"$workflowName: topDownGreedyOEarlyStop finished")

          val bottomUpGreedyNoOptimization = pasta.bottomUpSearch(
            globalSearch = false,
            oChains = false,
            oCleanEdges = false,
            oEarlyStop = false
          )
          println(s"$workflowName: bottomUpGreedyNoOptimization finished")

          val bottomUpGreedyOChains =
            pasta.bottomUpSearch(globalSearch = false, oCleanEdges = false, oEarlyStop = false)
          println(s"$workflowName: bottomUpGreedyOChains finished")

          val bottomUpGreedyOCleanEdges =
            pasta.bottomUpSearch(globalSearch = false, oChains = false, oEarlyStop = false)
          println(s"$workflowName: bottomUpGreedyOCleanEdges finished")

          val bottomUpGreedyOEarlyStop =
            pasta.bottomUpSearch(globalSearch = false, oChains = false, oCleanEdges = false)
          println(s"$workflowName: bottomUpGreedyOEarlyStop finished")

          val topDownGlobalNoOptimization = pasta.topDownSearch(
            globalSearch = true,
            oChains = false,
            oCleanEdges = false,
            oEarlyStop = false
          )
          println(s"$workflowName: topDownGlobalNoOptimization finished")

          val topDownGlobalOChains =
            pasta.topDownSearch(globalSearch = true, oCleanEdges = false, oEarlyStop = false)
          println(s"$workflowName: topDownGlobalOChains finished")

          val topDownGlobalOCleanEdges =
            pasta.topDownSearch(globalSearch = true, oChains = false, oEarlyStop = false)
          println(s"$workflowName: topDownGlobalOCleanEdges finished")

          val topDownGlobalOEarlyStop =
            pasta.topDownSearch(globalSearch = true, oChains = false, oCleanEdges = false)
          println(s"$workflowName: topDownGlobalOEarlyStop finished")

          val bottomUpGlobalNoOptimization = pasta.bottomUpSearch(
            globalSearch = true,
            oChains = false,
            oCleanEdges = false,
            oEarlyStop = false
          )
          println(s"$workflowName: bottomUpGlobalNoOptimization finished")

          val bottomUpGlobalOChains =
            pasta.bottomUpSearch(globalSearch = true, oCleanEdges = false, oEarlyStop = false)
          println(s"$workflowName: bottomUpGlobalOChains finished")

          val bottomUpGlobalOCleanEdges =
            pasta.bottomUpSearch(globalSearch = true, oChains = false, oEarlyStop = false)
          println(s"$workflowName: bottomUpGlobalOCleanEdges finished")

          val bottomUpGlobalOEarlyStop =
            pasta.bottomUpSearch(globalSearch = true, oChains = false, oCleanEdges = false)
          println(s"$workflowName: bottomUpGlobalOEarlyStop finished")
          val resultList = List(
            "baseline" -> baseline,
            "topDownGreedy" -> topDownGreedy,
            "bottomUpGreedy" -> bottomUpGreedy,
            "topDownGlobal" -> topDownGlobal,
            "bottomUpGlobal" -> bottomUpGlobal,
            "pastaBest" -> pastaBest,
            "topDownGreedyNoOptimization" -> topDownGreedyNoOptimization,
            "topDownGreedyOChains" -> topDownGreedyOChains,
            "topDownGreedyOCleanEdges" -> topDownGreedyOCleanEdges,
            "topDownGreedyOEarlyStop" -> topDownGreedyOEarlyStop,
            "bottomUpGreedyNoOptimization" -> bottomUpGreedyNoOptimization,
            "bottomUpGreedyOChains" -> bottomUpGreedyOChains,
            "bottomUpGreedyOCleanEdges" -> bottomUpGreedyOCleanEdges,
            "bottomUpGreedyOEarlyStop" -> bottomUpGreedyOEarlyStop,
            "topDownGlobalNoOptimization" -> topDownGlobalNoOptimization,
            "topDownGlobalOChains" -> topDownGlobalOChains,
            "topDownGlobalOCleanEdges" -> topDownGlobalOCleanEdges,
            "topDownGlobalOEarlyStop" -> topDownGlobalOEarlyStop,
            "bottomUpGlobalNoOptimization" -> bottomUpGlobalNoOptimization,
            "bottomUpGlobalOChains" -> bottomUpGlobalOChains,
            "bottomUpGlobalOCleanEdges" -> bottomUpGlobalOCleanEdges,
            "bottomUpGlobalOEarlyStop" -> bottomUpGlobalOEarlyStop
          )
          val results = resultList
            .map {
              case (_, result) =>
                s""""${ExperimentResult(
                  cost = result.cost,
                  searchTime = result.searchTimeNanoSeconds,
                  searchFinished = result.searchFinished,
                  numStatesExplored = result.numStatesExplored
                ).toString.replace("\"", "\"\"")}""""
            }
            .mkString(",")
          resultCSVWriter.write(stats + ",")
          resultCSVWriter.write(results + "\n")
          resultCSVWriter.flush()
          if (!Files.exists(planOutputDirectory)) Files.createDirectory(planOutputDirectory)
          val outputDirectory = planOutputDirectory.resolve(workflowName)
          if (!Files.exists(outputDirectory)) Files.createDirectory(outputDirectory)
          resultList.foreach {
            case (experimentName, result) =>
              renderRegionPlanToFile(
                physicalPlan = physicalPlan,
                matEdges = result.state,
                imageOutputPath = outputDirectory.resolve(s"$experimentName.png").toString
              )
          }
        } else {
          resultCSVWriter.write(stats + "\n")
          resultCSVWriter.write(",,,,,,,,," + "\n")
          resultCSVWriter.flush()
        }
        println(s"Finished $inputPath")
      } else {
        println(inputPath)
      }
    } catch {
      case error: Exception => throw error
    }
  }
}
