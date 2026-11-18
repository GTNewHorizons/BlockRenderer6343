package blockrenderer6343.integration.nei;

import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import codechicken.nei.guihook.IContainerInputHandler;
import codechicken.nei.guihook.IContainerObjectHandler;
import codechicken.nei.guihook.IContainerTooltipHandler;

public class InputHandler implements IContainerInputHandler, IContainerTooltipHandler, IContainerObjectHandler {

    public boolean canHandle(GuiContainer gui) {
        return MultiblockHandler.getHandlerFromGui(gui) != null;
    }

    @Override
    public boolean mouseClicked(GuiContainer gui, int mousex, int mousey, int button) {
        MultiblockHandler handler = MultiblockHandler.getHandlerFromGui(gui);
        if (handler != null) {
            if (handler.getCandidatePanel().handleClick(mousex, mousey, button)) {
                handler.getGuiHandler().cancelCameraDrag();
                return true;
            }
            handler.getGuiHandler().updateMousePosition(mousex, mousey);
            return handler.getGuiHandler().mouseClicked(button);
        }
        return false;
    }

    @Override
    public boolean lastKeyTyped(GuiContainer gui, char keyChar, int keyCode) {
        return false;
    }

    @Override
    public List<String> handleTooltip(GuiContainer gui, int mouseX, int mouseY, List<String> currenttip) {
        MultiblockHandler handler = MultiblockHandler.getHandlerFromGui(gui);
        if (handler != null && !handler.getCandidatePanel().contains(mouseX, mouseY)) {
            currenttip.addAll(handler.getGuiHandler().getTooltip());
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
        MultiblockHandler handler = MultiblockHandler.getHandlerFromGui(gui);
        if (handler != null) handler.getCandidatePanel().appendItemTooltip(mousex, mousey, currenttip);
        return currenttip;
    }

    @Override
    public boolean keyTyped(GuiContainer gui, char keyChar, int keyCode) {
        MultiblockHandler handler = MultiblockHandler.getHandlerFromGui(gui);
        return handler != null && handler.getGuiHandler().keyTyped(keyChar, keyCode);
    }

    @Override
    public void onKeyTyped(GuiContainer gui, char keyChar, int keyID) {}

    @Override
    public void onMouseClicked(GuiContainer gui, int mousex, int mousey, int button) {}

    @Override
    public void onMouseUp(GuiContainer gui, int mousex, int mousey, int button) {
        MultiblockHandler handler = MultiblockHandler.getHandlerFromGui(gui);
        if (handler != null) {
            if (button == 0) handler.getCandidatePanel().mouseUp(mousex, mousey, button);
            handler.getGuiHandler().updateMousePosition(mousex, mousey);
            handler.getGuiHandler().onMouseReleased(button);
        }
    }

    @Override
    public boolean mouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) {
        MultiblockHandler handler = MultiblockHandler.getHandlerFromGui(gui);
        if (handler != null) {
            if (handler.getCandidatePanel().onMouseWheel(scrolled, mousex, mousey)) return true;
            handler.getGuiHandler().updateMousePosition(mousex, mousey);
            return handler.getGuiHandler().handleMouseScrollUp(scrolled);
        }
        return false;
    }

    @Override
    public void onMouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) {}

    @Override
    public void onMouseDragged(GuiContainer gui, int amousex, int amousey, int button, long heldTime) {
        MultiblockHandler handler = MultiblockHandler.getHandlerFromGui(gui);
        if (handler != null && button == 0) {
            handler.getCandidatePanel().mouseDragged(amousex, amousey, button, heldTime);
            handler.getGuiHandler().updateMousePosition(amousex, amousey);
            handler.getGuiHandler().onMouseDragged();
        }
    }

    @Override
    public ItemStack getStackUnderMouse(GuiContainer gui, int mousex, int mousey) {
        MultiblockHandler handler = MultiblockHandler.getHandlerFromGui(gui);
        return handler == null ? null : handler.getCandidatePanel().getStackMouseOver(mousex, mousey);
    }

    @Override
    public boolean objectUnderMouse(GuiContainer gui, int mousex, int mousey) {
        MultiblockHandler handler = MultiblockHandler.getHandlerFromGui(gui);
        return handler != null && handler.getCandidatePanel().contains(mousex, mousey);
    }

    @Override
    public boolean shouldShowTooltip(GuiContainer gui) {
        MultiblockHandler handler = MultiblockHandler.getHandlerFromGui(gui);
        return handler == null || !handler.getCandidatePanel().getVerticalScroll().isMouseDragged();
    }

    @Override
    public void guiTick(GuiContainer gui) {}

    @Override
    public void refresh(GuiContainer gui) {}

    @Override
    public void load(GuiContainer gui) {}
}
