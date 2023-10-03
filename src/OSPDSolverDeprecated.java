import DualEdgeDAG.DualEdge;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OSPDSolverDeprecated {
    @Deprecated
    /**
     * This is not correct because only converting the edges in this set will not ensure validity.
     *
     * @param dag
     * @return
     */
    static Set<DualEdge> getMinimalCandidates(DirectedAcyclicGraph<Integer, DualEdge> dag) {
        Set<DualEdge> candidates = new HashSet<DualEdge>();
        // Test if the DAG is schedulable
        AllDirectedPaths<Integer, DualEdge> dijkstra = new AllDirectedPaths<Integer, DualEdge>(dag);
        for (Integer source : dag.vertexSet()) {
            for (Integer target : dag.getDescendants(source)) {
                List<GraphPath<Integer, DualEdge>> paths = dijkstra.getAllPaths(source, target, true, Integer.MAX_VALUE);
                List<GraphPath<Integer, DualEdge>> blockingPaths = new ArrayList<GraphPath<Integer, DualEdge>>();
                List<GraphPath<Integer, DualEdge>> pipelinedPaths = new ArrayList<GraphPath<Integer, DualEdge>>();
                for (GraphPath<Integer, DualEdge> path : paths) {
                    if (path.getEdgeList().stream().anyMatch(DualEdge::isBlkOrMat)) {
                        blockingPaths.add(path);
                    } else {
                        pipelinedPaths.add(path);
                    }
                }
                if (blockingPaths.size() > 0 && pipelinedPaths.size() > 0) {
                    for (GraphPath<Integer, DualEdge> path : pipelinedPaths) {
                        candidates.addAll(path.getEdgeList());
                    }
                }
            }
        }
        return candidates;
    }

    @Deprecated
    static Set<GraphPath<Integer, DualEdge>> getAllProblematicPaths(DirectedAcyclicGraph<Integer, DualEdge> dag) {
        AllDirectedPaths<Integer, DualEdge> dijkstra = new AllDirectedPaths<Integer, DualEdge>(dag);
        Set<GraphPath<Integer, DualEdge>> ppaths = new HashSet<GraphPath<Integer, DualEdge>>();
        for (Integer source : dag.vertexSet()) {
            for (Integer target : dag.getDescendants(source)) {
                List<GraphPath<Integer, DualEdge>> paths = dijkstra.getAllPaths(source, target, true, Integer.MAX_VALUE);
                Set<GraphPath<Integer, DualEdge>> blockingPaths = new HashSet<GraphPath<Integer, DualEdge>>();
                Set<GraphPath<Integer, DualEdge>> pipelinedPaths = new HashSet<GraphPath<Integer, DualEdge>>();
                for (GraphPath<Integer, DualEdge> path : paths) {
                    if (path.getEdgeList().stream().anyMatch(DualEdge::isBlkOrMat)) {
                        blockingPaths.add(path);
                    } else {
                        pipelinedPaths.add(path);
                    }
                }
                if (blockingPaths.size() > 0 && pipelinedPaths.size() > 0) {
                    ppaths.addAll(pipelinedPaths);
                }
            }
        }
        return ppaths;
    }
}