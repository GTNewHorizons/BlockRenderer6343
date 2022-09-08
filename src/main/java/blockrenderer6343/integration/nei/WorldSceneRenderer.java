package blockrenderer6343.integration.nei;

import codechicken.lib.vec.Vector3;
import codechicken.nei.NEIClientUtils;
import blockrenderer6343.mixins.GuiContainerMixin;
import gregtech.common.render.GT_Renderer_Block;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.function.Consumer;

public class WorldSceneRenderer {

    private static final FloatBuffer MODELVIEW_MATRIX_BUFFER = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private static final FloatBuffer PROJECTION_MATRIX_BUFFER = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private static final IntBuffer VIEWPORT_BUFFER = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
    protected static final FloatBuffer PIXEL_DEPTH_BUFFER = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    protected static final FloatBuffer OBJECT_POS_BUFFER = ByteBuffer.allocateDirect(3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

    public final World world;
    //the Blocks which this renderer needs to render
    public final List<BlockPosition> renderedBlocks;
    private Consumer<WorldSceneRenderer> beforeRender;
    private Consumer<WorldSceneRenderer> afterRender;
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

    public WorldSceneRenderer setAfterWorldRender(Consumer<WorldSceneRenderer> callback) {
        this.afterRender = callback;
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

    public void setRenderAllFaces(boolean renderAllFaces){
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
        // setupCamera
        setupCamera(x, y, width, height);

        // render TrackedDummyWorld
        drawWorld();

        // check lookingAt
        Minecraft mc = Minecraft.getMinecraft();
        int k = (NEIClientUtils.getGuiContainer().width - ((GuiContainerMixin)NEIClientUtils.getGuiContainer()).getXSize()) / 2;
        int l = (NEIClientUtils.getGuiContainer().height - ((GuiContainerMixin)NEIClientUtils.getGuiContainer()).getYSize()) / 2;
        int screenMouseX = (int) (mouseX / (NEIClientUtils.getGuiContainer().width * 1.0) * mc.displayWidth);
        int screenMouseY = mc.displayHeight - (int) (mouseY / (NEIClientUtils.getGuiContainer().height * 1.0) * mc.displayHeight);
        this.lastTraceResult = null;
        if (onLookingAt != null && mouseX > x + k && mouseX < x + k + width
            && mouseY > y + l && mouseY < y + l + height) {
            Vector3f hitPos = unProject(screenMouseX, screenMouseY);
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
        Vector3 vecY = new Vector3(0, Math.tan(rotationYaw) * vecX.mag(),0);
        Vector3 pos = vecX.copy().add(vecY).normalize().multiply(radius);
        this.eyePos = pos.add(lookAt.x, lookAt.y, lookAt.z).vector3f();
    }

    public void setupCamera(int x, int y, int width, int height) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);

        GlStateManager.pushAttrib();
        mc.entityRenderer.disableLightmap(0);
        GlStateManager.disableLighting();
        GlStateManager.enableDepth();
        GlStateManager.enableBlend();

        //translate gui coordinates to window ones (y is inverted)
        int k = (NEIClientUtils.getGuiContainer().width - ((GuiContainerMixin)NEIClientUtils.getGuiContainer()).getXSize()) / 2;
        int l = (NEIClientUtils.getGuiContainer().height - ((GuiContainerMixin)NEIClientUtils.getGuiContainer()).getYSize()) / 2;
        int windowX = (int) ((x + k) / (NEIClientUtils.getGuiContainer().width * 1.0) * mc.displayWidth);
        int windowWidth = (int) (Math.min(width, NEIClientUtils.getGuiContainer().width) / (NEIClientUtils.getGuiContainer().width* 1.0) * mc.displayWidth);
        int windowHeight = (int) (Math.min(height, NEIClientUtils.getGuiContainer().height) / (NEIClientUtils.getGuiContainer().height * 1.0) * mc.displayHeight);
        int windowY = mc.displayHeight - (int) ((y + l) / (NEIClientUtils.getGuiContainer().height * 1.0) * mc.displayHeight) - windowHeight;
        //setup viewport and clear GL buffers
        GlStateManager.viewport(windowX, windowY, windowWidth, windowHeight);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(windowX, windowY, windowWidth, windowHeight);
        clearView(x, y, width, height);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        //setup projection matrix to perspective
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();

        float aspectRatio = width / (height * 1.0f);
        GLU.gluPerspective(60.0f, aspectRatio, 0.1f, 10000.0f);

        //setup modelview matrix
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
        //reset viewport
        Minecraft minecraft = Minecraft.getMinecraft();
        GlStateManager.viewport(0, 0, minecraft.displayWidth, minecraft.displayHeight);

        //reset projection matrix
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();

        //reset modelview matrix
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();

        GlStateManager.disableBlend();
        GlStateManager.disableDepth();

        //reset attributes
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
            if (block.equals(Blocks.air))
                continue;
            block.setLightLevel(15);

            RenderBlocks bufferBuilder = new RenderBlocks();
            bufferBuilder.blockAccess = world;
            bufferBuilder.setRenderBounds(0, 0, 0, 1, 1, 1);
            bufferBuilder.renderAllFaces = renderAllFaces;
            if(world.getTileEntity(pos.x,pos.y,pos.z) != null){
                GT_Renderer_Block.INSTANCE.renderWorldBlock(world, pos.x, pos.y, pos.z, block, 0, bufferBuilder);
            }
            else{
                bufferBuilder.renderStandardBlock(block, pos.x,pos.y,pos.z);
            }
        }
        Tessellator.instance.draw();
        Tessellator.instance.setTranslation(0, 0, 0);

        RenderHelper.enableStandardItemLighting();
        GlStateManager.enableLighting();

        // render TESR
        for (int pass = 0; pass < 2; pass++) {
            ForgeHooksClient.setRenderPass(pass);
            int finalPass = pass;
            renderedBlocks.forEach(blockPosition ->{
                setDefaultPassRenderState(finalPass);
                TileEntity tile = world.getTileEntity(blockPosition.x,blockPosition.y,blockPosition.z);
                if (tile != null) {
                    if (tile.shouldRenderInPass(finalPass)) {
                        TileEntityRendererDispatcher.instance.renderTileEntity(tile,0);
                    }
                }
            });
        }
        ForgeHooksClient.setRenderPass(-1);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);

