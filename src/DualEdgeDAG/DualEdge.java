package DualEdgeDAG;

import org.jgrapht.graph.DefaultWeightedEdge;

public class DualEdge extends DefaultWeightedEdge {
    private boolean isBlocking;

    public DualEdge() {
        this.isBlocking = false;
    }

    public DualEdge(boolean isBlocking) {
        this.isBlocking = isBlocking;
    }

    public boolean isBlocking() {
        return isBlocking;
    }

    public void setBlocking(boolean blocking) {
        isBlocking = blocking;
    }

    @Override
    public String toString() {
        return "(" + this.getSource() + (isBlocking ? " : " : " -> ") + this.getTarget() + ")";
    }

    public Object getSource() {
        return super.getSource();
    }

    public Object getTarget() {
        return super.getTarget();
    }

    public double getWeight() {
        return super.getWeight();
    }
}
