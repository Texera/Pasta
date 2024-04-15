import DualEdgeDAG.DualEdge;
import ExperimentRunner.ExperimentRunner;
import WorkflowParser.DotFileParser;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class BatchRunWorkflowExperiments {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Please provide the directory path for workflow source files and the output directory path.");
            return;
        }

        String inputDirectoryPath = args[0];
        String outputBasePath = args[1];

        System.out.println("Input directory path: " + inputDirectoryPath);
        System.out.println("Output base path: " + outputBasePath);

        // Ensure the input path is a directory
        Path dirPath = Paths.get(inputDirectoryPath);
        if (!Files.isDirectory(dirPath)) {
            System.out.println("The provided input path is not a directory.");
            return;
        }

        // Ensure the output path is a directory, create if does not exist
        Path outputPath = Paths.get(outputBasePath);
        try {
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }
        } catch (IOException e) {
            System.err.println("Failed to create output directory: " + e.getMessage());
            return;
        }

        // Process each .dot file found in the directory
        Path csvFilePath = outputPath.resolve("workflow_results.csv");
        try (BufferedWriter csvWriter = Files.newBufferedWriter(csvFilePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

            try (Stream<Path> paths = Files.walk(dirPath)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".dot"))
                        .forEach(path -> {
                            try {
                                runExperiments(path, outputPath, csvWriter);
                            } catch (Exception e) {
                                System.err.println("Error processing file " + path + ": " + e.getMessage());
                            }
                        });
            } catch (IOException e) {
                System.err.println("Error reading files: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("Failed to write to CSV file: " + e.getMessage());
        }
    }

    private static void runExperiments(Path dotFilePath, Path baseOutputPath, BufferedWriter csvWriter) throws IOException {
        System.out.print("Processing file: " + dotFilePath + ". ");

        DirectedAcyclicGraph<Integer, DualEdge> workflowDAG = DotFileParser.parseDotFile(dotFilePath.toString());
        String fileName = dotFilePath.getFileName().toString();
        if (workflowDAG.vertexSet().size() < 10 || workflowDAG.edgeSet().size() < 10) {
            System.out.println("Skipped due to small DAG size.");
            return;
        }

        List<Map<String, String>> topDownResults = ExperimentRunner.runOptimalExecutionPlanFinder(workflowDAG, baseOutputPath.resolve(fileName).resolve("topDown"), false, true);
        topDownResults.forEach(individualResult->{
            try {
                csvWriter.write(fileName + ","
                        + "topDown" + ","
                        + individualResult.get("greedy") + ","
                        + individualResult.get("pruneByChains") + ","
                        + individualResult.get("pruneBySafeEdges") + ","
                        + individualResult.get("pruneByEarlyStopping") + ","
                        + individualResult.get("searchFinished") + ","
                        + individualResult.get("searchTime") + ","
                        + individualResult.get("numStatesExplored") + ","
                        + individualResult.get("osepCost") + "\n");
                csvWriter.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        List<Map<String, String>> bottomUpResults = ExperimentRunner.runOptimalExecutionPlanFinder(workflowDAG, baseOutputPath.resolve(fileName).resolve("bottomUp"), false, false);
        bottomUpResults.forEach(individualResult->{
            try {
                csvWriter.write(fileName + ","
                        + "bottomUp" + ","
                        + individualResult.get("greedy") + ","
                        + individualResult.get("pruneByChains") + ","
                        + individualResult.get("pruneBySafeEdges") + ","
                        + individualResult.get("pruneByEarlyStopping") + ","
                        + individualResult.get("searchFinished") + ","
                        + individualResult.get("searchTime") + ","
                        + individualResult.get("numStatesExplored") + ","
                        + individualResult.get("osepCost") + "\n");
                csvWriter.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
