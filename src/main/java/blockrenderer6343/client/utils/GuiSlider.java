package blockrenderer6343.client.utils;

import static blockrenderer6343.integration.nei.GuiMultiblockHandler.SCENE_HEIGHT;

import java.util.function.IntSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.MathHelper;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.config.GuiUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import it.unimi.dsi.fastutil.ints.IntConsumer;

public class GuiSlider extends BRButton {

    private int maxValue;
    private int minValue;
    private int value;
    private boolean dragging;
    private IntConsumer valueListener;
    private IntSupplier maxValueSupplier;
    private Int2ObjectFunction<String> valueStringSupplier;
    public final String name;
    private float scale;
    private final int originalHeight;

    public GuiSlider(String name, int x, int y, int width, int height, int value, int minValue, int maxValue) {
        super(0, x, y, width, height, "");
        this.name = name;
        this.value = value;
        this.maxValue = maxValue;
        this.minValue = minValue;
        this.originalHeight = height;
    }

    public GuiSlider(String name, int x, int y, int width, int height, int value, int minValue) {
        this(name, x, y, width, height, value, minValue, 0);
    }

    public GuiSlider setMaxValueSupplier(@NotNull IntSupplier supplier) {
        this.maxValueSupplier = supplier;
        return this;
    }

    public GuiSlider setValueListener(@NotNull IntConsumer listener) {
        this.valueListener = listener;
        return this;
    }

    public GuiSlider setTextSupplier(@NotNull Int2ObjectFunction<String> supplier) {
        this.valueStringSupplier = supplier;
        return this;
    }

    @Override
    public int getHoverState(boolean mouseOver) {
        return 0;
    }

    public int getMaxValue() {
        return maxValueSupplier == null ? maxValue : maxValueSupplier.getAsInt();
    }

    @Override
    public void scalePosition(int scaledGuiHeight, float scale) {
        this.scale = scale;
        height = Math.round(originalHeight * scale);
        yPosition = Math.round(SCENE_HEIGHT * scale + (height + 1) * index);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        maxValue = getMaxValue();
        mc.renderEngine.bindTexture(buttonTextures);
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        super.drawButton(mc, mouseX, mouseY);
        float sliderValue = minValue == maxValue ? 1 : (float) (value - minValue) / (maxValue - minValue);
        int pos = MathHelper.clamp_int((int) (xPosition + sliderValue * (width - 8)), xPosition, xPosition + width - 8);
        GuiUtils.drawContinuousTexturedBox(pos, yPosition, 0, 66, 8, height, 200, 20, 2, 3, 2, 2, 0);
        BRUtil.drawCenteredScaledString(
                getText(mc.fontRenderer),
                xPosition + (double) width / 2,
                yPosition - 1 + height - (mc.fontRenderer.FONT_HEIGHT * scale),
                isMouseOver(mouseX, mouseY) ? 0xFFFFA0 : 0xE0E0E0,
                scale);
    }

    public void setValue(int newValue) {
        setValue(newValue, true);
    }

    public void setValue(int newValue, boolean notify) {
        value = MathHelper.clamp_int(newValue, minValue, maxValue);
        if (notify && valueListener != null) valueListener.accept(value);
    }

    public void setValueFromMouse(int mouseX) {
        setValue(Math.round(minValue + (maxValue - minValue) * (float) (mouseX - xPosition + 4) / (width - 8)));
    }

    public String getText(FontRenderer font) {
        String val = ": " + (valueStringSupplier == null ? value + "" : valueStringSupplier.apply(value));
        String trimmed = font.trimStringToWidth(name, (int) (width / scale) - font.getStringWidth(val));
        return trimmed + val;
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        setValueFromMouse(mouseX);
        dragging = true;
        playClickSound();
        return true;
    }

    @Override
    public void mouseDragged(int mousePosX, int mousePosY) {
        if (dragging) {
            setValueFromMouse(mousePosX);
        }
    }

    @Override
    public boolean mouseScrolled(int mouseX, int mouseY, int scroll) {
        if (scroll == 0 || !isMouseOver(mouseX, mouseY) && !dragging) return false;
        if (scroll > 0) setValue(value + 1);
        else setValue(value - 1);
        return true;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        dragging = false;
    }
}
