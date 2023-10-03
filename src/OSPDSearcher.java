import DualEdgeDAG.DualEdge;
import javafx.util.Pair;
import org.jgrapht.GraphPath;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Collectors;

public class OSPDSearcher {
    static Pair<Double, Set<DualEdge>> searchBasedMinimalConversion(DirectedAcyclicGraph<Integer, DualEdge> dag) {
        Set<DualEdge> pipelinedEdges = dag.edgeSet().stream().filter(e -> !e.isBlkOrMat()).collect(Collectors.toSet());
        BigDecimal spaceSize = BigDecimal.valueOf((1L << pipelinedEdges.size()));
        System.out.println("All Pipelined Edges (" + pipelinedEdges.size() + ", " + spaceSize + " states in the search space)" + "): " + pipelinedEdges);
        Set<Integer> blockedVertices = OSPDUtils.getBlockedVertices(dag);
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
        Set<Set<DualEdge>> mustBreakPaths = OSPDUtils.getMinimalProblematicPaths(dag).stream().map(p -> new HashSet<DualEdge>(p.getEdgeList())).collect(Collectors.toSet());
        Set<GraphPath<Integer, DualEdge>> chainPaths = OSPDUtils.getChainPaths(dag);
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
                if (OSPDUtils.testStateValidity(dag, currentState)) {
                    // TODO: If valid, get cost
                    Double cost = OSPDUtils.getCost(currentState);
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
}