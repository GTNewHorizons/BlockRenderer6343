package blockrenderer6343.mixins;

import codechicken.lib.gui.GuiDraw;
import blockrenderer6343.utils.TooltipsFrameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import static blockrenderer6343.utils.TooltipsFrameRenderer.AnimationStyle.*;

@Mixin(value = GuiDraw.class)
public class CCLGuiDrawMixin {

    /**
     * @author Quarri6343
     * @reason BlockRenderer6343
     */
    @Overwrite(remap = false)
    public static void drawTooltipBox(int x, int y, int w, int h) {
        int bg = 0xf0100010;
        int zlevel = 300;
        TooltipsFrameRenderer.drawFrame(x + 1, y, x + w, y + 1, bg, bg, zlevel, Horizontal);
        TooltipsFrameRenderer.drawFrame(x + 1, y + h, x + w, y + h + 1, bg, bg, zlevel, Horizontal);
        TooltipsFrameRenderer.drawFrame(x + 1, y + 1, x + w, y + h, bg, bg, zlevel, None);
        TooltipsFrameRenderer.drawFrame(x, y + 1, x + 1, y + h, bg, bg, zlevel, Vertical);
        TooltipsFrameRenderer.drawFrame(x + w, y + 1, x + w + 1, y + h, bg, bg, zlevel, Vertical);
        int grad1 = 0x505000ff;
        int grad2 = 0x5028007F;
        TooltipsFrameRenderer.drawFrame(x + 1, y + 2, x + 2, y + h - 1, grad1, grad2, zlevel, Vertical);
        TooltipsFrameRenderer.drawFrame(x + w - 1, y + 2, x + w, y + h - 1, grad1, grad2, zlevel, Vertical);
        TooltipsFrameRenderer.drawFrame(x + 1, y + 1, x + w, y + 2, grad1, grad2, zlevel, Horizontal);
        TooltipsFrameRenderer.drawFrame(x + 1, y + h - 1, x + w, y + h, grad1, grad2, zlevel, Horizontal);
    }
}
