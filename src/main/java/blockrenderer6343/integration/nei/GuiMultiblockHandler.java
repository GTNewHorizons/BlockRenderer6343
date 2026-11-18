package blockrenderer6343.integration.nei;

import static blockrenderer6343.client.utils.BRUtil.FAKE_PLAYER;

import java.awt.Point;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;
import com.gtnewhorizon.structurelib.StructureEvent;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IChannelDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;

import blockrenderer6343.BlockRenderer6343;
import blockrenderer6343.api.utils.CreativeItemSource;
import blockrenderer6343.client.renderer.ImmediateWorldSceneRenderer;
import blockrenderer6343.client.renderer.PreviewCamera;
import blockrenderer6343.client.renderer.PreviewInteractionConfig;
import blockrenderer6343.client.renderer.WorldSceneRenderer;
import blockrenderer6343.client.utils.BRButton;
import blockrenderer6343.client.utils.BRUtil;
import blockrenderer6343.client.utils.ConstructableData;
import blockrenderer6343.client.utils.GuiSlider;
import blockrenderer6343.client.world.TrackedDummyWorld;
import codechicken.lib.gui.GuiDraw;
import codechicken.lib.math.MathHelper;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.recipe.GuiRecipe;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

@EventBusSubscriber(side = Side.CLIENT)
public abstract class GuiMultiblockHandler {

    protected ImmediateWorldSceneRenderer renderer;
    private final RenderBlocks renderBlocks = new RenderBlocks();

    public static final int SLOT_SIZE = 18;
    public static final int ICON_SIZE_X = 20;
    public static final int ICON_SIZE_Y = 12;
    public static final long NO_SELECTED_BLOCK = CoordinatePacker.pack(-100000, 0, -100000);
    public static final int UNDER_PREVIEW_Y = 136;
    public static final int BUTTON_RIGHT = 145;
    public static final int RECIPE_LAYOUT_X = 6;
    public static final int RECIPE_LAYOUT_Y = 33;
    public static final int RECIPE_WIDTH = 163;
    public static final int SCENE_HEIGHT = RECIPE_WIDTH - 10;
    protected final Long2ObjectMap<IStructureElement<Object>> structureElementMap = new Long2ObjectOpenHashMap<>();

    protected static final int BETWEEN_BUTTON_X = ICON_SIZE_X + 2;
    protected static final int SLIDER_WIDTH = BUTTON_RIGHT - BETWEEN_BUTTON_X - 2;
    protected static final float DEFAULT_RANGE_MULTIPLIER = 3.5f;
    protected static final int MAX_PLACE_ROUNDS = 2000;
    public static final BlockPos MB_PLACE_POS = new BlockPos(0, 64, 0);
    protected final BlockPos selectedBlock = new BlockPos().set(NO_SELECTED_BLOCK);
    protected static final ItemStack DEFAULT_TRIGGER = new ItemStack(StructureLibAPI.getDefaultHologramItem());

    protected int guiMouseX, guiMouseY, guiLeft, guiTop;
    protected int lastGuiMouseX, lastGuiMouseY;
    protected Vector3f center = new Vector3f();
    protected float rotationYaw, rotationPitch;
    protected float zoom;
    protected IConstructable renderingController, lastRenderingController;
    protected int layerIndex = -1;

    protected ItemStack tooltipBlockStack;

    protected ConstructableData constructableData;
    protected ItemStack stackForm;

    protected Consumer<List<List<ItemStack>>> onCandidateChanged;
    protected Consumer<List<ItemStack>> onIngredientChanged;

    protected int scrolled = 0;
    protected float scaleFactor;
    private boolean sceneRendererUpdatePending;
    private boolean deferIngredientRefresh;

