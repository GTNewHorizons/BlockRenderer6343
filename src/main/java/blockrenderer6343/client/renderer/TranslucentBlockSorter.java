package blockrenderer6343.client.renderer;

import org.joml.Vector3f;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongComparator;
import it.unimi.dsi.fastutil.longs.LongList;

public class TranslucentBlockSorter {

    private final LongArrayList source = new LongArrayList();
    private final LongArrayList sorted = new LongArrayList();
    private final Vector3f direction = new Vector3f();
    private final LongComparator comparator = (left, right) -> Double.compare(depth(right), depth(left));

    public LongList sort(LongList positions, Vector3f eye, Vector3f target) {
        float dx = target.x - eye.x;
        float dy = target.y - eye.y;
        float dz = target.z - eye.z;
        if (!source.equals(positions) || direction.x != dx || direction.y != dy || direction.z != dz) {
            source.clear();
            source.addAll(positions);
            sorted.clear();
            sorted.addAll(source);
            direction.set(dx, dy, dz);
            if (sorted.size() > 1) sorted.sort(comparator);
        }
        return sorted;
    }

    public void clear() {
        source.clear();
        sorted.clear();
    }

    public void trim() {
        source.trim();
        sorted.trim();
    }

    private double depth(long position) {
        return (CoordinatePacker.unpackX(position) + 0.5) * direction.x
                + (CoordinatePacker.unpackY(position) + 0.5) * direction.y
                + (CoordinatePacker.unpackZ(position) + 0.5) * direction.z;
    }
}
