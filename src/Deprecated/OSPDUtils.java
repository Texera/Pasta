package Deprecated;

import DualEdgeDAG.DualEdge;
import PhysicalDAG.SchedulabilityChecker;
import javafx.util.Pair;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OSPDUtils {

    public static Double getCost(Set<DualEdge> currentState) {
        return currentState.stream().map(DualEdge::getWeight).reduce(0.0, Double::sum);
    }

    public static Set<GraphPath<Integer, DualEdge>> getChainPaths(DirectedAcyclicGraph<Integer, DualEdge> dag) {
        AllDirectedPaths<Integer, DualEdge> dijkstra = new AllDirectedPaths<Integer, DualEdge>(dag);
        Set<GraphPath<Integer, DualEdge>> cpaths = new HashSet<GraphPath<Integer, DualEdge>>();
        for (Integer source : dag.vertexSet()) {
            for (Integer target : dag.getDescendants(source)) {
                List<GraphPath<Integer, DualEdge>> paths = dijkstra.getAllPaths(source, target, true, Integer.MAX_VALUE);
                for (GraphPath<Integer, DualEdge> path : paths) {
                    if (path.getVertexList().stream().noneMatch(v -> (!Objects.equals(v, path.getEndVertex()) && !Objects.equals(v, path.getStartVertex()) && dag.degreeOf(v) > 2)) && path.getLength() > 1)
                        cpaths.add(path);
                }
            }
        }

        Iterator<GraphPath<Integer, DualEdge>> iterator = cpaths.iterator();
        while (iterator.hasNext()) {
            GraphPath<Integer, DualEdge> gp = iterator.next();
            Set<DualEdge> gps = new HashSet<DualEdge>(gp.getEdgeList());
            HashSet<GraphPath<Integer, DualEdge>> pCopy = new HashSet<GraphPath<Integer, DualEdge>>(cpaths);
            pCopy.remove(gp);
            if (pCopy.stream().map(p -> new HashSet<DualEdge>(p.getEdgeList())).anyMatch(s -> s.containsAll(gps)))
                iterator.remove();
        }
        return cpaths;
    }

    public static Set<Integer> getBlockedVertices(DirectedAcyclicGraph<Integer, DualEdge> dag) {
        AllDirectedPaths<Integer, DualEdge> dijkstra = new AllDirectedPaths<Integer, DualEdge>(dag);
        Set<Integer> blockedVertices = new HashSet<Integer>();
        for (Integer source : dag.vertexSet()) {
            for (Integer target : dag.getDescendants(source)) {
                List<GraphPath<Integer, DualEdge>> paths = dijkstra.getAllPaths(source, target, true, Integer.MAX_VALUE);
                List<GraphPath<Integer, DualEdge>> blockingPaths = new ArrayList<GraphPath<Integer, DualEdge>>();
                for (GraphPath<Integer, DualEdge> path : paths) {
                    if (path.getEdgeList().stream().anyMatch(DualEdge::isBlkOrMat)) {
                        blockingPaths.add(path);
                    }
                }
                if (!blockingPaths.isEmpty()) {
                    blockedVertices.add(target);
                }
            }
        }
        return blockedVertices;
    }

    public static boolean testStateValidity(DirectedAcyclicGraph<Integer, DualEdge> dag, Set<DualEdge> currentState) {
        DirectedAcyclicGraph<Integer, DualEdge> dagCopy = (DirectedAcyclicGraph<Integer, DualEdge>) dag.clone();
        for (DualEdge e : currentState) {
            dagCopy.removeEdge(e);
            dagCopy.addEdge((Integer) e.getSource(), (Integer) e.getTarget());
            DualEdge eCopy = dagCopy.getEdge((Integer) e.getSource(), (Integer) e.getTarget());
            eCopy.setBlkOrMat(true);
            dagCopy.setEdgeWeight(eCopy, e.getWeight());
        }
        return SchedulabilityChecker.checkPhysicalDAGSchedulability(dagCopy, false);
    }

    public static Set<GraphPath<Integer, DualEdge>> getMinimalProblematicPaths(DirectedAcyclicGraph<Integer, DualEdge> dag) {
        Map<Pair<Integer, Integer>, Pair<Set<GraphPath<Integer, DualEdge>>, Set<GraphPath<Integer, DualEdge>>>> conflictingVertices = new HashMap<Pair<Integer, Integer>, Pair<Set<GraphPath<Integer, DualEdge>>, Set<GraphPath<Integer, DualEdge>>>>();
        AllDirectedPaths<Integer, DualEdge> dijkstra = new AllDirectedPaths<Integer, DualEdge>(dag);
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
                    Pair<Integer, Integer> currentVertexPair = new Pair<Integer, Integer>(source, target);
                    Pair<Set<GraphPath<Integer, DualEdge>>, Set<GraphPath<Integer, DualEdge>>> currentVPPaths = new Pair<Set<GraphPath<Integer, DualEdge>>, Set<GraphPath<Integer, DualEdge>>>(blockingPaths, pipelinedPaths);
                    for (Map.Entry<Pair<Integer, Integer>, Pair<Set<GraphPath<Integer, DualEdge>>, Set<GraphPath<Integer, DualEdge>>>> cv : conflictingVertices.entrySet()) {
                        reduceConflictPairs(dag, cv.getKey(), cv.getValue(), currentVertexPair, currentVPPaths);
                        reduceConflictPairs(dag, currentVertexPair, currentVPPaths, cv.getKey(), cv.getValue());

                    }
                    if (!currentVPPaths.getKey().isEmpty() && !currentVPPaths.getValue().isEmpty())
                        conflictingVertices.put(currentVertexPair, currentVPPaths);
                }
            }
        }

        Set<Pair<Integer, Integer>> toRemove = new HashSet<Pair<Integer, Integer>>();

        for (Map.Entry<Pair<Integer, Integer>, Pair<Set<GraphPath<Integer, DualEdge>>, Set<GraphPath<Integer, DualEdge>>>> cv : conflictingVertices.entrySet()) {
            if (cv.getValue().getKey().isEmpty() && cv.getValue().getValue().isEmpty()) toRemove.add(cv.getKey());
        }

        for (Pair<Integer, Integer> p : toRemove) {
            conflictingVertices.remove(p);
        }

        System.out.println(conflictingVertices);

        return conflictingVertices.values().stream().map(Pair::getValue).reduce(new HashSet<GraphPath<Integer, DualEdge>>(), (s1, s2) -> Stream.concat(s1.stream(), s2.stream()).collect(Collectors.toSet()));
    }

    public static void reduceConflictPairs(DirectedAcyclicGraph<Integer, DualEdge> dag, Pair<Integer, Integer> vp1, Pair<Set<GraphPath<Integer, DualEdge>>, Set<GraphPath<Integer, DualEdge>>> cp1, Pair<Integer, Integer> vp2, Pair<Set<GraphPath<Integer, DualEdge>>, Set<GraphPath<Integer, DualEdge>>> cp2) {
        if (Objects.equals(vp1.getKey(), vp2.getKey()) || dag.getDescendants(vp1.getKey()).contains(vp2.getKey())) {
            if (Objects.equals(vp1.getValue(), vp2.getValue()) || dag.getDescendants(vp2.getValue()).contains(vp1.getValue())) {
                // If all the paths are contained
                reducePathSet(cp1.getKey(), cp2.getKey());
                reducePathSet(cp1.getValue(), cp2.getValue());
            }
        }
    }

    public static void reducePathSet(Set<GraphPath<Integer, DualEdge>> pathSet1, Set<GraphPath<Integer, DualEdge>> pathSet2) {
        Iterator<GraphPath<Integer, DualEdge>> iterator = pathSet1.iterator();
        while (iterator.hasNext()) {
            GraphPath<Integer, DualEdge> cp1 = iterator.next();
            Set<DualEdge> es1 = new HashSet<DualEdge>(cp1.getEdgeList());
            boolean hasSubPath = false;
            for (Set<DualEdge> es2 : pathSet2.stream().map(GraphPath::getEdgeList).map(HashSet::new).collect(Collectors.toSet())) {
                if (es1.containsAll(es2)) {
                    hasSubPath = true;
                    break;
                }
            }
            if (hasSubPath) iterator.remove();
        }
    }
}