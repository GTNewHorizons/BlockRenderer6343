package blockrenderer6343.common;

import blockrenderer6343.BlockRenderer6343;
import blockrenderer6343.api.utils.BlockPosition;
import blockrenderer6343.api.utils.PositionedIStructureElement;
import blockrenderer6343.client.renderer.GlStateManager;
import blockrenderer6343.client.renderer.ImmediateWorldSceneRenderer;
import blockrenderer6343.client.renderer.WorldSceneRenderer;
import blockrenderer6343.client.world.TrackedDummyWorld;
import blockrenderer6343.mixins.GuiContainerMixin;
import codechicken.lib.gui.GuiDraw;
import codechicken.lib.math.MathHelper;
import codechicken.nei.NEIClientUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Vector3f;

public abstract class GUI_MultiblocksHandler<T> {
    protected static ImmediateWorldSceneRenderer renderer;

    public static final int SLOT_SIZE = 18;

    protected static final int recipeLayoutx = 8;
    protected static final int recipeLayouty = 50;
    protected static final int recipeWidth = 160;
    protected static final int sceneHeight = recipeWidth - 10;
    protected static int guiMouseX;
    protected static int guiMouseY;
    protected static int lastGuiMouseX;
    protected static int lastGuiMouseY;
    protected static Vector3f center;
    protected static float rotationYaw;
    protected static float rotationPitch;
    protected static float zoom;

    protected static ItemStack tooltipBlockStack;
    protected static BlockPosition selectedBlock;

    protected static int layerIndex = -1;

    protected List<ItemStack> ingredients = new ArrayList<>();
    protected List<PositionedIStructureElement> structureElements = new ArrayList<>();

    protected static final int ICON_SIZE_X = 20;
    protected static final int ICON_SIZE_Y = 20;
    protected static final int mouseOffsetX = 5;
    protected static final int mouseOffsetY = 43;
    protected static final int buttonsEndPosX = 165;
    protected static final int buttonsEndPosY = 155;
    protected static final int buttonsStartPosX = buttonsEndPosX - ICON_SIZE_X * 2 - 10;
    protected static final int buttonsStartPosY = buttonsEndPosY - ICON_SIZE_Y * 2 - 10;
    protected static final Map<GuiButton, Runnable> buttons = new HashMap<>();

    protected Consumer<List<ItemStack>> onIngredientChanged;

    protected T renderingController;
    protected T lastRenderingController;

    public GUI_MultiblocksHandler() {
        buttons.clear();

        GuiButton previousLayerButton =
                new GuiButton(0, buttonsStartPosX, buttonsEndPosY - ICON_SIZE_Y, ICON_SIZE_X, ICON_SIZE_Y, "<");
        GuiButton nextLayerButton = new GuiButton(
                0, buttonsEndPosX - ICON_SIZE_X, buttonsEndPosY - ICON_SIZE_Y, ICON_SIZE_X, ICON_SIZE_Y, ">");

        buttons.put(previousLayerButton, this::togglePreviousLayer);
        buttons.put(nextLayerButton, this::toggleNextLayer);
    }

    public void loadMultiblock(T multiblock) {
        renderingController = multiblock;
        if (lastRenderingController != renderingController) {
            loadNewMultiblock();
        } else {
            loadPreviousMultiblockAgain();
        }
    }

    protected void loadNewMultiblock() {
        layerIndex = -1;
        initializeSceneRenderer(true);
        lastRenderingController = renderingController;
    }

    protected void loadPreviousMultiblockAgain() {
        initializeSceneRenderer(false);
    }

    public void setOnIngredientChanged(Consumer<List<ItemStack>> callback) {
        onIngredientChanged = callback;
    }

    private void toggleNextLayer() {
        int height = (int) ((TrackedDummyWorld) renderer.world).getSize().getY() - 1;
        if (++layerIndex > height) {
            // if current layer index is more than max height, reset it
            // to display all layers
            layerIndex = -1;
        }
        setNextLayer(layerIndex);
    }

