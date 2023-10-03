package DualEdgeDAG;

import org.jgrapht.graph.DefaultWeightedEdge;

public class DualEdge extends DefaultWeightedEdge {
    private boolean blkOrMat;

    public DualEdge() {
        this.blkOrMat = false;
    }

    public DualEdge(boolean blkOrMat) {
        this.blkOrMat = blkOrMat;
    }

    public boolean isBlkOrMat() {
        return blkOrMat;
    }

    public void setBlkOrMat(boolean blkOrMat) {
        this.blkOrMat = blkOrMat;
    }

    @Override
    public String toString() {
        return "(" + this.getSource() + (blkOrMat ? " : " : " -> ") + this.getTarget() + ")";
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
