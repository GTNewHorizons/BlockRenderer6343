package blockrenderer6343.client.utils;

import static blockrenderer6343.integration.nei.GuiMultiblockHandler.SCENE_HEIGHT;

import java.util.function.IntSupplier;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.MathHelper;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.config.GuiUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import it.unimi.dsi.fastutil.ints.IntConsumer;

public class GuiSlider extends BRButton {

    private int maxValue;
    private int minValue;
    private int value;
    private IntConsumer valueListener;
    private IntConsumer releaseListener;
    private IntSupplier maxValueSupplier;
    private Int2ObjectFunction<String> valueStringSupplier;
    public final String name;
    private float scale;
    private final int originalHeight;
    private boolean dragging;
    private boolean deferred;
    private boolean inputMode;
    private boolean allowUnset;
    private boolean unset;
    private boolean dragChanged;
    private GuiTextField inputField;

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

    public GuiSlider setDeferredValueListener(@NotNull IntConsumer listener, @NotNull IntConsumer releaseListener) {
        this.valueListener = listener;
        this.releaseListener = releaseListener;
        this.deferred = true;
        return this;
    }

    public GuiSlider setTextSupplier(@NotNull Int2ObjectFunction<String> supplier) {
        this.valueStringSupplier = supplier;
        return this;
    }

    public GuiSlider setAllowUnset(boolean allowUnset) {
        this.allowUnset = allowUnset;
        ensureInputField();
        return this;
    }

    public GuiSlider setUnset(boolean unset) {
        this.unset = allowUnset && unset;
        updateInputText();
        return this;
    }

    public boolean isUnset() {
        return unset;
    }

    public boolean isInputMode() {
        return inputMode;
    }

    public void toggleInputMode() {
        ensureInputField();
        if (inputMode) commitInput();
        inputMode = !inputMode;
        inputField.setFocused(inputMode);
        if (inputMode) inputField.setText(unset ? "" : String.valueOf(value));
    }

    private void ensureInputField() {
        if (inputField != null) return;
        inputField = new GuiTextField(Minecraft.getMinecraft().fontRenderer, xPosition, yPosition, width, height);
        inputField.setEnableBackgroundDrawing(true);
        inputField.setMaxStringLength(12);
    }

    private void updateInputText() {
        if (inputField != null) inputField.setText(unset ? "No Set" : String.valueOf(value));
    }

    private void commitInput() {
        if (inputField == null) return;
        String text = inputField.getText().trim();
        if (text.isEmpty() || text.equalsIgnoreCase("no set") || text.equalsIgnoreCase("unset")) {
            if (allowUnset) setUnsetValue();
            else updateInputText();
            return;
        }
        try {
            long parsed = Long.parseLong(text);
            maxValue = getMaxValue();
            if (allowUnset && parsed < minValue) {
                setUnsetValue();
                return;
            }
            int bounded = parsed < minValue ? minValue : parsed > maxValue ? maxValue : (int) parsed;
            boolean changed = unset || value != bounded;
            unset = false;
            setValue(bounded);
            if (changed) finishDeferredChange();
        } catch (NumberFormatException ignored) {
            updateInputText();
        }
    }

    private void setUnsetValue() {
        boolean changed = !unset;
        unset = true;
        updateInputText();
        if (changed) notifyValue();
        if (changed) finishDeferredChange();
    }

    private void notifyValue() {
        if (valueListener != null) valueListener.accept(value);
    }

    private void finishDeferredChange() {
        if (deferred && releaseListener != null) releaseListener.accept(value);
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
        double range = (double) maxValue - minValue;
        double sliderValue = unset ? 0 : range == 0 ? 1 : (value - (double) minValue) / range;
        int pos = MathHelper.clamp_int((int) (xPosition + sliderValue * (width - 8)), xPosition, xPosition + width - 8);
        GuiUtils.drawContinuousTexturedBox(pos, yPosition, 0, 66, 8, height, 200, 20, 2, 3, 2, 2, 0);
        if (inputMode && inputField != null) {
            inputField.xPosition = xPosition;
            inputField.yPosition = yPosition;
            inputField.width = width;
            inputField.height = height;
            inputField.drawTextBox();
        } else {
            BRUtil.drawCenteredScaledString(
                    getText(mc.fontRenderer),
                    xPosition + (double) width / 2,
                    yPosition - 1 + height - (mc.fontRenderer.FONT_HEIGHT * scale),
                    isMouseOver(mouseX, mouseY) ? 0xFFFFA0 : 0xE0E0E0,
                    scale);
        }
    }

