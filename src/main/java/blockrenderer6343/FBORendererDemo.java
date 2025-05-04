package blockrenderer6343;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityDispenser;

import org.joml.Vector3f;

import com.cleanroommc.modularui.utils.GlStateManager;
import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;

import blockrenderer6343.client.renderer.FBOWorldSceneRenderer;
import cpw.mods.fml.client.registry.ClientRegistry;

// put new FBORendererDemo(); in FMLInitializationEvent to use
public class FBORendererDemo extends TileEntitySpecialRenderer {

    protected static FBOWorldSceneRenderer renderer;
    protected static final float DEFAULT_RANGE_MULTIPLIER = 3.5f;

    protected static float rotationYaw = 30f, rotationPitch = 45f, max = 1f;

    public FBORendererDemo() {
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityDispenser.class, this);
    }

    @Override
    public void renderTileEntityAt(TileEntity tileEntity, double x, double y, double z, float partialTick) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y + 2, z + 0.5);
        GlStateManager.scale(1f / 64, 1f / 64, 1);
        GlStateManager.translate(-32, -32, 0);

        renderInWorld(tileEntity, tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);

        GlStateManager.popMatrix();
    }

    private void renderInWorld(TileEntity tileEntity, int x, int y, int z) {
        if (renderer == null) {
            renderer = new FBOWorldSceneRenderer(tileEntity.getWorldObj(), 1080, 1080);
        }

        Vector3f center = new Vector3f(x + 0.5f, y - 0.5f, z + 0.5f);

        // placemultiblock
        // updaterenderer
        renderer.resetRenderedBlocks();
        renderer.setRenderAllFaces(true);
        long blockPos = CoordinatePacker.pack(x, y - 1, z);
        renderer.renderedBlocks.add(blockPos);

        float baseZoom = (float) (DEFAULT_RANGE_MULTIPLIER * Math.sqrt(max));
        float sizeFactor = (float) (1.0f + Math.log(max) / Math.log(10));

        float zoom = baseZoom * sizeFactor / 1.5f;

        renderer.setCameraLookAt(center, zoom, Math.toRadians(rotationPitch), Math.toRadians(rotationYaw));

        renderer.render(0, 0, 64, 64, 0, 0, true);
    }
}
