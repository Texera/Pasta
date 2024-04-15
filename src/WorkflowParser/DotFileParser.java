package WorkflowParser;

import DualEdgeDAG.DualEdge;
import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.nio.dot.DOTImporter;
import org.jgrapht.util.SupplierUtil;
import org.jgrapht.nio.Attribute;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

    public static class DotFileEdge {
        private int from;
        private int to;
        private Map<String, Attribute> attributes;

        public DotFileEdge(int from, int to, Map<String, Attribute> attributes) {
            this.from = from;
            this.to = to;
            this.attributes = attributes;
        }

        @Override
        public String toString() {
            return "Edge from " + from + " to " + to + " with attributes: " + attributes;
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

        Graph<DotFileVertex, DualEdge> graph = new DirectedAcyclicGraph<>(DualEdge.class);

        Map<DualEdge, Double> weights = new HashMap<>();

        DOTImporter<DotFileVertex, DualEdge> importer = new DOTImporter<>();
        importer.setVertexWithAttributesFactory((label, attributes) -> new DotFileVertex(label.hashCode(), attributes.get("label").toString()));
        importer.addEdgeAttributeConsumer((dotFileEdge, attribute)-> {
            String[] labels = attribute.toString().split(";");
            Double matSize = Double.parseDouble(labels[0].split(":")[1]);
            boolean isBlocking = labels[1].split(":")[1].contains("True");
            dotFileEdge.getFirst().setBlkOrMat(isBlocking);
            weights.put(dotFileEdge.getFirst(), matSize);
        });
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

        graph.edgeSet().forEach(initialDualEdge -> {
            DotFileVertex upstreamVertex = graph.getEdgeSource(initialDualEdge);
            DotFileVertex downstreamVertex = graph.getEdgeTarget(initialDualEdge);
//            boolean isEdgeBlocking = initialDualEdge.isBlkOrMat() || blockingOpNames.contains(upstreamVertex.label) || downstreamVertex.label.contains("Joiner") && graph.incomingEdgesOf(downstreamVertex).stream().map(incomingEdge -> graph.getEdgeSource(incomingEdge).getId()).max(Integer::compareTo).get().equals(upstreamVertex.getId());
//
            dualDAG.addEdge(upstreamVertex.getId(), downstreamVertex.getId(), new DualEdge(initialDualEdge.isBlkOrMat()));
            dualDAG.setEdgeWeight(upstreamVertex.getId(), downstreamVertex.getId(), weights.get(initialDualEdge));
        });

        return dualDAG;
    }
}
