package OSPD;

import DualEdgeDAG.DualEdge;
import OSPD.LogicalDAG.LogicalDAG;
import OSPD.PhysicalDAG.PhysicalDAG;
import com.google.common.collect.Sets;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class OSPDSearcher {
    private final LogicalDAG inputLogicalDAG;
    private final BigInteger searchSpaceSize;
    private final LinkedList<PhysicalDAG> searchQueue = new LinkedList<>();
    private final HashSet<PhysicalDAG> visitedSet = new HashSet<>();

    private final HashSet<PhysicalDAG> hopelessStates = new HashSet<>();

    private final boolean edgeIndependentCost = true;
    private final boolean verbose;
    private PhysicalDAG seedState;
    private PhysicalDAG OSPD;
    private boolean pruneByChains = false;
    private boolean pruneBySafeEdges = false;
    private boolean pruneByUnsalvageableStates = false;
    private boolean isGreedy = false;

    public OSPDSearcher(LogicalDAG inputLogicalDAG, boolean verbose) {
        this.inputLogicalDAG = inputLogicalDAG;
        this.seedState = new PhysicalDAG(inputLogicalDAG, inputLogicalDAG.getDualDAG().edgeSet());
        int numNBEdges = this.inputLogicalDAG.getDualDAG().edgeSet().size() - this.inputLogicalDAG.getBlockingEdges().size();
        this.searchSpaceSize = BigInteger.valueOf(2).pow(numNBEdges);
        this.verbose = verbose;
        if (this.verbose) {
            System.out.printf("Input logical DAG has %d edges, among which %d are blocking edges, and %d are non-blocking edges.%n",
                    this.inputLogicalDAG.getDualDAG().edgeSet().size(),
                    this.inputLogicalDAG.getBlockingEdges().size(),
                    numNBEdges);
        }
    }

    public PhysicalDAG execute() {
        PhysicalDAG allNonBlockingPipelinedState = new PhysicalDAG(inputLogicalDAG, inputLogicalDAG.getBlockingEdges());
        if (allNonBlockingPipelinedState.checkSchedulability()) {
            this.OSPD = allNonBlockingPipelinedState;
            if (this.verbose)
                System.out.println(this.inputLogicalDAG + " is natively schedulable. Search skipped.");
        } else {
            executeSearch();
        }
        return this.OSPD;
    }

    public void executeSearch() {
        if (this.verbose) {
            System.out.println("Starting search for logical DAG: " + this.inputLogicalDAG);
            System.out.printf("Complete search-space size: %s, i.e., %s.\n",
                    (new DecimalFormat("#,###")).format(this.searchSpaceSize),
                    (new DecimalFormat("0.###E0", DecimalFormatSymbols.getInstance(Locale.ROOT)))
                            .format(this.searchSpaceSize)
            );
            if (this.pruneByUnsalvageableStates) System.out.println("Using optimization 1: stop at hopeless states.");
            if (this.pruneByChains) System.out.println("Using optimization 2: prune by chains.");
            if (this.pruneBySafeEdges) System.out.println("Using optimization 3: prune by safe edges.");
        }

        if (this.pruneBySafeEdges) {
            Set<DualEdge> modifiedSeedStateMaterializedEdges = this.seedState.getMatLogicalEdges();
            modifiedSeedStateMaterializedEdges.removeAll(this.inputLogicalDAG.getSafeEdges());
            this.seedState = new PhysicalDAG(this.inputLogicalDAG, modifiedSeedStateMaterializedEdges);
        }

        this.OSPD = this.seedState;
        this.searchQueue.clear();
        this.visitedSet.clear();
        if (this.pruneByChains) {
            if (this.verbose) {
                System.out.println("Chains are: " + OSPDUtils.getChainPaths(inputLogicalDAG.getDualDAG()));
            }
            Set<DualEdge> modifiedSeedStateMatEdges = new HashSet<>(this.seedState.getMatLogicalEdges());
            List<Set<DualEdge>> zeroBlockingChains = new LinkedList<>();
            this.inputLogicalDAG.getChains().forEach(chain -> {
                List<DualEdge> chainEdgeList = chain.getEdgeList();
                if (chainEdgeList.stream().anyMatch(this.inputLogicalDAG::isBlockingEdge)) {
                    // A chain with at least one blocking edge does not need materialization on any non-blocking edges.
                    chainEdgeList.stream().filter(e -> !this.inputLogicalDAG.isBlockingEdge(e)).forEach(modifiedSeedStateMatEdges::remove);
                } else {
                    // A chain with no blocking edge needs at most one materialization on a non-blocking edge.
                    // Remove this chain from the materialized edges first.
                    chainEdgeList.forEach(modifiedSeedStateMatEdges::remove);
                    if (this.pruneBySafeEdges) {
                        List<DualEdge> nonSafeChangeEdgeList = new LinkedList<>(chainEdgeList);
                        nonSafeChangeEdgeList.removeAll(this.inputLogicalDAG.getSafeEdges());
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
                        if (!this.inputLogicalDAG.getSafeEdges().contains(minE))
                            newStateMatEdges.add(minE);
                    });
                });
                PhysicalDAG newState = new PhysicalDAG(this.inputLogicalDAG, newStateMatEdges);
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
                    PhysicalDAG newState = new PhysicalDAG(this.inputLogicalDAG, newStateMatEdges);
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
            if (visitedSet.size() > 1E7) {
                {
                    if (this.verbose) {
                        System.out.println(visitedSet.size() + " states visited, exceeds 100,000, search terminated early.");
                    }
                }
                break;
            }

            PhysicalDAG currentState = searchQueue.poll();
            if (currentState.checkSchedulability()) {
                if (currentState.getCost() < this.OSPD.getCost()) {
                    this.OSPD = currentState;
                }
            } else if (this.pruneByUnsalvageableStates) {
                if (this.inputLogicalDAG.getMustMaterializeAtLeastOneEdgeSets().stream().anyMatch(edgeSet -> edgeSet.stream().noneMatch(e -> currentState.getMatLogicalEdges().contains(e)))) {
                    // All its neighbors are hopeless
                    this.hopelessStates.add(currentState);
                    currentState.getMatLogicalEdges().forEach(lEdge -> {
                        if (!this.inputLogicalDAG.isBlockingEdge(lEdge)) {
                            Set<DualEdge> neighborStateMaterializedEdges = new HashSet<>(currentState.getMatLogicalEdges());
                            neighborStateMaterializedEdges.remove(lEdge);
                            PhysicalDAG neighborState = new PhysicalDAG(currentState.getLogicalDAG(), neighborStateMaterializedEdges);
                            this.hopelessStates.add(neighborState);
                        }
                    });
                    continue;
                }
            }

            List<PhysicalDAG> schedulableNeighbors = new LinkedList<>();
            currentState.getMatLogicalEdges().forEach(lEdge -> {
                if (!this.inputLogicalDAG.isBlockingEdge(lEdge)) {
                    Set<DualEdge> neighborStateMaterializedEdges = new HashSet<>(currentState.getMatLogicalEdges());
                    neighborStateMaterializedEdges.remove(lEdge);
                    PhysicalDAG neighborState = new PhysicalDAG(currentState.getLogicalDAG(), neighborStateMaterializedEdges);
                    if (!visitedSet.contains(neighborState)) {
                        if (this.isGreedy) {
                            if (neighborState.checkSchedulability()) {
                                schedulableNeighbors.add(neighborState);
                            }
                        } else {
                            if (!this.hopelessStates.contains(neighborState)) {
                                searchQueue.add(neighborState);
                                visitedSet.add(neighborState);
                                if (visitedSet.size() % 10000 == 0 && this.verbose) {
                                    System.out.println(visitedSet.size() + " states visited.");
                                }
                            }
                        }
                    }
                }
            });
            if (isGreedy) {
                Optional<PhysicalDAG> bestNeighbor = schedulableNeighbors.stream().max(Comparator.comparingDouble(PhysicalDAG::getCost));
                if (bestNeighbor.isPresent()) {
//                    if (this.verbose) {
//                        System.out.printf("Best neighbor of %s is %s\n", currentState, bestNeighbor);
//                    }
                    searchQueue.add(bestNeighbor.get());
                    visitedSet.add(bestNeighbor.get());
                }
            }
        }
        if (this.verbose) {
            System.out.println("Number of states visited: " + visitedSet.size());
            System.out.println("OSPD: " + this.OSPD);
            this.OSPD.showSchedulability();
        }
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

    public void setGreedy(boolean greedy) {
        isGreedy = greedy;
    }
}
