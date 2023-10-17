import DualEdgeDAG.DualDAGRepeatableGenerator;
import DualEdgeDAG.DualEdge;
import ExperimentRunner.ExperimentRunner;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.util.SupplierUtil;

import java.nio.file.Path;
import java.nio.file.Paths;

public class RunRandomDAGExperiment {
    public static void main(String[] args) {

        DirectedAcyclicGraph<Integer, DualEdge> randomDAGWithCost = new DirectedAcyclicGraph<>(
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
        graphGenerator.generateDualGraph(randomDAGWithCost);
        Path outputPath = Paths.get(String.format("/Users/xzliu/Desktop/Experiments/v%s_e%s_s%s_pB%s_fC_%s_pFC%s", numVertices, numEdges, seed, pBEdge, forceChain, pForceChain));

        ExperimentRunner.runOSPDSearcher(randomDAGWithCost, outputPath);
    }

}