package blockrenderer6343.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

import com.github.vfyjxf.nee.network.NEENetworkHandler;
import com.github.vfyjxf.nee.network.packet.PacketNEIPatternRecipe;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.constructable.ConstructableUtility;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IStructureElement;

import blockrenderer6343.BlockRenderer6343;
import blockrenderer6343.api.utils.BlockPosition;
import blockrenderer6343.api.utils.CreativeItemSource;
import blockrenderer6343.api.utils.PositionedIStructureElement;
import blockrenderer6343.client.renderer.ImmediateWorldSceneRenderer;
import blockrenderer6343.client.renderer.WorldSceneRenderer;
import blockrenderer6343.client.utils.GuiText;
import blockrenderer6343.client.utils.TooltipButton;
import blockrenderer6343.client.world.DummyWorld;
import blockrenderer6343.client.world.TrackedDummyWorld;
import codechicken.lib.gui.GuiDraw;
import codechicken.lib.math.MathHelper;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.recipe.GuiRecipe;

public abstract class GUI_MultiblocksHandler<T> {

    protected static ImmediateWorldSceneRenderer renderer;

    public static final int SLOT_SIZE = 18;

    protected static final int RECIPE_LAYOUT_X = 8;
    protected static final int RECIPE_LAYOUT_Y = 50;
    protected static final int RECIPE_WIDTH = 160;
    protected static final int sceneHeight = RECIPE_WIDTH - 10;
    protected static final int ICON_SIZE_X = 20;
    protected static final int ICON_SIZE_Y = 12;
    protected static final int MOUSE_OFFSET_X = 5;
    protected static final int MOUSE_OFFSET_Y = 43;
    protected static final int LAYER_BUTTON_X = -5;
    protected static final int UNDER_PREVIEW_Y = 135;
    protected static final int TIER_BUTTON_X = LAYER_BUTTON_X;
    protected static final int TIER_BUTTON_Y = UNDER_PREVIEW_Y - ICON_SIZE_Y;
    protected static final int PROJECT_BUTTON_X = 145;
    protected static final int OVERLAY_BUTTON_X = 145 - ICON_SIZE_X - 5;
    protected static final float DEFAULT_RANGE_MULTIPLIER = 3.5f;
    public static final int MAX_PLACE_ROUNDS = 2000;
    public static final BlockPosition MB_PLACE_POS = new BlockPosition(0, 64, 0);

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
    protected static int guiColorBg;
    protected static int guiColorFont;
    protected static int buttonColorEnabled;
    protected static int buttonColorDisabled;
    protected static int buttonColorHovered;

    protected static String guiTextLayer;
    protected static String guiLayerButtonTitle;
    protected static String guiTextTier;
    protected static String guiTierButtonTitle;
    protected static int initialTierButtonTitleWidth;
    protected static int initialLayerButtonTitleWidth;
    protected ClearGuiButton previousLayerButton, nextLayerButton;

    protected List<ItemStack> ingredients = new ArrayList<>();
    protected Consumer<List<ItemStack>> onIngredientChanged;
    protected final Map<GuiButton, Runnable> buttons = new HashMap<>();

    protected T renderingController;
    protected ItemStack stackForm;
    protected T lastRenderingController;

    protected final List<List<ItemStack>> candidates = new ArrayList<>();
    public static List<PositionedIStructureElement> structureElements = new ArrayList<>();
    protected Consumer<List<List<ItemStack>>> onCandidateChanged;
    protected static int tierIndex = 1;
    protected static EntityPlayer fakeMultiblockBuilder;

    protected int scrolled = 0;

    protected ClearGuiButton previousTierButton, nextTierButton;

