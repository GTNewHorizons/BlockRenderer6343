package blockrenderer6343.client.utils;

import static blockrenderer6343.integration.nei.GUI_MultiblockHandler.ICON_SIZE_X;
import static blockrenderer6343.integration.nei.GUI_MultiblockHandler.ICON_SIZE_Y;

import java.util.function.BooleanSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

public class ClearGuiButton extends GuiButton {

    private int colorEnabled;
    private int colorDisabled;
    private int colorHovered;
    private final BooleanSupplier booleanSupplier;

    public ClearGuiButton(int id, int x, int y, String displayString) {
        this(id, x, y, displayString, null);
    }

    public ClearGuiButton(int id, int x, int y, String displayString, BooleanSupplier booleanSupplier) {
        super(id, x, y, ICON_SIZE_X, ICON_SIZE_Y, displayString);
        this.booleanSupplier = booleanSupplier;
    }

    public void setColors(int clrEnabled, int clrDisabled, int clrHovered) {
        colorEnabled = clrEnabled;
        colorDisabled = clrDisabled;
        colorHovered = clrHovered;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible) {
            if (booleanSupplier != null && !booleanSupplier.getAsBoolean()) return;

            FontRenderer fontrenderer = mc.fontRenderer;
            this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition
                    && mouseX < this.xPosition + this.width
                    && mouseY < this.yPosition + this.height;
            int l = colorEnabled;

            if (packedFGColour != 0) {
                l = packedFGColour;
            } else if (!this.enabled) {
                l = colorDisabled;
            } else if (this.field_146123_n) {
                l = colorHovered;
            }

            this.drawCenteredString(
                    fontrenderer,
                    this.displayString,
                    this.xPosition + this.width / 2,
                    this.yPosition + (this.height - 8) / 2,
                    l);
        }
    }
}
