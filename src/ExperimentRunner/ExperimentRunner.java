package ExperimentRunner;

import DualEdgeDAG.DualEdge;
import OSPD.LogicalDAG.LogicalDAG;
import OSPD.OSPDSearcher;
import OSPD.PhysicalDAG.PhysicalDAG;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ExperimentRunner {
    public static void runOSPDSearcher(DirectedAcyclicGraph<Integer, DualEdge> inputLogicalDAG, Path outputPath) {

        if (!Files.exists(outputPath)) {
            try {
                Files.createDirectories(outputPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        LogicalDAG logicalDAG = new LogicalDAG(inputLogicalDAG);
        logicalDAG.renderDAGImageToPath(outputPath.resolve("input_logical_DAG.png").toString());

        OSPDSearcher ospdSearcherRule123 = new OSPDSearcher(logicalDAG);
        ospdSearcherRule123.setPruneByChains(true);
        ospdSearcherRule123.setPruneBySafeEdges(true);
        ospdSearcherRule123.setPruneByUnsalvageableStates(true);
        PhysicalDAG ospd123 = ospdSearcherRule123.execute();
        ospd123.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_1_2_3.png").toString());

        OSPDSearcher ospdSearcherRule23 = new OSPDSearcher(logicalDAG);
        ospdSearcherRule23.setPruneBySafeEdges(true);
        ospdSearcherRule23.setPruneByUnsalvageableStates(true);
        PhysicalDAG ospd23 = ospdSearcherRule23.execute();
        ospd23.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_2_3.png").toString());

        OSPDSearcher ospdSearcherRule12 = new OSPDSearcher(logicalDAG);
        ospdSearcherRule12.setPruneByChains(true);
        ospdSearcherRule12.setPruneBySafeEdges(true);
        PhysicalDAG ospd12 = ospdSearcherRule12.execute();
        ospd12.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_1_2.png").toString());

        OSPDSearcher ospdSearcherRule2 = new OSPDSearcher(logicalDAG);
        ospdSearcherRule2.setPruneBySafeEdges(true);
        PhysicalDAG ospd2 = ospdSearcherRule2.execute();
        ospd2.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_2.png").toString());

        OSPDSearcher ospdSearcherRule1 = new OSPDSearcher(logicalDAG);
        ospdSearcherRule1.setPruneByChains(true);
        PhysicalDAG ospd1 = ospdSearcherRule1.execute();
        ospd1.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_1.png").toString());

        OSPDSearcher ospdSearcherBaseline = new OSPDSearcher(logicalDAG);
        PhysicalDAG ospdOfBaseline = ospdSearcherBaseline.execute();
        ospdOfBaseline.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_baseline.png").toString());
    }
}