    private void togglePreviousLayer() {
        int height = (int) ((TrackedDummyWorld) renderer.world).getSize().getY() - 1;
        if (layerIndex == -1) {
            layerIndex = height;
        } else if (--layerIndex < 0) {
            layerIndex = -1;
        }
        setNextLayer(layerIndex);
    }

    private void setNextLayer(int newLayer) {
        layerIndex = newLayer;
        if (renderer != null) {
            TrackedDummyWorld world = ((TrackedDummyWorld) renderer.world);
            resetCenter();
            renderer.renderedBlocks.clear();
            int minY = (int) world.getMinPos().getY();
            List<BlockPosition> renderBlocks;
            if (newLayer == -1) {
                renderBlocks = world.placedBlocks;
                renderer.setRenderAllFaces(false);
            } else {
                renderBlocks = world.placedBlocks.stream()
                        .filter(pos -> pos.y - minY == newLayer)
                        .collect(Collectors.toList());
                renderer.setRenderAllFaces(true);
            }
            renderer.addRenderedBlocks(renderBlocks);
            scanIngredients();
        }
    }

    private void resetCenter() {
        TrackedDummyWorld world = (TrackedDummyWorld) renderer.world;
        Vector3f size = world.getSize();
        Vector3f minPos = world.getMinPos();
        center = new Vector3f(minPos.x + size.x / 2, minPos.y + size.y / 2, minPos.z + size.z / 2);
        renderer.setCameraLookAt(center, zoom, Math.toRadians(rotationPitch), Math.toRadians(rotationYaw));
    }

    public void drawMultiblock() {
        guiMouseX = GuiDraw.getMousePosition().x;
        guiMouseY = GuiDraw.getMousePosition().y;
        int k = (NEIClientUtils.getGuiContainer().width
                        - ((GuiContainerMixin) NEIClientUtils.getGuiContainer()).getXSize())
                / 2;
        int l = (NEIClientUtils.getGuiContainer().height
                        - ((GuiContainerMixin) NEIClientUtils.getGuiContainer()).getYSize())
                / 2;
        renderer.render(recipeLayoutx + k, recipeLayouty + l, recipeWidth, sceneHeight, lastGuiMouseX, lastGuiMouseY);
        drawMultiblockName();

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        tooltipBlockStack = null;

        MovingObjectPosition rayTraceResult = renderer.getLastTraceResult();
        boolean insideView = guiMouseX >= k + recipeLayoutx
                && guiMouseY >= l + recipeLayouty
                && guiMouseX < k + recipeLayoutx + recipeWidth
                && guiMouseY < l + recipeLayouty + sceneHeight;
        boolean leftClickHeld = Mouse.isButtonDown(0);
        boolean rightClickHeld = Mouse.isButtonDown(1);
        if (insideView) {
            if (leftClickHeld) {
                rotationPitch += guiMouseX - lastGuiMouseX + 360;
                rotationPitch = rotationPitch % 360;
                rotationYaw = (float) MathHelper.clip(rotationYaw + (guiMouseY - lastGuiMouseY), -89.9, 89.9);
            } else if (rightClickHeld) {
                int mouseDeltaY = guiMouseY - lastGuiMouseY;
                if (Math.abs(mouseDeltaY) > 1) {
                    zoom = (float) MathHelper.clip(zoom + (mouseDeltaY > 0 ? 0.5 : -0.5), 3, 999);
                }
            }
            renderer.setCameraLookAt(center, zoom, Math.toRadians(rotationPitch), Math.toRadians(rotationYaw));
        }

        // draw buttons
        for (GuiButton button : buttons.keySet()) {
            button.drawButton(Minecraft.getMinecraft(), guiMouseX - k - mouseOffsetX, guiMouseY - l - mouseOffsetY);
        }
        drawButtonsTitle();

        if (!(leftClickHeld || rightClickHeld)
                && rayTraceResult != null
                && !renderer.world.isAirBlock(rayTraceResult.blockX, rayTraceResult.blockY, rayTraceResult.blockZ)) {
            Block block = renderer.world.getBlock(rayTraceResult.blockX, rayTraceResult.blockY, rayTraceResult.blockZ);
            tooltipBlockStack = block.getPickBlock(
                    rayTraceResult,
                    renderer.world,
                    rayTraceResult.blockX,
                    rayTraceResult.blockY,
                    rayTraceResult.blockZ,
                    Minecraft.getMinecraft().thePlayer);
        }

        lastGuiMouseX = guiMouseX;
        lastGuiMouseY = guiMouseY;

        //        don't activate these
        //        GlStateManager.disableRescaleNormal();
        //        GlStateManager.disableLighting();
        //        RenderHelper.disableStandardItemLighting();
    }

