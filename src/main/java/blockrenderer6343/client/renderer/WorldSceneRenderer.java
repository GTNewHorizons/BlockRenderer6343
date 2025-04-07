package blockrenderer6343.client.renderer;

import static org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS;
import static org.lwjgl.opengl.GL11.GL_ALL_CLIENT_ATTRIB_BITS;
import static org.lwjgl.opengl.GL11.GL_ALPHA_TEST;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_LIGHTING;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glPopAttrib;
import static org.lwjgl.opengl.GL11.glPopClientAttrib;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushAttrib;
import static org.lwjgl.opengl.GL11.glPushClientAttrib;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glViewport;

import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.ForgeHooksClient;

import org.joml.Vector3f;
import org.joml.Vector4i;
import org.lwjgl.opengl.GL12;
import org.lwjgl.util.glu.GLU;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;

import blockrenderer6343.client.utils.ProjectionUtils;
import blockrenderer6343.client.world.TrackedDummyWorld;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: KilaBash, backported by Quarri6343
 * @Date: 2021/08/23
 * @Description: Abstract class, and extend a lot of features compared with the original one.
 */
public abstract class WorldSceneRenderer {

    public static int backgroundColor = 0xC6C6C6;
    // you have to place blocks in the world before use
    public final TrackedDummyWorld world;
    // the Blocks which this renderer needs to render
    public final LongSet renderedBlocks = new LongOpenHashSet();
    public final LongArrayList renderTranslucentBlocks = new LongArrayList();
    private Consumer<WorldSceneRenderer> beforeRender;
    private Consumer<WorldSceneRenderer> onRender;
    private Consumer<MovingObjectPosition> onLookingAt;
    private Consumer<WorldSceneRenderer> onPostBlockRendered;
    private MovingObjectPosition lastTraceResult;
    private final Vector3f eyePos = new Vector3f(0, 0, -10f);
    private final Vector3f lookAt = new Vector3f(0, 0, 0);
    private final Vector3f worldUp = new Vector3f(0, 1, 0);
    protected Vector4i rect = new Vector4i();
    private boolean renderAllFaces = false;
    private final RenderBlocks bufferBuilder = new RenderBlocks();

    public WorldSceneRenderer(TrackedDummyWorld world) {
        this.world = world;
    }

    public WorldSceneRenderer setBeforeWorldRender(Consumer<WorldSceneRenderer> callback) {
        this.beforeRender = callback;
        return this;
    }

    public WorldSceneRenderer setPostBlockRender(Consumer<WorldSceneRenderer> callback) {
        this.onPostBlockRendered = callback;
        return this;
    }

    public WorldSceneRenderer setOnWorldRender(Consumer<WorldSceneRenderer> callback) {
        this.onRender = callback;
        return this;
    }

    public void setRenderAllBlocks() {
        resetRenderedBlocks();
        setRenderAllFaces(false);
        this.renderedBlocks.addAll(world.blockMap.keySet());
        world.setVisibleYLevel(-1);
    }

    public void setRenderYLayer(int layer) {
        resetRenderedBlocks();
        setRenderAllFaces(true);

        int minY = (int) world.getMinPos().y();
        world.setVisibleYLevel(minY + layer);

        for (long pos : world.blockMap.keySet()) {
            if (CoordinatePacker.unpackY(pos) - minY == layer) {
                this.renderedBlocks.add(pos);
            }
        }
    }

    public WorldSceneRenderer setOnLookingAt(Consumer<MovingObjectPosition> onLookingAt) {
        this.onLookingAt = onLookingAt;
        return this;
    }

    public void setRenderAllFaces(boolean renderAllFaces) {
        this.renderAllFaces = renderAllFaces;
    }

    public MovingObjectPosition getLastTraceResult() {
        return lastTraceResult;
    }

    public void resetRenderedBlocks() {
        renderedBlocks.clear();
        renderTranslucentBlocks.clear();
    }

