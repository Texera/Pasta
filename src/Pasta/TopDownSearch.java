package Pasta;

import DualEdgeDAG.DualEdge;
import Pasta.ExecutionPlan.ExecutionPlan;
import Pasta.PhysicalPlan.PhysicalPlan;
import com.google.common.collect.Sets;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.stream.Collectors;

public class TopDownSearch {
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

    private final HashSet<ExecutionPlan> hopelessStates = new HashSet<>();

    private final boolean edgeIndependentCost = true;
    private final boolean verbose;
    private ExecutionPlan seedState;
    private ExecutionPlan goalState;
    private boolean pruneByChains = false;
    private boolean pruneBySafeEdges = false;
    private boolean pruneByEarlyStopping = false;
    private boolean isGreedy = false;
    private boolean searchFinished = false;

    public TopDownSearch(PhysicalPlan inputPhysicalPlan, boolean verbose) {
        this.inputPhysicalPlan = inputPhysicalPlan;
        this.seedState = new ExecutionPlan(inputPhysicalPlan, inputPhysicalPlan.getDualDAG().edgeSet());
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
                System.out.println(this.inputPhysicalPlan + " is natively schedulable. TopDownSearch skipped.");
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
            if (this.pruneByEarlyStopping) System.out.println("Using optimization: prune by early stopping.");
            if (this.pruneByChains) System.out.println("Using optimization: prune by chains.");
            if (this.pruneBySafeEdges) System.out.println("Using optimization: prune by safe edges.");
        }

        if (this.pruneBySafeEdges) {
            Set<DualEdge> modifiedSeedStateMaterializedEdges = this.seedState.getMaterializedPhysicalPlanEdges();
            modifiedSeedStateMaterializedEdges.removeAll(this.inputPhysicalPlan.getSafeEdges());
            this.seedState = new ExecutionPlan(this.inputPhysicalPlan, modifiedSeedStateMaterializedEdges);
        }

        this.goalState = this.seedState;
        this.searchQueue.clear();
        this.visitedSet.clear();
        if (this.pruneByChains) {
            if (this.verbose) {
                System.out.println("Chains are: " + PastaUtils.getChainPaths(inputPhysicalPlan.getDualDAG()));
            }
            Set<DualEdge> modifiedSeedStateMatEdges = new HashSet<>(this.seedState.getMaterializedPhysicalPlanEdges());
            List<Set<DualEdge>> zeroBlockingChains = new LinkedList<>();
            this.inputPhysicalPlan.getMaximalChains().forEach(chain -> {
                List<DualEdge> chainEdgeList = chain.getEdgeList();
                if (chainEdgeList.stream().anyMatch(this.inputPhysicalPlan::isBlockingEdge)) {
                    // A chain with at least one blocking edge does not need materialization on any non-blocking edges.
                    chainEdgeList.stream().filter(e -> !this.inputPhysicalPlan.isBlockingEdge(e)).forEach(modifiedSeedStateMatEdges::remove);
                } else {
                    // A chain with no blocking edge needs at most one materialization on a non-blocking edge.
                    // Remove this chain from the materialized edges first.
                    chainEdgeList.forEach(modifiedSeedStateMatEdges::remove);
                    if (this.pruneBySafeEdges) {
                        List<DualEdge> nonSafeChangeEdgeList = new LinkedList<>(chainEdgeList);
                        nonSafeChangeEdgeList.removeAll(this.inputPhysicalPlan.getSafeEdges());
                        if (!nonSafeChangeEdgeList.isEmpty())
                            zeroBlockingChains.add(new HashSet<>(nonSafeChangeEdgeList));
                    } else {
                        zeroBlockingChains.add(new HashSet<>(chainEdgeList));
                    }
                }
            });
            if (edgeIndependentCost) {
                Set<DualEdge> newStateMatEdges = new HashSet<>(modifiedSeedStateMatEdges);
                zeroBlockingChains.forEach(c -> {
                    Optional<DualEdge> minEdge = c.stream().min(Comparator.comparingDouble(DualEdge::getWeight));
                    minEdge.ifPresent(minE -> {
                        if (!this.inputPhysicalPlan.getSafeEdges().contains(minE))
                            newStateMatEdges.add(minE);
                    });
                });
                ExecutionPlan newState = new ExecutionPlan(this.inputPhysicalPlan, newStateMatEdges);
                this.searchQueue.add(newState);
                this.visitedSet.add(newState);
                if (this.verbose) {
                    System.out.println("Assuming edges have independent cost, the seed state is: " + newState);
                }
            } else {
                Set<List<DualEdge>> combinations = Sets.cartesianProduct(zeroBlockingChains);
                combinations.forEach(c -> {
                    Set<DualEdge> newStateMatEdges = new HashSet<>(modifiedSeedStateMatEdges);
                    newStateMatEdges.addAll(c);
                    ExecutionPlan newState = new ExecutionPlan(this.inputPhysicalPlan, newStateMatEdges);
                    if (this.verbose) {
                        System.out.println("Combination: " + c + ", state: " + newState);
                    }
                    this.searchQueue.add(newState);
                    this.visitedSet.add(newState);
                });
            }
        } else {
            this.searchQueue.add(seedState);
            this.visitedSet.add(seedState);
        }

