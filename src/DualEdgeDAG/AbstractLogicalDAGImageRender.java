package DualEdgeDAG;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxICell;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxStylesheet;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DirectedAcyclicGraph;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class AbstractLogicalDAGImageRender {

    public static void renderDAGToFile(String outputPath, JGraphXAdapter<Integer, DualEdge> graphAdapter) {
        BufferedImage image =
                mxCellRenderer.createBufferedImage(graphAdapter, null, 10, Color.WHITE, true, null);
        File imgFile = new File(outputPath);
        try {
            ImageIO.write(image, "PNG", imgFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static JGraphXAdapter<Integer, DualEdge> getGraphAdapter(DirectedAcyclicGraph<Integer, DualEdge> dag, String strokeColor) {
        JGraphXAdapter<Integer, DualEdge> graphAdapter = new JGraphXAdapter<Integer, DualEdge>(dag);
        mxHierarchicalLayout layout = new mxHierarchicalLayout(graphAdapter, SwingConstants.WEST);
//        layout.setIntraCellSpacing(50);
//        layout.setInterRankCellSpacing(50);
        layout.execute(graphAdapter.getDefaultParent());
        HashMap<DualEdge, mxICell> edgeToCellMap = graphAdapter.getEdgeToCellMap();
        mxStylesheet stylesheet = getMxStylesheet(graphAdapter);

        graphAdapter.setStylesheet(stylesheet);

        final int vertexDiameter = 30; // Set the diameter for the vertex
        graphAdapter.getModel().beginUpdate();
        try {
            for (Object vertex : graphAdapter.getChildVertices(graphAdapter.getDefaultParent())) {
                graphAdapter.getModel().getGeometry(vertex).setWidth(vertexDiameter);
                graphAdapter.getModel().getGeometry(vertex).setHeight(vertexDiameter);
            }
        } finally {
            graphAdapter.getModel().endUpdate();
        }

        DecimalFormat df = new DecimalFormat("#.#");

        for (Integer vertex : dag.vertexSet()) {
            graphAdapter.getVertexToCellMap().get(vertex).setValue("");
        }

        for (DualEdge edge : dag.edgeSet()) {
            if (edge.isBlkOrMat()) {
                edgeToCellMap.get(edge).setStyle(strokeColor);
            }
            edgeToCellMap.get(edge).setValue("");
        }
        return graphAdapter;
    }

    public static mxStylesheet getMxStylesheet(JGraphXAdapter<Integer, DualEdge> graphAdapter) {
        mxStylesheet stylesheet = graphAdapter.getStylesheet();
        Map<String, Object> edgeStyle = stylesheet.getDefaultEdgeStyle();
        Map<String, Object> vertexStyle = stylesheet.getDefaultVertexStyle();
        edgeStyle.put(mxConstants.STYLE_FONTSIZE, 7);
        edgeStyle.put(mxConstants.STYLE_FONTFAMILY, "Arial");
        edgeStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_BLOCK);
        edgeStyle.put(mxConstants.STYLE_ENDSIZE, 5);
        edgeStyle.put(mxConstants.STYLE_STROKEWIDTH, 4);
        edgeStyle.put(mxConstants.STYLE_STROKE_OPACITY, 50);
        edgeStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000");
        vertexStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_ELLIPSE);
        vertexStyle.put(mxConstants.STYLE_PERIMETER, mxConstants.PERIMETER_ELLIPSE);
        vertexStyle.put(mxConstants.STYLE_FONTSIZE, 7);
        vertexStyle.put(mxConstants.STYLE_FONTFAMILY, "Arial");
        vertexStyle.put(mxConstants.STYLE_ROUNDED, true);
        vertexStyle.put(mxConstants.STYLE_FILLCOLOR, "#FFFFFF");
        vertexStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000"); // Black color for vertex borders
        vertexStyle.put(mxConstants.STYLE_STROKEWIDTH, 3); // Increase the line width as needed
        stylesheet.setDefaultVertexStyle(vertexStyle); // Set the default style for vertices
        stylesheet.setDefaultEdgeStyle(edgeStyle); // Set the default style for edges
        return stylesheet;
    }
}