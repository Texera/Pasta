import DualEdgeDAG.DualDAGRepeatableGenerator;
import DualEdgeDAG.DualEdge;
import OSPD.LogicalDAG.LogicalDAG;
import OSPD.OSPDSearcher;
import OSPD.PhysicalDAG.PhysicalDAG;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.util.SupplierUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws IOException {
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
        if (!Files.exists(outputPath)) {
            try {
                Files.createDirectories(outputPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        LogicalDAG logicalDAG = new LogicalDAG(randomDAGWithCost);
        logicalDAG.renderDAGImageToPath(outputPath.resolve("input_logical_DAG.png").toString());
        OSPDSearcher ospdSearcherBaseline = new OSPDSearcher(logicalDAG);
        PhysicalDAG ospdOfBaseline = ospdSearcherBaseline.execute();
        ospdOfBaseline.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG.png").toString());
        OSPDSearcher ospdSearcherRule1 = new OSPDSearcher(logicalDAG);
        ospdSearcherRule1.setPruneByChains(true);
        PhysicalDAG ospd1 = ospdSearcherRule1.execute();
        ospd1.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_1.png").toString());

        OSPDSearcher ospdSearcherRule12 = new OSPDSearcher(logicalDAG);
        ospdSearcherRule12.setPruneByChains(true);
        ospdSearcherRule12.setPruneBySafeEdges(true);
        PhysicalDAG ospd12 = ospdSearcherRule12.execute();
        ospd12.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_1_2.png").toString());
    }
}