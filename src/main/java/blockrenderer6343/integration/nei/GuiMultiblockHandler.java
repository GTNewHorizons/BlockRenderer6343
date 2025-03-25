package blockrenderer6343.integration.nei;

import static blockrenderer6343.client.utils.BRUtil.FAKE_PLAYER;

import java.awt.Point;
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
import com.gtnewhorizon.structurelib.structure.IStructureElement;

import blockrenderer6343.BlockRenderer6343;
import blockrenderer6343.client.renderer.ImmediateWorldSceneRenderer;
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
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

@EventBusSubscriber(side = Side.CLIENT)
public abstract class GuiMultiblockHandler {

    protected static ImmediateWorldSceneRenderer renderer;
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
    public static final Long2ObjectMap<IStructureElement<Object>> structureElementMap = new Long2ObjectOpenHashMap<>();

    protected static final int BETWEEN_BUTTON_X = ICON_SIZE_X + 2;
    protected static final int SLIDER_WIDTH = BUTTON_RIGHT - BETWEEN_BUTTON_X - 2;
    protected static final float DEFAULT_RANGE_MULTIPLIER = 3.5f;
    protected static final int MAX_PLACE_ROUNDS = 2000;
    protected static final BlockPos MB_PLACE_POS = new BlockPos(0, 64, 0);
    protected static final BlockPos SELECTED_BLOCK = new BlockPos().set(NO_SELECTED_BLOCK);
    protected static final ItemStack DEFAULT_TRIGGER = new ItemStack(StructureLibAPI.getDefaultHologramItem());

    protected static int guiMouseX, guiMouseY, guiLeft, guiTop;
    protected static int lastGuiMouseX, lastGuiMouseY;
    protected static Vector3f center = new Vector3f();
    protected static float rotationYaw, rotationPitch;
    protected static float zoom;
    protected static IConstructable renderingController, lastRenderingController;

    protected ItemStack tooltipBlockStack;

    protected ConstructableData constructableData;
    protected ItemStack stackForm;

    protected Consumer<List<List<ItemStack>>> onCandidateChanged;
    protected Consumer<List<ItemStack>> onIngredientChanged;
    protected static int layerIndex = -1;

    protected int scrolled = 0;
    protected float scaleFactor;

    protected ItemStack trigger;
    protected int lastHeight;
    protected final ObjectList<BRButton> allButtons = new ObjectArrayList<>();
    protected GuiSlider tierSlider;
    protected Vector2i relativeMousePos = new Vector2i();
    protected GuiRecipe<?> recipeGui;
    private int buttonsInRow;
    protected String lastSearch = "";
    protected static Minecraft mc = Minecraft.getMinecraft();

    protected abstract void placeMultiblock();

    public void loadMultiblock(IConstructable multiblock, ItemStack stackForm, @NotNull ConstructableData data) {
        renderingController = multiblock;
        constructableData = data;
        recipeGui = (GuiRecipe<?>) NEIClientUtils.getGuiContainer();

        this.stackForm = stackForm;
        if (stackForm.stackSize == 0) stackForm.stackSize = 1;
        if (lastRenderingController != renderingController) {
            initGui();
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
                        -1,
                        -1).setTextSupplier(
                                value -> value == -1 ? I18n.format("blockrenderer6343.nei.all")
                                        : String.valueOf(value + 1))
                                .setMaxValueSupplier(
                                        () -> (int) (renderer.world.getMaxPos().y - renderer.world.getMinPos().y))
                                .setValueListener(this::setNextLayer).setIndex(1));
        loadChannels();
        addButtonInRow("P").setTooltip(I18n.format("blockrenderer6343.multiblock.project")).setClickAction(
                () -> BRUtil.projectMultiblock(
                        getBuildTriggerStack(),
                        stackForm,
                        MathHelper.floor_double(MB_PLACE_POS.y - renderer.world.getMinPos().y)));
        addButtonInRow("?").setTooltip(I18n.format("blockrenderer6343.multiblock.overlay"))
                .setClickAction(() -> BRUtil.neiOverlay(renderer));
        if (!constructableData.getChannelData().isEmpty()) {
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
        Object2IntMap<String> channels = constructableData.getChannelData();
        int curSliders = allButtons.size();
        if (channels.isEmpty()) return;
        int i = 0;
        for (Object2IntMap.Entry<String> entry : channels.object2IntEntrySet()) {
            String channel = entry.getKey();
            int startVal = constructableData.getCurrentChannel().equals(channel) ? constructableData.getCurrentTier()
                    : 0;
            allButtons.add(
                    new GuiSlider(
                            StringUtils.capitalize(channel),
                            0,
                            UNDER_PREVIEW_Y + (12 * (i + curSliders)),
                            SLIDER_WIDTH,
                            16,
                            startVal,
                            0,
                            entry.getIntValue())
                                    .setValueListener(val -> setChannelTier(channel, val))
                                    .setTextSupplier(
                                            value -> value == 0 ? I18n.format("blockrenderer6343.nei.not_set")
                                                    : String.valueOf(value))
                                    .setIndex(i + curSliders));
            i++;
        }
    }