    public GUI_MultiblocksHandler() {
        previousLayerButton = new ClearGuiButton(0, LAYER_BUTTON_X, UNDER_PREVIEW_Y, ICON_SIZE_X, ICON_SIZE_Y, "<");
        nextLayerButton = new ClearGuiButton(
                0,
                LAYER_BUTTON_X + ICON_SIZE_X,
                UNDER_PREVIEW_Y,
                ICON_SIZE_X,
                ICON_SIZE_Y,
                ">");

        buttons.put(previousLayerButton, this::togglePreviousLayer);
        buttons.put(nextLayerButton, this::toggleNextLayer);

        previousTierButton = new ClearGuiButton(0, TIER_BUTTON_X, TIER_BUTTON_Y, ICON_SIZE_X, ICON_SIZE_Y, "<");
        nextTierButton = new ClearGuiButton(
                0,
                TIER_BUTTON_X + ICON_SIZE_X,
                TIER_BUTTON_Y,
                ICON_SIZE_X,
                ICON_SIZE_Y,
                ">");
        TooltipButton projectMultiblocksButton = new TooltipButton(
                0,
                PROJECT_BUTTON_X,
                UNDER_PREVIEW_Y,
                ICON_SIZE_X,
                ICON_SIZE_Y,
                "P",
                StatCollector.translateToLocal("blockrenderer6343.multiblock.project"));
        TooltipButton overlayMultiblocksButton = new TooltipButton(
                0,
                OVERLAY_BUTTON_X,
                UNDER_PREVIEW_Y,
                ICON_SIZE_X,
                ICON_SIZE_Y,
                "?",
                StatCollector.translateToLocal("blockrenderer6343.multiblock.overlay"));

        buttons.put(previousTierButton, this::togglePreviousTier);
        buttons.put(nextTierButton, this::toggleNextTier);
        buttons.put(projectMultiblocksButton, this::projectMultiblocks);
        buttons.put(overlayMultiblocksButton, this::neiOverlay);
    }

    protected abstract String getMultiblockName();

    protected abstract void placeMultiblock();

    protected void setLocalizationAndColor() {
        guiTextLayer = GuiText.Layer.getLocal();
        guiColorBg = GuiText.BgColor.getColor();
        guiColorFont = GuiText.FontColor.getColor();
        buttonColorEnabled = GuiText.ButtonEnabledColor.getColor();
        buttonColorDisabled = GuiText.ButtonDisabledColor.getColor();
        buttonColorHovered = GuiText.ButtonHoveredColor.getColor();

        previousLayerButton.setColors(buttonColorEnabled, buttonColorDisabled, buttonColorHovered);
        nextLayerButton.setColors(buttonColorEnabled, buttonColorDisabled, buttonColorHovered);

        guiLayerButtonTitle = getLayerButtonTitle();

        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        initialLayerButtonTitleWidth = fontRenderer.getStringWidth(guiLayerButtonTitle);
        nextLayerButton.xPosition = LAYER_BUTTON_X + ICON_SIZE_X
                + initialLayerButtonTitleWidth
                - fontRenderer.getStringWidth("<") / 2;

        guiTextTier = GuiText.Tier.getLocal();
        previousTierButton.setColors(buttonColorEnabled, buttonColorDisabled, buttonColorHovered);
        nextTierButton.setColors(buttonColorEnabled, buttonColorDisabled, buttonColorHovered);

        guiTierButtonTitle = getTierButtonTitle();

        initialTierButtonTitleWidth = fontRenderer.getStringWidth(guiTierButtonTitle);
        nextTierButton.xPosition = TIER_BUTTON_X + ICON_SIZE_X
                + initialTierButtonTitleWidth
                - fontRenderer.getStringWidth("<") / 2;
    }

    public void loadMultiblock(T multiblock, ItemStack stackForm) {
        setLocalizationAndColor();
        renderingController = multiblock;
        this.stackForm = stackForm;
        if (stackForm.stackSize == 0) stackForm.stackSize = 1;
        if (lastRenderingController != renderingController) {
            loadNewMultiblock();
        } else {
            loadPreviousMultiblockAgain();
        }
        guiTierButtonTitle = getTierButtonTitle();
    }

