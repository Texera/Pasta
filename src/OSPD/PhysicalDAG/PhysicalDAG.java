package OSPD.PhysicalDAG;

import DualEdgeDAG.DualDAGImageRenderer;
import DualEdgeDAG.DualEdge;
import OSPD.LogicalDAG.LogicalDAG;
import com.mxgraph.model.mxICell;
import org.jgrapht.Graphs;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.util.SupplierUtil;

import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

public class PhysicalDAG {
    private final LogicalDAG logicalDAG;
    private final Set<DualEdge> matLogicalEdges;
    private final double cost;

    public PhysicalDAG(LogicalDAG logicalDAG, Set<DualEdge> matLogicalEdges) {
        this.logicalDAG = logicalDAG;
        this.matLogicalEdges = matLogicalEdges;
        assert matLogicalEdges.containsAll(this.logicalDAG.getBlockingEdges());
        this.cost = this.matLogicalEdges.stream().map(DualEdge::getWeight).reduce(0.0, Double::sum);
    }

    public LogicalDAG getLogicalDAG() {
        return logicalDAG;
    }

    public Set<DualEdge> getMatLogicalEdges() {
        return matLogicalEdges;
    }

    public DirectedAcyclicGraph<Integer, DualEdge> getDualDAG() {
        DirectedAcyclicGraph<Integer, DualEdge> dualDAG = new DirectedAcyclicGraph<>(SupplierUtil.createIntegerSupplier(), DualEdge::new, true);
        Graphs.addGraph(dualDAG, logicalDAG.getDualDAG());
        logicalDAG.getDualDAG().edgeSet().forEach(lEdge -> {
            dualDAG.removeEdge(lEdge);
            DualEdge pEdge = dualDAG.addEdge((Integer) lEdge.getSource(), (Integer) lEdge.getTarget());
            pEdge.setBlkOrMat(lEdge.isBlkOrMat());
            dualDAG.setEdgeWeight(pEdge, lEdge.getWeight());
        });
        matLogicalEdges.forEach(lEdge -> dualDAG.getEdge((Integer) lEdge.getSource(), (Integer) lEdge.getTarget()).setBlkOrMat(true));
        return dualDAG;
    }

    public boolean checkSchedulability() {
        return SchedulabilityChecker.checkPhysicalDAGSchedulability(getDualDAG(), false);
    }

    public boolean showSchedulability() {
        return SchedulabilityChecker.checkPhysicalDAGSchedulability(getDualDAG(), true);
    }

    public double getCost() {
        return this.cost;
    }

    public void renderDAGImageToPath(String path) {
        String matEdgeColor = "strokeColor=#eb6a57";
        String blockingEdgeColor = "strokeColor=#CCCC00";
        DirectedAcyclicGraph<Integer, DualEdge> dualDAG = getDualDAG();
        JGraphXAdapter<Integer, DualEdge> graphAdapter = DualDAGImageRenderer.getGraphAdapter(dualDAG, matEdgeColor);
        HashMap<DualEdge, mxICell> edgeToCellMap = graphAdapter.getEdgeToCellMap();
        for (DualEdge pEdge : dualDAG.edgeSet()) {
            if (logicalDAG.isBlockingEdge((Integer) pEdge.getSource(), (Integer) pEdge.getTarget()))
                edgeToCellMap.get(pEdge).setStyle(blockingEdgeColor);
        }
        DualDAGImageRenderer.renderDAGToFile(path, graphAdapter);
    }

    @Override
    public String toString() {
        return "OSPD.PhysicalDAG{" +
                "logicalDAG=" + logicalDAG +
                ", materializedEdges=" + matLogicalEdges +
                ", cost=" + cost +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhysicalDAG)) return false;
        PhysicalDAG that = (PhysicalDAG) o;
        return Objects.equals(getLogicalDAG(), that.getLogicalDAG()) && Objects.equals(getMatLogicalEdges(), that.getMatLogicalEdges());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLogicalDAG(), getMatLogicalEdges());
    }
}
