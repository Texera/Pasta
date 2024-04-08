package Pasta.PhysicalPlan;

import DualEdgeDAG.AbstractPhysicalPlanDAGImageRender;
import DualEdgeDAG.DualDAGImageRenderer;
import DualEdgeDAG.DualEdge;
import Pasta.PastaUtils;
import javafx.util.Pair;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.cycle.PatonCycleBase;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.AsUndirectedGraph;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.*;
import java.util.stream.Collectors;

public class PhysicalPlan {
    private final DirectedAcyclicGraph<Integer, DualEdge> dualDAG;
    private final Set<DualEdge> blockingEdges;

    private final Set<DualEdge> nonBlockingEdges;
    private final Set<GraphPath<Integer, DualEdge>> maximalChains;

    private final Set<Set<DualEdge>> maximalChainSets;
    private final AsUndirectedGraph<Integer, DualEdge> undirectedDualDAG;

    private final Set<List<DualEdge>> undirectedCycleBases;

    private final Set<Set<DualEdge>> allUndirectedCyclesWithBlockingEdgesEdgeSet;

    private final Set<DualEdge> safeEdges;

    private final Map<DualEdge, Set<Set<DualEdge>>> oppositeUndirectedCycleTraversalNBEdges;

    private final Set<Set<DualEdge>> mustMaterializeAtLeastOneEdgeSets;
    private final Map<Pair<Object, Object>, Double> materializationC2Costs = new HashMap<>();
    private final Map<Pair<Object, Object>, Double> pipeliningC2Costs = new HashMap<>();
    private boolean allUndirectedCyclesEnumerated = false;

    public PhysicalPlan(DirectedAcyclicGraph<Integer, DualEdge> dualDAG) {
        this.dualDAG = dualDAG;
        this.blockingEdges = this.dualDAG.edgeSet().stream().filter(DualEdge::isBlkOrMat).collect(Collectors.toSet());
        this.nonBlockingEdges = this.dualDAG.edgeSet().stream().filter(e -> !e.isBlkOrMat()).collect(Collectors.toSet());
        this.maximalChains = PastaUtils.getChainPaths(this.dualDAG);
        this.maximalChainSets = this.maximalChains.stream().map(chain-> new HashSet<>(chain.getEdgeList())).collect(Collectors.toSet());
        this.undirectedDualDAG = new AsUndirectedGraph<>(dualDAG);
        this.undirectedCycleBases = findUndirectedCycleBases();
//        System.out.println("Undirected cycle bases are:" + undirectedCycleBases);
        this.allUndirectedCyclesWithBlockingEdgesEdgeSet = this.findAllSimpleCyclesWithBlockingEdges();
//        System.out.println("All undirected cycles are: " + this.allUndirectedCyclesWithBlockingEdgesEdgeSet);
        this.safeEdges = allUndirectedCyclesEnumerated ? this.findSafeEdgesViaAllCycles() : this.findSafeEdgesViaMerging();
//        System.out.println("Safe edges are: " + safeEdges);
        this.oppositeUndirectedCycleTraversalNBEdges = this.findOppositeUCycleTraversalNBEdges();
//        System.out.println("Opposite undirected cycle traversal non-blocking edges: " + this.oppositeUndirectedCycleTraversalNBEdges);
        this.mustMaterializeAtLeastOneEdgeSets = this.oppositeUndirectedCycleTraversalNBEdges.values().parallelStream().flatMap(Set::stream).collect(Collectors.toSet());
//        System.out.println("Must have at least one materialization in each of the following sets of non-blocking edges: " + this.mustMaterializeAtLeastOneEdgeSets);
        this.generateC2Costs();
    }

    private static LinkedList<DualEdge> getCycleTraversalFromCycleEdgeSet(Set<DualEdge> undirectedCycle) {
        LinkedList<DualEdge> cycleTraversal = new LinkedList<>();
        Set<DualEdge> edgesInCycle = new HashSet<>(undirectedCycle);
        DualEdge currentEdge = edgesInCycle.iterator().next();
        while (!edgesInCycle.isEmpty()) {
            cycleTraversal.add(currentEdge);
            edgesInCycle.remove(currentEdge);
            if (!edgesInCycle.isEmpty()) {
                DualEdge finalCurrentEdge = currentEdge;
                Set<DualEdge> overlappingEdges = edgesInCycle.stream().filter(
                        edge -> edge.getSource().equals(finalCurrentEdge.getSource())
                                || edge.getSource().equals(finalCurrentEdge.getTarget())
                                || edge.getTarget().equals(finalCurrentEdge.getTarget())
                                || edge.getTarget().equals(finalCurrentEdge.getSource())
                ).collect(Collectors.toSet());
                if (!overlappingEdges.isEmpty()) {
                    currentEdge = overlappingEdges.iterator().next();
                } else {
                    throw new UnsupportedOperationException("Original cycle set: " + undirectedCycle + ", current traversal: " + cycleTraversal + ", current edge: " + finalCurrentEdge + ", remaining edges: " + edgesInCycle);
                }
            }
        }
        return cycleTraversal;
    }

