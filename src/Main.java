import DualEdgeDAG.DualDAGImageRenderer;
import DualEdgeDAG.DualDAGRepeatableGenerator;
import DualEdgeDAG.DualEdge;
import PhysicalDAG.SchedulabilityChecker;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.util.SupplierUtil;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        DirectedAcyclicGraph<Integer, DualEdge> dag = new DirectedAcyclicGraph<>(
                SupplierUtil.createIntegerSupplier(), DualEdge::new, true);
        int numVertices = 0, numEdges = 0, seed = 0;
        double pBEdge = 0, pForceChain = 0;
        boolean forceChain = false;
        // Loop through the command line arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-numVertices":
                    numVertices = Integer.parseInt(args[++i]);
                    break;
                case "-numEdges":
                    numEdges = Integer.parseInt(args[++i]);
                    break;
                case "-seed":
                    seed = Integer.parseInt(args[++i]);
                    break;
                case "-pBEdge":
                    pBEdge = Double.parseDouble(args[++i]);
                    break;
                case "-forceChain":
                    forceChain = Boolean.parseBoolean(args[++i]);
                    break;
                case "-pForceChain":
                    pForceChain = Double.parseDouble(args[++i]);
                    break;
                default:
                    System.err.println("Unknown argument: " + args[i]);
                    System.exit(1);
            }
        }
        DualDAGRepeatableGenerator<Integer> graphGenerator = new DualDAGRepeatableGenerator<>(numVertices, numEdges, seed, pBEdge, forceChain, pForceChain);
        graphGenerator.generateDualGraph(dag);
        DualDAGImageRenderer.renderDualDAG(dag, numVertices, numEdges, seed, pBEdge, forceChain, pForceChain);

        boolean schedulable = SchedulabilityChecker.checkPhysicalDAGSchedulability(dag, false);
        System.out.println("Is the generated DAG schedulable?: " + schedulable);
        if (!schedulable) {
            // Search-based algorithm
            System.out.println("Result of search-based algorithm: " + OSPDSearcherOld.searchBasedMinimalConversion(dag));
            // Generation-based algorithm
            System.out.println("Result of generation-based algorithm: " + OSPDGenerator.generationBasedMinimalConversion(dag));
        }
    }
}