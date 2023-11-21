import DualEdgeDAG.DualEdge;
import ExperimentRunner.ExperimentRunner;
import WorkflowParser.AlteryxYXMDParser;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

public class RunWorkflowExperiment {
    public static void main(String[] args) {
        long seed = 0;
        boolean randomWeight = false;
        String inputWorkflowPath;
        if (args.length > 0) {
            inputWorkflowPath = args[0];
            System.out.println("Input workflow source file path: " + inputWorkflowPath);
            if (args.length > 1) {
                seed = Long.parseLong(args[1]);
                randomWeight = true;
            }
        } else {
            System.out.println("Please provide a file path for a workflow source file.");
            return;
        }

        DirectedAcyclicGraph<Integer, DualEdge> alteryxDAG = AlteryxYXMDParser.parseYXMD(inputWorkflowPath);
        String fileName = Paths.get(inputWorkflowPath).getFileName().toString();

        if (randomWeight) {
            Random randomizer = new Random(seed);
            alteryxDAG.edgeSet().forEach(edge -> {
                alteryxDAG.setEdgeWeight(edge, randomizer.nextDouble() * 100);
            });
        }

        Path outputPath = Paths.get("/Users/xzliu/Desktop/Experiments/Alteryx").resolve(fileName).resolve("seed_" + seed);

        ExperimentRunner.runOSPDSearcher(alteryxDAG, outputPath, true);
    }
}
