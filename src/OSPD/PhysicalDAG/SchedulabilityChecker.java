package OSPD.PhysicalDAG;

import DualEdgeDAG.DualEdge;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.graph.DirectedPseudograph;

import java.util.*;
import java.util.stream.Collectors;

public class SchedulabilityChecker {
    public static boolean testIfSchedulableOld(DirectedAcyclicGraph<Integer, DualEdge> dag, boolean printState) {
        boolean schedulable = true;
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
                    schedulable = false;
                    if (printState) {
                        System.out.println(source + " to " + target + " has overlap:");
                        System.out.println("Blocking paths: " + blockingPaths);
                        System.out.println("Pipelined paths: " + pipelinedPaths);
                    }
                }
            }
        }
        return schedulable;
    }

    public static boolean checkPhysicalDAGSchedulability(DirectedAcyclicGraph<Integer, DualEdge> physicalDAG, boolean printState) {
        DirectedAcyclicGraph<Integer, DualEdge> matEdgeRemovedDAG = new DirectedAcyclicGraph<Integer, DualEdge>(DualEdge.class);
        Graphs.addGraph(matEdgeRemovedDAG, physicalDAG);
        Set<DualEdge> matEdgeSet = matEdgeRemovedDAG.edgeSet().stream().filter(DualEdge::isBlkOrMat).collect(Collectors.toSet());
        Objects.requireNonNull(matEdgeRemovedDAG);
        matEdgeSet.forEach(matEdgeRemovedDAG::removeEdge);
        ConnectivityInspector<Integer, DualEdge> inspector = new ConnectivityInspector<Integer, DualEdge>(matEdgeRemovedDAG);
        List<Set<Integer>> regions = inspector.connectedSets();
        DirectedPseudograph<Integer, DualEdge> regionGraph = new DirectedPseudograph<Integer, DualEdge>(DualEdge.class);
        Map<Integer, Integer> regionOfVertex = new HashMap<Integer, Integer>();

        for (int i = 0; i < regions.size(); ++i) {
            int finalI = i;
            regions.get(i).forEach((v) -> {
                regionOfVertex.put(v, finalI);
            });
            regionGraph.addVertex(i);
        }

        matEdgeSet.forEach((e) -> {
            DualEdge regionEdge = regionGraph.addEdge(regionOfVertex.get((Integer) e.getSource()), regionOfVertex.get((Integer) e.getTarget()));
            regionEdge.setBlkOrMat(true);
        });
        CycleDetector<Integer, DualEdge> regionCycleDetector = new CycleDetector(regionGraph);
        boolean schedulable = !regionCycleDetector.detectCycles();
        if (printState) {
            System.out.println("Showing this physical-DAG's schedulability below:");
            System.out.println("Input P-DAG is: " + physicalDAG);
            System.out.println("Regions are: " + regions);
            System.out.println("Region graph is: " + regionGraph);
            System.out.println("Input P-DAG's schedulability: " + schedulable);
        }

        return schedulable;
    }
}