package blockrenderer6343.integration.wdmla;

import blockrenderer6343.client.renderer.ImmediateWorldSceneRenderer;
import blockrenderer6343.client.renderer.WorldSceneRenderer;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

@EventBusSubscriber(side = Side.CLIENT)
public class HUDBlockHandler {

    protected static WorldSceneRenderer renderer;
    protected static final float DEFAULT_RANGE_MULTIPLIER = 3.5f;

    protected static Vector3f center = new Vector3f();
    protected static float rotationYaw, rotationPitch;
    protected static float zoom;

    //test code
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if(Minecraft.getMinecraft().thePlayer == null || Minecraft.getMinecraft().theWorld == null
            || Minecraft.getMinecraft().currentScreen != null) {
            return;
        }

        if (renderer == null || renderer.world != Minecraft.getMinecraft().theWorld) {
            renderer = new ImmediateWorldSceneRenderer(Minecraft.getMinecraft().theWorld){
                @Override
                protected void clearView(int x, int y, int width, int height) {
                    GL11.glEnable(GL11.GL_SCISSOR_TEST);
                    GL11.glScissor(x, y, width, height);
                    GL11.glDisable(GL11.GL_SCISSOR_TEST);
                }
            };
        }

        MovingObjectPosition target = rayTrace(Minecraft.getMinecraft().renderViewEntity, Minecraft.getMinecraft().playerController.getBlockReachDistance(), 0);
        if(target == null || target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }

        center = new Vector3f(target.blockX + 0.5f, target.blockY + 0.5f, target.blockZ + 0.5f);

        //placemultiblock
        //updaterenderer
        renderer.resetRenderedBlocks();
        renderer.setRenderAllFaces(true);
        long blockPos = CoordinatePacker.pack(target.blockX, target.blockY, target.blockZ);
        renderer.renderedBlocks.add(blockPos);

        float max = 1;
        float baseZoom = (float) (DEFAULT_RANGE_MULTIPLIER * Math.sqrt(max));
        float sizeFactor = (float) (1.0f + Math.log(max) / Math.log(10));

        zoom = baseZoom * sizeFactor / 1.5f;
        rotationYaw = 30f;
        rotationPitch = 45f;

        renderer.setCameraLookAt(center, zoom, Math.toRadians(rotationPitch), Math.toRadians(rotationYaw));

        renderer.render(
            100,
            100,
            100,
            100,
            0,
            0);
    }

    public static MovingObjectPosition rayTrace(EntityLivingBase entity, double par1, float par3) {
        Vec3 vec3 = entity.getPosition(par3);
        Vec3 vec31 = entity.getLook(par3);
        Vec3 vec32 = vec3.addVector(vec31.xCoord * par1, vec31.yCoord * par1, vec31.zCoord * par1);

        return entity.worldObj.rayTraceBlocks(vec3, vec32, true);
    }
}
