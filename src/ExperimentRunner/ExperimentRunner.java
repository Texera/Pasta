package ExperimentRunner;

import DualEdgeDAG.DualEdge;
import Pasta.BottomUpSearch;
import Pasta.ExecutionPlan.ExecutionPlan;
import Pasta.TopDownSearch;
import Pasta.PhysicalPlan.PhysicalPlan;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExperimentRunner {

    public static boolean runSchedulabilityChecker(DirectedAcyclicGraph<Integer, DualEdge> inputPhysicalPlan, Path outputPath) {
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
        System.out.print("Initializing the physical plan took: " + elapsedTime + " ms. ");
        physicalPlan.renderDAGImageToPath(outputPath.resolve("input_physical_plan.png").toString());
        physicalPlan.renderAbstractDAGToPath(outputPath.resolve("abstract_input_physical_plan.png").toString());
        boolean schedulability = new ExecutionPlan(physicalPlan, physicalPlan.getBlockingEdges()).checkSchedulability();
        System.out.println("Schedulability:" + schedulability);
        return schedulability;
    }

    public static List<Map<String, String>> runOptimalExecutionPlanFinder(DirectedAcyclicGraph<Integer, DualEdge> inputPhysicalPlan, Path outputPath, boolean verbose, boolean topDown) {
        List<Map<String, String>> allExpResults = new ArrayList<>();
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
        boolean schedulability = new ExecutionPlan(physicalPlan, physicalPlan.getBlockingEdges()).checkSchedulability();
        // skip schedulable workflows
        if (schedulability) return allExpResults;
        System.out.println(System.lineSeparator());
        if (topDown) {
            System.out.println("Starting Top-down Search");
            for (int i = 0; i < 16; i++) {
                boolean a = (i & 8) != 0; // 8 is binary 1000
                boolean b = (i & 4) != 0; // 4 is binary 0100
                boolean c = (i & 2) != 0; // 2 is binary 0010
                boolean d = (i & 1) != 0; // 1 is binary 0001
                allExpResults.add(runSingleTopDownExperiment(outputPath, verbose, physicalPlan, a, b, c, d));
            }
        } else {
            System.out.println("Starting Bottom-up Search");
            for (int i = 0; i < 16; i++) {
                boolean a = (i & 8) != 0; // 8 is binary 1000
                boolean b = (i & 4) != 0; // 4 is binary 0100
                boolean c = (i & 2) != 0; // 2 is binary 0010
                boolean d = (i & 1) != 0; // 1 is binary 0001
                allExpResults.add(runSingleBottomUpExperiment(outputPath, verbose, physicalPlan, a, b, c, d));
            }
        }
        return allExpResults;
    }

    private static Map<String, String> runSingleTopDownExperiment(Path outputPath, boolean verbose, PhysicalPlan physicalPlan, Boolean pruneByChains, Boolean pruneBySafeEdges, Boolean pruneByEarlyStopping, Boolean greedy) {
        Map<String, String> experimentResults = new HashMap<>();
        experimentResults.put("greedy", greedy.toString());
        experimentResults.put("pruneByChains", pruneByChains.toString());
        experimentResults.put("pruneBySafeEdges", pruneBySafeEdges.toString());
        experimentResults.put("pruneByEarlyStopping", pruneByEarlyStopping.toString());
        System.out.println(System.lineSeparator());
        Long startTime = System.currentTimeMillis();
        TopDownSearch searcher = new TopDownSearch(physicalPlan, verbose);
        searcher.setPruneByChains(pruneByChains);
        searcher.setPruneBySafeEdges(pruneBySafeEdges);
        searcher.setPruneByEarlyStopping(pruneByEarlyStopping);
        searcher.setGreedy(greedy);
        ExecutionPlan searchResult = searcher.execute();
        Long endTime = System.currentTimeMillis();
        Long elapsedTime = endTime - startTime;
        System.out.println(String.format("greedy_%b_chain_%b_safeEdge_%b_earlyStop_%b", greedy, pruneByChains, pruneBySafeEdges, pruneByEarlyStopping) + "took: " + elapsedTime + " ms");
        experimentResults.put("searchFinished", String.valueOf(searcher.getSearchQueue().isEmpty()));
        experimentResults.put("searchTime", elapsedTime.toString());
        experimentResults.put("numStatesExplored", String.valueOf(searcher.getVisitedSet().size()));
        experimentResults.put("osepCost", String.valueOf(searchResult.getCost()));
        searchResult.renderDAGImageToPath(outputPath.resolve(String.format("greedy_%b_chain_%b_safeEdge_%b_earlyStop_%b.png", greedy, pruneByChains, pruneBySafeEdges, pruneByEarlyStopping)).toString());
        return experimentResults;
    }

    private static Map<String, String> runSingleBottomUpExperiment(Path outputPath, boolean verbose, PhysicalPlan physicalPlan, Boolean pruneByChains, Boolean pruneBySafeEdges, Boolean pruneByEarlyStopping, Boolean greedy) {
        Map<String, String> experimentResults = new HashMap<>();
        experimentResults.put("greedy", greedy.toString());
        experimentResults.put("pruneByChains", pruneByChains.toString());
        experimentResults.put("pruneBySafeEdges", pruneBySafeEdges.toString());
        experimentResults.put("pruneByEarlyStopping", pruneByEarlyStopping.toString());
        System.out.println(System.lineSeparator());
        Long startTime = System.currentTimeMillis();
        BottomUpSearch searcher = new BottomUpSearch(physicalPlan, verbose);
        searcher.setPruneByChains(pruneByChains);
        searcher.setPruneBySafeEdges(pruneBySafeEdges);
        searcher.setPruneByEarlyStopping(pruneByEarlyStopping);
        searcher.setGreedy(greedy);
        ExecutionPlan searchResult = searcher.execute();
        Long endTime = System.currentTimeMillis();
        Long elapsedTime = endTime - startTime;
        System.out.println(String.format("greedy_%b_chain_%b_safeEdge_%b_earlyStop_%b", greedy, pruneByChains, pruneBySafeEdges, pruneByEarlyStopping) + "took: " + elapsedTime + " ms");
        experimentResults.put("searchFinished", String.valueOf(searcher.getSearchQueue().isEmpty()));
        experimentResults.put("searchTime", elapsedTime.toString());
        experimentResults.put("numStatesExplored", String.valueOf(searcher.getVisitedSet().size()));
        experimentResults.put("osepCost", String.valueOf(searchResult.getCost()));
        searchResult.renderDAGImageToPath(outputPath.resolve(String.format("greedy_%b_chain_%b_safeEdge_%b_earlyStop_%b.png", greedy, pruneByChains, pruneBySafeEdges, pruneByEarlyStopping)).toString());
        return experimentResults;
    }
}
