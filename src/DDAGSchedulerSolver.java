import DualEdgeDAG.DualEdge;
import PhysicalDAG.SchedulabilityChecker;
import com.google.common.collect.Sets;
import javafx.util.Pair;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DDAGSchedulerSolver {
    static Pair<Double, Set<DualEdge>> generationBasedMinimalConversion(DirectedAcyclicGraph<Integer, DualEdge> dag) {
        Set<GraphPath<Integer, DualEdge>> candidatePaths = getMinimalProblematicPaths(dag);
        List<Set<DualEdge>> edgeSets = candidatePaths.stream().map(p -> new HashSet<DualEdge>(p.getEdgeList())).collect(Collectors.toList());
        Set<List<DualEdge>> cp = Sets.cartesianProduct(edgeSets);
        Set<Set<DualEdge>> candidates = cp.stream().map(HashSet::new).collect(Collectors.toSet());
        System.out.println("Generation based algorithm had " + candidates.size() + " candidates: " + candidates);
        Pair<Double, Set<DualEdge>> optimum = new Pair<Double, Set<DualEdge>>(Double.MAX_VALUE, new HashSet<DualEdge>());
        Set<Set<DualEdge>> invalidSolutions = new HashSet<Set<DualEdge>>();
        for (Set<DualEdge> candidate : candidates) {
            if (testStateValidity(dag, candidate)) {
                Double cost = getCost(candidate);
                if (cost < optimum.getKey()) {
                    optimum = new Pair<Double, Set<DualEdge>>(cost, candidate);
                }
            } else {
                invalidSolutions.add(candidate);
            }
        }
        System.out.println(invalidSolutions.size() + " candidates are not valid: " + invalidSolutions);
        return optimum;
    }

    static Pair<Double, Set<DualEdge>> searchBasedMinimalConversion(DirectedAcyclicGraph<Integer, DualEdge> dag) {
        Set<DualEdge> pipelinedEdges = dag.edgeSet().stream().filter(e -> !e.isBlkOrMat()).collect(Collectors.toSet());
        BigDecimal spaceSize = BigDecimal.valueOf((1L << pipelinedEdges.size()));
        System.out.println("All Pipelined Edges (" + pipelinedEdges.size() + ", " + spaceSize + " states in the search space)" + "): " + pipelinedEdges);
        Set<Integer> blockedVertices = getBlockedVertices(dag);
        Iterator<DualEdge> edgeIterator = pipelinedEdges.iterator();
        while (edgeIterator.hasNext()) {
            DualEdge e = edgeIterator.next();
            Set<Integer> intersection = dag.getDescendants((Integer) e.getTarget());
            intersection.add((Integer) e.getTarget());
            intersection.retainAll(blockedVertices);
            if (intersection.isEmpty()) {
                // Can remove this from candidates
                edgeIterator.remove();
            }
        }
        System.out.println("After pruning by no-problem edges (" + pipelinedEdges.size() + "): " + pipelinedEdges);
        Set<Set<DualEdge>> mustBreakPaths = getMinimalProblematicPaths(dag).stream().map(p -> new HashSet<DualEdge>(p.getEdgeList())).collect(Collectors.toSet());
        Set<GraphPath<Integer, DualEdge>> chainPaths = getChainPaths(dag);
        System.out.println("Chains are: " + chainPaths);
        Set<Set<DualEdge>> chainPathSets = chainPaths.stream().map(p -> new HashSet<DualEdge>(p.getEdgeList())).collect(Collectors.toSet());
        Pair<Double, Set<DualEdge>> optimum = new Pair<Double, Set<DualEdge>>(Double.MAX_VALUE, new HashSet<DualEdge>());
        Set<DualEdge> currentState;
        LinkedList<Set<DualEdge>> bfsQueue = new LinkedList<Set<DualEdge>>();
        bfsQueue.add(pipelinedEdges);
        HashSet<Set<DualEdge>> visited = new HashSet<Set<DualEdge>>();
        while (!bfsQueue.isEmpty()) {
            currentState = bfsQueue.poll();
            if (!visited.contains(currentState)) {
                visited.add(currentState);
                // TODO: Test validity
                if (testStateValidity(dag, currentState)) {
                    // TODO: If valid, get cost
                    Double cost = getCost(currentState);
                    if (cost < optimum.getKey()) {
                        optimum = new Pair<Double, Set<DualEdge>>(cost, currentState);
                    }
                }
                boolean prunedByChains = false;
                for (Set<DualEdge> cp : chainPathSets) {
                    Set<DualEdge> cpIntersectState = new HashSet<DualEdge>(cp);
                    cpIntersectState.retainAll(currentState);
                    if (cpIntersectState.size() > 1) {
                        prunedByChains = true; // This state is useless, explore next ones.
                        // Jump straight to 1-edge cuts and 0-edge cuts on this chain
                        Set<DualEdge> noEdgeState = new HashSet<DualEdge>(currentState);
                        noEdgeState.removeAll(cpIntersectState);
                        if (mustBreakPaths.stream().noneMatch(ps -> {
                            Set<DualEdge> temp = new HashSet<DualEdge>(ps);
                            temp.retainAll(noEdgeState);
                            return temp.isEmpty();
                        }) && !visited.contains(noEdgeState))
                            bfsQueue.add(noEdgeState);
                        for (DualEdge e : cpIntersectState) {
                            Set<DualEdge> oneEdgeState = new HashSet<DualEdge>(noEdgeState);
                            oneEdgeState.add(e);
                            if (mustBreakPaths.stream().noneMatch(ps -> {
                                Set<DualEdge> temp = new HashSet<DualEdge>(ps);
                                temp.retainAll(oneEdgeState);
                                return temp.isEmpty();
                            }) && !visited.contains(oneEdgeState))
                                bfsQueue.add(oneEdgeState);
                        }
                    }
                }
                if (!prunedByChains) for (DualEdge e : currentState) {
                    Set<DualEdge> newState = new HashSet<DualEdge>(currentState);
                    newState.remove(e);
                    // Test if the new state does not satisfy the must-break paths condition
                    if (mustBreakPaths.stream().noneMatch(ps -> {
                        Set<DualEdge> temp = new HashSet<DualEdge>(ps);
                        temp.retainAll(newState);
                        return temp.isEmpty();
                    }) && !visited.contains(newState))
                        bfsQueue.add(newState);
                }
            }
        }
        System.out.println("Search-based algorithm explored " + visited.size() + " states, prune ratio: " + spaceSize.subtract(BigDecimal.valueOf(visited.size())).divide(spaceSize).multiply(BigDecimal.valueOf(100)) + "%.");
        return optimum;
    }

    static Set<GraphPath<Integer, DualEdge>> getChainPaths(DirectedAcyclicGraph<Integer, DualEdge> dag) {
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

    static Set<Integer> getBlockedVertices(DirectedAcyclicGraph<Integer, DualEdge> dag) {
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

    static Double getCost(Set<DualEdge> currentState) {
        return currentState.stream().map(DualEdge::getWeight).reduce(0.0, Double::sum);
    }

    static boolean testStateValidity(DirectedAcyclicGraph<Integer, DualEdge> dag, Set<DualEdge> currentState) {
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

    static Set<GraphPath<Integer, DualEdge>> getMinimalProblematicPaths(DirectedAcyclicGraph<Integer, DualEdge> dag) {
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

    static void reduceConflictPairs(DirectedAcyclicGraph<Integer, DualEdge> dag, Pair<Integer, Integer> vp1, Pair<Set<GraphPath<Integer, DualEdge>>, Set<GraphPath<Integer, DualEdge>>> cp1, Pair<Integer, Integer> vp2, Pair<Set<GraphPath<Integer, DualEdge>>, Set<GraphPath<Integer, DualEdge>>> cp2) {
        if (Objects.equals(vp1.getKey(), vp2.getKey()) || dag.getDescendants(vp1.getKey()).contains(vp2.getKey())) {
            if (Objects.equals(vp1.getValue(), vp2.getValue()) || dag.getDescendants(vp2.getValue()).contains(vp1.getValue())) {
                // If all the paths are contained
                reducePathSet(cp1.getKey(), cp2.getKey());
                reducePathSet(cp1.getValue(), cp2.getValue());
            }
        }
    }

    static void reducePathSet(Set<GraphPath<Integer, DualEdge>> pathSet1, Set<GraphPath<Integer, DualEdge>> pathSet2) {
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