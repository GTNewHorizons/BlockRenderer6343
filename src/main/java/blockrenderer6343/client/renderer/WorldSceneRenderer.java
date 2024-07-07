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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
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
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;

import org.lwjgl.opengl.GL12;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector3f;

import com.github.bartimaeusnek.bartworks.common.blocks.BW_GlasBlocks;

import blockrenderer6343.BlockRenderer6343;
import blockrenderer6343.api.utils.BlockPosition;
import blockrenderer6343.api.utils.Position;
import blockrenderer6343.api.utils.PositionedRect;
import blockrenderer6343.api.utils.Size;
import blockrenderer6343.client.utils.ProjectionUtils;
import blockrenderer6343.client.world.TrackedDummyWorld;
import codechicken.lib.vec.Vector3;
import gregtech.common.render.GT_Renderer_Block;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: KilaBash, backported by Quarri6343
 * @Date: 2021/08/23
 * @Description: Abstract class, and extend a lot of features compared with the original one.
 */
public abstract class WorldSceneRenderer {

    // you have to place blocks in the world before use
    public final World world;
    // the Blocks which this renderer needs to render
    public final Set<BlockPosition> renderedBlocks = new HashSet<>();
    private Consumer<WorldSceneRenderer> beforeRender;
    private Consumer<WorldSceneRenderer> onRender;
    private Consumer<MovingObjectPosition> onLookingAt;
    private int clearColor;
    private MovingObjectPosition lastTraceResult;
    private Vector3f eyePos = new Vector3f(0, 0, -10f);
    private Vector3f lookAt = new Vector3f(0, 0, 0);
    private Vector3f worldUp = new Vector3f(0, 1, 0);
    private boolean renderAllFaces = false;

    public WorldSceneRenderer(World world) {
        this.world = world;
    }

    public WorldSceneRenderer setBeforeWorldRender(Consumer<WorldSceneRenderer> callback) {
        this.beforeRender = callback;
        return this;
    }

    public WorldSceneRenderer setOnWorldRender(Consumer<WorldSceneRenderer> callback) {
        this.onRender = callback;
        return this;
    }

    public WorldSceneRenderer addRenderedBlocks(Collection<BlockPosition> blocks) {
        if (blocks != null) {
            this.renderedBlocks.addAll(blocks);
        }
        return this;
    }

    public WorldSceneRenderer setOnLookingAt(Consumer<MovingObjectPosition> onLookingAt) {
        this.onLookingAt = onLookingAt;
        return this;
    }

    public void setRenderAllFaces(boolean renderAllFaces) {
        this.renderAllFaces = renderAllFaces;
    }

    public void setClearColor(int clearColor) {
        this.clearColor = clearColor;
    }

    public MovingObjectPosition getLastTraceResult() {
        return lastTraceResult;
    }

    /**
     * Renders scene on given coordinates with given width and height, and RGB background color Note that this will
     * ignore any transformations applied currently to projection/view matrix, so specified coordinates are scaled MC
     * gui coordinates. It will return matrices of projection and view in previous state after rendering
     */
    public void render(int x, int y, int width, int height, int mouseX, int mouseY) {
        PositionedRect positionedRect = getPositionedRect(x, y, width, height);
        PositionedRect mouse = getPositionedRect(mouseX, mouseY, 0, 0);
        mouseX = mouse.position.x;
        mouseY = mouse.position.y;
        // setupCamera
        setupCamera(positionedRect);

        // render TrackedDummyWorld
        drawWorld();

        // check lookingAt
        this.lastTraceResult = null;
        if (onLookingAt != null && mouseX > positionedRect.position.x
                && mouseX < positionedRect.position.x + positionedRect.size.width
                && mouseY > positionedRect.position.y
                && mouseY < positionedRect.position.y + positionedRect.size.height) {
            Vector3f lookVec = ProjectionUtils.unProject(positionedRect, eyePos, lookAt, mouseX, mouseY);
            MovingObjectPosition result = rayTrace(lookVec);
            if (result != null) {
                this.lastTraceResult = result;
                onLookingAt.accept(result);
            }
        }

        // resetcamera
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
        this.eyePos = eyePos;
        this.lookAt = lookAt;
        this.worldUp = worldUp;
    }

