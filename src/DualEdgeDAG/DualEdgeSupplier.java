package DualEdgeDAG;

import java.util.function.Supplier;

public class DualEdgeSupplier implements Supplier<DualEdge> {
    @Override
    public DualEdge get() {
        return new DualEdge(false);
    }
}