    public void setValue(int newValue) {
        setValue(newValue, true);
    }

    public void setValue(int newValue, boolean notify) {
        maxValue = getMaxValue();
        boolean changed = unset || value != MathHelper.clamp_int(newValue, minValue, maxValue);
        value = MathHelper.clamp_int(newValue, minValue, maxValue);
        unset = false;
        updateInputText();
        if (notify && changed && valueListener != null) valueListener.accept(value);
        if (dragging && changed) dragChanged = true;
    }

    public void setValueFromMouse(int mouseX) {
        double range = (double) maxValue - minValue;
        if (range == 0) {
            if (allowUnset && mouseX <= xPosition + 4) {
                boolean changed = !unset;
                if (changed) dragChanged = true;
                unset = true;
                if (changed) notifyValue();
                updateInputText();
            } else {
                setValue(minValue);
            }
            return;
        }
        double fraction = (double) (mouseX - xPosition - 4) / (width - 8);
        double candidate = minValue + range * fraction;
        if (allowUnset && candidate <= minValue) {
            boolean changed = !unset;
            if (changed && dragging) dragChanged = true;
            unset = true;
            if (changed) notifyValue();
            updateInputText();
            return;
        }
        if (candidate <= Integer.MIN_VALUE) {
            setValue(Integer.MIN_VALUE);
        } else if (candidate >= Integer.MAX_VALUE) {
            setValue(Integer.MAX_VALUE);
        } else {
            setValue((int) Math.round(candidate));
        }
    }

    public String getText(FontRenderer font) {
        if (unset) return name + ": No Set";
        String val = ": " + (valueStringSupplier == null ? value + "" : valueStringSupplier.apply(value));
        String trimmed = font.trimStringToWidth(name, (int) (width / scale) - font.getStringWidth(val));
        return trimmed + val;
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        if (inputMode) {
            inputField.mouseClicked(mouseX, mouseY, 0);
            return true;
        }
        dragging = true;
        dragChanged = false;
        setValueFromMouse(mouseX);
        playClickSound();
        return true;
    }

    @Override
    public void mouseDragged(int mousePosX, int mousePosY) {
        if (dragging && !inputMode) {
            setValueFromMouse(mousePosX);
        }
    }

    @Override
    public boolean mouseScrolled(int mouseX, int mouseY, int scroll) {
        if (scroll == 0 || !isMouseOver(mouseX, mouseY) && !dragging) return false;
        maxValue = getMaxValue();
        boolean changedBefore = unset;
        int previousValue = value;
        if (unset) {
            if (scroll > 0) setValue(minValue);
        } else if (scroll > 0) {
            if (value < maxValue) setValue(value + 1);
        } else if (value > minValue) {
            setValue(value - 1);
        } else if (allowUnset) {
            unset = true;
            notifyValue();
            updateInputText();
        }
        if (deferred && (changedBefore != unset || previousValue != value)) finishDeferredChange();
        return true;
    }

    public boolean keyTyped(char keyChar, int keyCode) {
        if (!inputMode || inputField == null) return false;
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            commitInput();
            return true;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            inputMode = false;
            inputField.setFocused(false);
            updateInputText();
            return true;
        }
        if (keyChar >= ' ' && (keyChar < '0' || keyChar > '9')) return true;
        inputField.textboxKeyTyped(keyChar, keyCode);
        return true;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        boolean wasDragging = dragging;
        dragging = false;
        if (wasDragging && dragChanged) finishDeferredChange();
        dragChanged = false;
    }

    @Override
    public @NotNull List<String> getTooltip(Vector2i mousePos) {
        if (!isMouseOver(mousePos)) return Collections.emptyList();
        return Collections.singletonList(minValue + "~" + getMaxValue());
    }
}
