package blockrenderer6343.mixins;

import static blockrenderer6343.utils.TooltipsFrameRenderer.AnimationStyle.*;

import blockrenderer6343.utils.TooltipsFrameRenderer;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Enhance Tooltips rendering
 */
@Mixin(value = GuiScreen.class, priority = -1000)
public class GuiScreenMixin extends Gui {

    @Shadow
    public int width;

    @Shadow
    public int height;

    @Shadow
    protected static RenderItem itemRender;

    /**
     * @author Quarri6343
     * @reason BlockRenderer6343
     */
    @SuppressWarnings("rawtypes")
    @Inject(method = "drawHoveringText", at = @At("HEAD"), cancellable = true, remap = false)
    protected void drawHoveringText(
            List p_146283_1_, int p_146283_2_, int p_146283_3_, FontRenderer font, CallbackInfo ci) {
        DrawHoveringText_2(p_146283_1_, p_146283_2_, p_146283_3_, font);
        ci.cancel();
    }

    private void DrawHoveringText_2(List p_146283_1_, int p_146283_2_, int p_146283_3_, FontRenderer font) {
        if (!p_146283_1_.isEmpty()) {

            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            int k = 0;
            Iterator iterator = p_146283_1_.iterator();

            while (iterator.hasNext()) {
                String s = (String) iterator.next();
                int l = font.getStringWidth(s);

                if (l > k) {
                    k = l;
                }
            }

            int j2 = p_146283_2_ + 12;
            int k2 = p_146283_3_ - 12;
            int i1 = 8;

            if (p_146283_1_.size() > 1) {
                i1 += 2 + (p_146283_1_.size() - 1) * 10;
            }

            if (j2 + k > this.width) {
                j2 -= 28 + k;
            }

            if (k2 + i1 + 6 > this.height) {
                k2 = this.height - i1 - 6;
            }

            this.zLevel = 300.0F;
            itemRender.zLevel = 300.0F;
            int j1 = -267386864;
            TooltipsFrameRenderer.drawFrame(j2 - 3, k2 - 4, j2 + k + 3, k2 - 3, j1, j1, zLevel, Horizontal);
            TooltipsFrameRenderer.drawFrame(j2 - 3, k2 + i1 + 3, j2 + k + 3, k2 + i1 + 4, j1, j1, zLevel, Horizontal);
            TooltipsFrameRenderer.drawFrame(j2 - 3, k2 - 3, j2 + k + 3, k2 + i1 + 3, j1, j1, zLevel, None);
            TooltipsFrameRenderer.drawFrame(j2 - 4, k2 - 3, j2 - 3, k2 + i1 + 3, j1, j1, zLevel, Vertical);
            TooltipsFrameRenderer.drawFrame(j2 + k + 3, k2 - 3, j2 + k + 4, k2 + i1 + 3, j1, j1, zLevel, Vertical);
            int k1 = 1347420415;
            int l1 = (k1 & 16711422) >> 1 | k1 & -16777216;
            TooltipsFrameRenderer.drawFrame(j2 - 3, k2 - 3 + 1, j2 - 3 + 1, k2 + i1 + 3 - 1, k1, l1, zLevel, Vertical);
            TooltipsFrameRenderer.drawFrame(
                    j2 + k + 2, k2 - 3 + 1, j2 + k + 3, k2 + i1 + 3 - 1, k1, l1, zLevel, Vertical);
            TooltipsFrameRenderer.drawFrame(j2 - 3, k2 - 3, j2 + k + 3, k2 - 3 + 1, k1, k1, zLevel, Horizontal);
            TooltipsFrameRenderer.drawFrame(j2 - 3, k2 + i1 + 2, j2 + k + 3, k2 + i1 + 3, l1, l1, zLevel, Horizontal);

            TooltipsFrameRenderer.setAppleCoretooltip(j2, k2, k, i1);

            for (int i2 = 0; i2 < p_146283_1_.size(); ++i2) {
                String s1 = (String) p_146283_1_.get(i2);

                font.drawStringWithShadow(s1, j2, k2, -1);

                if (i2 == 0) {
                    k2 += 2;
                }

                k2 += 10;
            }

            this.zLevel = 0.0F;
            itemRender.zLevel = 0.0F;
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            RenderHelper.enableStandardItemLighting();
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        }
    }
}
