import DualEdgeDAG.DualEdge;
import LogicalDAG.LogicalDAG;
import PhysicalDAG.PhysicalDAG;

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

    public OSPDSearcher(LogicalDAG inputLogicalDAG) {
        this.inputLogicalDAG = inputLogicalDAG;
        this.seedState = new PhysicalDAG(inputLogicalDAG, inputLogicalDAG.getDualDAG().edgeSet());
        this.searchSpaceSize = BigDecimal.valueOf((1L << (this.inputLogicalDAG.getDualDAG().edgeSet().size() - this.inputLogicalDAG.getBlockingEdges().size())));
    }

    public void execute() {
        PhysicalDAG allNonBlockingPipelinedState = new PhysicalDAG(inputLogicalDAG, inputLogicalDAG.getBlockingEdges());
        if (allNonBlockingPipelinedState.showSchedulability()) {
            System.out.println(this.inputLogicalDAG + " is natively schedulable. Search skipped.");
        } else {
            executeSearch();
        }
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
            if (visitedSet.size() % 10000 == 0) {
                System.out.println(visitedSet.size() + " states visited.");
            }
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
                    }
                }
            });
        }
        System.out.println("Search completed, number of states visited: " + visitedSet.size());
        System.out.println("OSPD: " + this.OSPD);
        this.OSPD.showSchedulability();
    }
}
