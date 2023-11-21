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
    public static void runOSPDSearcher(DirectedAcyclicGraph<Integer, DualEdge> inputLogicalDAG, Path outputPath, boolean verbose) {

        if (!Files.exists(outputPath)) {
            try {
                Files.createDirectories(outputPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        long startTime = System.currentTimeMillis();
        LogicalDAG logicalDAG = new LogicalDAG(inputLogicalDAG);
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        System.out.println("Initializing the logical DAG took: " + elapsedTime + " ms");
        logicalDAG.renderDAGImageToPath(outputPath.resolve("input_logical_DAG.png").toString());

        System.out.println(System.lineSeparator());
        startTime = System.currentTimeMillis();
        OSPDSearcher greedySearcher = new OSPDSearcher(logicalDAG, verbose);
        greedySearcher.setPruneByChains(true);
        greedySearcher.setPruneBySafeEdges(true);
        greedySearcher.setPruneByUnsalvageableStates(true);
        greedySearcher.setGreedy(true);
        PhysicalDAG greedyOptimum = greedySearcher.execute();
        endTime = System.currentTimeMillis();
        elapsedTime = endTime - startTime;
        System.out.println("Using greedy took: " + elapsedTime + " ms");
        greedyOptimum.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_greedy.png").toString());

        System.out.println(System.lineSeparator());
        startTime = System.currentTimeMillis();
        OSPDSearcher ospdSearcherRule123 = new OSPDSearcher(logicalDAG, verbose);
        ospdSearcherRule123.setPruneByChains(true);
        ospdSearcherRule123.setPruneBySafeEdges(true);
        ospdSearcherRule123.setPruneByUnsalvageableStates(true);
        PhysicalDAG ospd123 = ospdSearcherRule123.execute();
        endTime = System.currentTimeMillis();
        elapsedTime = endTime - startTime;
        System.out.println("Using all three rules took: " + elapsedTime + " ms");
        ospd123.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_1_2_3.png").toString());

        System.out.println(System.lineSeparator());
        startTime = System.currentTimeMillis();
        OSPDSearcher ospdSearcherRule23 = new OSPDSearcher(logicalDAG, verbose);
        ospdSearcherRule23.setPruneBySafeEdges(true);
        ospdSearcherRule23.setPruneByUnsalvageableStates(true);
        PhysicalDAG ospd23 = ospdSearcherRule23.execute();
        endTime = System.currentTimeMillis();
        elapsedTime = endTime - startTime;
        System.out.println("Using rule 2 and 3 took: " + elapsedTime + " ms");
        ospd23.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_2_3.png").toString());

        System.out.println(System.lineSeparator());
        startTime = System.currentTimeMillis();
        OSPDSearcher ospdSearcherRule13 = new OSPDSearcher(logicalDAG, verbose);
        ospdSearcherRule13.setPruneByChains(true);
        ospdSearcherRule13.setPruneByUnsalvageableStates(true);
        PhysicalDAG ospd13 = ospdSearcherRule13.execute();
        endTime = System.currentTimeMillis();
        elapsedTime = endTime - startTime;
        System.out.println("Using rule 1 and 3 took: " + elapsedTime + " ms");
        ospd23.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_1_3.png").toString());

        startTime = System.currentTimeMillis();
        OSPDSearcher ospdSearcherRule12 = new OSPDSearcher(logicalDAG, verbose);
        ospdSearcherRule12.setPruneByChains(true);
        ospdSearcherRule12.setPruneBySafeEdges(true);
        PhysicalDAG ospd12 = ospdSearcherRule12.execute();
        endTime = System.currentTimeMillis();
        elapsedTime = endTime - startTime;
        System.out.println("Using rule 1 and 2 took: " + elapsedTime + " ms");
        ospd12.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_1_2.png").toString());

        System.out.println(System.lineSeparator());
        startTime = System.currentTimeMillis();
        OSPDSearcher ospdSearcherRule2 = new OSPDSearcher(logicalDAG, verbose);
        ospdSearcherRule2.setPruneBySafeEdges(true);
        PhysicalDAG ospd2 = ospdSearcherRule2.execute();
        endTime = System.currentTimeMillis();
        elapsedTime = endTime - startTime;
        System.out.println("Using rule 2 took: " + elapsedTime + " ms");
        ospd2.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_2.png").toString());

        System.out.println(System.lineSeparator());
        startTime = System.currentTimeMillis();
        OSPDSearcher ospdSearcherRule1 = new OSPDSearcher(logicalDAG, verbose);
        ospdSearcherRule1.setPruneByChains(true);
        PhysicalDAG ospd1 = ospdSearcherRule1.execute();
        endTime = System.currentTimeMillis();
        elapsedTime = endTime - startTime;
        System.out.println("Using rule 1 took: " + elapsedTime + " ms");
        ospd1.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_1.png").toString());

        System.out.println(System.lineSeparator());
        startTime = System.currentTimeMillis();
        OSPDSearcher ospdSearcherRule3 = new OSPDSearcher(logicalDAG, verbose);
        ospdSearcherRule3.setPruneByUnsalvageableStates(true);
        PhysicalDAG ospd3 = ospdSearcherRule3.execute();
        endTime = System.currentTimeMillis();
        elapsedTime = endTime - startTime;
        System.out.println("Using rule 3 took: " + elapsedTime + " ms");
        ospd1.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_3.png").toString());

        System.out.println(System.lineSeparator());
        startTime = System.currentTimeMillis();
        OSPDSearcher ospdSearcherBaseline = new OSPDSearcher(logicalDAG, verbose);
        PhysicalDAG ospdOfBaseline = ospdSearcherBaseline.execute();
        endTime = System.currentTimeMillis();
        elapsedTime = endTime - startTime;
        System.out.println("Baseline search took: " + elapsedTime + " ms");
        ospdOfBaseline.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_baseline.png").toString());
    }
}
