package blockrenderer6343.client.utils;

import static blockrenderer6343.integration.nei.GuiMultiblockHandler.ICON_SIZE_X;
import static blockrenderer6343.integration.nei.GuiMultiblockHandler.ICON_SIZE_Y;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;

import cpw.mods.fml.client.config.GuiButtonExt;

public class BRButton extends GuiButtonExt {

    protected Runnable clickAction;
    protected String tooltip = "";
    protected int index;

    public BRButton(int id, int xPos, int yPos, int width, int height, String displayString) {
        super(id, xPos, yPos, width, height, displayString);
    }

    public BRButton(int xPos, int yPos, String displayString) {
        this(0, xPos, yPos, ICON_SIZE_X, ICON_SIZE_Y, displayString);
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        playClickSound();
        clickAction.run();
        return true;
    }

    public void mouseDragged(int mouseX, int mouseY) {}

    public boolean mouseScrolled(int mouseX, int mouseY, int scroll) {
        return false;
    }

    public BRButton setTooltip(String tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public BRButton setClickAction(Runnable action) {
        this.clickAction = action;
        return this;
    }

    public BRButton setIndex(int index) {
        this.index = index;
        return this;
    }

    public boolean isMouseOver(Vector2i mousePos) {
        return isMouseOver(mousePos.x, mousePos.y);
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= this.xPosition && mouseY >= this.yPosition
                && mouseX < this.xPosition + this.width
                && mouseY < this.yPosition + this.height;
    }

    public static void playClickSound() {
        Minecraft.getMinecraft().getSoundHandler()
                .playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
    }

    public @NotNull List<String> getTooltip(Vector2i mousePos) {
        if (!isMouseOver(mousePos) || tooltip.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(tooltip.split("\n"));
    }

    public void scalePosition(int scaledScene, float scale) {
        this.yPosition = scaledScene + ((height + 1) * (index / 2));
    }
}
