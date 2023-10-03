package PhysicalDAG;

import DualEdgeDAG.DualEdge;
import LogicalDAG.LogicalDAG;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.Set;

public class PhysicalDAG {
    private final LogicalDAG logicalDAG;

    private final Set<DualEdge> materializedEdges;

    public PhysicalDAG(LogicalDAG logicalDAG, Set<DualEdge> materializedEdges) {
        this.logicalDAG = logicalDAG;
        this.materializedEdges = materializedEdges;
    }

    public DirectedAcyclicGraph<Integer, DualEdge> getDualDAG() {
        DirectedAcyclicGraph<Integer, DualEdge> dualDAG = new DirectedAcyclicGraph<>(DualEdge.class);
        Graphs.addGraph(dualDAG, logicalDAG.getDualDAG());
        materializedEdges.forEach(pEdge -> dualDAG.getEdge((Integer) pEdge.getSource(), (Integer) pEdge.getTarget()).setBlkOrMat(true));
        return dualDAG;
    }

    public boolean checkSchedulability() {
        return SchedulabilityChecker.checkPhysicalDAGSchedulability(getDualDAG(), false);
    }
}
