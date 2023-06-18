package blockrenderer6343.api.utils;

import java.util.Objects;

public class BlockPosition {

    public int x;
    public int y;
    public int z;

    public BlockPosition(int var1, int var2, int var3) {
        this.x = var1;
        this.y = var2;
        this.z = var3;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockPosition that = (BlockPosition) o;
        return x == that.x && y == that.y && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}