    protected ItemStack trigger;
    protected int lastHeight;
    protected final ObjectList<BRButton> allButtons = new ObjectArrayList<>();
    protected GuiSlider tierSlider;
    protected Vector2i relativeMousePos = new Vector2i();
    private int buttonsInRow;
    protected String lastSearch = "";
    private final Long2BooleanMap checkedBlocks = new Long2BooleanOpenHashMap();
    private int clearedStructureElementCount;
    private final PreviewCamera camera = new PreviewCamera();
    private int cameraDragButton = -1;
    private boolean candidateClickPending;
    private int candidateClickX;
    private int candidateClickY;
    protected static Minecraft mc = Minecraft.getMinecraft();

    protected abstract void placeMultiblock();

    public void loadMultiblock(IConstructable multiblock, ItemStack stackForm, @NotNull ConstructableData data) {
        renderingController = multiblock;
        constructableData = data;

        this.stackForm = stackForm;
        if (stackForm.stackSize == 0) stackForm.stackSize = 1;
        if (lastRenderingController != renderingController) {
            initGui();
            if (buttonsInRow % 2 == 0) buttonsInRow++;
            addButtonInRow("R").setTooltip(I18n.format("blockrenderer6343.nei.reset_camera"))
                    .setClickAction(this::resetCameraPosition);
            loadNewMultiblock();
        } else {
            initializeSceneRenderer(false);
        }
    }

    protected void initGui() {
        allButtons.clear();
        buttonsInRow = 0;
        allButtons.add(
                tierSlider = (GuiSlider) new GuiSlider(
                        I18n.format("blockrenderer6343.nei.tier"),
                        0,
                        UNDER_PREVIEW_Y,
                        SLIDER_WIDTH,
                        16,
                        1,
                        1,
                        constructableData.getMaxTotalTier()).setValueListener(this::setTier).setIndex(0));
        allButtons.add(
                new GuiSlider(
                        I18n.format("blockrenderer6343.nei.layer"),
                        0,
                        UNDER_PREVIEW_Y + 12,
                        SLIDER_WIDTH,
                        16,
                        0,
                        0).setTextSupplier(
                                value -> value == 0 ? I18n.format("blockrenderer6343.nei.all") : String.valueOf(value))
                                .setMaxValueSupplier(
                                        () -> (int) (renderer.world.getMaxPos().y - renderer.world.getMinPos().y + 1))
                                .setValueListener(this::setActiveLayer).setIndex(1));
        loadChannels();
        addButtonInRow("P").setTooltip(I18n.format("blockrenderer6343.multiblock.project")).setClickAction(
                () -> BRUtil.projectMultiblock(
                        getBuildTriggerStack(),
                        stackForm,
                        MathHelper.floor_double(MB_PLACE_POS.y - renderer.world.getMinPos().y)));
        addButtonInRow("?").setTooltip(I18n.format("blockrenderer6343.multiblock.overlay"))
                .setClickAction(() -> BRUtil.neiOverlay(renderer));
        if (!constructableData.getChannelMaxTierMap().isEmpty()) {
            addButtonInRow("C").setTooltip(I18n.format("blockrenderer6343.multiblock.copy_channels"))
                    .setClickAction(() -> BRUtil.copyToHologram(trigger));
        }
    }

    protected BRButton addButtonInRow(@NotNull String displayString) {
        int totalButtons = buttonsInRow++;
        int yOffset = UNDER_PREVIEW_Y + (totalButtons / 2) * SLOT_SIZE;
        BRButton button = new BRButton(BUTTON_RIGHT - (totalButtons % 2) * BETWEEN_BUTTON_X, yOffset, displayString)
                .setIndex(totalButtons);
        allButtons.add(button);
        return button;
    }

