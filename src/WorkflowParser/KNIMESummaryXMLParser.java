package WorkflowParser;

import DualEdgeDAG.DualEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.util.SupplierUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class KNIMESummaryXMLParser {

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

    public static DirectedAcyclicGraph<Integer, DualEdge> parseKNIMEXML(String filePath) {
        DirectedAcyclicGraph<Integer, DualEdge> dualDAG = new DirectedAcyclicGraph<>(SupplierUtil.createIntegerSupplier(), DualEdge::new, true);
        Element xmlFileRoot;
        try {
            xmlFileRoot = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(filePath).getDocumentElement();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }

        Map<Integer, Node> nodesMap = new HashMap<>();

        NodeList vertexList = ((Element) xmlFileRoot.getElementsByTagName("nodes").item(0)).getElementsByTagName("node");
        for (int i = 0; i < vertexList.getLength(); i++) {
            Element vertex = (Element) vertexList.item(i);
            String id = vertex.getAttribute("id");
            dualDAG.addVertex(Integer.valueOf(id));
            nodesMap.put(Integer.valueOf(id), vertex);
        }

        for (int i = 0; i < vertexList.getLength(); i++) {
            Element srcVertexElement = (Element) vertexList.item(i);
            int srcID = Integer.parseInt(srcVertexElement.getAttribute("id"));
            Element outputPortsElement = (Element) srcVertexElement.getElementsByTagName("outputs").item(0);
            NodeList outputPortElementList = outputPortsElement.getElementsByTagName("output");
            for (int j = 0; j < outputPortElementList.getLength(); j++) {
                Element outputPortElement = (Element) outputPortElementList.item(j);

                double cost = 10;

                if (outputPortElement.getAttribute("type").equals("table") && outputPortElement.getElementsByTagName("dataSummary").getLength() > 0) {
                    Element dataSummaryElement = (Element) outputPortElement.getElementsByTagName("dataSummary").item(0);
                    String dataSummaryText = dataSummaryElement.getTextContent();

                    String[] parts = dataSummaryText.split(", ");
                    String rowsPart = parts[0].split(": ")[1];
                    String colsPart = parts[1].split(": ")[1];

                    double rows = Double.parseDouble(rowsPart);
                    double cols = Double.parseDouble(colsPart);

                    cost = (rows * cols) * 8 / 1024;
                }

                if (outputPortElement.getElementsByTagName("successors").getLength() > 0) {
                    NodeList successorsList = ((Element) outputPortElement.getElementsByTagName("successors").item(0)).getElementsByTagName("successor");
                    for (int k = 0; k < successorsList.getLength(); k++) {
                        Element successorElement = (Element) successorsList.item(k);
                        int dstID = Integer.parseInt(successorElement.getAttribute("id"));
                        DualEdge newEdge = dualDAG.addEdge(srcID, dstID);
                        if (newEdge != null) {
                            dualDAG.setEdgeWeight(newEdge, cost);
                            if (!(srcVertexElement.getAttribute("type").equals("Manipulator") || srcVertexElement.getAttribute("type").equals("Predictor") || srcVertexElement.getAttribute("type").equals("Source")) || blockingOpNames.contains(srcVertexElement.getAttribute("name"))) {
                                newEdge.setBlkOrMat(true);
                            }

                        }
                    }
                }
            }
        }

        Set<Integer> isolatedVertices = new HashSet<>();
        for (Integer vertex : dualDAG.vertexSet()) {
            if (dualDAG.inDegreeOf(vertex) == 0 && dualDAG.outDegreeOf(vertex) == 0) {
                isolatedVertices.add(vertex);
            }
        }

        for (Integer isolatedVertex : isolatedVertices) {
            dualDAG.removeVertex(isolatedVertex);
        }


        return dualDAG;
    }
}
