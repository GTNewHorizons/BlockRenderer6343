package blockrenderer6343.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.config.GuiButtonExt;

public class TooltipButton extends GuiButtonExt {

    private final String hoverString;

    public TooltipButton(int id, int xPos, int yPos, int width, int height, String displayString, String hoverString) {
        super(id, xPos, yPos, width, height, displayString);
        this.hoverString = hoverString;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        super.drawButton(mc, mouseX, mouseY);
        if (isMouseOver(mouseX, mouseY)) {
            int textWidth = mc.fontRenderer.getStringWidth(this.hoverString);
            drawTooltipBox(mc.fontRenderer, mouseX - 10, mouseY - 17, textWidth + 1, this.height, 33, 33, 33, 200);
        }

    }

    private boolean isMouseOver(int mouseX, int mouseY) {
        return this.enabled && this.visible
                && mouseX >= this.xPosition
                && mouseY >= this.yPosition
                && mouseX < this.xPosition + this.width
                && mouseY < this.yPosition + this.height;
    }

    public void drawTooltipBox(FontRenderer fontRenderer, int x, int y, int w, int h, int r, int g, int b, int a) {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setColorRGBA(r, g, b, a);
        tessellator.addVertex(x, y + h, 0D);
        tessellator.addVertex(x + w, y + h, 0D);
        tessellator.addVertex(x + w, y, 0D);
        tessellator.addVertex(x, y, 0D);
        tessellator.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        fontRenderer.drawString(hoverString, x + 2, y + 3, 0XFFFFFF);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }
}
