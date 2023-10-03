package LogicalDAG;

import DualEdgeDAG.DualEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;

public class LogicalDAG {
    private final DirectedAcyclicGraph<Integer, DualEdge> dualDAG;

    public LogicalDAG(DirectedAcyclicGraph<Integer, DualEdge> dualDAG) {
        this.dualDAG = dualDAG;
    }

    public DirectedAcyclicGraph<Integer, DualEdge> getDualDAG() {
        return dualDAG;
    }
}
