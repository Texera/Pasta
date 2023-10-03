package DualEdgeDAG;

import org.jgrapht.Graph;

import java.util.*;

public class DualDAGRepeatableGenerator<V> {
    private final Random randomizer;
    private final int numOfVertexes;
    private final int numOfEdges;
    private final double pForceChain;
    private final double pBEdge;
    private final boolean forceChain;

    public DualDAGRepeatableGenerator(int vertices, int edges, long seed, double pBEdge, boolean forceChain, double pForceChain) {
        this.numOfVertexes = vertices;
        this.numOfEdges = edges;
        this.randomizer = new Random(seed);
        this.pBEdge = pBEdge;
        this.forceChain = forceChain;
        this.pForceChain = pForceChain;
    }

    public void generateDualGraph(Graph<V, DualEdge> graph) {
        List<V> vertices = new ArrayList<>(numOfVertexes);
        Set<Integer> edgeGeneratorIds = new HashSet<>();

        for (int i = 0; i < numOfVertexes; i++) {
            vertices.add(graph.addVertex());
        }

        int edgesAdded = 0;

        while (edgesAdded < numOfEdges) {
            int edgeGeneratorId;
            do {
                edgeGeneratorId = randomizer.nextInt(numOfVertexes * (numOfVertexes - 1));
            } while (edgeGeneratorIds.contains(edgeGeneratorId));

            int fromVertexId = edgeGeneratorId / numOfVertexes;
            int toVertexId = edgeGeneratorId % (numOfVertexes - 1);
            if (toVertexId >= fromVertexId) {
                ++toVertexId;
            }

            if (forceChain && this.randomizer.nextDouble() < pForceChain) {
                if (graph.outDegreeOf(vertices.get(fromVertexId)) >= 1 || graph.inDegreeOf(vertices.get(toVertexId)) >= 1)
                    continue;
            }

            try {
                DualEdge newEdge = graph.addEdge(vertices.get(fromVertexId), vertices.get(toVertexId));
                if (newEdge != null) {
                    graph.setEdgeWeight(newEdge, this.randomizer.nextDouble() * 10.0);
                    newEdge.setBlkOrMat(randomizer.nextDouble() < pBEdge);
                }
            } catch (IllegalArgumentException e) {
                // okay, that's fine; omit cycle
            }
            edgesAdded++;
        }
    }
}

