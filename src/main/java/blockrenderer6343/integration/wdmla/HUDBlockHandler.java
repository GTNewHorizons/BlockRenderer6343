package blockrenderer6343.integration.wdmla;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;

import blockrenderer6343.client.renderer.ImmediateWorldSceneRenderer;
import blockrenderer6343.client.renderer.WorldSceneRenderer;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

// @EventBusSubscriber(side = Side.CLIENT)
public class HUDBlockHandler {

    protected static WorldSceneRenderer renderer;
    protected static final float DEFAULT_RANGE_MULTIPLIER = 3.5f;
    protected static final float max = 1f;

    protected static float demoRotationPitch = 30f;
    protected static long demoLastTime;

    public static void drawWorldBlock(int x, int y, int width, int height, int blockX, int blockY, int blockZ,
            float rotationYaw, float rotationPitch) {
        World world = Minecraft.getMinecraft().theWorld;
        if (renderer == null || renderer.world != world) {
            renderer = new ImmediateWorldSceneRenderer(world) {

                @Override
                protected void clearView(int x, int y, int width, int height) {
                    GL11.glEnable(GL11.GL_SCISSOR_TEST);
                    GL11.glScissor(x, y, width, height);
                    GL11.glDisable(GL11.GL_SCISSOR_TEST);
                }
            };
        }

        Vector3f center = new Vector3f(blockX + 0.5f, blockY + 0.5f, blockZ + 0.5f);

        renderer.resetRenderedBlocks();
        renderer.setRenderAllFaces(true);
        long blockPos = CoordinatePacker.pack(blockX, blockY, blockZ);
        renderer.renderedBlocks.add(blockPos);

        float baseZoom = (float) (DEFAULT_RANGE_MULTIPLIER * Math.sqrt(max));
        float sizeFactor = (float) (1.0f + Math.log(max) / Math.log(10));
        float zoom = baseZoom * sizeFactor / 1.5f;

        renderer.setCameraLookAt(center, zoom, Math.toRadians(rotationPitch), Math.toRadians(rotationYaw));

        renderer.render(x, y, width, height, 0, 0);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void demo(RenderGameOverlayEvent.Post event) {
        if (Minecraft.getMinecraft().thePlayer == null || Minecraft.getMinecraft().theWorld == null
                || Minecraft.getMinecraft().currentScreen != null) {
            return;
        }

        MovingObjectPosition target = rayTrace(
                Minecraft.getMinecraft().renderViewEntity,
                Minecraft.getMinecraft().playerController.getBlockReachDistance(),
                0);
        if (target == null || target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }

        demoRotationPitch += (Minecraft.getMinecraft().theWorld.getTotalWorldTime() - demoLastTime) * 1.0f;
        HUDBlockHandler.drawWorldBlock(
                100,
                100,
                100,
                100,
                target.blockX,
                target.blockY,
                target.blockZ,
                30f,
                demoRotationPitch);
        demoLastTime = Minecraft.getMinecraft().theWorld.getTotalWorldTime();
    }

    public static MovingObjectPosition rayTrace(EntityLivingBase entity, double par1, float par3) {
        Vec3 vec3 = entity.getPosition(par3);
        Vec3 vec31 = entity.getLook(par3);
        Vec3 vec32 = vec3.addVector(vec31.xCoord * par1, vec31.yCoord * par1, vec31.zCoord * par1);

        return entity.worldObj.rayTraceBlocks(vec3, vec32, true);
    }
}