    protected void loadChannels() {
        Object2IntMap<String> channels = constructableData.getChannelMaxTierMap();
        int curSliders = allButtons.size();
        if (channels.isEmpty()) return;
        int i = 0;
        for (Object2IntMap.Entry<String> entry : channels.object2IntEntrySet()) {
            String channel = entry.getKey();
            IChannelDefinition definition = constructableData.getChannelDefinitions().get(channel);
            int minimum = definition == null ? 0 : Math.max(1, definition.getMinimumValue());
            int startVal = minimum;
            boolean initiallyUnset = definition != null;
            if (constructableData.getCurrentChannels().contains(channel)) {
                startVal = Math.max(minimum, Math.min(entry.getIntValue(), constructableData.getCurrentTier()));
                initiallyUnset = false;
            }
            GuiSlider slider = new GuiSlider(
                    StringUtils.capitalize(channel),
                    0,
                    UNDER_PREVIEW_Y + (12 * (i + curSliders)),
                    SLIDER_WIDTH,
                    16,
                    startVal,
                    minimum,
                    entry.getIntValue());
            slider.setAllowUnset(definition != null).setUnset(initiallyUnset).setTextSupplier(
                    value -> definition == null && value == 0 ? I18n.format("blockrenderer6343.nei.not_set")
                            : String.valueOf(value));
            slider.setIndex(i + curSliders);
            slider.setDeferredValueListener(val -> {
                if (setChannelTier(channel, val, slider.isUnset(), false)) {
                    sceneRendererUpdatePending = true;
                    deferIngredientRefresh = true;
                }
            }, val -> {
                updatePendingSceneRenderer();
                refreshDeferredIngredients();
            });
            allButtons.add(slider);
            i++;
        }
    }

    protected void loadNewMultiblock() {
        trigger = DEFAULT_TRIGGER.copy();
        int tier = constructableData.getCurrentTier();
        Collection<String> channels = constructableData.getCurrentChannels();

        if (channels.isEmpty()) {
            tierSlider.setValue(trigger.stackSize = tier, false);
        } else {
            for (String channel : channels) {
                setChannelTier(channel, tier, false);
            }
        }

        layerIndex = -1;
        lastSearch = "";
        initializeSceneRenderer(true);
        lastRenderingController = renderingController;
    }

    private void setTier(int tier) {
        if (tier <= 0) {
            tier = 1;
        }

        if (renderer != null && tier == trigger.stackSize) return;

        trigger.stackSize = tier;
        initializeSceneRenderer(false);
    }

    protected void setChannelTier(String channel, int tier) {
        setChannelTier(channel, tier, true);
    }

    protected void setChannelTier(String channel, int tier, boolean rebuild) {
        setChannelTier(channel, tier, false, rebuild);
    }

    private boolean setChannelTier(String channel, int tier, boolean unset, boolean rebuild) {
        IChannelDefinition definition = constructableData.getChannelDefinitions().get(channel);
        if (definition == null && tier < 0) return false;

        boolean hasCurrent = ChannelDataAccessor.hasSubChannel(trigger, channel);
        int current = ChannelDataAccessor.hasSubChannel(trigger, channel)
                ? ChannelDataAccessor.getChannelData(trigger, channel)
                : 0;

        if (unset) {
            if (!hasCurrent) return false;
            ChannelDataAccessor.unsetChannelData(trigger, channel);
        } else if (definition != null) {
            tier = definition.normalizeValue(tier);
            if (hasCurrent && current == tier) return false;
            ChannelDataAccessor.setChannelData(trigger, channel, tier);
        } else if (tier > 0) {
            if (hasCurrent && current == tier) return false;
            ChannelDataAccessor.setChannelData(trigger, channel, tier);
        } else {
            if (!hasCurrent) return false;
            ChannelDataAccessor.unsetChannelData(trigger, channel);
        }

        if (rebuild) {
            initializeSceneRenderer(false);
        }
        return true;
    }

    private void setActiveLayer(int newLayer) {
        setActiveLayerIndex(newLayer - 1);
    }

    private void setActiveLayerIndex(int newLayer) {
        int height = (int) renderer.world.getSize().y() - 1;
        if (newLayer < 0 || newLayer > height) {
            // if current layer index is more than max height, reset it
            // to display all layers
            newLayer = -1;
        }

        if (newLayer == layerIndex) return;

        layerIndex = newLayer;

        updateRenderer();
    }