    protected void loadNewMultiblock() {
        trigger = DEFAULT_TRIGGER.copy();
        int tier = constructableData.getCurrentTier();
        String channel = constructableData.getCurrentChannel();

        if (channel.isEmpty()) {
            tierSlider.setValue(trigger.stackSize = tier, false);
        } else {
            setChannelTier(channel, tier, false);
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
        trigger.stackSize = tier;
        initializeSceneRenderer(false);
    }

    private void setChannelTier(String channel, int tier) {
        setChannelTier(channel, tier, true);
    }

    private void setChannelTier(String channel, int tier, boolean rebuild) {
        if (tier < 0) return;
        if (tier > 0) {
            ChannelDataAccessor.setChannelData(trigger, channel, tier);
        } else {
            ChannelDataAccessor.unsetChannelData(trigger, channel);
        }
        if (rebuild) {
            initializeSceneRenderer(false);
        }
    }

    private void setNextLayer(int newLayer) {
        int height = (int) renderer.world.getSize().y() - 1;
        if (newLayer < 0 || newLayer > height) {
            // if current layer index is more than max height, reset it
            // to display all layers
            newLayer = -1;
        }
        layerIndex = newLayer;
        if (renderer == null) return;

        TrackedDummyWorld world = renderer.world;
        resetCenter();
        renderer.renderedBlocks.clear();
        int minY = (int) world.getMinPos().y();
        LongSet renderBlocks;
        if (newLayer == -1) {
            renderBlocks = world.blockMap.keySet();
            renderer.setRenderAllFaces(false);
        } else {
            renderBlocks = new LongOpenHashSet();
            for (long pos : world.blockMap.keySet()) {
                if (CoordinatePacker.unpackY(pos) - minY == newLayer) {
                    renderBlocks.add(pos);
                }
            }
            renderer.setRenderAllFaces(true);
        }
        renderer.addRenderedBlocks(renderBlocks);
        onIngredientChanged.accept(BRUtil.getIngredients(renderer));
    }

    private void resetCenter() {
        TrackedDummyWorld world = renderer.world;
        Vector3f size = world.getSize();
        Vector3f minPos = world.getMinPos();
        center.set(minPos.x + size.x / 2, minPos.y + size.y / 2, minPos.z + size.z / 2);
        renderer.setCameraLookAt(center, zoom, Math.toRadians(rotationPitch), Math.toRadians(rotationYaw));
    }

    public void drawMultiblock() {
        guiMouseX = GuiDraw.getMousePosition().x;
        guiMouseY = GuiDraw.getMousePosition().y;
        guiLeft = recipeGui.guiLeft;
        guiTop = recipeGui.guiTop;

        int guiHeight = recipeGui.height;
        scaleFactor = Math.min((float) recipeGui.height / 500, 1f);
        int scaledScene = Math.round(SCENE_HEIGHT * scaleFactor);
        renderer.render(
                RECIPE_LAYOUT_X + guiLeft,
                RECIPE_LAYOUT_Y + guiTop,
                RECIPE_WIDTH,
                scaledScene,
                lastGuiMouseX,
                lastGuiMouseY);

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        tooltipBlockStack = null;

        MovingObjectPosition rayTraceResult = renderer.getLastTraceResult();
        boolean leftClickHeld = Mouse.isButtonDown(0);
        boolean rightClickHeld = Mouse.isButtonDown(1);
        boolean middleClickHeld = Mouse.isButtonDown(2);

        if (isInsideView()) {
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
                Vector3f lookAt = renderer.getLookAt();
                Vector3f eyePos = renderer.getEyePos();
                Vector3f worldUp = renderer.getWorldUp();
                Vector3f lookDir = new Vector3f(lookAt).sub(eyePos);
                Vector3f rightDir = lookDir.cross(worldUp, new Vector3f()).normalize();
                Vector3f upDir = rightDir.cross(lookDir, new Vector3f()).normalize();
                Vector3f offset = new Vector3f(
                        -mouseDeltaX * rightDir.x() + mouseDeltaY * upDir.x(),
                        -mouseDeltaX * rightDir.y() + mouseDeltaY * upDir.y(),
                        -mouseDeltaX * rightDir.z() + mouseDeltaY * upDir.z()).mul(0.15f);
                center.add(offset);
            }
            if (scrolled != 0) {
                zoom = (float) MathHelper.clip(zoom - scrolled * 5, 3, 999);
                scrolled = 0;
            }

            renderer.setCameraLookAt(center, zoom, Math.toRadians(rotationPitch), Math.toRadians(rotationYaw));
        }
        Point recipePos = recipeGui.getRecipePosition(0);
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
        renderer.world.updateEntitiesForNEI();

        FAKE_PLAYER.setWorld(renderer.world);
        renderer.world.unloadEntities(Collections.singletonList(FAKE_PLAYER));

        if (!StructureLibAPI.isInstrumentEnabled()) {
            StructureLibAPI.enableInstrument(BlockRenderer6343.MOD_ID);
        }

        structureElementMap.clear();
        placeMultiblock();

        if (StructureLibAPI.isInstrumentEnabled()) {
            StructureLibAPI.disableInstrument();
        }

        Vector3f size = renderer.world.getSize();
        Vector3f minPos = renderer.world.getMinPos();
        center = new Vector3f(minPos.x + size.x / 2, minPos.y + size.y / 2, minPos.z + size.z / 2);

        renderer.renderedBlocks.clear();
        renderer.addRenderedBlocks(renderer.world.blockMap.keySet());
        renderer.setOnLookingAt(ray -> {});

        renderer.setOnWorldRender(this::onRendererRender);
        renderer.setPostBlockRender(this::onPostBlocksRendered);

        SELECTED_BLOCK.set(NO_SELECTED_BLOCK);
        onCandidateChanged.accept(Collections.emptyList());
        setNextLayer(layerIndex);

        if (resetCamera) {
            float max = Math.max(Math.max(size.x, size.y), size.z);
            float baseZoom = (float) (DEFAULT_RANGE_MULTIPLIER * Math.sqrt(max));
            float sizeFactor = (float) (1.0f + Math.log(max) / Math.log(10));

            zoom = baseZoom * sizeFactor / 1.5f;
            rotationYaw = 20.0f;
            rotationPitch = 50f;
            if (renderer != null) {
                resetCenter();
            }
        } else {
            renderer.setCameraLookAt(eyePos, lookAt, worldUp);
        }
    }

