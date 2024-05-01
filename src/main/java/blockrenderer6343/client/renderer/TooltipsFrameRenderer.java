package blockrenderer6343.client.renderer;

import javax.annotation.Nonnull;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.enums.Mods;
import squeek.applecore.client.TooltipOverlayHandler;

/**
 * Draw Animated Tooltips Frame
 */
public class TooltipsFrameRenderer {

    public enum AnimationStyle {
        Horizontal,
        Vertical,
        None
    }

    public static float alpha;
    public static boolean sDraw = false;
    private static final float sAnimationDuration = 250;

    private long mFrameTimeNanos;

    public TooltipsFrameRenderer() {
        FMLCommonHandler.instance().bus().register(this);
    }

    private void update(float deltaMillis) {
        if (sDraw) {
            alpha = Math.min(alpha + deltaMillis / sAnimationDuration, 1);
            sDraw = false;
        } else {
            alpha = 0;
        }
    }

    @SubscribeEvent
    public void onRenderTick(@Nonnull TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            final long lastFrameTime = mFrameTimeNanos;
            mFrameTimeNanos = System.nanoTime();
            final long deltaMillis = (mFrameTimeNanos - lastFrameTime) / 1000000;
            update(deltaMillis);
        }
    }

    // Drawing Tooltips Frame parts
    public static void drawFrame(int left, int top, int right, int bottom, int startColor, int endColor, float zLevel,
            AnimationStyle style) {
        sDraw = true;

        float f = (float) (startColor >> 24 & 255) / 255.0F;
        float f1 = (float) (startColor >> 16 & 255) / 255.0F;
        float f2 = (float) (startColor >> 8 & 255) / 255.0F;
        float f3 = (float) (startColor & 255) / 255.0F;
        float f4 = (float) (endColor >> 24 & 255) / 255.0F;
        float f5 = (float) (endColor >> 16 & 255) / 255.0F;
        float f6 = (float) (endColor >> 8 & 255) / 255.0F;
        float f7 = (float) (endColor & 255) / 255.0F;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setColorRGBA_F(f1, f2, f3, f + 100 / 255.0f);
        tessellator.addVertex(
                style == AnimationStyle.Horizontal ? (double) left + ((double) right - (double) left) * alpha
                        : (double) right,
                (double) top,
                (double) zLevel);
        tessellator.addVertex((double) left, (double) top, (double) zLevel);
        tessellator.setColorRGBA_F(f5, f6, f7, f4 + 100 / 255.0f);
        tessellator.addVertex(
                (double) left,
                style == AnimationStyle.Vertical ? (double) top + ((double) bottom - (double) top) * alpha
                        : (double) bottom,
                (double) zLevel);
        tessellator.addVertex(
                style == AnimationStyle.Horizontal ? (double) left + ((double) right - (double) left) * alpha
                        : (double) right,
                style == AnimationStyle.Vertical ? (double) top + ((double) bottom - (double) top) * alpha
                        : (double) bottom,
                (double) zLevel);
        tessellator.draw();
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    // call this when the frame drawing is finished
    public static void setAppleCoretooltip(int j2, int k2, int k, int i1) {
        if (Mods.AppleCore.isModLoaded()) {
            TooltipOverlayHandler.toolTipX = j2;
            TooltipOverlayHandler.toolTipY = k2;
            TooltipOverlayHandler.toolTipW = k;
            TooltipOverlayHandler.toolTipH = i1;
        }
    }
}