        if (this.verbose) {
            System.out.println("Seed is: " + this.searchQueue.peek());
        }

        while (!searchQueue.isEmpty()) {
            if (visitedSet.size() > 1E5) {
                {
                    System.out.println(visitedSet.size() + " states visited, exceeds 100,000, search terminated early.");
                }
                break;
            }

            ExecutionPlan currentState = searchQueue.poll();
            if (currentState.checkSchedulability()) {
                if (currentState.getCost() < this.goalState.getCost()) {
                    this.goalState = currentState;
                }
            } else if (this.pruneByEarlyStopping) {
                if (this.inputPhysicalPlan.getMustMaterializeAtLeastOneEdgeSets().stream().anyMatch(edgeSet -> edgeSet.stream().noneMatch(e -> currentState.getMaterializedPhysicalPlanEdges().contains(e)))) {
                    // All its neighbors are hopeless
                    this.hopelessStates.add(currentState);
                    currentState.getMaterializedPhysicalPlanEdges().forEach(lEdge -> {
                        if (!this.inputPhysicalPlan.isBlockingEdge(lEdge)) {
                            Set<DualEdge> neighborStateMaterializedEdges = new HashSet<>(currentState.getMaterializedPhysicalPlanEdges());
                            neighborStateMaterializedEdges.remove(lEdge);
                            ExecutionPlan neighborState = new ExecutionPlan(currentState.getPhysicalPlan(), neighborStateMaterializedEdges);
                            this.hopelessStates.add(neighborState);
                        }
                    });
                    continue;
                }
            }

            Set<ExecutionPlan> unvisitedNeighborStates = currentState.getMaterializedPhysicalPlanEdges().stream()
                    .filter(lEdge -> !this.inputPhysicalPlan.isBlockingEdge(lEdge))
                    .map(lEdge -> {
                Set<DualEdge> neighborStateMaterializedEdges =
                        new HashSet<>(currentState.getMaterializedPhysicalPlanEdges());
                neighborStateMaterializedEdges.remove(lEdge);
                return new ExecutionPlan(currentState.getPhysicalPlan(), neighborStateMaterializedEdges);
            })
                    .filter(neighborState -> !visitedSet.contains(neighborState)).collect(Collectors.toSet());

            if (this.pruneByEarlyStopping) {
                unvisitedNeighborStates = unvisitedNeighborStates.stream()
                        .filter(neighborState ->
                                this.hopelessStates.stream().noneMatch(
                                        ancestorState ->
                                                ancestorState.getMaterializedPhysicalPlanEdges()
                                                        .containsAll(neighborState.getMaterializedPhysicalPlanEdges())
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

    public void setPruneByChains(boolean pruneByChains) {
        this.pruneByChains = pruneByChains;
    }

    public void setPruneBySafeEdges(boolean pruneBySafeEdges) {
        this.pruneBySafeEdges = pruneBySafeEdges;
    }

    public void setPruneByEarlyStopping(boolean pruneByEarlyStopping) {
        this.pruneByEarlyStopping = pruneByEarlyStopping;
    }

    public void setGreedy(boolean greedy) {
        isGreedy = greedy;
    }
}
