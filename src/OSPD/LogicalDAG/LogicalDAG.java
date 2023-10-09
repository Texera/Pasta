package OSPD.LogicalDAG;

import DualEdgeDAG.DualDAGImageRenderer;
import DualEdgeDAG.DualEdge;
import OSPD.OSPDUtils;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.cycle.PatonCycleBase;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.AsUndirectedGraph;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.*;
import java.util.stream.Collectors;

public class LogicalDAG {
    private final DirectedAcyclicGraph<Integer, DualEdge> dualDAG;
    private final Set<DualEdge> blockingEdges;

    private final Set<DualEdge> nonBlockingEdges;
    private final Set<GraphPath<Integer, DualEdge>> chains;

    private final AsUndirectedGraph<Integer, DualEdge> uDualDAG;

    private final Set<List<DualEdge>> uCycleBases;

    private final Set<DualEdge> safeEdges;

    public LogicalDAG(DirectedAcyclicGraph<Integer, DualEdge> dualDAG) {
        this.dualDAG = dualDAG;
        this.blockingEdges = this.dualDAG.edgeSet().stream().filter(DualEdge::isBlkOrMat).collect(Collectors.toSet());
        this.nonBlockingEdges = this.dualDAG.edgeSet().stream().filter(e -> !e.isBlkOrMat()).collect(Collectors.toSet());
        this.chains = OSPDUtils.getChainPaths(this.dualDAG);
        this.uDualDAG = new AsUndirectedGraph<>(dualDAG);
        this.uCycleBases = findUCycleBases();
        System.out.println("Undirected cycle bases are:" + uCycleBases);
        this.safeEdges = this.findSafeEdges();
        System.out.println("Safe edges are: " + safeEdges);
    }

    public Set<GraphPath<Integer, DualEdge>> getChains() {
        return chains;
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

    public AsUndirectedGraph<Integer, DualEdge> getuDualDAG() {
        return uDualDAG;
    }

    private Set<List<DualEdge>> findUCycleBases() {
        PatonCycleBase<Integer, DualEdge> cycleFinder = new PatonCycleBase<>(this.uDualDAG);
        return cycleFinder.getCycleBasis().getCycles();
    }

    private Set<DualEdge> findSafeEdges() {
        Set<Set<DualEdge>> mergedUCycleBaseSets = this.uCycleBases.stream().map(HashSet::new).collect(Collectors.toSet());
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

    public Set<List<DualEdge>> getUCycleBases() {
        return uCycleBases;
    }

    public Set<DualEdge> getSafeEdges() {
        return safeEdges;
    }

    @Override
    public String toString() {
        return "OSPD.LogicalDAG{" +
                "dualDAG=" + dualDAG +
                ", blockingEdges=" + blockingEdges +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LogicalDAG)) return false;
        LogicalDAG that = (LogicalDAG) o;
        return Objects.equals(getDualDAG(), that.getDualDAG());
    }
}
