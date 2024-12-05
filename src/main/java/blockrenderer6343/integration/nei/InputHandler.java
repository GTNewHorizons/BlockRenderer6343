package blockrenderer6343.integration.nei;

import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import codechicken.nei.guihook.IContainerInputHandler;
import codechicken.nei.guihook.IContainerTooltipHandler;

public class InputHandler implements IContainerInputHandler, IContainerTooltipHandler {

    private MultiblockHandler activeHandler;

    public boolean canHandle(GuiContainer gui) {
        return (activeHandler = MultiblockHandler.getHandlerFromGui(gui)) != null;
    }

    @Override
    public boolean mouseClicked(GuiContainer gui, int mousex, int mousey, int button) {
        if (canHandle(gui)) {
            return activeHandler.getGuiHandler().mouseClicked(button);
        }
        return false;
    }

    @Override
    public boolean lastKeyTyped(GuiContainer gui, char keyChar, int keyCode) {
        return false;
    }

    @Override
    public List<String> handleTooltip(GuiContainer gui, int mouseX, int mouseY, List<String> currenttip) {
        if (canHandle(gui)) {
            currenttip.addAll(activeHandler.getGuiHandler().getTooltip());
        }
        return currenttip;
    }

    @Override
    public List<String> handleItemDisplayName(GuiContainer gui, ItemStack itemstack, List<String> currenttip) {
        return currenttip;
    }

    @Override
    public List<String> handleItemTooltip(GuiContainer gui, ItemStack itemstack, int mousex, int mousey,
            List<String> currenttip) {
        return currenttip;
    }

    @Override
    public boolean keyTyped(GuiContainer gui, char keyChar, int keyCode) {
        return false;
    }

    @Override
    public void onKeyTyped(GuiContainer gui, char keyChar, int keyID) {}

    @Override
    public void onMouseClicked(GuiContainer gui, int mousex, int mousey, int button) {}

    @Override
    public void onMouseUp(GuiContainer gui, int mousex, int mousey, int button) {
        if (canHandle(gui) && button == 0) {
            activeHandler.getGuiHandler().onMouseReleased();
        }
    }

    @Override
    public boolean mouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) {
        if (canHandle(gui)) {
            return activeHandler.getGuiHandler().handleMouseScrollUp(scrolled);
        }
        return false;
    }

    @Override
    public void onMouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) {}

    @Override
    public void onMouseDragged(GuiContainer gui, int amousex, int amousey, int button, long heldTime) {
        if (canHandle(gui) && button == 0) {
            activeHandler.getGuiHandler().onMouseDragged();
        }
    }
}
