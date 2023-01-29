package blockrenderer6343.client.renderer;

import javax.annotation.Nonnull;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import squeek.applecore.client.TooltipOverlayHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

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
    public static void drawFrame(int p_73733_1_, int p_73733_2_, int p_73733_3_, int p_73733_4_, int p_73733_5_,
            int p_73733_6_, float zLevel, AnimationStyle style) {
        sDraw = true;

        float f = (float) (p_73733_5_ >> 24 & 255) / 255.0F;
        float f1 = (float) (p_73733_5_ >> 16 & 255) / 255.0F;
        float f2 = (float) (p_73733_5_ >> 8 & 255) / 255.0F;
        float f3 = (float) (p_73733_5_ & 255) / 255.0F;
        float f4 = (float) (p_73733_6_ >> 24 & 255) / 255.0F;
        float f5 = (float) (p_73733_6_ >> 16 & 255) / 255.0F;
        float f6 = (float) (p_73733_6_ >> 8 & 255) / 255.0F;
        float f7 = (float) (p_73733_6_ & 255) / 255.0F;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setColorRGBA_F(f1, f2, f3, f + 100 / 255.0f);
        tessellator.addVertex(
                style == AnimationStyle.Horizontal
                        ? (double) p_73733_1_ + ((double) p_73733_3_ - (double) p_73733_1_) * alpha
                        : (double) p_73733_3_,
                (double) p_73733_2_,
                (double) zLevel);
        tessellator.addVertex((double) p_73733_1_, (double) p_73733_2_, (double) zLevel);
        tessellator.setColorRGBA_F(f5, f6, f7, f4 + 100 / 255.0f);
        tessellator.addVertex(
                (double) p_73733_1_,
                style == AnimationStyle.Vertical
                        ? (double) p_73733_2_ + ((double) p_73733_4_ - (double) p_73733_2_) * alpha
                        : (double) p_73733_4_,
                (double) zLevel);
        tessellator.addVertex(
                style == AnimationStyle.Horizontal
                        ? (double) p_73733_1_ + ((double) p_73733_3_ - (double) p_73733_1_) * alpha
                        : (double) p_73733_3_,
                style == AnimationStyle.Vertical
                        ? (double) p_73733_2_ + ((double) p_73733_4_ - (double) p_73733_2_) * alpha
                        : (double) p_73733_4_,
                (double) zLevel);
        tessellator.draw();
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    // call this when the frame drawing is finished
    public static void setAppleCoretooltip(int j2, int k2, int k, int i1) {
        if (Loader.isModLoaded("AppleCore")) {
            TooltipOverlayHandler.toolTipX = j2;
            TooltipOverlayHandler.toolTipY = k2;
            TooltipOverlayHandler.toolTipW = k;
            TooltipOverlayHandler.toolTipH = i1;
        }
    }
}
