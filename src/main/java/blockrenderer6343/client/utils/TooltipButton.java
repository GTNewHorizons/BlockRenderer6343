package blockrenderer6343.client.utils;

import java.util.function.BooleanSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.config.GuiButtonExt;

public class TooltipButton extends GuiButtonExt {

    public final String hoverString;
    private final BooleanSupplier supplier;

    public TooltipButton(int id, int xPos, int yPos, int width, int height, String displayString, String hoverString) {
        this(id, xPos, yPos, width, height, displayString, hoverString, null);
    }

    public TooltipButton(int id, int xPos, int yPos, int width, int height, String displayString, String hoverString,
            BooleanSupplier supplier) {
        super(id, xPos, yPos, width, height, displayString);
        this.hoverString = hoverString;
        this.supplier = supplier;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (supplier != null && !supplier.getAsBoolean()) return;
        super.drawButton(mc, mouseX, mouseY);
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return this.enabled && this.visible
                && mouseX >= this.xPosition
                && mouseY >= this.yPosition
                && mouseX < this.xPosition + this.width
                && mouseY < this.yPosition + this.height;
    }

    public void drawTooltipBox(FontRenderer fontRenderer, int x, int y, int w, int h) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setColorRGBA(33, 33, 33, 200);
        tessellator.addVertex(x, y + h, 0D);
        tessellator.addVertex(x + w, y + h, 0D);
        tessellator.addVertex(x + w, y, 0D);
        tessellator.addVertex(x, y, 0D);
        tessellator.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        fontRenderer.drawString(hoverString, x + 2, y + 3, 0XFFFFFF);
    }
}
