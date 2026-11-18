package blockrenderer6343.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.MinecraftForgeClient;

public class PreviewRenderContext implements AutoCloseable {

    private final Minecraft minecraft = Minecraft.getMinecraft();
    private final TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
    private final RenderManager manager = RenderManager.instance;
    private final World previewWorld;
    private final boolean remote;
    private final long worldTime;
    private final EntityLivingBase renderViewEntity = minecraft.renderViewEntity;
    private final int renderPass = MinecraftForgeClient.getRenderPass();
    private final int worldRenderPass = ForgeHooksClient.getWorldRenderPass();
    private final float brightnessX = OpenGlHelper.lastBrightnessX;
    private final float brightnessY = OpenGlHelper.lastBrightnessY;
    private final World tileWorld = dispatcher.field_147550_f;
    private final EntityLivingBase tileViewer = dispatcher.field_147551_g;
    private final TextureManager tileTextures = dispatcher.field_147553_e;
    private final FontRenderer tileFont = dispatcher.getFontRenderer();
    private final float tileYaw = dispatcher.field_147562_h;
    private final float tilePitch = dispatcher.field_147563_i;
    private final double tileX = dispatcher.field_147560_j;
    private final double tileY = dispatcher.field_147561_k;
    private final double tileZ = dispatcher.field_147558_l;
    private final double tileOriginX = TileEntityRendererDispatcher.staticPlayerX;
    private final double tileOriginY = TileEntityRendererDispatcher.staticPlayerY;
    private final double tileOriginZ = TileEntityRendererDispatcher.staticPlayerZ;
    private final World entityWorld = manager.worldObj;
    private final EntityLivingBase entityViewer = manager.livingPlayer;
    private final Entity pointedEntity = manager.field_147941_i;
    private final TextureManager entityTextures = manager.renderEngine;
    private final FontRenderer entityFont = manager.getFontRenderer();
    private final GameSettings entityOptions = manager.options;
    private final float entityYaw = manager.playerViewY;
    private final float entityPitch = manager.playerViewX;
    private final double entityX = manager.viewerPosX;
    private final double entityY = manager.viewerPosY;
    private final double entityZ = manager.viewerPosZ;
    private final double entityOriginX = RenderManager.renderPosX;
    private final double entityOriginY = RenderManager.renderPosY;
    private final double entityOriginZ = RenderManager.renderPosZ;
    private boolean closed;

    public PreviewRenderContext(World world, EntityLivingBase viewer) {
        previewWorld = world;
        remote = world.isRemote;
        worldTime = world.getTotalWorldTime();
        try {
            world.isRemote = true;
            if (minecraft.theWorld != null) {
                world.getWorldInfo().incrementTotalWorldTime(minecraft.theWorld.getTotalWorldTime());
            }
            minecraft.renderViewEntity = viewer;
            dispatcher.cacheActiveRenderInfo(world, minecraft.renderEngine, minecraft.fontRenderer, viewer, 0);
            manager.cacheActiveRenderInfo(
                    world,
                    minecraft.renderEngine,
                    minecraft.fontRenderer,
                    viewer,
                    null,
                    minecraft.gameSettings,
                    0);
            manager.playerViewY = viewer.rotationYaw;
            manager.playerViewX = viewer.rotationPitch;
            // Geometry uses absolute preview coordinates; nested entity renderers must share this origin.
            TileEntityRendererDispatcher.staticPlayerX = RenderManager.renderPosX = 0;
            TileEntityRendererDispatcher.staticPlayerY = RenderManager.renderPosY = 0;
            TileEntityRendererDispatcher.staticPlayerZ = RenderManager.renderPosZ = 0;
        } catch (RuntimeException | LinkageError failure) {
            close();
            throw failure;
        }
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        minecraft.renderViewEntity = renderViewEntity;
        previewWorld.isRemote = remote;
        previewWorld.getWorldInfo().incrementTotalWorldTime(worldTime);
        ForgeHooksClient.setRenderPass(renderPass);
        ForgeHooksClient.worldRenderPass = worldRenderPass;
        try {
            if (dispatcher.field_147550_f != tileWorld) dispatcher.func_147543_a(tileWorld);
        } finally {
            dispatcher.field_147551_g = tileViewer;
            dispatcher.field_147553_e = tileTextures;
            dispatcher.field_147557_n = tileFont;
            dispatcher.field_147562_h = tileYaw;
            dispatcher.field_147563_i = tilePitch;
            dispatcher.field_147560_j = tileX;
            dispatcher.field_147561_k = tileY;
            dispatcher.field_147558_l = tileZ;
            TileEntityRendererDispatcher.staticPlayerX = tileOriginX;
            TileEntityRendererDispatcher.staticPlayerY = tileOriginY;
            TileEntityRendererDispatcher.staticPlayerZ = tileOriginZ;
            manager.set(entityWorld);
            manager.livingPlayer = entityViewer;
            manager.field_147941_i = pointedEntity;
            manager.renderEngine = entityTextures;
            manager.fontRenderer = entityFont;
            manager.options = entityOptions;
            manager.playerViewY = entityYaw;
            manager.playerViewX = entityPitch;
            manager.viewerPosX = entityX;
            manager.viewerPosY = entityY;
            manager.viewerPosZ = entityZ;
            RenderManager.renderPosX = entityOriginX;
            RenderManager.renderPosY = entityOriginY;
            RenderManager.renderPosZ = entityOriginZ;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightnessX, brightnessY);
        }
    }
}
