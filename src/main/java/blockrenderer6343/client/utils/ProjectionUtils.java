package blockrenderer6343.client.utils;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import blockrenderer6343.api.utils.PositionedRect;

public class ProjectionUtils {

    public static Vector3f unProject(PositionedRect rect, Vector3f eyePos, Vector3f lookat, int mouseX, int mouseY) {
        int width = rect.size.width;
        int height = rect.size.height;

        double aspectRatio = ((double) width / (double) height);
        double fov = ((60 / 2d)) * (Math.PI / 180);

        double a = -((double) (mouseX - rect.position.x) / (double) width - 0.5) * 2;
        double b = -((double) (height - (mouseY - rect.position.y)) / (double) height - 0.5) * 2;
        double tanf = Math.tan(fov);

        Vector3f lookVec = new Vector3f();
        Vector3f.sub(eyePos, lookat, lookVec);
        float yawn = (float) Math.atan2(lookVec.x, -lookVec.z);
        float pitch = (float) Math.atan2(lookVec.y, Math.sqrt(lookVec.x * lookVec.x + lookVec.z * lookVec.z));

        Matrix4f rot = new Matrix4f();
        rot.rotate(yawn, new Vector3f(0, -1, 0));
        rot.rotate(pitch, new Vector3f(1, 0, 0));
        Vector4f foward = new Vector4f(0, 0, 1, 0);
        Vector4f up = new Vector4f(0, 1, 0, 0);
        Vector4f left = new Vector4f(1, 0, 0, 0);
        Matrix4f.transform(rot, foward, foward);
        Matrix4f.transform(rot, up, up);
        Matrix4f.transform(rot, left, left);

        Vector3f result = new Vector3f(foward.x, foward.y, foward.z);
        Vector3f.add(
                result,
                new Vector3f(
                        (float) (left.x * tanf * aspectRatio * a),
                        (float) (left.y * tanf * aspectRatio * a),
                        (float) (left.z * tanf * aspectRatio * a)),
                result);
        Vector3f.add(
                result,
                new Vector3f((float) (up.x * tanf * b), (float) (up.y * tanf * b), (float) (up.z * tanf * b)),
                result);
        return normalize(result);
    }

    public static Vector3f normalize(Vector3f vec) {
        float length = (float) Math.sqrt(vec.x * vec.x + vec.y * vec.y + vec.z * vec.z);

        vec.x /= length;
        vec.y /= length;
        vec.z /= length;

        return vec;
    }
}