    public void setCameraLookAt(Vector3f lookAt, double radius, double rotationPitch, double rotationYaw) {
        this.lookAt = lookAt;
        Vector3 vecX = new Vector3(Math.cos(rotationPitch), 0, Math.sin(rotationPitch));
        Vector3 vecY = new Vector3(0, Math.tan(rotationYaw) * vecX.mag(), 0);
        Vector3 pos = vecX.copy().add(vecY).normalize().multiply(radius);
        this.eyePos = pos.add(lookAt.x, lookAt.y, lookAt.z).vector3f();
    }

    protected PositionedRect getPositionedRect(int x, int y, int width, int height) {
        return new PositionedRect(new Position(x, y), new Size(width, height));
    }

    public void setupCamera(PositionedRect positionedRect) {
        int x = positionedRect.getPosition().x;
        int y = positionedRect.getPosition().y;
        int width = positionedRect.getSize().width;
        int height = positionedRect.getSize().height;

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
        setGlClearColorFromInt(clearColor, clearColor >> 24);
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

        final int savedAo = mc.gameSettings.ambientOcclusion;
        mc.gameSettings.ambientOcclusion = 0;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        try {
            tessellator.setBrightness(15 << 20 | 15 << 4);
            for (BlockPosition pos : renderedBlocks) {
                Block block = world.getBlock(pos.x, pos.y, pos.z);
                if (block.equals(Blocks.air)) continue;

                RenderBlocks bufferBuilder = new RenderBlocks();
                bufferBuilder.blockAccess = world;
                bufferBuilder.setRenderBounds(0, 0, 0, 1, 1, 1);
                bufferBuilder.renderAllFaces = renderAllFaces;
                if (BlockRenderer6343.isBartworksLoaded && block instanceof BW_GlasBlocks bwGlass) {
                    // this mod cannot render renderpass = 1 blocks for now
                    bufferBuilder.renderStandardBlockWithColorMultiplier(
                            block,
                            pos.x,
                            pos.y,
                            pos.z,
                            bwGlass.getColor(world.getBlockMetadata(pos.x, pos.y, pos.z))[0] / 255f,
                            bwGlass.getColor(world.getBlockMetadata(pos.x, pos.y, pos.z))[1] / 255f,
                            bwGlass.getColor(world.getBlockMetadata(pos.x, pos.y, pos.z))[2] / 255f);
                } else if (BlockRenderer6343.isGTLoaded) {
                    if (!GT_Renderer_Block.INSTANCE.renderWorldBlock(
                            world,
                            pos.x,
                            pos.y,
                            pos.z,
                            block,
                            block.getRenderType(),
                            bufferBuilder)) {
                        bufferBuilder.renderBlockByRenderType(block, pos.x, pos.y, pos.z);
                    }
                } else {
                    bufferBuilder.renderBlockByRenderType(block, pos.x, pos.y, pos.z);
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

        RenderHelper.enableStandardItemLighting();
        glEnable(GL_LIGHTING);

        // render TESR
        TileEntityRendererDispatcher tesr = TileEntityRendererDispatcher.instance;
        for (int pass = 0; pass < 2; pass++) {
            ForgeHooksClient.setRenderPass(pass);
            int finalPass = pass;
            renderedBlocks.forEach(blockPosition -> {
                setDefaultPassRenderState(finalPass);
                TileEntity tile = world.getTileEntity(blockPosition.x, blockPosition.y, blockPosition.z);
                if (tile != null && tesr.hasSpecialRenderer(tile)) {
                    if (tile.shouldRenderInPass(finalPass)) {
                        tesr.renderTileEntityAt(tile, blockPosition.x, blockPosition.y, blockPosition.z, 0);
                    }
                }
            });
        }
        ForgeHooksClient.setRenderPass(-1);
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
        glDepthMask(true);
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

    public MovingObjectPosition rayTrace(Vector3f lookVec) {
        Vec3 startPos = Vec3.createVectorHelper(this.eyePos.x, this.eyePos.y, this.eyePos.z);
        lookVec.scale(100); // range: 100 Blocks
        Vec3 endPos = Vec3.createVectorHelper(
                (lookVec.x + startPos.xCoord),
                (lookVec.y + startPos.yCoord),
                (lookVec.z + startPos.zCoord));
        return ((TrackedDummyWorld) this.world).rayTraceBlockswithTargetMap(startPos, endPos, renderedBlocks);
    }
}