    public Map<Pair<Object, Object>, Double> getMaterializationC2Costs() {
        return materializationC2Costs;
    }

    public Map<Pair<Object, Object>, Double> getPipeliningC2Costs() {
        return pipeliningC2Costs;
    }

    private void generateC2Costs() {
        Random randomizer = new Random(0);
        this.getDualDAG().edgeSet().forEach(edge -> {
            this.materializationC2Costs.put(new Pair<>(edge.getSource(), edge.getTarget()), randomizer.nextDouble() * 100);
            this.pipeliningC2Costs.put(new Pair<>(edge.getSource(), edge.getTarget()), randomizer.nextDouble() * 100);
        });
    }

    public Set<GraphPath<Integer, DualEdge>> getMaximalChains() {
        return maximalChains;
    }

    public Set<Set<DualEdge>> getMaximalChainSets() {
        return this.maximalChainSets;
    }

    public Set<DualEdge> getBlockingEdges() {
        return blockingEdges;
    }

    public DirectedAcyclicGraph<Integer, DualEdge> getDualDAG() {
        return dualDAG;
    }

    public boolean isNonBlockingEdge(DualEdge edge) {
        return this.nonBlockingEdges.contains(edge);
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

    public void renderAbstractDAGToPath(String path) {
        String blockingEdgeColor = "strokeColor=#b22812";
        JGraphXAdapter<Integer, DualEdge> graphAdapter = AbstractPhysicalPlanDAGImageRender.getGraphAdapter(getDualDAG(), blockingEdgeColor);
        AbstractPhysicalPlanDAGImageRender.renderDAGToFile(path, graphAdapter);
    }

    public AsUndirectedGraph<Integer, DualEdge> getUndirectedDualDAG() {
        return undirectedDualDAG;
    }

    private Set<List<DualEdge>> findUndirectedCycleBases() {
        PatonCycleBase<Integer, DualEdge> cycleFinder = new PatonCycleBase<>(this.undirectedDualDAG);
        return cycleFinder.getCycleBasis().getCycles();
    }

    private Set<Set<DualEdge>> findAllSimpleCyclesWithBlockingEdges() {
        Set<Set<DualEdge>> cycleBases = new HashSet<>(this.undirectedCycleBases.stream().map(HashSet::new).collect(Collectors.toSet()));

        Set<Set<DualEdge>> allCycles = new HashSet<>(cycleBases);

        this.allUndirectedCyclesEnumerated = true;

        while (true) {
            Set<Set<DualEdge>> currentBase = new HashSet<>(allCycles);
            currentBase.forEach(i -> {
                currentBase.forEach(j -> {
                    if (i != j) {
                        Set<DualEdge> overlap = new HashSet<>(i);
                        overlap.retainAll(j);
                        if (!overlap.isEmpty()) {
                            Set<DualEdge> newCycle = new HashSet<>(i);
                            newCycle.addAll(j);
                            newCycle.removeAll(overlap);
                            if (!allCycles.contains(newCycle)) {
                                try {
                                    getCycleTraversalFromCycleEdgeSet(newCycle);
//                                    System.out.println("New cycle found: " + newCycle);
                                    allCycles.add(newCycle);
                                } catch (UnsupportedOperationException e) {
//                                    System.out.println("Not a cycle: " + newCycle);
                                }
                            }
                        }
                    }
                });
            });
            if (currentBase.equals(allCycles)) break;
//            System.out.println("Merged 1 round.");
            if (allCycles.size() > 1000) {
                System.out.println("Too many cycles. Pruning with limited cycle knowledge.");
                this.allUndirectedCyclesEnumerated = false;
                break;
            }
        }

        return allCycles.stream().filter(cycle -> cycle.stream().anyMatch(this::isBlockingEdge)).collect(Collectors.toSet());
    }

    private Set<DualEdge> findSafeEdgesViaMerging() {
        Set<Set<DualEdge>> mergedUCycleBaseSets = this.undirectedCycleBases.stream().map(HashSet::new).collect(Collectors.toSet());
        boolean merged = true;

        // Keep merging until no more merges are possible
        while (merged) {
            merged = false;

            for (Iterator<Set<DualEdge>> iterator1 = mergedUCycleBaseSets.iterator(); iterator1.hasNext(); ) {
                Set<DualEdge> set1 = iterator1.next();

                for (Iterator<Set<DualEdge>> iterator2 = mergedUCycleBaseSets.iterator(); iterator2.hasNext(); ) {
                    Set<DualEdge> set2 = iterator2.next();

                    if (set1 != set2 && !Collections.disjoint(set1, set2)) { // disjoint is the negation of overlapping
                        // Merge set2 into set1 and remove set2
                        set1.addAll(set2);
                        iterator2.remove();
                        merged = true;
                        break;
                    }
                }

                if (merged) break;
            }
        }
        System.out.println("Merged cycle sets: " + mergedUCycleBaseSets);

        Set<Set<DualEdge>> mergedUCycleBaseSetsWithBlockingEdges = mergedUCycleBaseSets.stream().filter(s -> s.stream().anyMatch(this::isBlockingEdge)).collect(Collectors.toSet());

        return this.nonBlockingEdges.stream().filter(e -> mergedUCycleBaseSetsWithBlockingEdges.stream().noneMatch(s -> s.contains(e))).collect(Collectors.toSet());
    }

    private Set<DualEdge> findSafeEdgesViaAllCycles() {
        return this.nonBlockingEdges.stream().filter(nbEdge -> this.allUndirectedCyclesWithBlockingEdgesEdgeSet.stream().noneMatch(cycle -> cycle.contains(nbEdge))).collect(Collectors.toSet());
    }

    private Map<DualEdge, Set<Set<DualEdge>>> findOppositeUCycleTraversalNBEdges() {
        Map<DualEdge, Set<Set<DualEdge>>> oppositeUCycleTraversalNBEdges = new HashMap<>();
        this.blockingEdges.forEach(bE -> oppositeUCycleTraversalNBEdges.put(bE, new HashSet<>()));
        this.allUndirectedCyclesWithBlockingEdgesEdgeSet.forEach(undirectedCycleEdgeSet -> {
            LinkedList<DualEdge> cycleTraversal = getCycleTraversalFromCycleEdgeSet(undirectedCycleEdgeSet);
            Map<DualEdge, Boolean> isForwardTraversalEdge = new HashMap<>();
            isForwardTraversalEdge.put(cycleTraversal.get(0), true);
            for (int i = 1; i < cycleTraversal.size(); i++) {
                DualEdge currentEdge = cycleTraversal.get(i);
                DualEdge previousEdge = cycleTraversal.get(i - 1);
                if (currentEdge.getSource().equals(previousEdge.getTarget()) || previousEdge.getSource().equals(currentEdge.getTarget())) {
                    isForwardTraversalEdge.put(currentEdge, isForwardTraversalEdge.get(previousEdge));
                } else {
                    isForwardTraversalEdge.put(currentEdge, !isForwardTraversalEdge.get(previousEdge));
                }
            }
            undirectedCycleEdgeSet.stream().filter(this::isBlockingEdge).forEach(bE -> {
                Set<DualEdge> oppositeTraversalEdges = undirectedCycleEdgeSet.stream().filter(e -> !isForwardTraversalEdge.get(e).equals(isForwardTraversalEdge.get(bE))).collect(Collectors.toSet());
                if (oppositeTraversalEdges.stream().noneMatch(this::isBlockingEdge)) {
                    // Only add those with all-non-blocking edges.
                    oppositeUCycleTraversalNBEdges.get(bE).add(oppositeTraversalEdges);
                }
//                System.out.println("For blocking edge " + bE + ", on Cycle " + cycleTraversal + ", " + oppositeTraversalEdges + " have the opposite direction.");
            });
        });
        return oppositeUCycleTraversalNBEdges;
    }

    public Set<List<DualEdge>> getUndirectedCycleBases() {
        return undirectedCycleBases;
    }

    public Set<DualEdge> getSafeEdges() {
        return safeEdges;
    }

    public Map<DualEdge, Set<Set<DualEdge>>> getOppositeUndirectedCycleTraversalNBEdges() {
        return oppositeUndirectedCycleTraversalNBEdges;
    }

    public Set<Set<DualEdge>> getMustMaterializeAtLeastOneEdgeSets() {
        return mustMaterializeAtLeastOneEdgeSets;
    }

    @Override
    public String toString() {
        return "Pasta.PhysicalPlan{" +
                "dualDAG=" + dualDAG +
                ", blockingEdges=" + blockingEdges +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhysicalPlan)) return false;
        PhysicalPlan that = (PhysicalPlan) o;
        return Objects.equals(getDualDAG(), that.getDualDAG());
    }
}