    /**
     * Renders scene on given coordinates with given width and height, and RGB background color Note that this will
     * ignore any transformations applied currently to projection/view matrix, so specified coordinates are scaled MC
     * gui coordinates. It will return matrices of projection and view in previous state after rendering
     */
    public void render(int x, int y, int width, int height, int mouseX, int mouseY) {
        rect.set(x, y, width, height);
        // setupCamera
        setupCamera();

        // render TrackedDummyWorld
        drawWorld();

        // check lookingAt
        this.lastTraceResult = null;
        if (onLookingAt != null && isInsideRect(mouseX, mouseY)) {
            Vector3f lookVec = ProjectionUtils.unProject(rect, eyePos, lookAt, mouseX, mouseY);
            MovingObjectPosition result = rayTrace(lookVec);
            if (result != null) {
                this.lastTraceResult = result;
                onLookingAt.accept(result);
            }
        }

        resetCamera();
    }

    public Vector3f getEyePos() {
        return eyePos;
    }

    public Vector3f getLookAt() {
        return lookAt;
    }

    public Vector3f getWorldUp() {
        return worldUp;
    }

    public void setCameraLookAt(Vector3f eyePos, Vector3f lookAt, Vector3f worldUp) {
        this.eyePos.set(eyePos);
        this.lookAt.set(lookAt);
        this.worldUp.set(worldUp);
    }

    public void setCameraLookAt(Vector3f lookAt, double radius, double rotationPitch, double rotationYaw) {
        this.lookAt.set(lookAt);
        eyePos.set((float) Math.cos(rotationPitch), 0, (float) Math.sin(rotationPitch))
                .add(0, (float) (Math.tan(rotationYaw) * eyePos.length()), 0).normalize().mul((float) radius)
                .add(lookAt);
    }

    public void setupCamera() {
        int x = rect.x;
        int y = rect.y;
        int width = rect.z;
        int height = rect.w;

        Minecraft mc = Minecraft.getMinecraft();
        glPushAttrib(GL_ALL_ATTRIB_BITS);
        glPushClientAttrib(GL_ALL_CLIENT_ATTRIB_BITS);
        mc.entityRenderer.disableLightmap(0);
        glDisable(GL_LIGHTING);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);

        // setup viewport and clear GL buffers
        glViewport(x, y, width, height);

        clearView(x, y, width, height);

        // setup projection matrix to perspective
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();

        float aspectRatio = width / (height * 1.0f);
        GLU.gluPerspective(60.0f, aspectRatio, 0.1f, 10000.0f);

