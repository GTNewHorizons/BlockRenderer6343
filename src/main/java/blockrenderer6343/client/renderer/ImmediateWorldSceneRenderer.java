package blockrenderer6343.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MovingObjectPosition;

import org.lwjgl.opengl.GL11;

import blockrenderer6343.client.world.TrackedDummyWorld;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: KilaBash, backported by Quarri6343
 * @Date: 2021/8/24
 * @Description: Renders a preview directly into the current framebuffer.
 */
@SideOnly(Side.CLIENT)
public class ImmediateWorldSceneRenderer extends WorldSceneRenderer {

    public ImmediateWorldSceneRenderer(TrackedDummyWorld world) {
        super(world);
    }

    @Override
    public MovingObjectPosition pick(int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        return super.pick(getScaledX(mc, resolution, mouseX), mc.displayHeight - getScaledY(mc, resolution, mouseY));
    }

    @Override
    public void render(int x, int y, int width, int height, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        // compute window size from scaled width & height
        int windowWidth = getScaledX(mc, resolution, width);
        int windowHeight = getScaledY(mc, resolution, height);
        // translate gui coordinates to window's ones (y is inverted)
        int windowX = getScaledX(mc, resolution, x);
        int windowY = mc.displayHeight - getScaledY(mc, resolution, y) - windowHeight;
        int windowMouseX = getScaledX(mc, resolution, mouseX);
        int windowMouseY = mc.displayHeight - getScaledY(mc, resolution, mouseY);
        windowWidth = Math.max(0, windowWidth);
        windowHeight = Math.max(0, windowHeight);
        try {
            super.render(windowX, windowY, windowWidth, windowHeight, windowMouseX, windowMouseY);
        } finally {
            GL11.glPushAttrib(GL11.GL_SCISSOR_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            try {
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                GL11.glScissor(windowX, windowY, windowWidth, windowHeight);
                GL11.glDepthMask(true);
                GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            } finally {
                GL11.glPopAttrib();
            }
        }
    }

    private int getScaledX(Minecraft mc, ScaledResolution res, int x) {
        return (int) (x / (res.getScaledWidth() * 1.0) * mc.displayWidth);
    }

    private int getScaledY(Minecraft mc, ScaledResolution res, int y) {
        return (int) (y / (res.getScaledHeight() * 1.0) * mc.displayHeight);
    }

    @Override
    protected void clearView(int x, int y, int width, int height) {
        GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);
        try {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(x, y, width, height);
            super.clearView(x, y, width, height);
        } finally {
            GL11.glPopAttrib();
        }
    }
}
