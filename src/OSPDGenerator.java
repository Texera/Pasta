import DualEdgeDAG.DualEdge;
import com.google.common.collect.Sets;
import javafx.util.Pair;
import org.jgrapht.GraphPath;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class OSPDGenerator {
    static Pair<Double, Set<DualEdge>> generateOSPD(DirectedAcyclicGraph<Integer, DualEdge> dag) {
        Set<GraphPath<Integer, DualEdge>> candidatePaths = OSPDUtils.getMinimalProblematicPaths(dag);
        List<Set<DualEdge>> edgeSets = candidatePaths.stream().map(p -> new HashSet<DualEdge>(p.getEdgeList())).collect(Collectors.toList());
        Set<List<DualEdge>> cp = Sets.cartesianProduct(edgeSets);
        Set<Set<DualEdge>> candidates = cp.stream().map(HashSet::new).collect(Collectors.toSet());
        System.out.println("Generation based algorithm had " + candidates.size() + " candidates: " + candidates);
        Pair<Double, Set<DualEdge>> optimum = new Pair<Double, Set<DualEdge>>(Double.MAX_VALUE, new HashSet<DualEdge>());
        Set<Set<DualEdge>> invalidSolutions = new HashSet<Set<DualEdge>>();
        for (Set<DualEdge> candidate : candidates) {
            if (OSPDUtils.testStateValidity(dag, candidate)) {
                Double cost = OSPDUtils.getCost(candidate);
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
}