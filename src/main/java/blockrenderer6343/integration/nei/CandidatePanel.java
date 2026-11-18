package blockrenderer6343.integration.nei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.api.ShortcutInputHandler;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.recipe.AcceptsFollowingTooltipLineHandler;
import codechicken.nei.scroll.ScrollBar;
import codechicken.nei.scroll.ScrollBar.OverflowType;
import codechicken.nei.scroll.ScrollContainer;

public class CandidatePanel extends ScrollContainer {

    private static final int SLOT_SIZE = 18;
    private final List<PositionedStack> candidates = new ArrayList<>();
    private int[] permutations = new int[0];
    private int[] permutationOffsets = new int[0];
    private int columns = 1;
    private int cycle;
    private int reportedCycle;
    private AcceptsFollowingTooltipLineHandler acceptsTooltip;

    public CandidatePanel() {
        setVerticalScroll(ScrollBar.defaultVerticalBar().setOverflowType(OverflowType.AUTO));
    }

    public void setCandidates(List<List<ItemStack>> results) {
        getVerticalScroll().onClickReleased(this, 0, 0, 0);
        candidates.clear();
        acceptsTooltip = null;
        for (List<ItemStack> result : results) {
            candidates.add(new PositionedStack(result, 0, 0));
        }
        permutations = new int[candidates.size()];
        permutationOffsets = new int[candidates.size()];
        yScroll = 0;
        actualHeight = 0;
        w = h = 0;
    }

    public void layout(int right, int top, int maxWidth, int maxHeight) {
        if (maxWidth < SLOT_SIZE + getVerticalScroll().getTrackWidth() || maxHeight < SLOT_SIZE) {
            w = h = 0;
            return;
        }
        columns = Math.max(1, Math.min(8, maxWidth / SLOT_SIZE));
        int rows = (candidates.size() + columns - 1) / columns;
        boolean overflow = rows * SLOT_SIZE > maxHeight;
        int barWidth = overflow ? getVerticalScroll().getTrackWidth() : 0;
        columns = Math.max(1, Math.min(columns, (maxWidth - barWidth) / SLOT_SIZE));
        columns = Math.min(columns, Math.max(1, candidates.size()));
        rows = (candidates.size() + columns - 1) / columns;
        w = actualWidth = columns * SLOT_SIZE;
        actualHeight = rows * SLOT_SIZE;
        h = Math.min(actualHeight, maxHeight);
        x = right - w - barWidth;
        y = top;
        setVerticalScrollOffset(yScroll);
        if (!canScrollVertical()) getVerticalScroll().onClickReleased(this, 0, 0, 0);
    }

    public void setCycle(int cycle) {
        int elapsed = cycle - reportedCycle;
        reportedCycle = cycle;
        if (!NEIClientUtils.shiftKey()) this.cycle += elapsed;
    }

    public boolean isVisible() {
        return !candidates.isEmpty() && w > 0 && h > 0;
    }

    @Override
    public boolean contains(int mouseX, int mouseY) {
        return isVisible() && boundsOutside().contains(mouseX, mouseY);
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        if (isVisible()) {
            if (!Mouse.isButtonDown(0)) mouseUp(mouseX, mouseY, 0);
            super.draw(mouseX, mouseY);
        }
    }

    @Override
    protected void drawContent(int mouseX, int mouseY) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            NEIClientUtils.gl2DRenderContext(() -> GuiDraw.drawRect(x, y, w, h, 0xE0202020));
            RenderHelper.enableGUIStandardItemLighting();
            GL11.glColor4f(1, 1, 1, 1);
            int start = (yScroll / SLOT_SIZE) * columns;
            int end = Math.min(candidates.size(), ((yScroll + h + SLOT_SIZE - 1) / SLOT_SIZE) * columns);
            for (int index = start; index < end; index++) {
                PositionedStack stack = candidates.get(index);
                int permutation = Math.floorMod(cycle + permutationOffsets[index], stack.items.length);
                if (permutations[index] != permutation) {
                    stack.setPermutationToRender(permutation);
                    permutations[index] = permutation;
                }
                int slotX = x + index % columns * SLOT_SIZE;
                int slotY = y + index / columns * SLOT_SIZE - yScroll;
                GuiContainerManager.drawItem(slotX + 1, slotY + 1, stack.item);
                if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE
                        && mouseY >= slotY
                        && mouseY < slotY + SLOT_SIZE
                        && boundsInside().contains(mouseX, mouseY)) {
                    NEIClientUtils
                            .gl2DRenderContext(() -> GuiDraw.drawRect(slotX, slotY, SLOT_SIZE, SLOT_SIZE, 0x60FFFFFF));
                }
            }
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    @Override
    public ItemStack getStackMouseOver(int mouseX, int mouseY) {
        PositionedStack hovered = getHoveredCandidate(mouseX, mouseY);
        return hovered == null ? null : hovered.item;
    }

    private PositionedStack getHoveredCandidate(int mouseX, int mouseY) {
        int index = getHoveredIndex(mouseX, mouseY);
        return index < 0 ? null : candidates.get(index);
    }

    private int getHoveredIndex(int mouseX, int mouseY) {
        if (!isVisible() || !boundsInside().contains(mouseX, mouseY)) return -1;
        int index = ((mouseY - y + yScroll) / SLOT_SIZE) * columns + (mouseX - x) / SLOT_SIZE;
        return index < candidates.size() ? index : -1;
    }

    public void appendItemTooltip(int mouseX, int mouseY, List<String> tooltip) {
        PositionedStack hovered = getHoveredCandidate(mouseX, mouseY);
        if (hovered == null || hovered.items.length <= 1 || !NEIClientConfig.showCycledIngredientsTooltip()) {
            acceptsTooltip = null;
            return;
        }
        if (acceptsTooltip == null || acceptsTooltip.tooltipGUID != hovered) {
            acceptsTooltip = AcceptsFollowingTooltipLineHandler
                    .of(hovered, new ArrayList<>(Arrays.asList(hovered.items)), hovered.item);
        }
        if (acceptsTooltip != null) {
            acceptsTooltip.setActiveStack(hovered.item);
            tooltip.add(GuiDraw.TOOLTIP_HANDLER + GuiDraw.getTipLineId(acceptsTooltip));
        }
    }

    @Override
    public boolean handleClick(int mouseX, int mouseY, int button) {
        if (!contains(mouseX, mouseY)) return false;
        if (button == 0 && super.handleClick(mouseX, mouseY, button)) return true;
        ItemStack stack = getStackMouseOver(mouseX, mouseY);
        if (stack != null) ShortcutInputHandler.handleMouseClick(stack);
        return true;
    }

    @Override
    public boolean onMouseWheel(int direction, int mouseX, int mouseY) {
        if (getVerticalScroll().isMouseDragged()) return true;
        if (!contains(mouseX, mouseY)) return false;
        if (direction == 0) return true;
        int index = getHoveredIndex(mouseX, mouseY);
        if (NEIClientUtils.shiftKey() && index >= 0) {
            PositionedStack hovered = candidates.get(index);
            if (hovered.items.length > 1) {
                int permutation = Math.floorMod(permutations[index] + (direction > 0 ? -1 : 1), hovered.items.length);
                hovered.setPermutationToRender(permutation);
                permutations[index] = permutation;
                permutationOffsets[index] = permutation - Math.floorMod(cycle, hovered.items.length);
                return true;
            }
        }
        super.onMouseWheel(direction, mouseX, mouseY);
        return true;
    }
}
