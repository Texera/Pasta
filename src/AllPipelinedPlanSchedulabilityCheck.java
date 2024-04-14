import DualEdgeDAG.DualEdge;
import ExperimentRunner.ExperimentRunner;
import WorkflowParser.DotFileParser;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

public class AllPipelinedPlanSchedulabilityCheck {
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
        Path csvFilePath = outputPath.resolve("schedulability_results.csv");
        try (BufferedWriter csvWriter = Files.newBufferedWriter(csvFilePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            csvWriter.write("File Name, Schedulability\n");

            try (Stream<Path> paths = Files.walk(dirPath)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".dot"))
                        .forEach(path -> {
                            try {
                                boolean result = processDotFile(path, outputPath);
                                String fileName = path.getFileName().toString();
                                csvWriter.write(fileName + "," + result + "\n");
                                csvWriter.flush();
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

    private static boolean processDotFile(Path dotFilePath, Path baseOutputPath) throws IOException {
        System.out.print("Processing file: " + dotFilePath + ". ");

        DirectedAcyclicGraph<Integer, DualEdge> workflowDAG = DotFileParser.parseDotFile(dotFilePath.toString());
        String fileName = dotFilePath.getFileName().toString();

        // Use the user-specified output path and create a subdirectory for each file
        Path fileOutputPath = baseOutputPath.resolve(fileName).resolve("SchedulabilityCheck");
        Files.createDirectories(fileOutputPath);

        return ExperimentRunner.runSchedulabilityChecker(workflowDAG, fileOutputPath);
    }
}



