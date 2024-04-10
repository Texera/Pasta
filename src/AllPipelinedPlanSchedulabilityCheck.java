import DualEdgeDAG.DualEdge;
import ExperimentRunner.ExperimentRunner;
import WorkflowParser.DotFileParser;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

public class AllPipelinedPlanSchedulabilityCheck {
    public static void main(String[] args) {
        String inputWorkflowPath;
        if (args.length > 0) {
            inputWorkflowPath = args[0];
            System.out.println("Input workflow source file path: " + inputWorkflowPath);
        } else {
            System.out.println("Please provide a file path for a workflow source file.");
            return;
        }

        DirectedAcyclicGraph<Integer, DualEdge> workflowDAG = DotFileParser.parseDotFile(inputWorkflowPath);

        String fileName = Paths.get(inputWorkflowPath).getFileName().toString();


        // TODO: Please replace the path here to be your desired output path for this experiment.

        Path outputPath = Paths.get("/Users/xzliu/Desktop/Experiments").resolve(fileName).resolve("SchedulabilityCheck");
        ExperimentRunner.runOptimalExecutionPlanFinder(workflowDAG, outputPath.resolve("topDown"), true, true, true);
    }
}
