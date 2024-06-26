package Pasta;

import DualEdgeDAG.DualEdge;
import Pasta.ExecutionPlan.ExecutionPlan;
import Pasta.PhysicalPlan.PhysicalPlan;
import com.google.common.collect.Sets;
import org.jgrapht.GraphPath;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.stream.Collectors;

public class BottomUpSearch {
    private final PhysicalPlan inputPhysicalPlan;
    private final BigInteger searchSpaceSize;

    public LinkedList<ExecutionPlan> getSearchQueue() {
        return searchQueue;
    }

    private final LinkedList<ExecutionPlan> searchQueue = new LinkedList<>();

    public HashSet<ExecutionPlan> getVisitedSet() {
        return visitedSet;
    }

    private final HashSet<ExecutionPlan> visitedSet = new HashSet<>();
    private final HashSet<ExecutionPlan> schedulableStates = new HashSet<>();

    private final boolean edgeIndependentCost = true;
    private final boolean verbose;
    private ExecutionPlan seedState;
    private ExecutionPlan goalState;
    private boolean pruneByChains = false;
    private boolean pruneBySafeEdges = false;
    private boolean pruneByEarlyStopping = false;
    private boolean isGreedy = false;

    public BottomUpSearch(PhysicalPlan inputPhysicalPlan, boolean verbose) {
        this.inputPhysicalPlan = inputPhysicalPlan;
        this.seedState = new ExecutionPlan(inputPhysicalPlan, inputPhysicalPlan.getBlockingEdges()); // All-pipelined
        int numNBEdges = this.inputPhysicalPlan.getDualDAG().edgeSet().size() - this.inputPhysicalPlan.getBlockingEdges().size();
        this.searchSpaceSize = BigInteger.valueOf(2).pow(numNBEdges);
        this.verbose = verbose;
        if (this.verbose) {
            System.out.printf("Input physical plan DAG has %d edges, among which %d are blocking edges, and %d are non-blocking edges.%n",
                    this.inputPhysicalPlan.getDualDAG().edgeSet().size(),
                    this.inputPhysicalPlan.getBlockingEdges().size(),
                    numNBEdges);
        }
    }

    public ExecutionPlan execute() {
        ExecutionPlan allNonBlockingPipelinedState = new ExecutionPlan(inputPhysicalPlan, inputPhysicalPlan.getBlockingEdges());
        if (allNonBlockingPipelinedState.checkSchedulability()) {
            this.goalState = allNonBlockingPipelinedState;
            if (this.verbose)
                System.out.println(this.inputPhysicalPlan + " is natively schedulable. Bottom-up Search skipped.");
        } else {
            executeSearch();
        }
        return this.goalState;
    }