    private void updateRenderer() {
        if (renderer == null) return;

        if (layerIndex > -1) {
            renderer.setRenderYLayer(layerIndex);
        } else {
            renderer.setRenderAllBlocks();
        }

        if (!deferIngredientRefresh || hasActiveSearch()) {
            onIngredientChanged.accept(BRUtil.getIngredients(renderer));
        }

        if (hasActiveSearch()) {
            checkedBlocks.clear();

            boolean foundMatch = false;

            LongIterator iter = renderer.renderedBlocks.longIterator();

            while (iter.hasNext()) {
                long coord = iter.nextLong();

                boolean matches = checkedBlocks.computeIfAbsent(BRUtil.hashBlock(renderer.world, coord), b -> {
                    int x = CoordinatePacker.unpackX(coord);
                    int y = CoordinatePacker.unpackY(coord);
                    int z = CoordinatePacker.unpackZ(coord);

                    Block block = renderer.world.getBlock(x, y, z);

                    ItemStack stack = new ItemStack(block, 1, BRUtil.getDamageValue(renderer.world, block, coord));

                    return matchesSearch(stack, lastSearch);
                });

                foundMatch |= matches;

                if (!matches) {
                    renderer.renderTranslucentBlocks.add(coord);
                    iter.remove();
                }
            }

            if (foundMatch) {
                renderer.setRenderAllFaces(true);
            }
        }
    }

    private void updateCamera() {
        if (PreviewInteractionConfig.isLegacyControls()) {
            camera.reset(center, zoom, rotationPitch, rotationYaw);
        } else {
            camera.update(center, zoom, rotationPitch, rotationYaw, cameraDragButton >= 0, System.nanoTime());
        }
        renderer.getEyePos().set(camera.getEyePosition());
        renderer.getLookAt().set(camera.getLookAt());
    }

    private void resetCameraPosition() {
        if (renderer == null) return;
        Vector3f minPos = renderer.world.getMinPos();
        Vector3f size = renderer.world.getSize();
        center.set(minPos.x + size.x / 2, minPos.y + size.y / 2, minPos.z + size.z / 2);
        float max = Math.max(1, Math.max(Math.max(size.x, size.y), size.z));
        float baseZoom = (float) (DEFAULT_RANGE_MULTIPLIER * Math.sqrt(max));
        float sizeFactor = (float) (1.0f + Math.log(max) / Math.log(10));

        zoom = PreviewCamera.clampDistance(baseZoom * sizeFactor / 1.5f);
        rotationYaw = 20.0f;
        rotationPitch = 50f;

        camera.reset(center, zoom, rotationPitch, rotationYaw);
        cancelCameraDrag();
        scrolled = 0;
        updateCamera();
    }

    private void focusCameraAtMouse() {
        MovingObjectPosition hit = renderer.pick(guiMouseX, guiMouseY);
        if (hit == null || hit.hitVec == null) return;
        float pendingZoom = zoom / camera.getDistance();
        if (camera
                .focus(new Vector3f((float) hit.hitVec.xCoord, (float) hit.hitVec.yCoord, (float) hit.hitVec.zCoord))) {
            center.set(camera.getPivot());
            zoom = PreviewCamera.clampDistance(camera.getDistance() * pendingZoom);
            renderer.getEyePos().set(camera.getEyePosition());
            renderer.getLookAt().set(camera.getLookAt());
        }
    }

    private void updateCameraInput(int viewportHeight) {
        if (candidateClickPending
                && (Math.abs(guiMouseX - candidateClickX) > 3 || Math.abs(guiMouseY - candidateClickY) > 3)) {
            candidateClickPending = false;
        }
        if (cameraDragButton >= 0 && !Mouse.isButtonDown(cameraDragButton)) {
            onMouseReleased(cameraDragButton);
        }
        applyCameraDrag(guiMouseX - lastGuiMouseX, guiMouseY - lastGuiMouseY, viewportHeight);
        if (scrolled != 0) {
            zoom = PreviewInteractionConfig.isLegacyControls() ? (float) MathHelper.clip(zoom - scrolled * 5, 3, 999)
                    : PreviewCamera.clampDistance(zoom * (float) Math.pow(1.12, -scrolled));
            scrolled = 0;
        }
    }

