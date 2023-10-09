package OSPD;

import DualEdgeDAG.DualEdge;
import OSPD.LogicalDAG.LogicalDAG;
import OSPD.PhysicalDAG.PhysicalDAG;
import com.google.common.collect.Sets;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class OSPDSearcher {
    private final LogicalDAG inputLogicalDAG;
    private final BigDecimal searchSpaceSize;
    private final LinkedList<PhysicalDAG> searchQueue = new LinkedList<>();
    private final HashSet<PhysicalDAG> visitedSet = new HashSet<>();
    private PhysicalDAG seedState;
    private PhysicalDAG OSPD;

    private boolean pruneByChains = false;

    private boolean pruneBySafeEdges = false;

    private boolean pruneByUnsalvageableStates = false;

    public OSPDSearcher(LogicalDAG inputLogicalDAG) {
        this.inputLogicalDAG = inputLogicalDAG;
        this.seedState = new PhysicalDAG(inputLogicalDAG, inputLogicalDAG.getDualDAG().edgeSet());
        this.searchSpaceSize = BigDecimal.valueOf((1L << (this.inputLogicalDAG.getDualDAG().edgeSet().size() - this.inputLogicalDAG.getBlockingEdges().size())));
    }

    public PhysicalDAG execute() {
        PhysicalDAG allNonBlockingPipelinedState = new PhysicalDAG(inputLogicalDAG, inputLogicalDAG.getBlockingEdges());
        if (allNonBlockingPipelinedState.showSchedulability()) {
            this.OSPD = allNonBlockingPipelinedState;
            System.out.println(this.inputLogicalDAG + " is natively schedulable. Search skipped.");
        } else {
            executeSearch();
        }
        return this.OSPD;
    }

    public void executeSearch() {
        System.out.println("Starting search for logical DAG: " + this.inputLogicalDAG);
        System.out.println("Complete search-space size: " + this.searchSpaceSize);
        if (this.pruneBySafeEdges) {
            System.out.println("Using rule 2: prune by safe edges.");
            Set<DualEdge> modifiedSeedStateMaterializedEdges = this.seedState.getMatLogicalEdges();
            modifiedSeedStateMaterializedEdges.removeAll(this.inputLogicalDAG.getSafeEdges());
            this.seedState = new PhysicalDAG(this.inputLogicalDAG, modifiedSeedStateMaterializedEdges);
        }

        this.OSPD = this.seedState;
        this.searchQueue.clear();
        this.visitedSet.clear();
        if (this.pruneByChains) {
            System.out.println("Using rule 1: prune by chains.");
            System.out.println("Chains are: " + OSPDUtils.getChainPaths(inputLogicalDAG.getDualDAG()));
            Set<DualEdge> modifiedSeedStateMatEdges = new HashSet<>(this.seedState.getMatLogicalEdges());
            List<Set<DualEdge>> zeroBlockingChains = new LinkedList<>();
            this.inputLogicalDAG.getChains().forEach(chain -> {
                List<DualEdge> chainEdgeList = chain.getEdgeList();
                if (chainEdgeList.stream().anyMatch(this.inputLogicalDAG::isBlockingEdge)) {
                    // A chain with at least one blocking edge does not need materialization on any non-blocking edges.
                    chainEdgeList.stream().filter(e -> !this.inputLogicalDAG.isBlockingEdge(e)).forEach(modifiedSeedStateMatEdges::remove);
                } else {
                    // A chain with no blocking edge needs at most one materialization on a non-blocking edge.
                    zeroBlockingChains.add(new HashSet<>(chainEdgeList));
                    // Remove this chain from the materialized edges first.
                    chainEdgeList.forEach(modifiedSeedStateMatEdges::remove);
                }
            });
            Set<List<DualEdge>> combinations = Sets.cartesianProduct(zeroBlockingChains);
            combinations.forEach(c -> {
                Set<DualEdge> newStateMatEdges = new HashSet<>(modifiedSeedStateMatEdges);
                newStateMatEdges.addAll(c);
                PhysicalDAG newState = new PhysicalDAG(this.inputLogicalDAG, newStateMatEdges);
                System.out.println("Combination: " + c + ", state: " + newState);
                this.searchQueue.add(newState);
                this.visitedSet.add(newState);
            });
        } else {
            this.searchQueue.add(seedState);
            this.visitedSet.add(seedState);
        }
        while (!searchQueue.isEmpty()) {
            PhysicalDAG currentState = searchQueue.poll();
            if (currentState.checkSchedulability()) {
                if (currentState.getCost() < this.OSPD.getCost()) {
                    this.OSPD = currentState;
                }
            }
            currentState.getMatLogicalEdges().forEach(lEdge -> {
                if (!this.inputLogicalDAG.isBlockingEdge(lEdge)) {
                    Set<DualEdge> neighborStateMaterializedEdges = new HashSet<>(currentState.getMatLogicalEdges());
                    neighborStateMaterializedEdges.remove(lEdge);
                    PhysicalDAG neighborState = new PhysicalDAG(currentState.getLogicalDAG(), neighborStateMaterializedEdges);
                    if (!visitedSet.contains(neighborState)) {
                        searchQueue.add(neighborState);
                        visitedSet.add(neighborState);
                        if (visitedSet.size() % 10000 == 0) {
                            System.out.println(visitedSet.size() + " states visited.");
                        }
                    }
                }
            });
        }
        System.out.println("Search completed, number of states visited: " + visitedSet.size());
        System.out.println("OSPD: " + this.OSPD);
        this.OSPD.showSchedulability();
    }

    public void setPruneByChains(boolean pruneByChains) {
        this.pruneByChains = pruneByChains;
    }

    public void setPruneBySafeEdges(boolean pruneBySafeEdges) {
        this.pruneBySafeEdges = pruneBySafeEdges;
    }

    public void setPruneByUnsalvageableStates(boolean pruneByUnsalvageableStates) {
        this.pruneByUnsalvageableStates = pruneByUnsalvageableStates;
    }
}