    protected void loadNewMultiblock() {
        layerIndex = -1;
        tierIndex = 1;
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
            Set<BlockPosition> renderBlocks;
            if (newLayer == -1) {
                renderBlocks = world.placedBlocks;
                renderer.setRenderAllFaces(false);
            } else {
                renderBlocks = world.placedBlocks.stream().filter(pos -> pos.y - minY == newLayer)
                        .collect(Collectors.toSet());
                renderer.setRenderAllFaces(true);
            }
            renderer.addRenderedBlocks(renderBlocks);
            scanIngredients();
            guiLayerButtonTitle = getLayerButtonTitle();
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
        // NEI guiLeft
        int k = (NEIClientUtils.getGuiContainer().width - 176) / 2;
        // NEI guiTop
        int l = (NEIClientUtils.getGuiContainer().height
                - Math.min(Math.max(NEIClientUtils.getGuiContainer().height - 68, 166), 370)) / 2;
        renderer.render(
                RECIPE_LAYOUT_X + k,
                RECIPE_LAYOUT_Y + l,
                RECIPE_WIDTH,
                sceneHeight,
                lastGuiMouseX,
                lastGuiMouseY);
        drawMultiblockName();

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        tooltipBlockStack = null;

        MovingObjectPosition rayTraceResult = renderer.getLastTraceResult();
        boolean insideView = guiMouseX >= k + RECIPE_LAYOUT_X && guiMouseY >= l + RECIPE_LAYOUT_Y
                && guiMouseX < k + RECIPE_LAYOUT_X + RECIPE_WIDTH
                && guiMouseY < l + RECIPE_LAYOUT_Y + sceneHeight;
        boolean leftClickHeld = Mouse.isButtonDown(0);
        boolean rightClickHeld = Mouse.isButtonDown(1);
        boolean middleClickHeld = Mouse.isButtonDown(2);

        if (insideView) {
            if (leftClickHeld) {
                rotationPitch += guiMouseX - lastGuiMouseX + 360;
                rotationPitch = rotationPitch % 360;
                rotationYaw = (float) MathHelper.clip(rotationYaw + (guiMouseY - lastGuiMouseY), -89.9, 89.9);
            } else if (rightClickHeld) {
                int mouseDeltaY = guiMouseY - lastGuiMouseY;
                if (Math.abs(mouseDeltaY) > 1) {
                    zoom = (float) MathHelper.clip(zoom + (mouseDeltaY > 0 ? 0.15 : -0.15), 3, 999);
                }
            }
            if (middleClickHeld) {
                int mouseDeltaX = guiMouseX - lastGuiMouseX;
                int mouseDeltaY = guiMouseY - lastGuiMouseY;
                // generated by copilot
                Vector3f lookAt = renderer.getLookAt();
                Vector3f eyePos = renderer.getEyePos();
                Vector3f worldUp = renderer.getWorldUp();
                Vector3f lookDir = Vector3f.sub(lookAt, eyePos, null);
                Vector3f rightDir = Vector3f.cross(lookDir, worldUp, null);
                rightDir.normalise();
                Vector3f upDir = Vector3f.cross(rightDir, lookDir, null);
                upDir.normalise();
                Vector3f offset = new Vector3f(
                        -mouseDeltaX * rightDir.x + mouseDeltaY * upDir.x,
                        -mouseDeltaX * rightDir.y + mouseDeltaY * upDir.y,
                        -mouseDeltaX * rightDir.z + mouseDeltaY * upDir.z);
                offset.scale(0.15f);
                Vector3f.add(center, offset, center);
            }
            if (scrolled != 0) {
                zoom = (float) MathHelper.clip(zoom - scrolled * 5, 3, 999);
                scrolled = 0;
            }

            renderer.setCameraLookAt(center, zoom, Math.toRadians(rotationPitch), Math.toRadians(rotationYaw));
        }

        // draw buttons
        for (GuiButton button : buttons.keySet()) {
            button.drawButton(Minecraft.getMinecraft(), guiMouseX - k - MOUSE_OFFSET_X, guiMouseY - l - MOUSE_OFFSET_Y);
        }
        drawButtonsTitle();

        if (!(leftClickHeld || rightClickHeld) && rayTraceResult != null
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

        // don't activate these
        // GlStateManager.disableRescaleNormal();
        // GlStateManager.disableLighting();
        // RenderHelper.disableStandardItemLighting();
    }

    private void drawMultiblockName() {
        String localizedName = getMultiblockName();
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        List<String> lines = fontRenderer.listFormattedStringToWidth(localizedName, RECIPE_WIDTH - 10);
        for (int i = 0; i < lines.size(); i++) {
            fontRenderer.drawString(
                    lines.get(i),
                    (RECIPE_WIDTH - fontRenderer.getStringWidth(lines.get(i))) / 2,
                    fontRenderer.FONT_HEIGHT * i,
                    guiColorFont);
        }
    }

    protected String getLayerButtonTitle() {
        return guiTextLayer + ": " + (layerIndex == -1 ? "A" : Integer.toString(layerIndex + 1));
    }

    protected void drawButtonsTitle() {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        fontRenderer.drawString(
                guiLayerButtonTitle,
                LAYER_BUTTON_X + ICON_SIZE_X
                        + (initialLayerButtonTitleWidth - fontRenderer.getStringWidth(guiLayerButtonTitle)) / 2,
                UNDER_PREVIEW_Y + 2,
                guiColorFont);
        fontRenderer.drawString(
                guiTierButtonTitle,
                TIER_BUTTON_X + ICON_SIZE_X
                        + (initialTierButtonTitleWidth - fontRenderer.getStringWidth(guiTierButtonTitle)) / 2,
                TIER_BUTTON_Y + 2,
                guiColorFont);
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
        ((DummyWorld) renderer.world).updateEntitiesForNEI();
        renderer.setClearColor(guiColorBg);

        placeMultiblock();

        Vector3f size = ((TrackedDummyWorld) renderer.world).getSize();
        Vector3f minPos = ((TrackedDummyWorld) renderer.world).getMinPos();
        center = new Vector3f(minPos.x + size.x / 2, minPos.y + size.y / 2, minPos.z + size.z / 2);

        renderer.renderedBlocks.clear();
        renderer.addRenderedBlocks(((TrackedDummyWorld) renderer.world).placedBlocks);
        renderer.setOnLookingAt(ray -> {});

        renderer.setOnWorldRender(this::onRendererRender);

        selectedBlock = null;
        onBlockSelected();
        setNextLayer(layerIndex);

        if (resetCamera) {
            float max = Math.max(Math.max(Math.max(size.x, size.y), size.z), 1);
            // Compact Series multiblocks compat
            if (size.x >= 30 || size.y >= 30 || size.z >= 30) {
                zoom = (float) (DEFAULT_RANGE_MULTIPLIER * 4 * Math.sqrt(max));
            }
            // Mega Series multiblocks compat
            if (size.x >= 15 && size.y >= 15 && size.z >= 11) {
                zoom = (float) (DEFAULT_RANGE_MULTIPLIER * 2 * Math.sqrt(max));
            } else {
                zoom = (float) (DEFAULT_RANGE_MULTIPLIER * Math.sqrt(max));
            }
            rotationYaw = 20.0f;
            rotationPitch = 50f;
            if (renderer != null) {
                resetCenter();
            }
        } else {
            renderer.setCameraLookAt(eyePos, lookAt, worldUp);
        }
    }

    public void handleMouseScrollUp(int scrolled) {
        this.scrolled = scrolled;
    }

    protected void toggleNextTier() {
        tierIndex++;
        guiTierButtonTitle = getTierButtonTitle();
        initializeSceneRenderer(false);
    }

    protected void togglePreviousTier() {
        if (tierIndex > 1) {
            tierIndex--;
            guiTierButtonTitle = getTierButtonTitle();
            initializeSceneRenderer(false);
        }
    }

    protected String getTierButtonTitle() {
        return guiTextTier + ": " + tierIndex;
    }

    public void onRendererRender(WorldSceneRenderer renderer) {
        BlockPosition look = renderer.getLastTraceResult() == null ? null
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

    protected void projectMultiblocks() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        World baseWorld = Minecraft.getMinecraft().theWorld;
        MovingObjectPosition lookingPos = player.rayTrace(10, 1);
        if (lookingPos.typeOfHit == MovingObjectPosition.MovingObjectType.MISS) return;
        int playerDir = MathHelper.floor_double((player.rotationYaw * 4F) / 360F + 0.5D) & 3;
        ItemStack itemStack = stackForm.copy();

        if (!baseWorld.isAirBlock(lookingPos.blockX, lookingPos.blockY + 1, lookingPos.blockZ)) {
            player.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("blockrenderer6343.no_space")));
            return;
        }