        if (afterRender != null) {
            afterRender.accept(this);
        }
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
        Vec3 endPos = Vec3.createVectorHelper((hitPos.x - startPos.xCoord), (hitPos.y - startPos.yCoord), (hitPos.z - startPos.zCoord));
        return ((TrackedDummyWorld)this.world).rayTraceBlockswithTargetMap(startPos, endPos, renderedBlocks);
    }

    public Vector3f project(BlockPosition pos) {
        //read current rendering parameters
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODELVIEW_MATRIX_BUFFER);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION_MATRIX_BUFFER);
        GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT_BUFFER);

        //rewind buffers after write by OpenGL glGet calls
        MODELVIEW_MATRIX_BUFFER.rewind();
        PROJECTION_MATRIX_BUFFER.rewind();
        VIEWPORT_BUFFER.rewind();

        //call gluProject with retrieved parameters
        GLU.gluProject(pos.x + 0.5f, pos.y + 0.5f, pos.z + 0.5f, MODELVIEW_MATRIX_BUFFER, PROJECTION_MATRIX_BUFFER, VIEWPORT_BUFFER, OBJECT_POS_BUFFER);

        //rewind buffers after read by gluProject
        VIEWPORT_BUFFER.rewind();
        PROJECTION_MATRIX_BUFFER.rewind();
        MODELVIEW_MATRIX_BUFFER.rewind();

        //rewind buffer after write by gluProject
        OBJECT_POS_BUFFER.rewind();

        //obtain position in Screen
        float winX = OBJECT_POS_BUFFER.get();
        float winY = OBJECT_POS_BUFFER.get();
        float winZ = OBJECT_POS_BUFFER.get();

        //rewind buffer after read
        OBJECT_POS_BUFFER.rewind();

        return new Vector3f(winX, winY, winZ);
    }

    public Vector3f unProject(int mouseX, int mouseY) {
        //read depth of pixel under mouse
        GL11.glReadPixels(mouseX, mouseY, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, PIXEL_DEPTH_BUFFER);

        //rewind buffer after write by glReadPixels
        PIXEL_DEPTH_BUFFER.rewind();

        //retrieve depth from buffer (0.0-1.0f)
        float pixelDepth = PIXEL_DEPTH_BUFFER.get();

        //rewind buffer after read
        PIXEL_DEPTH_BUFFER.rewind();

        //read current rendering parameters
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODELVIEW_MATRIX_BUFFER);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION_MATRIX_BUFFER);
        GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT_BUFFER);

        //rewind buffers after write by OpenGL glGet calls
        MODELVIEW_MATRIX_BUFFER.rewind();
        PROJECTION_MATRIX_BUFFER.rewind();
        VIEWPORT_BUFFER.rewind();

        //call gluUnProject with retrieved parameters
        GLU.gluUnProject(mouseX, mouseY, pixelDepth, MODELVIEW_MATRIX_BUFFER, PROJECTION_MATRIX_BUFFER, VIEWPORT_BUFFER, OBJECT_POS_BUFFER);

        //rewind buffers after read by gluUnProject
        VIEWPORT_BUFFER.rewind();
        PROJECTION_MATRIX_BUFFER.rewind();
        MODELVIEW_MATRIX_BUFFER.rewind();

        //rewind buffer after write by gluUnProject
        OBJECT_POS_BUFFER.rewind();

        //obtain absolute position in world
        float posX = OBJECT_POS_BUFFER.get();
        float posY = OBJECT_POS_BUFFER.get();
        float posZ = OBJECT_POS_BUFFER.get();

        //rewind buffer after read
        OBJECT_POS_BUFFER.rewind();
        return new Vector3f(posX, posY, posZ);
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
        setupCamera(x, y, width, height);

        drawWorld();

        Vector3f hitPos = unProject(mouseX, mouseY);
        MovingObjectPosition result = rayTrace(hitPos);

        resetCamera();

        return result;
    }

    /***
     * For better performance, You'd better do project in setAfterWorldRender(Consumer)
     * @param pos BlockPos
     * @param depth should pass Depth Test
     * @return x, y, z
     */
    protected Vector3f blockPos2ScreenPos(BlockPosition pos, boolean depth, int x, int y, int width, int height){
        // render a frame
        GlStateManager.enableDepth();
        setupCamera(x, y, width, height);

        drawWorld();
        Vector3f winPos = project(pos);

        resetCamera();

        return winPos;
    }
}
