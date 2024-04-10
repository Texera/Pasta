package WorkflowParser;

import DualEdgeDAG.DualEdge;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.nio.dot.DOTImporter;
import org.jgrapht.util.SupplierUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class DotFileParser {

    public static class DotFileVertex {
        private final int id;
        private final String label;

        public DotFileVertex(int id, String label) {
            this.id = id;
            this.label = label;
        }

        // Getters and setters
        public int getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return id + " [" + label + "]";
        }
    }


    private static final Set<String> blockingOpNames = readBlockingOperatorNames("src/WorkflowParser/KNIMEBlockingPorts.conf");


    private static Set<String> readBlockingOperatorNames(String configPath) {
        Set<String> configSet = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(configPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                configSet.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return configSet;
    }

    public static DirectedAcyclicGraph<Integer, DualEdge> parseDotFile(String filePath) {

        Graph<DotFileVertex, DefaultEdge> graph = new DirectedAcyclicGraph<>(DefaultEdge.class);

        DOTImporter<DotFileVertex, DefaultEdge> importer = new DOTImporter<>();
        importer.setVertexWithAttributesFactory((label, attributes) -> new DotFileVertex(Integer.parseInt(label), attributes.get("label").toString()));
        // Import the DOT file
        try {
            importer.importGraph(graph, new FileReader(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }


        DirectedAcyclicGraph<Integer, DualEdge> dualDAG = new DirectedAcyclicGraph<>(SupplierUtil.createIntegerSupplier(), DualEdge::new, true);

        graph.vertexSet().forEach(dotFileVertex -> {
            dualDAG.addVertex(dotFileVertex.getId());
        });

        graph.edgeSet().forEach(defaultEdge -> {
            DotFileVertex upstreamVertex = graph.getEdgeSource(defaultEdge);
            DotFileVertex downstreamVertex = graph.getEdgeTarget(defaultEdge);
            boolean isEdgeBlocking = blockingOpNames.contains(upstreamVertex.label) || downstreamVertex.label.contains("Joiner") && graph.incomingEdgesOf(downstreamVertex).stream().map(incomingEdge -> graph.getEdgeSource(incomingEdge).getId()).max(Integer::compareTo).get().equals(upstreamVertex.getId());
            dualDAG.addEdge(upstreamVertex.getId(), downstreamVertex.getId(), new DualEdge(isEdgeBlocking));
            dualDAG.setEdgeWeight(upstreamVertex.getId(), downstreamVertex.getId(), 1.0);
        });

        return dualDAG;
    }
}
