package blockrenderer6343.client.renderer;

import blockrenderer6343.api.utils.BlockPosition;
import blockrenderer6343.api.utils.Position;
import blockrenderer6343.api.utils.PositionedRect;
import blockrenderer6343.api.utils.Size;
import blockrenderer6343.client.utils.ProjectionUtils;
import blockrenderer6343.client.world.TrackedDummyWorld;
import codechicken.lib.vec.Vector3;
import com.github.bartimaeusnek.bartworks.common.blocks.BW_GlasBlocks;
import gregtech.common.render.GT_Renderer_Block;
import java.util.ArrayList;
import java.util.List;
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
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector3f;

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
    public final List<BlockPosition> renderedBlocks;
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
        renderedBlocks = new ArrayList<>();
    }

    public WorldSceneRenderer setBeforeWorldRender(Consumer<WorldSceneRenderer> callback) {
        this.beforeRender = callback;
        return this;
    }

    public WorldSceneRenderer setOnWorldRender(Consumer<WorldSceneRenderer> callback) {
        this.onRender = callback;
        return this;
    }

    public WorldSceneRenderer addRenderedBlocks(List<BlockPosition> blocks) {
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
     * Renders scene on given coordinates with given width and height, and RGB background color
     * Note that this will ignore any transformations applied currently to projection/view matrix,
     * so specified coordinates are scaled MC gui coordinates.
     * It will return matrices of projection and view in previous state after rendering
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
        if (onLookingAt != null
                && mouseX > positionedRect.position.x
                && mouseX < positionedRect.position.x + positionedRect.size.width
                && mouseY > positionedRect.position.y
                && mouseY < positionedRect.position.y + positionedRect.size.height) {
            Vector3f hitPos = ProjectionUtils.unProject(mouseX, mouseY);
            MovingObjectPosition result = rayTrace(hitPos);
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
        GlStateManager.pushAttrib();
        mc.entityRenderer.disableLightmap(0);
        GlStateManager.disableLighting();
        GlStateManager.enableDepth();
        GlStateManager.enableBlend();

        // setup viewport and clear GL buffers
        GlStateManager.viewport(x, y, width, height);

        clearView(x, y, width, height);

        // setup projection matrix to perspective
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();

        float aspectRatio = width / (height * 1.0f);
        GLU.gluPerspective(60.0f, aspectRatio, 0.1f, 10000.0f);

        // setup modelview matrix
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GLU.gluLookAt(eyePos.x, eyePos.y, eyePos.z, lookAt.x, lookAt.y, lookAt.z, worldUp.x, worldUp.y, worldUp.z);
    }

    public static void setGlClearColorFromInt(int colorValue, int opacity) {
        int i = (colorValue & 16711680) >> 16;
        int j = (colorValue & 65280) >> 8;
        int k = (colorValue & 255);
        GL11.glClearColor(i / 255.0f, j / 255.0f, k / 255.0f, opacity / 255.0f);
    }

    protected void clearView(int x, int y, int width, int height) {
        setGlClearColorFromInt(clearColor, clearColor >> 24);
        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    public static void resetCamera() {
        // reset viewport
        Minecraft minecraft = Minecraft.getMinecraft();
        GlStateManager.viewport(0, 0, minecraft.displayWidth, minecraft.displayHeight);

        // reset projection matrix
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();

        // reset modelview matrix
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();

        GlStateManager.disableBlend();
        GlStateManager.disableDepth();

        // reset attributes
        GlStateManager.popAttrib();
    }

    protected void drawWorld() {
        if (beforeRender != null) {
            beforeRender.accept(this);
        }

        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.enableCull();
        GlStateManager.enableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        mc.entityRenderer.disableLightmap(0);
        mc.renderEngine.bindTexture(TextureMap.locationBlocksTexture);
        GlStateManager.disableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();

        Tessellator.instance.startDrawingQuads();
        Tessellator.instance.setBrightness(15 << 20 | 15 << 4);
        for (BlockPosition pos : renderedBlocks) {
            Block block = world.getBlock(pos.x, pos.y, pos.z);
            if (block.equals(Blocks.air)) continue;
            block.setLightLevel(15);

            RenderBlocks bufferBuilder = new RenderBlocks();
            bufferBuilder.blockAccess = world;
            bufferBuilder.setRenderBounds(0, 0, 0, 1, 1, 1);
            bufferBuilder.renderAllFaces = renderAllFaces;
            if (block instanceof BW_GlasBlocks) {
                // this mod cannot render renderpass = 1 blocks for now
                bufferBuilder.renderStandardBlockWithColorMultiplier(
                        block,
                        pos.x,
                        pos.y,
                        pos.z,
                        ((BW_GlasBlocks) block).getColor(world.getBlockMetadata(pos.x, pos.y, pos.z))[0] / 255f,
                        ((BW_GlasBlocks) block).getColor(world.getBlockMetadata(pos.x, pos.y, pos.z))[1] / 255f,
                        ((BW_GlasBlocks) block).getColor(world.getBlockMetadata(pos.x, pos.y, pos.z))[2] / 255f);
            } else if (!GT_Renderer_Block.INSTANCE.renderWorldBlock(
                    world, pos.x, pos.y, pos.z, block, block.getRenderType(), bufferBuilder)) {
                bufferBuilder.renderBlockByRenderType(block, pos.x, pos.y, pos.z);
            }
        }
        if (onRender != null) {
            onRender.accept(this);
        }

        Tessellator.instance.draw();
        Tessellator.instance.setTranslation(0, 0, 0);

        RenderHelper.enableStandardItemLighting();
        GlStateManager.enableLighting();

        // render TESR
        for (int pass = 0; pass < 2; pass++) {
            ForgeHooksClient.setRenderPass(pass);
            int finalPass = pass;
            renderedBlocks.forEach(blockPosition -> {
                setDefaultPassRenderState(finalPass);
                TileEntity tile = world.getTileEntity(blockPosition.x, blockPosition.y, blockPosition.z);
                if (tile != null) {
                    if (tile.shouldRenderInPass(finalPass)) {
                        TileEntityRendererDispatcher.instance.renderTileEntity(tile, 0);
                    }
                }
            });
        }
        ForgeHooksClient.setRenderPass(-1);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
    }

    public static void setDefaultPassRenderState(int pass) {
        GlStateManager.color(1, 1, 1, 1);
        if (pass == 0) { // SOLID
            GlStateManager.enableDepth();
            GlStateManager.disableBlend();
            GlStateManager.depthMask(true);
        } else { // TRANSLUCENT
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.depthMask(false);
        }
    }

    public MovingObjectPosition rayTrace(Vector3f hitPos) {
        Vec3 startPos = Vec3.createVectorHelper(this.eyePos.x, this.eyePos.y, this.eyePos.z);
        hitPos.scale(2); // Double view range to ensure pos can be seen.
        Vec3 endPos = Vec3.createVectorHelper(
                (hitPos.x - startPos.xCoord), (hitPos.y - startPos.yCoord), (hitPos.z - startPos.zCoord));
        return ((TrackedDummyWorld) this.world).rayTraceBlockswithTargetMap(startPos, endPos, renderedBlocks);
    }

    /***
     * For better performance, You'd better handle the event setOnLookingAt(Consumer) or getLastTraceResult()
     * @param mouseX xPos in Texture
     * @param mouseY yPos in Texture
     * @return RayTraceResult Hit
     */
    protected MovingObjectPosition screenPos2BlockPosFace(int mouseX, int mouseY, int x, int y, int width, int height) {
        // render a frame
        GlStateManager.enableDepth();
        setupCamera(getPositionedRect(x, y, width, height));

        drawWorld();

        Vector3f hitPos = ProjectionUtils.unProject(mouseX, mouseY);
        MovingObjectPosition result = rayTrace(hitPos);

        resetCamera();

        return result;
    }

    /***
     * For better performance, You'd better do project in setOnWorldRender(Consumer)
     * @param pos BlockPos
     * @param depth should pass Depth Test
     * @return x, y, z
     */
    protected Vector3f blockPos2ScreenPos(BlockPosition pos, boolean depth, int x, int y, int width, int height) {
        // render a frame
        GlStateManager.enableDepth();
        setupCamera(getPositionedRect(x, y, width, height));

        drawWorld();
        Vector3f winPos = ProjectionUtils.project(pos);

        resetCamera();

        return winPos;
    }
}
