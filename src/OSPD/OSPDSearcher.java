package OSPD;

import DualEdgeDAG.DualEdge;
import OSPD.LogicalDAG.LogicalDAG;
import OSPD.PhysicalDAG.PhysicalDAG;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class OSPDSearcher {
    private final LogicalDAG inputLogicalDAG;

    private final PhysicalDAG seedState;
    private final BigDecimal searchSpaceSize;
    private final LinkedList<PhysicalDAG> searchQueue = new LinkedList<>();
    private final HashSet<PhysicalDAG> visitedSet = new HashSet<>();
    private PhysicalDAG OSPD;

    private boolean pruneByChains = false;

    private boolean pruneBySafeEdges = false;

    private boolean pruneByUnsalvageableStates = false;

    public OSPDSearcher(LogicalDAG inputLogicalDAG) {
        this.inputLogicalDAG = inputLogicalDAG;
        this.seedState = new PhysicalDAG(inputLogicalDAG, inputLogicalDAG.getDualDAG().edgeSet());
        this.searchSpaceSize = BigDecimal.valueOf((1L << (this.inputLogicalDAG.getDualDAG().edgeSet().size() - this.inputLogicalDAG.getBlockingEdges().size())));
        System.out.println("Chains: " + OSPDUtils.getChainPaths(inputLogicalDAG.getDualDAG()));
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
        System.out.println("Search-space size: " + this.searchSpaceSize);
        this.OSPD = this.seedState;
        this.searchQueue.clear();
        this.visitedSet.clear();
        this.searchQueue.add(seedState);
        this.visitedSet.add(seedState);
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