    public boolean handleMouseScrollUp(int scrolled) {
        if (isInsideView()) {
            this.scrolled = scrolled;
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
        long selectedBlock = SELECTED_BLOCK.asLong();
        if (selectedBlock == lookingBlock) {
            renderBlockOverLay(selectedBlock, Blocks.glass.getIcon(0, 6));
            return;
        }
        renderBlockOverLay(lookingBlock, Blocks.stained_glass.getIcon(0, 7));
        renderBlockOverLay(selectedBlock, Blocks.stained_glass.getIcon(0, 14));
    }

    private void renderBlockOverLay(long pos, IIcon icon) {
        if (pos == NO_SELECTED_BLOCK) return;

        renderBlocks.blockAccess = renderer.world;
        renderBlocks.setRenderBounds(0, 0, 0, 1, 1, 1);
        renderBlocks.renderAllFaces = true;
        int x = CoordinatePacker.unpackX(pos);
        int y = CoordinatePacker.unpackY(pos);
        int z = CoordinatePacker.unpackZ(pos);
        Block block = renderer.world.getBlock(x, y, z);
        renderBlocks.renderBlockUsingTexture(block, x, y, z, icon);
    }

    public boolean mouseClicked(int button) {
        for (BRButton buttons : allButtons) {
            if (buttons.mousePressed(mc, relativeMousePos.x, relativeMousePos.y)) {
                SELECTED_BLOCK.set(NO_SELECTED_BLOCK);
                onCandidateChanged.accept(Collections.emptyList());
                return true;
            }
        }
        if (button == 1 && renderer != null) {
            MovingObjectPosition rayTrace = renderer.getLastTraceResult();
            if (rayTrace == null) {
                if (SELECTED_BLOCK.asLong() != NO_SELECTED_BLOCK) {
                    SELECTED_BLOCK.set(NO_SELECTED_BLOCK);
                    onCandidateChanged.accept(Collections.emptyList());
                    return true;
                }
                return false;
            }
            long pos = CoordinatePacker.pack(rayTrace.blockX, rayTrace.blockY, rayTrace.blockZ);
            onCandidateChanged.accept(
                    BRUtil.scanCandidates(
                            getContextObject(),
                            structureElementMap.get(pos),
                            getOriginalTriggerStack(),
                            SELECTED_BLOCK.set(pos)));
        }
        return false;
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

    @NotNull
    protected ItemStack getOriginalTriggerStack() {
        return DEFAULT_TRIGGER;
    }

    @NotNull
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
        if (searchText.isEmpty() && !lastSearch.isEmpty()) {
            renderer.resetRenderedBlocks();
            renderer.setRenderAllFaces(false);
            renderer.addRenderedBlocks(renderer.world.blockMap.keySet());
        }

        if (searchText.equals(lastSearch) || (lastSearch = searchText).isEmpty()) return;
        renderer.resetRenderedBlocks();
        boolean foundAny = false;
        Long2BooleanMap checkedBlocks = new Long2BooleanOpenHashMap();
        for (Long2ObjectMap.Entry<Block> entry : renderer.world.blockMap.long2ObjectEntrySet()) {
            boolean add = checkedBlocks.computeIfAbsent(BRUtil.hashBlock(renderer.world, entry.getLongKey()), b -> {
                Block block = entry.getValue();
                ItemStack stack = new ItemStack(
                        block,
                        1,
                        BRUtil.getDamageValue(renderer.world, block, entry.getLongKey()));
                return matchesSearch(stack, searchText);
            });

            if (add) {
                foundAny = true;
                renderer.renderedBlocks.add(entry.getLongKey());
            } else {
                renderer.renderOpaqueBlocks.add(entry.getLongKey());
            }
        }

        if (!foundAny) {
            renderer.renderOpaqueBlocks.clear();
            renderer.renderedBlocks.addAll(renderer.world.blockMap.keySet());
            renderer.setRenderAllFaces(false);
        } else {
            renderer.setRenderAllFaces(true);
        }
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
        if (tooltipBlockStack != null) {
            return getHoveredTooltip(tooltipBlockStack);
        }

        for (BRButton button : allButtons) {
            List<String> tooltip = button.getTooltip(relativeMousePos);
            if (!tooltip.isEmpty()) {
                return tooltip;
            }
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
        structureElementMap.put(pos, element);
    }
}