    public void executeSearch() {
        if (this.verbose) {
            System.out.println("Starting search for physical plan DAG: " + this.inputPhysicalPlan);
            System.out.printf("Complete search-space size: %s, i.e., %s.\n",
                    (new DecimalFormat("#,###")).format(this.searchSpaceSize),
                    (new DecimalFormat("0.###E0", DecimalFormatSymbols.getInstance(Locale.ROOT)))
                            .format(this.searchSpaceSize)
            );
            if (this.pruneByEarlyStopping) System.out.println("Using optimization: early stopping");
            if (this.pruneByChains) System.out.println("Using optimization: prune by chains.");
            if (this.pruneBySafeEdges) System.out.println("Using optimization: prune by safe edges.");
        }

        this.goalState = this.seedState;
        this.searchQueue.clear();
        this.visitedSet.clear();
        this.searchQueue.add(seedState);
        this.visitedSet.add(seedState);

        if (this.verbose) {
            System.out.println("Seed is: " + this.searchQueue.peek());
        }

        while (!searchQueue.isEmpty()) {
            if (visitedSet.size() > 50000) {
                {
                    System.out.println(visitedSet.size() + " states visited, exceeds 50,000, search terminated early.");
                }
                break;
            }

            ExecutionPlan currentState = searchQueue.poll();
            if (currentState.checkSchedulability()) {
                // Memoization for schedulable states
                if (this.pruneByEarlyStopping) {
                    this.schedulableStates.add(currentState);
                }
                if (currentState.getCost() < this.goalState.getCost()) {
                    this.goalState = currentState;
                }
                // Avoid expansion for a schedulable state.
                if (this.pruneByEarlyStopping) continue;
            }
            // Assuming pipelining an edge always reduces the cost, when a state is schedulable, no need to expand its neighbors.
            Set<DualEdge> candidateEdges = new HashSet<>(currentState.getPipelinedPhysicalPlanEdges());
            if (this.pruneByChains) {
                // Chains that already have materialized edges.
                Set<Set<DualEdge>> chainsWithMatEdges = this.inputPhysicalPlan.getMaximalChainSets().stream().filter(
                        chainSet -> chainSet.stream().anyMatch(edge -> currentState.getMaterializedPhysicalPlanEdges().contains(edge))
                ).collect(Collectors.toSet());

                Set<Set<DualEdge>> allPipelinedChains = new HashSet<>(this.inputPhysicalPlan.getMaximalChainSets());
                allPipelinedChains.removeAll(chainsWithMatEdges);

                // Exclude edges in the same chain as a materialized edge.
                candidateEdges.removeAll(chainsWithMatEdges.stream().flatMap(Set::stream).collect(Collectors.toSet()));

                // For all-pipelined chains, keep only one edge with the lowest cost.

                if (edgeIndependentCost) {
                    Set<Set<DualEdge>> bestEdgeRemovedChains = allPipelinedChains.stream().map(chain -> {
                        Set<DualEdge> bestEdgeRemovedChain = new HashSet<>(chain);
                        Optional<DualEdge> bestEdge = bestEdgeRemovedChain.stream().min(Comparator.comparingDouble(DualEdge::getWeight));
                        bestEdge.ifPresent(bestEdgeRemovedChain::remove);
                        return bestEdgeRemovedChain;
                    }).collect(Collectors.toSet());

                    candidateEdges.removeAll(bestEdgeRemovedChains.stream().flatMap(Set::stream).collect(Collectors.toSet()));
                }
            }

            if (this.pruneBySafeEdges) {
                candidateEdges.removeAll(this.inputPhysicalPlan.getSafeEdges());
            }

            Set<ExecutionPlan> unvisitedNeighborStates = candidateEdges.stream().map(lEdge -> {
                Set<DualEdge> neighborStateMaterializedEdges = new HashSet<>(currentState.getMaterializedPhysicalPlanEdges());
                neighborStateMaterializedEdges.add(lEdge);
                return new ExecutionPlan(currentState.getPhysicalPlan(), neighborStateMaterializedEdges);
            }).filter(neighborState -> !visitedSet.contains(neighborState)).collect(Collectors.toSet());

            if (this.pruneByEarlyStopping) {
                unvisitedNeighborStates = unvisitedNeighborStates.stream()
                        .filter(neighborState ->
                                this.schedulableStates.stream().noneMatch(
                                        ancestorState ->
                                                ancestorState.getPipelinedPhysicalPlanEdges()
                                                        .containsAll(neighborState.getPipelinedPhysicalPlanEdges())
                                )
                        ).collect(Collectors.toSet());
            }

            if (isGreedy) {
                Optional<ExecutionPlan> bestNeighbor = unvisitedNeighborStates.stream().min(Comparator.comparingDouble(ExecutionPlan::getCost));
                if (bestNeighbor.isPresent()) {
                    if (this.verbose) {
                        System.out.printf("Best neighbor of %s is %s\n", currentState, bestNeighbor);
                    }
                    searchQueue.add(bestNeighbor.get());
                    visitedSet.add(bestNeighbor.get());
                }
            } else {
                unvisitedNeighborStates.forEach(neighborState -> {
                    searchQueue.add(neighborState);
                    visitedSet.add(neighborState);
                    if (visitedSet.size() % 10000 == 0 && this.verbose) {
                        System.out.println(visitedSet.size() + " states visited.");
                    }
                });
            }
        }

        System.out.println("Number of states visited: " + visitedSet.size());
        System.out.println("Goal State: " + this.goalState);
        if (this.verbose) {
            this.goalState.showSchedulability();
        }
    }

    public void setPruneByEarlyStopping(boolean pruneByEarlyStopping) {
        this.pruneByEarlyStopping = pruneByEarlyStopping;
    }

    public void setPruneByChains(boolean pruneByChains) {
        this.pruneByChains = pruneByChains;
    }

    public void setPruneBySafeEdges(boolean pruneBySafeEdges) {
        this.pruneBySafeEdges = pruneBySafeEdges;
    }

    public void setGreedy(boolean greedy) {
        isGreedy = greedy;
    }
}
