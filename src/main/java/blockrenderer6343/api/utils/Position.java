package blockrenderer6343.api.utils;

import com.google.common.base.Objects;

public class Position {

    public static final Position ORIGIN = new Position(0, 0);

    public final int x;
    public final int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Position add(Position other) {
        return new Position(x + other.x, y + other.y);
    }

    public Position subtract(Position other) {
        return new Position(x - other.x, y - other.y);
    }

    public Position add(Size size) {
        return new Position(x + size.width, y + size.height);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position position)) return false;
        return x == position.x && y == position.y;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(x, y);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("x", x).add("y", y).toString();
    }
}