    private void drawMultiblockName() {
        String localizedName = getMultiblockName();
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        List<String> lines = fontRenderer.listFormattedStringToWidth(localizedName, recipeWidth - 10);
        for (int i = 0; i < lines.size(); i++) {
            fontRenderer.drawString(
                    lines.get(i),
                    (recipeWidth - fontRenderer.getStringWidth(lines.get(i))) / 2,
                    fontRenderer.FONT_HEIGHT * i,
                    0x333333);
        }
    }

    protected abstract String getMultiblockName();

    protected void drawButtonsTitle() {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        String layerText = "Layer: " + (layerIndex == -1 ? "A" : Integer.toString(layerIndex + 1));
        fontRenderer.drawString(
                layerText,
                buttonsStartPosX + (buttonsEndPosX - buttonsStartPosX - fontRenderer.getStringWidth(layerText)) / 2,
                buttonsStartPosY + ICON_SIZE_Y,
                0x333333);
    }

    protected void initializeSceneRenderer(boolean resetCamera) {
        Vector3f eyePos = new Vector3f(), lookAt = new Vector3f(), worldUp = new Vector3f();
        if (!resetCamera) {
            try {
                eyePos = renderer.getEyePos();
                lookAt = renderer.getLookAt();
                worldUp = renderer.getWorldUp();
            } catch (NullPointerException e) {
                BlockRenderer6343.error("please reset camera on your first renderer call!");
            }
        }

        renderer = new ImmediateWorldSceneRenderer(new TrackedDummyWorld());
        renderer.world.updateEntities();
        renderer.setClearColor(0xC6C6C6);

        placeMultiblock();

        Vector3f size = ((TrackedDummyWorld) renderer.world).getSize();
        Vector3f minPos = ((TrackedDummyWorld) renderer.world).getMinPos();
        center = new Vector3f(minPos.x + size.x / 2, minPos.y + size.y / 2, minPos.z + size.z / 2);

        renderer.renderedBlocks.clear();
        renderer.addRenderedBlocks(((TrackedDummyWorld) renderer.world).placedBlocks);
        renderer.setOnLookingAt(ray -> {});

        renderer.setOnWorldRender(this::onRendererRender);
        //        world.setRenderFilter(pos -> worldSceneRenderer.renderedBlocksMap.keySet().stream().anyMatch(c ->
        // c.contains(pos)));

        selectedBlock = null;
        setNextLayer(layerIndex);

        if (resetCamera) {
            float max = Math.max(Math.max(Math.max(size.x, size.y), size.z), 1);
            zoom = (float) (3.5 * Math.sqrt(max));
            rotationYaw = 20.0f;
            rotationPitch = 50f;
            if (renderer != null) {
                resetCenter();
            }
        } else {
            renderer.setCameraLookAt(eyePos, lookAt, worldUp);
        }
    }

    protected abstract void placeMultiblock();

