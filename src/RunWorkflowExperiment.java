import DualEdgeDAG.DualEdge;
import ExperimentRunner.ExperimentRunner;
import WorkflowParser.AlteryxYXMDParser;
import WorkflowParser.KNIMESummaryXMLParser;
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

        DirectedAcyclicGraph<Integer, DualEdge> workflowDAG;
        String fileType = inputWorkflowPath.substring(inputWorkflowPath.lastIndexOf(".") + 1);
        if (fileType.equals("yxmd")) {
            workflowDAG = AlteryxYXMDParser.parseYXMD(inputWorkflowPath);
        } else {
            workflowDAG = KNIMESummaryXMLParser.parseKNIMEXML(inputWorkflowPath);
        }

        String fileName = Paths.get(inputWorkflowPath).getFileName().toString();

        if (randomWeight) {
            Random randomizer = new Random(seed);
            workflowDAG.edgeSet().forEach(edge -> {
                workflowDAG.setEdgeWeight(edge, randomizer.nextDouble() * 100);
            });
        }

        Path outputPath;

        // TODO: Please replace the path here to be your desired output path for this experiment.

        if (randomWeight) {
            outputPath = Paths.get("/Users/xzliu/Desktop/Experiments").resolve(fileType).resolve(fileName).resolve("seed_" + seed);
        } else {
            outputPath = Paths.get("/Users/xzliu/Desktop/Experiments").resolve(fileType).resolve(fileName).resolve("realCost");
        }

        ExperimentRunner.runOptimalExecutionPlanFinder(workflowDAG, outputPath, true);
    }
}