        // setup modelview matrix
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        GLU.gluLookAt(eyePos.x, eyePos.y, eyePos.z, lookAt.x, lookAt.y, lookAt.z, worldUp.x, worldUp.y, worldUp.z);
    }

    public static void setGlClearColorFromInt(int colorValue, int opacity) {
        int i = (colorValue & 16711680) >> 16;
        int j = (colorValue & 65280) >> 8;
        int k = (colorValue & 255);
        glClearColor(i / 255.0f, j / 255.0f, k / 255.0f, opacity / 255.0f);
    }

    protected void clearView(int x, int y, int width, int height) {
        setGlClearColorFromInt(backgroundColor, backgroundColor >> 24);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public static void resetCamera() {
        // reset viewport
        Minecraft minecraft = Minecraft.getMinecraft();
        glViewport(0, 0, minecraft.displayWidth, minecraft.displayHeight);

        // reset modelview matrix
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();

        // reset projection matrix
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();

        glMatrixMode(GL_MODELVIEW);

        // reset attributes
        glPopClientAttrib();
        glPopAttrib();
    }

    private double getDistanceSq(long coord) {
        int x = CoordinatePacker.unpackX(coord);
        int y = CoordinatePacker.unpackY(coord);
        int z = CoordinatePacker.unpackZ(coord);

        double xd = eyePos.x - x;
        double yd = eyePos.y - y;
        double zd = eyePos.z - z;

        return xd * xd + yd * yd + zd * zd;
    }

    protected void drawWorld() {
        if (beforeRender != null) {
            beforeRender.accept(this);
        }

        Minecraft mc = Minecraft.getMinecraft();
        glEnable(GL_CULL_FACE);
        glEnable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
        mc.entityRenderer.disableLightmap(0);
        mc.renderEngine.bindTexture(TextureMap.locationBlocksTexture);
        glDisable(GL_LIGHTING);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_ALPHA_TEST);

        if (!renderTranslucentBlocks.isEmpty()) {
            renderTranslucentBlocks.sort((a, b) -> -Double.compare(getDistanceSq(a), getDistanceSq(b)));
        }

        Tessellator tessellator = Tessellator.instance;
        renderBlocks(tessellator, renderedBlocks, false);
        renderBlocks(tessellator, renderTranslucentBlocks, true);

        if (onPostBlockRendered != null) {
            onPostBlockRendered.accept(this);
        }

        RenderHelper.enableStandardItemLighting();
        glEnable(GL_LIGHTING);

        // render TESR
        TileEntityRendererDispatcher tesr = TileEntityRendererDispatcher.instance;
        for (int pass = 0; pass < 2; pass++) {
            ForgeHooksClient.setRenderPass(pass);
            int finalPass = pass;
            renderedBlocks.forEach(pos -> {
                int x = CoordinatePacker.unpackX(pos);
                int y = CoordinatePacker.unpackY(pos);
                int z = CoordinatePacker.unpackZ(pos);
                setDefaultPassRenderState(finalPass);
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile != null && tesr.hasSpecialRenderer(tile)) {
                    if (tile.shouldRenderInPass(finalPass)) {
                        tesr.renderTileEntityAt(tile, x, y, z, 0);
                    }
                }
            });
        }
        ForgeHooksClient.setRenderPass(-1);
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
        glDepthMask(true);
    }

    public void renderBlocks(Tessellator tessellator, LongCollection blocksToRender, boolean transparent) {
        if (blocksToRender.isEmpty()) return;
        Minecraft mc = Minecraft.getMinecraft();
        final int savedAo = mc.gameSettings.ambientOcclusion;
        mc.gameSettings.ambientOcclusion = 0;
        tessellator.startDrawingQuads();
        try {
            if (transparent) {
                tessellator.setColorRGBA_F(1f, 1f, 1f, 0.3f);
                tessellator.disableColor();
            }
            tessellator.setBrightness(15 << 20 | 15 << 4);
            for (int i = 0; i < 2; i++) {
                for (long pos : blocksToRender) {
                    int x = CoordinatePacker.unpackX(pos);
                    int y = CoordinatePacker.unpackY(pos);
                    int z = CoordinatePacker.unpackZ(pos);
                    Block block = world.getBlock(x, y, z);
                    if (block.equals(Blocks.air) || !block.canRenderInPass(i)) continue;

                    bufferBuilder.blockAccess = world;
                    bufferBuilder.setRenderBounds(0, 0, 0, 1, 1, 1);
                    bufferBuilder.renderAllFaces = renderAllFaces;
                    bufferBuilder.renderBlockByRenderType(block, x, y, z);
                }
            }
            if (onRender != null) {
                onRender.accept(this);
            }
        } finally {
            mc.gameSettings.ambientOcclusion = savedAo;
            tessellator.draw();
            tessellator.setTranslation(0, 0, 0);
        }
    }

    public static void setDefaultPassRenderState(int pass) {
        glColor4f(1, 1, 1, 1);
        if (pass == 0) { // SOLID
            glEnable(GL_DEPTH_TEST);
            glDisable(GL_BLEND);
            glDepthMask(true);
        } else { // TRANSLUCENT
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glDepthMask(false);
        }
    }

    public boolean isInsideRect(int x, int y) {
        return x > rect.x() && x < rect.x() + rect.z() && y > rect.y() && y < rect.y() + rect.w();
    }

    public MovingObjectPosition rayTrace(Vector3f lookVec) {
        Vec3 startPos = Vec3.createVectorHelper(this.eyePos.x, this.eyePos.y, this.eyePos.z);
        lookVec.mul(100); // range: 100 Blocks
        Vec3 endPos = Vec3.createVectorHelper(
                (lookVec.x + startPos.xCoord),
                (lookVec.y + startPos.yCoord),
                (lookVec.z + startPos.zCoord));
        return this.world.rayTraceBlocksWithTargetMap(startPos, endPos, world.blockMap.keySet());
    }
}