    private void applyCameraDrag(int deltaX, int deltaY, int viewportHeight) {
        boolean legacy = PreviewInteractionConfig.isLegacyControls();
        if (cameraDragButton == PreviewInteractionConfig.getRotateButton()) {
            rotationPitch = (rotationPitch + deltaX) % 360;
            rotationYaw = (float) MathHelper.clip(rotationYaw + deltaY, -89.9, 89.9);
        } else if (cameraDragButton == PreviewInteractionConfig.getZoomButton()) {
            if (legacy) {
                if (Math.abs(deltaY) > 1) zoom = (float) MathHelper.clip(zoom + Math.copySign(0.15f, deltaY), 3, 999);
            } else {
                zoom = PreviewCamera.clampDistance(zoom * (float) Math.exp(deltaY * 0.012));
            }
        } else if (cameraDragButton == PreviewInteractionConfig.getPanButton()) {
            if (legacy) camera.pan(deltaX, deltaY, 0.15f, center);
            else camera.pan(deltaX, deltaY, viewportHeight, center);
        }
    }

    public void drawMultiblock(int recipeIndex) {
        updatePendingSceneRenderer();
        GuiRecipe<?> recipeGui = (GuiRecipe<?>) NEIClientUtils.getGuiContainer();
        guiMouseX = GuiDraw.getMousePosition().x;
        guiMouseY = GuiDraw.getMousePosition().y;
        guiLeft = recipeGui.guiLeft;
        guiTop = recipeGui.guiTop;

        int guiHeight = recipeGui.height;
        scaleFactor = Math.min((float) recipeGui.height / 500, 1f);
        int scaledScene = Math.round(SCENE_HEIGHT * scaleFactor);
        updateCameraInput(scaledScene);
        updateCamera();
        renderer.render(
                RECIPE_LAYOUT_X + guiLeft,
                RECIPE_LAYOUT_Y + guiTop,
                RECIPE_WIDTH,
                scaledScene,
                guiMouseX,
                guiMouseY);

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        tooltipBlockStack = null;

        MovingObjectPosition rayTraceResult = renderer.getLastTraceResult();
        boolean leftClickHeld = Mouse.isButtonDown(0);
        boolean rightClickHeld = Mouse.isButtonDown(1);
        Point recipePos = recipeGui.getRecipePosition(recipeIndex);
        relativeMousePos.set(guiMouseX - guiLeft - recipePos.x, guiMouseY - guiTop - recipePos.y);

        for (BRButton button : allButtons) {
            button.scalePosition(scaledScene, scaleFactor);
            button.drawButton(mc, relativeMousePos.x, relativeMousePos.y);
        }

        if (!(leftClickHeld || rightClickHeld) && rayTraceResult != null
                && !renderer.world.isAirBlock(rayTraceResult.blockX, rayTraceResult.blockY, rayTraceResult.blockZ)) {
            Block block = renderer.world.getBlock(rayTraceResult.blockX, rayTraceResult.blockY, rayTraceResult.blockZ);
            tooltipBlockStack = block.getPickBlock(
                    rayTraceResult,
                    renderer.world,
                    rayTraceResult.blockX,
                    rayTraceResult.blockY,
                    rayTraceResult.blockZ,
                    mc.thePlayer);
        }

        lastHeight = guiHeight;
        lastGuiMouseX = guiMouseX;
        lastGuiMouseY = guiMouseY;

        // don't activate these
        // GlStateManager.disableRescaleNormal();
        // GlStateManager.disableLighting();
        // RenderHelper.disableStandardItemLighting();
    }

