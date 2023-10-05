package LogicalDAG;

import DualEdgeDAG.DualDAGImageRenderer;
import DualEdgeDAG.DualEdge;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class LogicalDAG {
    private final DirectedAcyclicGraph<Integer, DualEdge> dualDAG;
    private final Set<DualEdge> blockingEdges;

    public LogicalDAG(DirectedAcyclicGraph<Integer, DualEdge> dualDAG) {
        this.dualDAG = dualDAG;
        this.blockingEdges = this.dualDAG.edgeSet().stream().filter(DualEdge::isBlkOrMat).collect(Collectors.toSet());
    }

    public Set<DualEdge> getBlockingEdges() {
        return blockingEdges;
    }

    public DirectedAcyclicGraph<Integer, DualEdge> getDualDAG() {
        return dualDAG;
    }

    public boolean isBlockingEdge(DualEdge edge) {
        return this.blockingEdges.contains(edge);
    }

    public boolean isBlockingEdge(Integer fromVertex, Integer toVertex) {
        return this.blockingEdges.stream().anyMatch(lEdge -> lEdge.getSource().equals(fromVertex) && lEdge.getTarget().equals(toVertex));
    }

    public void renderDAGImageToPath(String path) {
        String blockingEdgeColor = "strokeColor=#CCCC00";
        JGraphXAdapter<Integer, DualEdge> graphAdapter = DualDAGImageRenderer.getGraphAdapter(getDualDAG(), blockingEdgeColor);
        DualDAGImageRenderer.renderDAGToFile(path, graphAdapter);
    }

    @Override
    public String toString() {
        return "LogicalDAG{" +
                "dualDAG=" + dualDAG +
                ", blockingEdges=" + blockingEdges +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LogicalDAG)) return false;
        LogicalDAG that = (LogicalDAG) o;
        return Objects.equals(getDualDAG(), that.getDualDAG());
    }
}
