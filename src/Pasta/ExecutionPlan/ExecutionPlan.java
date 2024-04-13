package Pasta.ExecutionPlan;

import DualEdgeDAG.DualDAGImageRenderer;
import DualEdgeDAG.DualEdge;
import Pasta.PhysicalPlan.PhysicalPlan;
import com.mxgraph.model.mxICell;
import org.jgrapht.Graphs;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.util.SupplierUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ExecutionPlan {
    private final PhysicalPlan physicalPlan;
    private final Set<DualEdge> materializedPhysicalPlanEdges;

    private final Set<DualEdge> pipelinedPhysicalPlanEdges;

    private final double cost;

    private final boolean schedulability;

    public ExecutionPlan(PhysicalPlan physicalPlan, Set<DualEdge> materializedPhysicalPlanEdges) {
        this.physicalPlan = physicalPlan;
        this.materializedPhysicalPlanEdges = new HashSet<>(materializedPhysicalPlanEdges);
        assert materializedPhysicalPlanEdges.containsAll(this.physicalPlan.getBlockingEdges());
        this.pipelinedPhysicalPlanEdges = (new HashSet<>(this.physicalPlan.getDualDAG().edgeSet()));
        this.pipelinedPhysicalPlanEdges.removeAll(this.materializedPhysicalPlanEdges);
        this.schedulability = SchedulabilityChecker.checkPhysicalDAGSchedulability(getDualDAG(), false);
        this.cost = this.schedulability ?
                (this.materializedPhysicalPlanEdges.stream().map(DualEdge::getWeight).reduce(0.0, Double::sum)
                - this.physicalPlan.getBlockingEdges().stream().map(DualEdge::getWeight).reduce(0.0, Double::sum))
        : Double.MAX_VALUE;
//        this.cost = this.materializedPhysicalPlanEdges.stream().map(e -> this.physicalPlan.getMaterializationC2Costs().get(new Pair<>(e.getSource(), e.getTarget()))).reduce(0.0, Double::sum) + pipelinedPhysicalPlanEdges.stream().map(e -> this.physicalPlan.getPipeliningC2Costs().get(new Pair<>(e.getSource(), e.getTarget()))).reduce(0.0, Double::sum);

    }

    public PhysicalPlan getPhysicalPlan() {
        return physicalPlan;
    }

    public Set<DualEdge> getMaterializedPhysicalPlanEdges() {
        return materializedPhysicalPlanEdges;
    }

    public Set<DualEdge> getPipelinedPhysicalPlanEdges() {
        return pipelinedPhysicalPlanEdges;
    }

    public DirectedAcyclicGraph<Integer, DualEdge> getDualDAG() {
        DirectedAcyclicGraph<Integer, DualEdge> dualDAG = new DirectedAcyclicGraph<>(SupplierUtil.createIntegerSupplier(), DualEdge::new, true);
        Graphs.addGraph(dualDAG, physicalPlan.getDualDAG());
        physicalPlan.getDualDAG().edgeSet().forEach(lEdge -> {
            dualDAG.removeEdge(lEdge);
            DualEdge pEdge = dualDAG.addEdge((Integer) lEdge.getSource(), (Integer) lEdge.getTarget());
            pEdge.setBlkOrMat(lEdge.isBlkOrMat());
            dualDAG.setEdgeWeight(pEdge, lEdge.getWeight());
        });
        materializedPhysicalPlanEdges.forEach(lEdge -> dualDAG.getEdge((Integer) lEdge.getSource(), (Integer) lEdge.getTarget()).setBlkOrMat(true));
        return dualDAG;
    }

    public boolean checkSchedulability() {
        return this.schedulability;
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
            if (physicalPlan.isBlockingEdge((Integer) pEdge.getSource(), (Integer) pEdge.getTarget()))
                edgeToCellMap.get(pEdge).setStyle(blockingEdgeColor);
        }
        DualDAGImageRenderer.renderDAGToFile(path, graphAdapter);
    }

    @Override
    public String toString() {
        return "Pasta.ExecutionPlan{" +
//                "PhysicalPlan=" + physicalPlan +
//                ", materializedEdges=" + materializedPhysicalPlanEdges +
//                ", cost=" + cost +
                "cost=" + cost +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExecutionPlan)) return false;
        ExecutionPlan that = (ExecutionPlan) o;
        return Objects.equals(getPhysicalPlan(), that.getPhysicalPlan()) && Objects.equals(getMaterializedPhysicalPlanEdges(), that.getMaterializedPhysicalPlanEdges());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPhysicalPlan(), getMaterializedPhysicalPlanEdges());
    }
}