    private boolean isInsideView() {
        return guiMouseX >= guiLeft + RECIPE_LAYOUT_X && guiMouseX <= guiLeft + RECIPE_LAYOUT_X + RECIPE_WIDTH
                && guiMouseY >= guiTop + RECIPE_LAYOUT_Y
                && guiMouseY <= guiTop + RECIPE_LAYOUT_Y + (SCENE_HEIGHT * scaleFactor);
    }

    protected @NotNull String getMultiblockName() {
        return stackForm == null ? "" : I18n.format(stackForm.getDisplayName());
    }

    protected void initializeSceneRenderer(boolean resetCamera) {
        sceneRendererUpdatePending = false;
        resetCamera |= renderer == null;

        if (renderer == null) {
            renderer = new ImmediateWorldSceneRenderer(new TrackedDummyWorld());
        } else {
            renderer.world.clear();
            renderer.resetRenderedBlocks();
        }
        renderer.world.updateEntitiesForNEI();

        FAKE_PLAYER.setWorld(renderer.world);
        renderer.world.unloadEntities(Collections.singletonList(FAKE_PLAYER));

        boolean instrumentEnabledByUs = !StructureLibAPI.isInstrumentEnabled();
        if (instrumentEnabledByUs) {
            StructureLibAPI.enableInstrument(BlockRenderer6343.MOD_ID);
        }

        clearedStructureElementCount = structureElementMap.size();
        structureElementMap.clear();
        try {
            placeMultiblock();
        } finally {
            if (instrumentEnabledByUs) StructureLibAPI.disableInstrument();
        }

        if ((long) structureElementMap.size() * 4L + 64L < clearedStructureElementCount) {
            ((Long2ObjectOpenHashMap<?>) structureElementMap).trim();
            clearedStructureElementCount = structureElementMap.size();
        }
        renderer.world.trimIfOversized();

        renderer.setRenderAllBlocks();
        renderer.trimIfOversized();

        renderer.setOnLookingAt(ray -> {});

        renderer.setOnWorldRender(this::onRendererRender);
        renderer.setPostBlockRender(this::onPostBlocksRendered);

        selectedBlock.set(NO_SELECTED_BLOCK);
        onCandidateChanged.accept(Collections.emptyList());

        setActiveLayerIndex(layerIndex);
        updateRenderer();

        if (resetCamera) {
            resetCameraPosition();
        } else {
            updateCamera();
        }
    }

    private void updatePendingSceneRenderer() {
        if (!sceneRendererUpdatePending) return;
        sceneRendererUpdatePending = false;
        initializeSceneRenderer(false);
    }

    private void refreshDeferredIngredients() {
        if (!deferIngredientRefresh || renderer == null) return;
        deferIngredientRefresh = false;
        if (hasActiveSearch()) return;
        onIngredientChanged.accept(BRUtil.getIngredients(renderer));
    }

    private boolean hasActiveSearch() {
        return lastSearch != null && !lastSearch.isEmpty();
    }

    public boolean handleMouseScrollUp(int scrolled) {
        if (isInsideView()) {
            if (!PreviewInteractionConfig.isLegacyControls() && cameraDragButton < 0) focusCameraAtMouse();
            this.scrolled += scrolled;
            return true;
        }

        for (BRButton button : allButtons) {
            if (button.mouseScrolled(relativeMousePos.x, relativeMousePos.y, scrolled)) {
                return true;
            }
        }

        return false;
    }

    public void onRendererRender(WorldSceneRenderer renderer) {
        MovingObjectPosition lookingPos = renderer.getLastTraceResult();
        long lookingBlock = lookingPos == null ? NO_SELECTED_BLOCK
                : CoordinatePacker.pack(lookingPos.blockX, lookingPos.blockY, lookingPos.blockZ);
        long selectedPosition = selectedBlock.asLong();
        if (selectedPosition == lookingBlock) {
            renderBlockOverLay(selectedPosition, Blocks.glass.getIcon(0, 6));
            return;
        }
        renderBlockOverLay(lookingBlock, Blocks.stained_glass.getIcon(0, 7));
        renderBlockOverLay(selectedPosition, Blocks.stained_glass.getIcon(0, 14));
    }

