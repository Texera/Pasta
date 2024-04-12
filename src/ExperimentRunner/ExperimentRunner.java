package ExperimentRunner;

import DualEdgeDAG.DualEdge;
import Pasta.BottomUpSearch;
import Pasta.ExecutionPlan.ExecutionPlan;
import Pasta.TopDownSearch;
import Pasta.PhysicalPlan.PhysicalPlan;
import org.checkerframework.common.subtyping.qual.Bottom;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ExperimentRunner {
    public static void runOptimalExecutionPlanFinder(DirectedAcyclicGraph<Integer, DualEdge> inputPhysicalPlan, Path outputPath, boolean verbose, boolean topDown, boolean onlyCheckShedulability) {
        if (!Files.exists(outputPath)) {
            try {
                Files.createDirectories(outputPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        long startTime = System.currentTimeMillis();
        PhysicalPlan physicalPlan = new PhysicalPlan(inputPhysicalPlan);
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        System.out.println("Initializing the physical plan took: " + elapsedTime + " ms");
        physicalPlan.renderDAGImageToPath(outputPath.resolve("input_physical_plan.png").toString());
        physicalPlan.renderAbstractDAGToPath(outputPath.resolve("abstract_input_physical_plan.png").toString());

        if (onlyCheckShedulability) {
            System.out.println(new ExecutionPlan(physicalPlan, physicalPlan.getBlockingEdges()).checkSchedulability());
            return;
        }

        if (topDown) {
            System.out.println(System.lineSeparator());
            startTime = System.currentTimeMillis();
            TopDownSearch greedySearcher = new TopDownSearch(physicalPlan, verbose);
            greedySearcher.setPruneByChains(true);
            greedySearcher.setPruneBySafeEdges(true);
            greedySearcher.setPruneByUnsalvageableStates(true);
            greedySearcher.setGreedy(true);
            ExecutionPlan greedyOptimum = greedySearcher.execute();
            endTime = System.currentTimeMillis();
            elapsedTime = endTime - startTime;
            System.out.println("Using greedy took: " + elapsedTime + " ms");
            greedyOptimum.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_greedy.png").toString());

            System.out.println(System.lineSeparator());
            startTime = System.currentTimeMillis();
            TopDownSearch topDownSearchRule123 = new TopDownSearch(physicalPlan, verbose);
            topDownSearchRule123.setPruneByChains(true);
            topDownSearchRule123.setPruneBySafeEdges(true);
            topDownSearchRule123.setPruneByUnsalvageableStates(true);
            ExecutionPlan oep123 = topDownSearchRule123.execute();
            endTime = System.currentTimeMillis();
            elapsedTime = endTime - startTime;
            System.out.println("Using all three rules took: " + elapsedTime + " ms");
            oep123.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_1_2_3.png").toString());

            System.out.println(System.lineSeparator());
            startTime = System.currentTimeMillis();
            TopDownSearch topDownSearchRule23 = new TopDownSearch(physicalPlan, verbose);
            topDownSearchRule23.setPruneBySafeEdges(true);
            topDownSearchRule23.setPruneByChains(true);
            ExecutionPlan oep23 = topDownSearchRule23.execute();
            endTime = System.currentTimeMillis();
            elapsedTime = endTime - startTime;
            System.out.println("Using rule 2 and 3 took: " + elapsedTime + " ms");
            oep23.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_2_3.png").toString());

            System.out.println(System.lineSeparator());
            startTime = System.currentTimeMillis();
            TopDownSearch topDownSearchRule13 = new TopDownSearch(physicalPlan, verbose);
            topDownSearchRule13.setPruneByUnsalvageableStates(true);
            topDownSearchRule13.setPruneBySafeEdges(true);
            ExecutionPlan oep13 = topDownSearchRule13.execute();
            endTime = System.currentTimeMillis();
            elapsedTime = endTime - startTime;
            System.out.println("Using rule 1 and 3 took: " + elapsedTime + " ms");
            oep23.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_1_3.png").toString());

            startTime = System.currentTimeMillis();
            TopDownSearch topDownSearchRule12 = new TopDownSearch(physicalPlan, verbose);
            topDownSearchRule12.setPruneByUnsalvageableStates(true);
            topDownSearchRule12.setPruneByChains(true);
            ExecutionPlan oep12 = topDownSearchRule12.execute();
            endTime = System.currentTimeMillis();
            elapsedTime = endTime - startTime;
            System.out.println("Using rule 1 and 2 took: " + elapsedTime + " ms");
            oep12.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_1_2.png").toString());

            System.out.println(System.lineSeparator());
            startTime = System.currentTimeMillis();
            TopDownSearch topDownSearchRule2 = new TopDownSearch(physicalPlan, verbose);
            topDownSearchRule2.setPruneByChains(true);
            ExecutionPlan oep2 = topDownSearchRule2.execute();
            endTime = System.currentTimeMillis();
            elapsedTime = endTime - startTime;
            System.out.println("Using rule 2 took: " + elapsedTime + " ms");
            oep2.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_2.png").toString());

            System.out.println(System.lineSeparator());
            startTime = System.currentTimeMillis();
            TopDownSearch topDownSearchRule3 = new TopDownSearch(physicalPlan, verbose);
            topDownSearchRule3.setPruneBySafeEdges(true);
            ExecutionPlan oep3 = topDownSearchRule3.execute();
            endTime = System.currentTimeMillis();
            elapsedTime = endTime - startTime;
            System.out.println("Using rule 3 took: " + elapsedTime + " ms");
            oep3.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_3.png").toString());

            System.out.println(System.lineSeparator());
            startTime = System.currentTimeMillis();
            TopDownSearch topDownSearchRule1 = new TopDownSearch(physicalPlan, verbose);
            topDownSearchRule1.setPruneByUnsalvageableStates(true);
            ExecutionPlan oep1 = topDownSearchRule1.execute();
            endTime = System.currentTimeMillis();
            elapsedTime = endTime - startTime;
            System.out.println("Using rule 1 took: " + elapsedTime + " ms");
            oep1.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_1.png").toString());

            System.out.println(System.lineSeparator());
            startTime = System.currentTimeMillis();
            TopDownSearch topDownSearchBaseline = new TopDownSearch(physicalPlan, verbose);
            ExecutionPlan oepOfBaseline = topDownSearchBaseline.execute();
            endTime = System.currentTimeMillis();
            elapsedTime = endTime - startTime;
            System.out.println("Baseline search took: " + elapsedTime + " ms");
            oepOfBaseline.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_baseline.png").toString());
        } else {
            System.out.println(System.lineSeparator());
            startTime = System.currentTimeMillis();
            BottomUpSearch greedySearcher = new BottomUpSearch(physicalPlan, verbose);
            greedySearcher.setPruneByChains(true);
            greedySearcher.setPruneBySafeEdges(true);
            greedySearcher.setGreedy(true);
            ExecutionPlan greedyOptimum = greedySearcher.execute();
            endTime = System.currentTimeMillis();
            elapsedTime = endTime - startTime;
            System.out.println("Using greedy took: " + elapsedTime + " ms");
            greedyOptimum.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_greedy.png").toString());

            System.out.println(System.lineSeparator());
            startTime = System.currentTimeMillis();
            BottomUpSearch bottomUpSearchRule123 = new BottomUpSearch(physicalPlan, verbose);
            bottomUpSearchRule123.setPruneBySafeEdges(true);
            bottomUpSearchRule123.setPruneByChains(true);
            bottomUpSearchRule123.setPruneByEarlyStopping(true);
            ExecutionPlan oep123 = bottomUpSearchRule123.execute();
            endTime = System.currentTimeMillis();
            elapsedTime = endTime - startTime;
            System.out.println("Using rule 1, 2 and 3 took: " + elapsedTime + " ms");
            oep123.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_1_2_3.png").toString());

            System.out.println(System.lineSeparator());
            startTime = System.currentTimeMillis();
            BottomUpSearch bottomUpSearchRule23 = new BottomUpSearch(physicalPlan, verbose);
            bottomUpSearchRule23.setPruneBySafeEdges(true);
            bottomUpSearchRule23.setPruneByChains(true);
            ExecutionPlan oep23 = bottomUpSearchRule23.execute();
            endTime = System.currentTimeMillis();
            elapsedTime = endTime - startTime;
            System.out.println("Using rule 2 and 3 took: " + elapsedTime + " ms");
            oep23.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_2_3.png").toString());

            System.out.println(System.lineSeparator());
            startTime = System.currentTimeMillis();
            BottomUpSearch bottomUpSearchRule2 = new BottomUpSearch(physicalPlan, verbose);
            bottomUpSearchRule2.setPruneByChains(true);
            ExecutionPlan oep2 = bottomUpSearchRule2.execute();
            endTime = System.currentTimeMillis();
            elapsedTime = endTime - startTime;
            System.out.println("Using rule 2 took: " + elapsedTime + " ms");
            oep23.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_2.png").toString());

            System.out.println(System.lineSeparator());
            startTime = System.currentTimeMillis();
            BottomUpSearch bottomUpSearchRule3 = new BottomUpSearch(physicalPlan, verbose);
            bottomUpSearchRule3.setPruneBySafeEdges(true);
            ExecutionPlan oep3 = bottomUpSearchRule3.execute();
            endTime = System.currentTimeMillis();
            elapsedTime = endTime - startTime;
            System.out.println("Using rule 3 took: " + elapsedTime + " ms");
            oep23.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_3.png").toString());

            System.out.println(System.lineSeparator());
            startTime = System.currentTimeMillis();
            BottomUpSearch bottomUpSearchBaseline = new BottomUpSearch(physicalPlan, verbose);
            ExecutionPlan oepOfBaseline = bottomUpSearchBaseline.execute();
            endTime = System.currentTimeMillis();
            elapsedTime = endTime - startTime;
            System.out.println("Baseline search took: " + elapsedTime + " ms");
            oepOfBaseline.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_baseline.png").toString());
        }
    }
}
