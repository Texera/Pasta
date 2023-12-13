package ExperimentRunner;

import DualEdgeDAG.DualEdge;
import Pasta.ExecutionPlan.ExecutionPlan;
import Pasta.PastaFinder;
import Pasta.PhysicalPlan.PhysicalPlan;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ExperimentRunner {
    public static void runOptimalExecutionPlanFinder(DirectedAcyclicGraph<Integer, DualEdge> inputPhysicalPlan, Path outputPath, boolean verbose) {

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

        System.out.println(System.lineSeparator());
        startTime = System.currentTimeMillis();
        PastaFinder greedySearcher = new PastaFinder(physicalPlan, verbose);
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
        PastaFinder pastaFinderRule123 = new PastaFinder(physicalPlan, verbose);
        pastaFinderRule123.setPruneByChains(true);
        pastaFinderRule123.setPruneBySafeEdges(true);
        pastaFinderRule123.setPruneByUnsalvageableStates(true);
        ExecutionPlan oep123 = pastaFinderRule123.execute();
        endTime = System.currentTimeMillis();
        elapsedTime = endTime - startTime;
        System.out.println("Using all three rules took: " + elapsedTime + " ms");
        oep123.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_1_2_3.png").toString());

        System.out.println(System.lineSeparator());
        startTime = System.currentTimeMillis();
        PastaFinder pastaFinderRule23 = new PastaFinder(physicalPlan, verbose);
        pastaFinderRule23.setPruneBySafeEdges(true);
        pastaFinderRule23.setPruneByChains(true);
        ExecutionPlan oep23 = pastaFinderRule23.execute();
        endTime = System.currentTimeMillis();
        elapsedTime = endTime - startTime;
        System.out.println("Using rule 2 and 3 took: " + elapsedTime + " ms");
        oep23.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_2_3.png").toString());

        System.out.println(System.lineSeparator());
        startTime = System.currentTimeMillis();
        PastaFinder pastaFinderRule13 = new PastaFinder(physicalPlan, verbose);
        pastaFinderRule13.setPruneByUnsalvageableStates(true);
        pastaFinderRule13.setPruneBySafeEdges(true);
        ExecutionPlan oep13 = pastaFinderRule13.execute();
        endTime = System.currentTimeMillis();
        elapsedTime = endTime - startTime;
        System.out.println("Using rule 1 and 3 took: " + elapsedTime + " ms");
        oep23.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_1_3.png").toString());

        startTime = System.currentTimeMillis();
        PastaFinder pastaFinderRule12 = new PastaFinder(physicalPlan, verbose);
        pastaFinderRule12.setPruneByUnsalvageableStates(true);
        pastaFinderRule12.setPruneByChains(true);
        ExecutionPlan oep12 = pastaFinderRule12.execute();
        endTime = System.currentTimeMillis();
        elapsedTime = endTime - startTime;
        System.out.println("Using rule 1 and 2 took: " + elapsedTime + " ms");
        oep12.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_1_2.png").toString());

        System.out.println(System.lineSeparator());
        startTime = System.currentTimeMillis();
        PastaFinder pastaFinderRule2 = new PastaFinder(physicalPlan, verbose);
        pastaFinderRule2.setPruneByChains(true);
        ExecutionPlan oep2 = pastaFinderRule2.execute();
        endTime = System.currentTimeMillis();
        elapsedTime = endTime - startTime;
        System.out.println("Using rule 2 took: " + elapsedTime + " ms");
        oep2.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_2.png").toString());

        System.out.println(System.lineSeparator());
        startTime = System.currentTimeMillis();
        PastaFinder pastaFinderRule3 = new PastaFinder(physicalPlan, verbose);
        pastaFinderRule3.setPruneBySafeEdges(true);
        ExecutionPlan oep3 = pastaFinderRule3.execute();
        endTime = System.currentTimeMillis();
        elapsedTime = endTime - startTime;
        System.out.println("Using rule 3 took: " + elapsedTime + " ms");
        oep3.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_3.png").toString());

        System.out.println(System.lineSeparator());
        startTime = System.currentTimeMillis();
        PastaFinder pastaFinderRule1 = new PastaFinder(physicalPlan, verbose);
        pastaFinderRule1.setPruneByUnsalvageableStates(true);
        ExecutionPlan oep1 = pastaFinderRule1.execute();
        endTime = System.currentTimeMillis();
        elapsedTime = endTime - startTime;
        System.out.println("Using rule 1 took: " + elapsedTime + " ms");
        oep1.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_rule_1.png").toString());

        System.out.println(System.lineSeparator());
        startTime = System.currentTimeMillis();
        PastaFinder pastaFinderBaseline = new PastaFinder(physicalPlan, verbose);
        ExecutionPlan oepOfBaseline = pastaFinderBaseline.execute();
        endTime = System.currentTimeMillis();
        elapsedTime = endTime - startTime;
        System.out.println("Baseline search took: " + elapsedTime + " ms");
        oepOfBaseline.renderDAGImageToPath(outputPath.resolve("optimal_schedulable_physical_DAG_baseline.png").toString());
    }
}
