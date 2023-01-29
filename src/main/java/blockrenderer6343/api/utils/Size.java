package blockrenderer6343.api.utils;

import java.util.Objects;

public class Size {

    public static final Size ZERO = new Size(0, 0);

    public final int width;
    public final int height;

    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Size)) return false;
        Size size = (Size) o;
        return width == size.width && height == size.height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height);
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this).add("width", width).add("height", height).toString();
    }
}
