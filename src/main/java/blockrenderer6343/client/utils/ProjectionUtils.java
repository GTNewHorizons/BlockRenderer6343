package blockrenderer6343.client.utils;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4i;

public class ProjectionUtils {

    private static final Matrix4f ROT = new Matrix4f();
    private static final Vector3f MUT_3F = new Vector3f();
    private static final Vector3f RESULT = new Vector3f();

    public static Vector3f unProject(Vector4i rect, Vector3f eyePos, Vector3f lookat, int mouseX, int mouseY) {
        int width = rect.z();
        int height = rect.w();

        double aspectRatio = ((double) width / (double) height);
        double fov = Math.toRadians(30);

        double a = -((double) (mouseX - rect.x()) / (double) width - 0.5) * 2;
        double b = -((double) (height - (mouseY - rect.y())) / (double) height - 0.5) * 2;
        double tanf = Math.tan(fov);

        eyePos.sub(lookat, MUT_3F);
        float yawn = (float) Math.atan2(MUT_3F.x, -MUT_3F.z);
        float pitch = (float) Math.atan2(MUT_3F.y, Math.sqrt(MUT_3F.x * MUT_3F.x + MUT_3F.z * MUT_3F.z));

        ROT.identity().rotate(yawn, 0, -1, 0).rotate(pitch, 1, 0, 0);
        RESULT.set(ROT.transformPosition(MUT_3F.set(0, 0, 1)));
        ROT.transformPosition(MUT_3F.set(1, 0, 0));
        RESULT.add(
                (float) (MUT_3F.x * tanf * aspectRatio * a),
                (float) (MUT_3F.y * tanf * aspectRatio * a),
                (float) (MUT_3F.z * tanf * aspectRatio * a));
        ROT.transformPosition(MUT_3F.set(0, 1, 0));
        RESULT.add((float) (MUT_3F.x * tanf * b), (float) (MUT_3F.y * tanf * b), (float) (MUT_3F.z * tanf * b));
        return normalize(RESULT);
    }

    public static Vector3f normalize(Vector3f vec) {
        float length = (float) Math.sqrt(vec.x * vec.x + vec.y * vec.y + vec.z * vec.z);

        vec.x /= length;
        vec.y /= length;
        vec.z /= length;

        return vec;
    }
}
