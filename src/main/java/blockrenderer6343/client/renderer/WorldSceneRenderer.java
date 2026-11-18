package blockrenderer6343.client.renderer;

import static org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS;
import static org.lwjgl.opengl.GL11.GL_ALL_CLIENT_ATTRIB_BITS;
import static org.lwjgl.opengl.GL11.GL_ALPHA_TEST;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_GREATER;
import static org.lwjgl.opengl.GL11.GL_LIGHTING;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glAlphaFunc;
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

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.MinecraftForgeClient;

import org.joml.Vector3f;
import org.joml.Vector4i;
import org.lwjgl.opengl.GL12;
import org.lwjgl.util.glu.GLU;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;
import com.mojang.authlib.GameProfile;

import blockrenderer6343.BlockRenderer6343;
import blockrenderer6343.client.utils.ColorUtils;
import blockrenderer6343.client.utils.ProjectionUtils;
import blockrenderer6343.client.utils.RevisionedLongSet;
import blockrenderer6343.client.world.ClientFakePlayer;
import blockrenderer6343.client.world.TrackedDummyWorld;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
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

    // you have to place blocks in the world before use
    public final TrackedDummyWorld world;
    // the Blocks which this renderer needs to render
    public final LongSet renderedBlocks = new RevisionedLongSet();
    public final LongArrayList renderTranslucentBlocks = new LongArrayList();
    private Consumer<WorldSceneRenderer> beforeRender;
    private Consumer<WorldSceneRenderer> onRender;
    private Consumer<MovingObjectPosition> onLookingAt;
    private Consumer<WorldSceneRenderer> onPostBlockRendered;
    private MovingObjectPosition lastTraceResult;
    private final LongArrayList translucentCandidates = new LongArrayList();
    private final TranslucentBlockSorter translucentSorter = new TranslucentBlockSorter();
    private final TranslucentBlockSorter ghostSorter = new TranslucentBlockSorter();
    private final ArrayList<TileEntity> visibleTiles = new ArrayList<>();
    private final LongSet[] failedBlockPasses = { new LongOpenHashSet(), new LongOpenHashSet() };
    private final LongSet[] failedTilePasses = { new LongOpenHashSet(), new LongOpenHashSet() };
    private long failureRevision = Long.MIN_VALUE;
    private final ClientFakePlayer previewViewer;
    private int clearedRenderedBlockCount;
    private int clearedTranslucentBlockCount;
    private final Vector3f eyePos = new Vector3f(0, 0, -10f);
    private final Vector3f lookAt = new Vector3f(0, 0, 0);
    private final Vector3f worldUp = new Vector3f(0, 1, 0);
    protected Vector4i rect = new Vector4i();
    private boolean renderAllFaces = false;
    private final RenderBlocks bufferBuilder = new RenderBlocks();
    private Predicate<Block> staticBlockPredicate = block -> false;
    private final LongArrayList staticOpaqueBlocks = new LongArrayList();
    private final LongArrayList dynamicBlocks = new LongArrayList();
    private long opaqueWorldRevision = Long.MIN_VALUE;
    private long opaqueSelectionRevision = Long.MIN_VALUE;
    private long geometryRevision;

    public WorldSceneRenderer(TrackedDummyWorld world) {
        this.world = world;
        previewViewer = new ClientFakePlayer(world, new GameProfile(new UUID(0, 0), "MultiblockViewer"));
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
        if (this.renderAllFaces != renderAllFaces) opaqueWorldRevision = Long.MIN_VALUE;
        this.renderAllFaces = renderAllFaces;
    }

    public void setStaticBlockPredicate(Predicate<Block> predicate) {
        staticBlockPredicate = predicate;
        opaqueWorldRevision = Long.MIN_VALUE;
    }

    public MovingObjectPosition getLastTraceResult() {
        return lastTraceResult;
    }

    public MovingObjectPosition pick(int mouseX, int mouseY) {
        if (rect.z <= 0 || rect.w <= 0 || !isInsideRect(mouseX, mouseY)) return null;
        return rayTrace(ProjectionUtils.unProject(rect, eyePos, lookAt, mouseX, mouseY));
    }

    public void resetRenderedBlocks() {
        if (!renderedBlocks.isEmpty()) {
            clearedRenderedBlockCount = renderedBlocks.size();
        }
        if (!renderTranslucentBlocks.isEmpty()) {
            clearedTranslucentBlockCount = renderTranslucentBlocks.size();
        }
        renderedBlocks.clear();
        renderTranslucentBlocks.clear();
        translucentCandidates.clear();
        translucentSorter.clear();
        ghostSorter.clear();
        staticOpaqueBlocks.clear();
        dynamicBlocks.clear();
        opaqueWorldRevision = Long.MIN_VALUE;
        clearRenderFailures();
        lastTraceResult = null;
    }

    public void trimIfOversized() {
        if ((long) renderedBlocks.size() * 4L + 64L < clearedRenderedBlockCount) {
            ((RevisionedLongSet) renderedBlocks).trim();
            translucentCandidates.trim();
            translucentSorter.trim();
            visibleTiles.trimToSize();
            staticOpaqueBlocks.trim();
            dynamicBlocks.trim();
            clearedRenderedBlockCount = renderedBlocks.size();
        }
        if ((long) renderTranslucentBlocks.size() * 4L + 64L < clearedTranslucentBlockCount) {
            renderTranslucentBlocks.trim();
            ghostSorter.trim();
            clearedTranslucentBlockCount = renderTranslucentBlocks.size();
        }
    }

    /**
     * Renders scene on given coordinates with given width and height, and RGB background color Note that this will
     * ignore any transformations applied currently to projection/view matrix, so specified coordinates are scaled MC
     * gui coordinates. It will return matrices of projection and view in previous state after rendering
     */
    public void render(int x, int y, int width, int height, int mouseX, int mouseY) {
        if (width <= 0 || height <= 0) {
            lastTraceResult = null;
            return;
        }
        rect.set(x, y, width, height);
        // setupCamera
        setupCamera();

        try {
            updatePreviewViewer();
            try (PreviewRenderContext ignored = new PreviewRenderContext(world, previewViewer)) {
                drawWorld();

                this.lastTraceResult = null;
                if (onLookingAt != null && isInsideRect(mouseX, mouseY)) {
                    MovingObjectPosition result = rayTrace(
                            ProjectionUtils.unProject(rect, eyePos, lookAt, mouseX, mouseY));
                    if (result != null) {
                        this.lastTraceResult = result;
                        onLookingAt.accept(result);
                    }
                }
            }
        } finally {
            resetCamera();
        }
    }

    private void updatePreviewViewer() {
        previewViewer.setPosition(eyePos.x, eyePos.y, eyePos.z);
        previewViewer.prevPosX = previewViewer.lastTickPosX = eyePos.x;
        previewViewer.prevPosY = previewViewer.lastTickPosY = eyePos.y;
        previewViewer.prevPosZ = previewViewer.lastTickPosZ = eyePos.z;
        double dx = lookAt.x - eyePos.x;
        double dy = lookAt.y - eyePos.y;
        double dz = lookAt.z - eyePos.z;
        previewViewer.prevRotationYaw = previewViewer.rotationYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        previewViewer.prevRotationPitch = previewViewer.rotationPitch = (float) -Math
                .toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        Minecraft mc = Minecraft.getMinecraft();
        previewViewer.ticksExisted = mc.thePlayer == null ? 0 : mc.thePlayer.ticksExisted;
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
        glDepthMask(true);
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
        int bg = ColorUtils.neiBackground.getColor();
        setGlClearColorFromInt(bg, bg >> 24);
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
        if (beforeRender != null) beforeRender.accept(this);
        if (failureRevision != world.getRevision()) {
            clearRenderFailures();
            failureRevision = world.getRevision();
        }

        Minecraft mc = Minecraft.getMinecraft();
        Tessellator tessellator = Tessellator.instance;
        int savedAo = mc.gameSettings.ambientOcclusion;
        int previousPass = MinecraftForgeClient.getRenderPass();
        int previousWorldPass = ForgeHooksClient.getWorldRenderPass();
        mc.gameSettings.ambientOcclusion = 0;
        try {
            collectVisibleTiles();
            collectOpaqueBlocks();
            if (!staticOpaqueBlocks.isEmpty()) {
                prepareBlockState(0);
                PreviewGeometryCache.getInstance().render(
                        this,
                        geometryRevision,
                        () -> renderBlockPass(tessellator, staticOpaqueBlocks, false, 0));
            }
            renderBlockPass(tessellator, dynamicBlocks, false, 0);
            renderTileEntityPass(0);
            collectTranslucentBlocks(dynamicBlocks);
            renderBlockPass(tessellator, translucentSorter.sort(translucentCandidates, eyePos, lookAt), false, 1);
            renderTileEntityPass(1);
            renderBlocks(tessellator, ghostSorter.sort(renderTranslucentBlocks, eyePos, lookAt), true);
            renderSelectionOverlay(tessellator);
            if (onPostBlockRendered != null) onPostBlockRendered.accept(this);
        } finally {
            visibleTiles.clear();
            bufferBuilder.blockAccess = null;
            ForgeHooksClient.setRenderPass(previousPass);
            ForgeHooksClient.worldRenderPass = previousWorldPass;
            mc.gameSettings.ambientOcclusion = savedAo;
            tessellator.setTranslation(0, 0, 0);
        }
    }

    private void clearRenderFailures() {
        for (LongSet failures : failedBlockPasses) failures.clear();
        for (LongSet failures : failedTilePasses) failures.clear();
    }

    private void collectOpaqueBlocks() {
        long worldRevision = world.getRevision();
        long selectionRevision = ((RevisionedLongSet) renderedBlocks).getRevision();
        if (opaqueWorldRevision == worldRevision && opaqueSelectionRevision == selectionRevision) return;
        staticOpaqueBlocks.clear();
        dynamicBlocks.clear();
        ForgeHooksClient.setRenderPass(0);
        ForgeHooksClient.worldRenderPass = 0;
        for (LongIterator iterator = renderedBlocks.iterator(); iterator.hasNext();) {
            long pos = iterator.nextLong();
            Block block = world.blockMap.get(pos);
            if (block == null) continue;
            boolean cacheable = false;
            try {
                cacheable = !world.tileMap.containsKey(pos) && !block.hasTileEntity(world.blockMetaMap.get(pos))
                        && staticBlockPredicate.test(block)
                        && block.getRenderBlockPass() == 0
                        && !block.canRenderInPass(1);
            } catch (RuntimeException | LinkageError failure) {
                // Keep unsupported blocks on the normal rendering path and its failure handling.
            }
            if (cacheable) staticOpaqueBlocks.add(pos);
            else dynamicBlocks.add(pos);
        }
        opaqueWorldRevision = worldRevision;
        opaqueSelectionRevision = selectionRevision;
        geometryRevision++;
    }

    private void collectVisibleTiles() {
        visibleTiles.clear();
        TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
        for (Long2ObjectMap.Entry<TileEntity> entry : Long2ObjectMaps.fastIterable(world.tileMap)) {
            TileEntity tile = entry.getValue();
            long pos = entry.getLongKey();
            if (tile == null || tile.isInvalid()
                    || !renderedBlocks.contains(pos)
                    || (failedTilePasses[0].contains(pos) && failedTilePasses[1].contains(pos)))
                continue;
            try {
                if (dispatcher.hasSpecialRenderer(tile)) visibleTiles.add(tile);
            } catch (RuntimeException | LinkageError failure) {
                reportRenderFailure(failedTilePasses[0], pos, tile, 0, failure);
                failedTilePasses[1].add(pos);
            }
        }
    }

    private void collectTranslucentBlocks(LongCollection positions) {
        translucentCandidates.clear();
        ForgeHooksClient.setRenderPass(1);
        ForgeHooksClient.worldRenderPass = 1;
        for (LongIterator iterator = positions.iterator(); iterator.hasNext();) {
            long pos = iterator.nextLong();
            if (failedBlockPasses[1].contains(pos)) continue;
            Block block = world.blockMap.get(pos);
            if (block == null) continue;
            try {
                if (block.canRenderInPass(1)) translucentCandidates.add(pos);
            } catch (RuntimeException | LinkageError failure) {
                reportRenderFailure(failedBlockPasses[1], pos, block, 1, failure);
            }
        }
    }

    private void renderTileEntityPass(int pass) {
        if (visibleTiles.isEmpty()) return;
        ForgeHooksClient.setRenderPass(pass);
        ForgeHooksClient.worldRenderPass = pass;
        TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
        for (TileEntity tile : visibleTiles) {
            long pos = CoordinatePacker.pack(tile.xCoord, tile.yCoord, tile.zCoord);
            if (tile.isInvalid() || failedTilePasses[pass].contains(pos)) continue;
            try {
                if (!tile.shouldRenderInPass(pass)) continue;
            } catch (RuntimeException | LinkageError failure) {
                reportRenderFailure(failedTilePasses[pass], pos, tile, pass, failure);
                continue;
            }
            glPushAttrib(GL_ALL_ATTRIB_BITS);
            glPushClientAttrib(GL_ALL_CLIENT_ATTRIB_BITS);
            glMatrixMode(GL_PROJECTION);
            glPushMatrix();
            glMatrixMode(GL_MODELVIEW);
            glPushMatrix();
            float brightnessX = OpenGlHelper.lastBrightnessX;
            float brightnessY = OpenGlHelper.lastBrightnessY;
            try {
                setDefaultPassRenderState(pass);
                RenderHelper.enableStandardItemLighting();
                glEnable(GL12.GL_RESCALE_NORMAL);
                OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
                glEnable(GL_TEXTURE_2D);
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
                dispatcher.renderTileEntityAt(tile, tile.xCoord, tile.yCoord, tile.zCoord, 0);
            } catch (RuntimeException | LinkageError failure) {
                reportRenderFailure(failedTilePasses[pass], pos, tile, pass, failure);
            } finally {
                try {
                    finishDrawing(Tessellator.instance);
                } finally {
                    Tessellator.instance.setTranslation(0, 0, 0);
                    glMatrixMode(GL_MODELVIEW);
                    glPopMatrix();
                    glMatrixMode(GL_PROJECTION);
                    glPopMatrix();
                    glMatrixMode(GL_MODELVIEW);
                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightnessX, brightnessY);
                    glPopClientAttrib();
                    glPopAttrib();
                }
            }
        }
    }

    public void renderBlocks(Tessellator tessellator, LongCollection blocksToRender, boolean transparent) {
        if (blocksToRender.isEmpty()) return;
        Minecraft mc = Minecraft.getMinecraft();
        int savedAo = mc.gameSettings.ambientOcclusion;
        int previousPass = MinecraftForgeClient.getRenderPass();
        int previousWorldPass = ForgeHooksClient.getWorldRenderPass();
        mc.gameSettings.ambientOcclusion = 0;
        try {
            for (int pass = 0; pass < 2; pass++) {
                renderBlockPass(tessellator, blocksToRender, transparent, pass);
            }
        } finally {
            bufferBuilder.blockAccess = null;
            ForgeHooksClient.setRenderPass(previousPass);
            ForgeHooksClient.worldRenderPass = previousWorldPass;
            mc.gameSettings.ambientOcclusion = savedAo;
            tessellator.setTranslation(0, 0, 0);
        }
    }

    private void renderBlockPass(Tessellator tessellator, LongCollection positions, boolean transparent, int pass) {
        if (positions.isEmpty()) return;
        ForgeHooksClient.setRenderPass(pass);
        ForgeHooksClient.worldRenderPass = pass;
        prepareBlockState(transparent ? 1 : pass);
        beginDrawing(tessellator, transparent);
        try {
            for (LongIterator iterator = positions.iterator(); iterator.hasNext();) {
                long pos = iterator.nextLong();
                if (failedBlockPasses[pass].contains(pos)) continue;
                Block block = world.blockMap.get(pos);
                if (block == null) continue;
                try {
                    if (!block.canRenderInPass(pass)) continue;
                    resetBlockRenderer();
                    tessellator.setTranslation(0, 0, 0);
                    tessellator.setBrightness(15 << 20 | 15 << 4);
                    if (!transparent) tessellator.setColorOpaque_F(1, 1, 1);
                    bufferBuilder.renderBlockByRenderType(
                            block,
                            CoordinatePacker.unpackX(pos),
                            CoordinatePacker.unpackY(pos),
                            CoordinatePacker.unpackZ(pos));
                } catch (RuntimeException | LinkageError failure) {
                    reportRenderFailure(failedBlockPasses[pass], pos, block, pass, failure);
                    // Flush complete geometry before restarting a batch left by an incompatible renderer.
                    finishDrawing(tessellator);
                    prepareBlockState(transparent ? 1 : pass);
                    beginDrawing(tessellator, transparent);
                }
                // Some legacy renderers finish the shared batch themselves.
                if (!tessellator.isDrawing) {
                    prepareBlockState(transparent ? 1 : pass);
                    beginDrawing(tessellator, transparent);
                }
            }
        } finally {
            try {
                finishDrawing(tessellator);
            } finally {
                tessellator.setTranslation(0, 0, 0);
            }
        }
    }

    private void resetBlockRenderer() {
        bufferBuilder.blockAccess = world;
        bufferBuilder.clearOverrideBlockTexture();
        bufferBuilder.renderAllFaces = renderAllFaces;
        bufferBuilder.useInventoryTint = false;
        bufferBuilder.enableAO = false;
        bufferBuilder.renderFromInside = false;
        bufferBuilder.flipTexture = false;
        bufferBuilder.field_152631_f = false;
        bufferBuilder.lockBlockBounds = false;
        bufferBuilder.uvRotateEast = 0;
        bufferBuilder.uvRotateWest = 0;
        bufferBuilder.uvRotateSouth = 0;
        bufferBuilder.uvRotateNorth = 0;
        bufferBuilder.uvRotateTop = 0;
        bufferBuilder.uvRotateBottom = 0;
        bufferBuilder.setRenderBounds(0, 0, 0, 1, 1, 1);
        bufferBuilder.partialRenderBounds = false;
    }

    private static void prepareBlockState(int pass) {
        setDefaultPassRenderState(pass);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        Minecraft mc = Minecraft.getMinecraft();
        mc.entityRenderer.disableLightmap(0);
        mc.renderEngine.bindTexture(TextureMap.locationBlocksTexture);
        RenderHelper.disableStandardItemLighting();
        glDisable(GL_LIGHTING);
        glEnable(GL_CULL_FACE);
        glEnable(GL12.GL_RESCALE_NORMAL);
        glEnable(GL_TEXTURE_2D);
    }

    private static void beginDrawing(Tessellator tessellator, boolean transparent) {
        tessellator.startDrawingQuads();
        tessellator.setTranslation(0, 0, 0);
        tessellator.setBrightness(15 << 20 | 15 << 4);
        tessellator.setColorRGBA_F(1, 1, 1, transparent ? 0.3f : 1);
        if (transparent) tessellator.disableColor();
    }

    private static void finishDrawing(Tessellator tessellator) {
        if (tessellator.isDrawing) tessellator.draw();
    }

    private void renderSelectionOverlay(Tessellator tessellator) {
        if (onRender == null) return;
        ForgeHooksClient.setRenderPass(1);
        ForgeHooksClient.worldRenderPass = 1;
        prepareBlockState(1);
        beginDrawing(tessellator, false);
        try {
            onRender.accept(this);
        } finally {
            finishDrawing(tessellator);
            tessellator.setTranslation(0, 0, 0);
        }
    }

    private static void reportRenderFailure(LongSet failures, long pos, Object rendered, int pass, Throwable failure) {
        if (!failures.add(pos)) return;
        BlockRenderer6343.LOG.warn(
                "Skipping preview renderer {} at ({}, {}, {}) in pass {} until the scene changes",
                rendered.getClass().getName(),
                CoordinatePacker.unpackX(pos),
                CoordinatePacker.unpackY(pos),
                CoordinatePacker.unpackZ(pos),
                pass,
                failure);
    }

    public static void setDefaultPassRenderState(int pass) {
        glColor4f(1, 1, 1, 1);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.1f);
        if (pass == 0) { // SOLID
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
        lookVec.mul(eyePos.distance(lookAt) + world.getSize().length() + 1);
        Vec3 endPos = Vec3.createVectorHelper(
                (lookVec.x + startPos.xCoord),
                (lookVec.y + startPos.yCoord),
                (lookVec.z + startPos.zCoord));
        return this.world.rayTraceBlocksWithTargetMap(startPos, endPos, renderedBlocks);
    }
}