        itemStack.getItem().onItemUse(
                itemStack,
                player,
                baseWorld,
                lookingPos.blockX,
                lookingPos.blockY + 1,
                lookingPos.blockZ,
                0,
                lookingPos.blockX,
                lookingPos.blockY,
                lookingPos.blockZ);
        ConstructableUtility.handle(
                getTriggerStack(),
                player,
                baseWorld,
                lookingPos.blockX,
                lookingPos.blockY + 1,
                lookingPos.blockZ,
                playerDir);
        baseWorld.setBlockToAir(lookingPos.blockX, lookingPos.blockY + 1, lookingPos.blockZ);
        baseWorld.removeTileEntity(lookingPos.blockX, lookingPos.blockY + 1, lookingPos.blockZ);
    }

    protected void neiOverlay() {
        NBTTagCompound recipeInputs = new NBTTagCompound();
        GuiRecipe<?> currentScreen = (GuiRecipe<?>) Minecraft.getMinecraft().currentScreen;
        Minecraft.getMinecraft().displayGuiScreen(currentScreen.firstGui);
        for (int i = 0; i < ingredients.size(); i++) {
            ItemStack itemStack = ingredients.get(i);
            if (itemStack != null) {
                NBTTagCompound itemStackNBT = new NBTTagCompound();
                itemStack.writeToNBT(itemStackNBT);
                itemStackNBT.setInteger("Count", itemStack.stackSize);
                recipeInputs.setTag("#" + i, itemStackNBT);
            }
        }
        NEENetworkHandler.getInstance().sendToServer(new PacketNEIPatternRecipe(recipeInputs, new NBTTagCompound()));
    }

    private void scanIngredients() {
        List<ItemStack> ingredients = new ArrayList<>();
        for (BlockPosition renderedBlock : renderer.renderedBlocks) {
            Block block = renderer.world.getBlock(renderedBlock.x, renderedBlock.y, renderedBlock.z);
            if (block.equals(Blocks.air)) continue;
            int meta = renderer.world.getBlockMetadata(renderedBlock.x, renderedBlock.y, renderedBlock.z);
            int qty = block.quantityDropped(renderer.world.rand);
            ArrayList<ItemStack> itemstacks = new ArrayList<>();
            if (qty != 1) {
                itemstacks.add(new ItemStack(block));
            } else {
                itemstacks = block.getDrops(renderer.world, renderedBlock.x, renderedBlock.y, renderedBlock.z, meta, 0);
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
            // NEI guiLeft
            int k = (NEIClientUtils.getGuiContainer().width - 176) / 2;
            // NEI guiTop
            int l = (NEIClientUtils.getGuiContainer().height
                    - Math.min(Math.max(NEIClientUtils.getGuiContainer().height - 68, 166), 370)) / 2;
            if (buttons.getKey().mousePressed(
                    Minecraft.getMinecraft(),
                    guiMouseX - k - MOUSE_OFFSET_X,
                    guiMouseY - l - MOUSE_OFFSET_Y)) {
                buttons.getValue().run();
                selectedBlock = null;
                onBlockSelected();
                return true;
            }
        }
        if (button == 1 && renderer != null) {
            if (renderer.getLastTraceResult() == null) {
                if (selectedBlock != null) {
                    selectedBlock = null;
                    onBlockSelected();
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

    @NotNull
    protected static ItemStack getTriggerStack() {
        return new ItemStack(StructureLibAPI.getDefaultHologramItem(), tierIndex);
    }

    protected void scanCandidates() {
        candidates.clear();
        if (selectedBlock != null) {
            for (PositionedIStructureElement structureElement : structureElements) {
                if (structureElement.x == selectedBlock.x && structureElement.y == selectedBlock.y
                        && structureElement.z == selectedBlock.z) {

                    IStructureElement.BlocksToPlace blocksToPlace = structureElement.element.getBlocksToPlace(
                            (IConstructable) renderingController,
                            renderer.world,
                            selectedBlock.x,
                            selectedBlock.y,
                            selectedBlock.z,
                            getTriggerStack(),
                            AutoPlaceEnvironment.fromLegacy(
                                    CreativeItemSource.instance,
                                    fakeMultiblockBuilder,
                                    iChatComponent -> {}));
                    if (blocksToPlace != null) {
                        Predicate<ItemStack> predicate = blocksToPlace.getPredicate();
                        Set<ItemStack> rawCandidates = CreativeItemSource.instance
                                .takeEverythingMatches(predicate, false, 0).keySet();

                        List<List<ItemStack>> stackedCandidates = new ArrayList<>();
                        for (ItemStack rawCandidate : rawCandidates) {
                            boolean added = false;
                            for (List<ItemStack> stackedCandidate : stackedCandidates) {
                                List<String> firstCandidateTooltip = stackedCandidate.get(0)
                                        .getTooltip(fakeMultiblockBuilder, false);
                                List<String> rawCandidateTooltip = rawCandidate
                                        .getTooltip(fakeMultiblockBuilder, false);
                                if (firstCandidateTooltip.size() > 1 && rawCandidateTooltip.size() > 1
                                        && firstCandidateTooltip.get(1).equals(rawCandidateTooltip.get(1))) {
                                    stackedCandidate.add(rawCandidate);
                                    added = true;
                                    break;
                                }
                            }
                            if (!added) {
                                List<ItemStack> newStackedCandidate = new ArrayList<>();
                                newStackedCandidate.add(rawCandidate);
                                stackedCandidates.add(newStackedCandidate);
                            }
                        }

                        candidates.addAll(stackedCandidates);

                        if (onCandidateChanged != null) {
                            onCandidateChanged.accept(candidates);
                        }
                    }
                    return;
                }
            }
        }
        if (onCandidateChanged != null) {
            onCandidateChanged.accept(candidates);
        }
    }

    protected void onBlockSelected() {
        scanCandidates();
    };

    public void setOnCandidateChanged(Consumer<List<List<ItemStack>>> callback) {
        onCandidateChanged = callback;
    }

    public List<String> handleTooltip() {
        if (tooltipBlockStack != null) {
            return tooltipBlockStack.getTooltip(
                    Minecraft.getMinecraft().thePlayer,
                    Minecraft.getMinecraft().gameSettings.advancedItemTooltips);
        } else {
            return null;
        }
    }

    protected static class ClearGuiButton extends GuiButton {

        private int colorEnabled;
        private int colorDisabled;
        private int colorHovered;

        public ClearGuiButton(int p_i1021_1_, int p_i1021_2_, int p_i1021_3_, int p_i1021_4_, int p_i1021_5_,
                String p_i1021_6_) {
            super(p_i1021_1_, p_i1021_2_, p_i1021_3_, p_i1021_4_, p_i1021_5_, p_i1021_6_);
        }

        public void setColors(int clrEnabled, int clrDisabled, int clrHovered) {
            colorEnabled = clrEnabled;
            colorDisabled = clrDisabled;
            colorHovered = clrHovered;
        }

        @Override
        public void drawButton(Minecraft p_146112_1_, int p_146112_2_, int p_146112_3_) {
            if (this.visible) {
                FontRenderer fontrenderer = p_146112_1_.fontRenderer;
                this.field_146123_n = p_146112_2_ >= this.xPosition && p_146112_3_ >= this.yPosition
                        && p_146112_2_ < this.xPosition + this.width
                        && p_146112_3_ < this.yPosition + this.height;
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
}
