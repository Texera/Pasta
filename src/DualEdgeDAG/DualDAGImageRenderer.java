package DualEdgeDAG;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.model.mxICell;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxStylesheet;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DirectedAcyclicGraph;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class DualDAGImageRenderer {
    public static void renderLogicalDAG(DirectedAcyclicGraph<Integer, DualEdge> dag, String outputPath) {

    }

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
        mxIGraphLayout layout = new mxHierarchicalLayout(graphAdapter);
        layout.execute(graphAdapter.getDefaultParent());
        HashMap<DualEdge, mxICell> edgeToCellMap = graphAdapter.getEdgeToCellMap();
        mxStylesheet stylesheet = getMxStylesheet(graphAdapter);

        graphAdapter.setStylesheet(stylesheet);

        DecimalFormat df = new DecimalFormat("#.#");

        for (DualEdge edge : dag.edgeSet()) {
            if (edge.isBlkOrMat()) {
                edgeToCellMap.get(edge).setStyle(strokeColor);
            }
            edgeToCellMap.get(edge).setValue(df.format(dag.getEdgeWeight(edge)));
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
        edgeStyle.put(mxConstants.STYLE_ENDSIZE, 2);
        edgeStyle.put(mxConstants.STYLE_STROKEWIDTH, 1);
        edgeStyle.put(mxConstants.STYLE_STROKE_OPACITY, 50);
        vertexStyle.put(mxConstants.STYLE_FONTFAMILY, "Arial");
        vertexStyle.put(mxConstants.STYLE_ROUNDED, true);
        stylesheet.setDefaultVertexStyle(vertexStyle); // Set the default style for vertices
        stylesheet.setDefaultEdgeStyle(edgeStyle); // Set the default style for edges
        return stylesheet;
    }
}