    public void onRendererRender(WorldSceneRenderer renderer) {
        BlockPosition look = renderer.getLastTraceResult() == null
                ? null
                : new BlockPosition(
                        renderer.getLastTraceResult().blockX,
                        renderer.getLastTraceResult().blockY,
                        renderer.getLastTraceResult().blockZ);
        if (look != null && look.equals(selectedBlock)) {
            renderBlockOverLay(selectedBlock, Blocks.glass.getIcon(0, 6));
            return;
        }
        renderBlockOverLay(look, Blocks.stained_glass.getIcon(0, 7));
        renderBlockOverLay(selectedBlock, Blocks.stained_glass.getIcon(0, 14));
    }

    private void scanIngredients() {
        List<ItemStack> ingredients = new ArrayList<>();
        for (BlockPosition renderedBlock : renderer.renderedBlocks) {
            Block block = renderer.world.getBlock(renderedBlock.x, renderedBlock.y, renderedBlock.z);
            if (block.equals(Blocks.air)) continue;
            int meta = renderer.world.getBlockMetadata(renderedBlock.x, renderedBlock.y, renderedBlock.z);
            ArrayList<ItemStack> itemstacks =
                    block.getDrops(renderer.world, renderedBlock.x, renderedBlock.y, renderedBlock.z, meta, 0);
            if (itemstacks.size() == 0) { // glass
                itemstacks.add(new ItemStack(block));
            }
            boolean added = false;
            for (ItemStack ingredient : ingredients) {
                if (NEIClientUtils.areStacksSameTypeWithNBT(ingredient, itemstacks.get(0))) {
                    ingredient.stackSize++;
                    added = true;
                    break;
                }
            }
            if (!added) ingredients.add(itemstacks.get(0));
        }
        this.ingredients = ingredients;

        if (onIngredientChanged != null) {
            onIngredientChanged.accept(ingredients);
        }
    }

    private void renderBlockOverLay(BlockPosition pos, IIcon icon) {
        if (pos == null) return;

        RenderBlocks bufferBuilder = new RenderBlocks();
        bufferBuilder.blockAccess = renderer.world;
        bufferBuilder.setRenderBounds(0, 0, 0, 1, 1, 1);
        bufferBuilder.renderAllFaces = true;
        Block block = renderer.world.getBlock(pos.x, pos.y, pos.z);
        bufferBuilder.renderBlockUsingTexture(block, pos.x, pos.y, pos.z, icon);
    }

    public boolean mouseClicked(int button) {
        for (Map.Entry<GuiButton, Runnable> buttons : buttons.entrySet()) {
            int k = (NEIClientUtils.getGuiContainer().width
                            - ((GuiContainerMixin) NEIClientUtils.getGuiContainer()).getXSize())
                    / 2;
            int l = (NEIClientUtils.getGuiContainer().height
                            - ((GuiContainerMixin) NEIClientUtils.getGuiContainer()).getYSize())
                    / 2;
            if (buttons.getKey()
                    .mousePressed(
                            Minecraft.getMinecraft(), guiMouseX - k - mouseOffsetX, guiMouseY - l - mouseOffsetY)) {
                buttons.getValue().run();
                selectedBlock = null;
                return true;
            }
        }
        if (button == 1 && renderer != null) {
            if (renderer.getLastTraceResult() == null) {
                if (selectedBlock != null) {
                    selectedBlock = null;
                    return true;
                }
                return false;
            }
            selectedBlock = new BlockPosition(
                    renderer.getLastTraceResult().blockX,
                    renderer.getLastTraceResult().blockY,
                    renderer.getLastTraceResult().blockZ);
            onBlockSelected();
        }
        return false;
    }

    protected abstract void onBlockSelected();

    public List<String> handleTooltip() {
        if (tooltipBlockStack != null)
            return tooltipBlockStack.getTooltip(
                    Minecraft.getMinecraft().thePlayer, Minecraft.getMinecraft().gameSettings.advancedItemTooltips);
        else return null;
    }
}
