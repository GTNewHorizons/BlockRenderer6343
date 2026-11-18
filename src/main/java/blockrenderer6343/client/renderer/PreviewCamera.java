package blockrenderer6343.client.renderer;

import org.joml.Vector3f;
import org.joml.Vector3fc;

public class PreviewCamera {

    private static final float MIN_DISTANCE = 0.5f;
    private static final float MAX_DISTANCE = 999;
    private static final float MAX_ELEVATION = 89.9f;
    private static final float TAN_HALF_FOV = (float) Math.tan(Math.toRadians(30));
    private final Vector3f pivot = new Vector3f();
    private final Vector3f eye = new Vector3f();
    private final Vector3f lookAt = new Vector3f();
    private float framingX;
    private float framingY;
    private float distance;
    private float azimuth;
    private float elevation;
    private long lastFrameNanos;
    private boolean initialized;

    public void reset(Vector3fc targetPivot, float distance, float azimuth, float elevation) {
        pivot.set(targetPivot);
        this.distance = clampDistance(distance);
        this.azimuth = wrapDegrees(azimuth);
        this.elevation = clampElevation(elevation);
        framingX = 0;
        framingY = 0;
        lastFrameNanos = 0;
        initialized = true;
        updateEye();
    }

    public void update(Vector3fc targetPivot, float targetDistance, float targetAzimuth, float targetElevation,
            boolean dragging, long frameNanos) {
        if (!initialized) reset(targetPivot, targetDistance, targetAzimuth, targetElevation);
        float seconds = lastFrameNanos == 0 ? 0 : Math.max(0, Math.min((frameNanos - lastFrameNanos) * 1e-9f, 0.05f));
        lastFrameNanos = frameNanos;
        float blend = (float) -Math.expm1(-(dragging ? 48 : 22) * seconds);
        targetDistance = clampDistance(targetDistance);
        // Interpolate logarithmically so zoom speed stays proportional to viewing distance.
        distance *= (float) Math.exp(Math.log(targetDistance / distance) * blend);
        if (Math.abs(distance - targetDistance) < Math.max(0.0001f, targetDistance * 0.00001f)) {
            distance = targetDistance;
        }
        float angleDelta = wrapDegrees(targetAzimuth - azimuth);
        azimuth = wrapDegrees(azimuth + angleDelta * blend);
        if (Math.abs(angleDelta) < 0.001f) azimuth = wrapDegrees(targetAzimuth);
        targetElevation = clampElevation(targetElevation);
        elevation += (targetElevation - elevation) * blend;
        if (Math.abs(targetElevation - elevation) < 0.001f) elevation = targetElevation;
        pivot.lerp(targetPivot, blend);
        if (pivot.distanceSquared(targetPivot) < 1e-8f) pivot.set(targetPivot);
        updateEye();
    }

    public void pan(float deltaX, float deltaY, int viewportHeight, Vector3f targetPivot) {
        if (!initialized || viewportHeight <= 0) return;
        pan(deltaX, deltaY, 2 * distance * TAN_HALF_FOV / viewportHeight, targetPivot);
    }

    public void pan(float deltaX, float deltaY, float unitsPerPixel, Vector3f targetPivot) {
        if (!initialized) return;
        double horizontal = Math.toRadians(azimuth);
        double vertical = Math.toRadians(elevation);
        float sinH = (float) Math.sin(horizontal);
        float cosH = (float) Math.cos(horizontal);
        float sinV = (float) Math.sin(vertical);
        float cosV = (float) Math.cos(vertical);
        float scale = unitsPerPixel;
        targetPivot.add(
                (-deltaX * sinH - deltaY * sinV * cosH) * scale,
                deltaY * cosV * scale,
                (deltaX * cosH - deltaY * sinV * sinH) * scale);
    }

    public Vector3fc getPivot() {
        return pivot;
    }

    public Vector3fc getEyePosition() {
        return eye;
    }

    public Vector3fc getLookAt() {
        return lookAt;
    }

    public float getDistance() {
        return distance;
    }

    public boolean focus(Vector3fc targetPivot) {
        if (!initialized) return false;
        double horizontal = Math.toRadians(azimuth);
        double vertical = Math.toRadians(elevation);
        float sinH = (float) Math.sin(horizontal);
        float cosH = (float) Math.cos(horizontal);
        float sinV = (float) Math.sin(vertical);
        float cosV = (float) Math.cos(vertical);
        float dx = eye.x - targetPivot.x();
        float dy = eye.y - targetPivot.y();
        float dz = eye.z - targetPivot.z();
        float depth = dx * cosV * cosH + dy * sinV + dz * cosV * sinH;
        if (!Float.isFinite(depth) || depth < MIN_DISTANCE || depth > MAX_DISTANCE) return false;
        // Normalized framing keeps the picked point at the same screen position during orbit and zoom.
        framingX = (dx * sinH - dz * cosH) / depth;
        framingY = (-dx * sinV * cosH + dy * cosV - dz * sinV * sinH) / depth;
        pivot.set(targetPivot);
        distance = depth;
        updateEye();
        return true;
    }

    public static float clampDistance(float distance) {
        return Math.max(MIN_DISTANCE, Math.min(MAX_DISTANCE, distance));
    }

    private static float clampElevation(float elevation) {
        return Math.max(-MAX_ELEVATION, Math.min(MAX_ELEVATION, elevation));
    }

    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360;
        if (wrapped >= 180) wrapped -= 360;
        if (wrapped < -180) wrapped += 360;
        return wrapped;
    }

    private void updateEye() {
        double horizontal = Math.toRadians(azimuth);
        double vertical = Math.toRadians(elevation);
        float sinH = (float) Math.sin(horizontal);
        float cosH = (float) Math.cos(horizontal);
        float sinV = (float) Math.sin(vertical);
        float cosV = (float) Math.cos(vertical);
        float offsetX = framingX * distance;
        float offsetY = framingY * distance;
        lookAt.set(
                pivot.x + offsetX * sinH - offsetY * sinV * cosH,
                pivot.y + offsetY * cosV,
                pivot.z - offsetX * cosH - offsetY * sinV * sinH);
        eye.set(lookAt.x + distance * cosV * cosH, lookAt.y + distance * sinV, lookAt.z + distance * cosV * sinH);
    }
}
