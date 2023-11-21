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

public class AlteryxYXMDParser {


    private static final Set<String> blockingOpDLLNames = readBlockingOperationEngineDllEntryPoints("/Users/xzliu/IdeaProjects/DualEdgeDAG/src/WorkflowParser/AlteryxBlockingOperations.conf");


    private static Set<String> readBlockingOperationEngineDllEntryPoints(String configPath) {
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

    public static DirectedAcyclicGraph<Integer, DualEdge> parseYXMD(String filePath) {
        DirectedAcyclicGraph<Integer, DualEdge> dualDAG = new DirectedAcyclicGraph<>(SupplierUtil.createIntegerSupplier(), DualEdge::new, true);
        Element yxmdDocRoot;
        try {
            yxmdDocRoot = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(filePath).getDocumentElement();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }

        Map<Integer, Node> alteryxNodesMap = new HashMap<>();

        NodeList vertexList = ((Element) yxmdDocRoot.getElementsByTagName("Nodes").item(0)).getElementsByTagName("Node");
        for (int i = 0; i < vertexList.getLength(); i++) {
            Element vertex = (Element) vertexList.item(i);
            if (vertex.getElementsByTagName("EngineSettings").getLength() > 0) {
                String id = vertex.getAttribute("ToolID");
                dualDAG.addVertex(Integer.valueOf(id));
                alteryxNodesMap.put(Integer.valueOf(id), vertex);
            }
        }

        NodeList edgeList = ((Element) yxmdDocRoot.getElementsByTagName("Connections").item(0)).getElementsByTagName("Connection");
        for (int i = 0; i < edgeList.getLength(); i++) {
            Element edge = (Element) edgeList.item(i);
            Element origin = (Element) edge.getElementsByTagName("Origin").item(0);
            int src = Integer.parseInt(origin.getAttribute("ToolID"));
            Element destination = (Element) edge.getElementsByTagName("Destination").item(0);
            int dest = Integer.parseInt(destination.getAttribute("ToolID"));
            Element srcNode = (Element) alteryxNodesMap.get(src);
            Element destNode = (Element) alteryxNodesMap.get(dest);
            DualEdge newEdge = dualDAG.addEdge(src, dest);
            Element destEngineSettings = (Element) destNode.getElementsByTagName("EngineSettings").item(0);
            if (destEngineSettings.getAttribute("EngineDllEntryPoint").equals("AlteryxJoin")
                    && (destination.getAttribute("Connection").equals("Right"))) {
                newEdge.setBlkOrMat(true);
            }
            Element srcEngineSettings = (Element) srcNode.getElementsByTagName("EngineSettings").item(0);
            if (blockingOpDLLNames.contains(srcEngineSettings.getAttribute("EngineDllEntryPoint"))) {
                newEdge.setBlkOrMat(true);
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