    private void renderBlockOverLay(long pos, IIcon icon) {
        if (pos == NO_SELECTED_BLOCK) return;

        renderBlocks.blockAccess = renderer.world;
        renderBlocks.setRenderBounds(0, 0, 0, 1, 1, 1);
        renderBlocks.renderAllFaces = true;
        int x = CoordinatePacker.unpackX(pos);
        int y = CoordinatePacker.unpackY(pos);
        int z = CoordinatePacker.unpackZ(pos);
        // Selection is a cube overlay and must not invoke the selected mod's renderer again.
        renderBlocks.setOverrideBlockTexture(icon);
        try {
            renderBlocks.renderStandardBlock(Blocks.glass, x, y, z);
        } finally {
            renderBlocks.clearOverrideBlockTexture();
        }
    }

    public boolean mouseClicked(int button) {
        if (button == 2) {
            for (BRButton candidate : allButtons) {
                if (candidate instanceof GuiSlider slider && slider.isMouseOver(relativeMousePos)) {
                    slider.toggleInputMode();
                    return true;
                }
            }
        }
        if (button == 0) {
            for (BRButton candidate : allButtons) {
                if (candidate instanceof GuiSlider slider && slider.isInputMode()
                        && !slider.isMouseOver(relativeMousePos)) {
                    slider.toggleInputMode();
                }
            }
            for (BRButton buttons : allButtons) {
                if (buttons.mousePressed(mc, relativeMousePos.x, relativeMousePos.y)) {
                    selectedBlock.set(NO_SELECTED_BLOCK);
                    onCandidateChanged.accept(Collections.emptyList());
                    return true;
                }
            }
        }
        boolean cameraDrag = beginCameraDrag(button);
        if (button == 1 && cameraDrag && PreviewInteractionConfig.isLegacyControls()) {
            showCandidatesAtMouse();
        }
        return cameraDrag;
    }

    private void showCandidatesAtMouse() {
        MovingObjectPosition rayTrace = renderer.pick(guiMouseX, guiMouseY);
        if (rayTrace == null) {
            if (selectedBlock.asLong() != NO_SELECTED_BLOCK) {
                selectedBlock.set(NO_SELECTED_BLOCK);
                onCandidateChanged.accept(Collections.emptyList());
                return;
            }
            return;
        }
        long pos = CoordinatePacker.pack(rayTrace.blockX, rayTrace.blockY, rayTrace.blockZ);
        onCandidateChanged.accept(
                BRUtil.scanCandidates(
                        getContextObject(),
                        structureElementMap.get(pos),
                        getOriginalTriggerStack(),
                        selectedBlock.set(pos)));
    }

    private boolean beginCameraDrag(int button) {
        if (renderer == null || button < 0 || button > 2 || !isInsideView()) return false;
        if (!PreviewInteractionConfig.isLegacyControls() && button != PreviewInteractionConfig.getPanButton())
            focusCameraAtMouse();
        cameraDragButton = button;
        candidateClickPending = button == 1 && !PreviewInteractionConfig.isLegacyControls();
        candidateClickX = guiMouseX;
        candidateClickY = guiMouseY;
        lastGuiMouseX = guiMouseX;
        lastGuiMouseY = guiMouseY;
        return true;
    }

    public void updateMousePosition(int mouseX, int mouseY) {
        relativeMousePos.add(mouseX - guiMouseX, mouseY - guiMouseY);
        guiMouseX = mouseX;
        guiMouseY = mouseY;
    }

    public void cancelCameraDrag() {
        cameraDragButton = -1;
        candidateClickPending = false;
    }

    public void onMouseDragged() {
        for (BRButton button : allButtons) {
            button.mouseDragged(relativeMousePos.x, relativeMousePos.y);
        }
    }

