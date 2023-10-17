import DualEdgeDAG.DualEdge;
import ExperimentRunner.ExperimentRunner;
import WorkflowParser.AlteryxYXMDParser;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.nio.file.Path;
import java.nio.file.Paths;

public class RunWorkflowExperiment {
    public static void main(String[] args) {
        String inputWorkflowPath;
        if (args.length > 0) {
            inputWorkflowPath = args[0];
            System.out.println("Input workflow source file path: " + inputWorkflowPath);
        } else {
            System.out.println("Please provide a file path for a workflow source file.");
            return;
        }

        DirectedAcyclicGraph<Integer, DualEdge> alteryxDAG = AlteryxYXMDParser.parseYXMD(inputWorkflowPath);
        String fileName = Paths.get(inputWorkflowPath).getFileName().toString();
        Path outputPath = Paths.get("/Users/xzliu/Desktop/Experiments/Alteryx").resolve(fileName);

        ExperimentRunner.runOSPDSearcher(alteryxDAG, outputPath);
    }
}
