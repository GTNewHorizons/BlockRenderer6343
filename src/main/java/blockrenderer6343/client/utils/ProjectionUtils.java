package blockrenderer6343.client.utils;

import blockrenderer6343.api.utils.BlockPosition;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector3f;

public class ProjectionUtils {

    private static final FloatBuffer MODELVIEW_MATRIX_BUFFER =
            ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private static final FloatBuffer PROJECTION_MATRIX_BUFFER =
            ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private static final IntBuffer VIEWPORT_BUFFER =
            ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
    protected static final FloatBuffer PIXEL_DEPTH_BUFFER =
            ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    protected static final FloatBuffer OBJECT_POS_BUFFER =
            ByteBuffer.allocateDirect(3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

    public static Vector3f project(BlockPosition pos) {
        // read current rendering parameters
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODELVIEW_MATRIX_BUFFER);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION_MATRIX_BUFFER);
        GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT_BUFFER);

        // rewind buffers after write by OpenGL glGet calls
        MODELVIEW_MATRIX_BUFFER.rewind();
        PROJECTION_MATRIX_BUFFER.rewind();
        VIEWPORT_BUFFER.rewind();

        // call gluProject with retrieved parameters
        GLU.gluProject(
                pos.x + 0.5f,
                pos.y + 0.5f,
                pos.z + 0.5f,
                MODELVIEW_MATRIX_BUFFER,
                PROJECTION_MATRIX_BUFFER,
                VIEWPORT_BUFFER,
                OBJECT_POS_BUFFER);

        // rewind buffers after read by gluProject
        VIEWPORT_BUFFER.rewind();
        PROJECTION_MATRIX_BUFFER.rewind();
        MODELVIEW_MATRIX_BUFFER.rewind();

        // rewind buffer after write by gluProject
        OBJECT_POS_BUFFER.rewind();

        // obtain position in Screen
        float winX = OBJECT_POS_BUFFER.get();
        float winY = OBJECT_POS_BUFFER.get();
        float winZ = OBJECT_POS_BUFFER.get();

        // rewind buffer after read
        OBJECT_POS_BUFFER.rewind();

        return new Vector3f(winX, winY, winZ);
    }

    public static Vector3f unProject(int mouseX, int mouseY) {
        // read depth of pixel under mouse
        GL11.glReadPixels(mouseX, mouseY, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, PIXEL_DEPTH_BUFFER);

        // rewind buffer after write by glReadPixels
        PIXEL_DEPTH_BUFFER.rewind();

        // retrieve depth from buffer (0.0-1.0f)
        float pixelDepth = PIXEL_DEPTH_BUFFER.get();

        // rewind buffer after read
        PIXEL_DEPTH_BUFFER.rewind();

        // read current rendering parameters
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODELVIEW_MATRIX_BUFFER);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION_MATRIX_BUFFER);
        GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT_BUFFER);

        // rewind buffers after write by OpenGL glGet calls
        MODELVIEW_MATRIX_BUFFER.rewind();
        PROJECTION_MATRIX_BUFFER.rewind();
        VIEWPORT_BUFFER.rewind();

        // call gluUnProject with retrieved parameters
        GLU.gluUnProject(
                mouseX,
                mouseY,
                pixelDepth,
                MODELVIEW_MATRIX_BUFFER,
                PROJECTION_MATRIX_BUFFER,
                VIEWPORT_BUFFER,
                OBJECT_POS_BUFFER);

        // rewind buffers after read by gluUnProject
        VIEWPORT_BUFFER.rewind();
        PROJECTION_MATRIX_BUFFER.rewind();
        MODELVIEW_MATRIX_BUFFER.rewind();

        // rewind buffer after write by gluUnProject
        OBJECT_POS_BUFFER.rewind();

        // obtain absolute position in world
        float posX = OBJECT_POS_BUFFER.get();
        float posY = OBJECT_POS_BUFFER.get();
        float posZ = OBJECT_POS_BUFFER.get();

        // rewind buffer after read
        OBJECT_POS_BUFFER.rewind();
        return new Vector3f(posX, posY, posZ);
    }
}