    public void onMouseReleased() {
        for (BRButton button : allButtons) {
            button.mouseReleased(relativeMousePos.x, relativeMousePos.y);
        }
    }

    public void onMouseReleased(int button) {
        if (button == 0) onMouseReleased();
        if (button != cameraDragButton) return;
        boolean candidateClick = candidateClickPending && button == 1
                && isInsideView()
                && Math.abs(guiMouseX - candidateClickX) <= 3
                && Math.abs(guiMouseY - candidateClickY) <= 3;
        cancelCameraDrag();
        if (candidateClick) showCandidatesAtMouse();
    }

    public boolean keyTyped(char keyChar, int keyCode) {
        for (BRButton button : allButtons) {
            if (button instanceof GuiSlider slider && slider.isInputMode() && slider.keyTyped(keyChar, keyCode)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    protected ItemStack getOriginalTriggerStack() {
        return DEFAULT_TRIGGER;
    }

    protected void placeSurvivalPreview(ISurvivalConstructable constructable) {
        ISurvivalBuildEnvironment environment = ISurvivalBuildEnvironment
                .create(CreativeItemSource.instance, FAKE_PLAYER);
        renderer.world.hasChanged();
        for (int round = 0; round < MAX_PLACE_ROUNDS; round++) {
            int result = constructable.survivalConstruct(getBuildTriggerStack(), Integer.MAX_VALUE, environment);
            // Multipart builders can sum completion codes to -2 after making progress.
            if (renderer.world.hasChanged()) continue;
            if (round == 0 && result == -2) {
                constructable.construct(getBuildTriggerStack(), false);
            }
            return;
        }
    }

    protected ItemStack getBuildTriggerStack() {
        return trigger;
    }

    public void setOnCandidateChanged(Consumer<List<List<ItemStack>>> callback) {
        onCandidateChanged = callback;
    }

    public void setOnIngredientChanged(Consumer<List<ItemStack>> callback) {
        onIngredientChanged = callback;
    }

    public void recalculateSearch(String searchText) {
        if (renderer == null) return;

        if (searchText.equals(lastSearch)) return;
        lastSearch = searchText;

        updateRenderer();
    }

    public boolean matchesSearch(ItemStack stack, String searchText) {
        boolean matches = StringUtils.containsIgnoreCase(stack.getDisplayName(), searchText);
        if (!matches) {
            List<String> tooltip = stack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips);
            matches = tooltip.stream().anyMatch(s -> StringUtils.containsIgnoreCase(s, searchText));
        }
        return matches;
    }

    public List<String> getHoveredTooltip(@NotNull ItemStack stack) {
        return stack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips);
    }

    public @NotNull List<String> getTooltip() {
        for (BRButton button : allButtons) {
            List<String> tooltip = button.getTooltip(relativeMousePos);
            if (!tooltip.isEmpty()) {
                return tooltip;
            }
        }

        if (tooltipBlockStack != null) {
            return getHoveredTooltip(tooltipBlockStack);
        }

        return Collections.emptyList();
    }

    protected Object getContextObject() {
        return renderingController;
    }

    protected void onPostBlocksRendered(WorldSceneRenderer renderer) {}

    protected void onElementAdded(@NotNull IStructureElement<Object> element, long pos) {}

    @SubscribeEvent
    @SuppressWarnings({ "unused", "unchecked" })
    public static void OnStructureEvent(StructureEvent.StructureElementVisitedEvent event) {
        if (!BlockRenderer6343.MOD_ID.equals(event.getInstrumentIdentifier())) return;
        GuiMultiblockHandler handler = MultiblockHandler.getCurrentGuiHandler();
        if (handler == null) return;
        IStructureElement<Object> element = (IStructureElement<Object>) event.getElement();
        long pos = CoordinatePacker.pack(event.getX(), event.getY(), event.getZ());
        handler.onElementAdded(element, pos);
        handler.structureElementMap.put(pos, element);
    }
}
