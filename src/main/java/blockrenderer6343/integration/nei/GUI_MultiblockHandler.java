package blockrenderer6343.integration.nei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

import com.github.vfyjxf.nee.network.NEENetworkHandler;
import com.github.vfyjxf.nee.network.packet.PacketNEIPatternRecipe;
import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.alignment.constructable.ConstructableUtility;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.item.ItemConstructableTrigger;
import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.mojang.authlib.GameProfile;

import blockrenderer6343.BlockRenderer6343;
import blockrenderer6343.api.utils.BlockPosition;
import blockrenderer6343.api.utils.CreativeItemSource;
import blockrenderer6343.api.utils.PositionedIStructureElement;
import blockrenderer6343.client.renderer.ImmediateWorldSceneRenderer;
import blockrenderer6343.client.renderer.WorldSceneRenderer;
import blockrenderer6343.client.utils.GuiText;
import blockrenderer6343.client.utils.TooltipButton;
import blockrenderer6343.client.world.ClientFakePlayer;
import blockrenderer6343.client.world.DummyWorld;
import blockrenderer6343.client.world.TrackedDummyWorld;
import codechicken.lib.gui.GuiDraw;
import codechicken.lib.math.MathHelper;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.recipe.GuiRecipe;

public abstract class GUI_MultiblockHandler<T> {

    protected static ImmediateWorldSceneRenderer renderer;

    public static final int SLOT_SIZE = 18;

    protected static final int RECIPE_LAYOUT_X = 8;
    protected static final int RECIPE_LAYOUT_Y = 50;
    protected static final int RECIPE_WIDTH = 160;
    protected static final int sceneHeight = RECIPE_WIDTH - 10;
    protected static final int ICON_SIZE_X = 20;
    protected static final int ICON_SIZE_Y = 12;
    protected static final int MOUSE_OFFSET_X = 5;
    protected static final int MOUSE_OFFSET_Y = 37;
    protected static final int BUTTON_LEFT = -5;
    protected static final int UNDER_PREVIEW_Y = 153;
    protected static final int BUTTON_RIGHT = 145;
    protected static final int BETWEEN_BUTTON_X = ICON_SIZE_X + 3;
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
    protected static int initialChannelTierButtonTitleWidth;
    protected ClearGuiButton previousLayerButton, nextLayerButton;
    protected ClearGuiButton previousTierButton, nextTierButton;
    protected ClearGuiButton previousChannelButton, nextChannelButton;
    protected ClearGuiButton previousChannelTier, nextChannelTier;

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
    protected int blocksBelowController;
    protected static int scaledSceneHeight = sceneHeight;

    protected int channelIndex;
    protected ItemStack trigger;
    public static Set<String> channels = new HashSet<>();
    protected int[] channelTier;
    protected String[] channelArray;
    protected boolean useMasterChannel = true;
    protected String channelTitle, channelTierTitle;
    protected int lastHeight;

    public GUI_MultiblockHandler() {
        previousTierButton = new ClearGuiButton(1, BUTTON_LEFT, UNDER_PREVIEW_Y, "<");
        nextTierButton = new ClearGuiButton(1, BUTTON_LEFT + ICON_SIZE_X, UNDER_PREVIEW_Y, ">");
        previousLayerButton = new ClearGuiButton(2, BUTTON_LEFT, UNDER_PREVIEW_Y + ICON_SIZE_Y, "<");
        nextLayerButton = new ClearGuiButton(2, BUTTON_LEFT + ICON_SIZE_X, UNDER_PREVIEW_Y + ICON_SIZE_Y, ">");
        previousChannelButton = new ClearGuiButton(
                4,
                BUTTON_LEFT,
                UNDER_PREVIEW_Y + ICON_SIZE_Y * 3,
                "<",
                this::hasChannels);
        nextChannelButton = new ClearGuiButton(
                4,
                BUTTON_LEFT + ICON_SIZE_X,
                UNDER_PREVIEW_Y + ICON_SIZE_Y * 3,
                ">",
                this::hasChannels);
        previousChannelTier = new ClearGuiButton(
                5,
                BUTTON_LEFT,
                UNDER_PREVIEW_Y + ICON_SIZE_Y * 4,
                "<",
                this::hasChannels);
        nextChannelTier = new ClearGuiButton(
                5,
                BUTTON_LEFT + ICON_SIZE_X,
                UNDER_PREVIEW_Y + ICON_SIZE_Y * 4,
                ">",
                this::hasChannels);
        TooltipButton projectMultiblocksButton = new TooltipButton(
                1,
                BUTTON_RIGHT,
                UNDER_PREVIEW_Y,
                ICON_SIZE_X,
                ICON_SIZE_Y,
                "P",
                StatCollector.translateToLocal("blockrenderer6343.multiblock.project"));
        TooltipButton overlayMultiblocksButton = new TooltipButton(
                1,
                BUTTON_RIGHT - BETWEEN_BUTTON_X,
                UNDER_PREVIEW_Y,
                ICON_SIZE_X,
                ICON_SIZE_Y,
                "?",
                StatCollector.translateToLocal("blockrenderer6343.multiblock.overlay"));
        TooltipButton copyChannelButton = new TooltipButton(
                1,
                BUTTON_RIGHT - BETWEEN_BUTTON_X * 2,
                UNDER_PREVIEW_Y,
                ICON_SIZE_X,
                ICON_SIZE_Y,
                "C",
                StatCollector.translateToLocal("blockrenderer6343.multiblock.copy_channels"),
                this::hasChannels);

        buttons.put(previousLayerButton, this::togglePreviousLayer);
        buttons.put(nextLayerButton, this::toggleNextLayer);
        buttons.put(previousTierButton, this::togglePreviousTier);
        buttons.put(nextTierButton, this::toggleNextTier);
        buttons.put(previousChannelButton, this::togglePreviousChannel);
        buttons.put(nextChannelButton, this::toggleNextChannel);
        buttons.put(previousChannelTier, this::togglePreviousChannelTier);
        buttons.put(nextChannelTier, this::toggleNextChannelTier);
        buttons.put(projectMultiblocksButton, this::projectMultiblock);
        buttons.put(overlayMultiblocksButton, this::neiOverlay);
        buttons.put(copyChannelButton, this::copyToHologram);
    }

    protected abstract void placeMultiblock();

    protected void setupColors() {
        guiTextLayer = GuiText.Layer.getLocal();
        guiColorBg = GuiText.BgColor.getColor();
        guiColorFont = GuiText.FontColor.getColor();
        buttonColorEnabled = GuiText.ButtonEnabledColor.getColor();
        buttonColorDisabled = GuiText.ButtonDisabledColor.getColor();
        buttonColorHovered = GuiText.ButtonHoveredColor.getColor();
    }

    protected void setupButtonText() {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        guiTextTier = GuiText.Tier.getLocal();
        refreshButtonText();
        initialLayerButtonTitleWidth = fontRenderer.getStringWidth(guiLayerButtonTitle);
        initialTierButtonTitleWidth = fontRenderer.getStringWidth(guiTierButtonTitle);
        if (hasChannels()) {
            initialChannelTierButtonTitleWidth = fontRenderer.getStringWidth(channelTierTitle);
        }
        for (GuiButton button : buttons.keySet()) {
            if (button instanceof ClearGuiButton clearButton) {
                clearButton.setColors(buttonColorEnabled, buttonColorDisabled, buttonColorHovered);
            }
        }
        refreshButtonPos();
    }

    protected void refreshButtonPos() {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        nextTierButton.xPosition = BUTTON_LEFT + ICON_SIZE_X
                + initialTierButtonTitleWidth
                - fontRenderer.getStringWidth("<") / 2;
        nextLayerButton.xPosition = BUTTON_LEFT + ICON_SIZE_X
                + initialLayerButtonTitleWidth
                - fontRenderer.getStringWidth("<") / 2;
        nextChannelButton.xPosition = BUTTON_LEFT + ICON_SIZE_X
                + fontRenderer.getStringWidth(StatCollector.translateToLocal("blockrenderer6343.nei.channel"));
        nextChannelTier.xPosition = BUTTON_LEFT + ICON_SIZE_X
                + initialChannelTierButtonTitleWidth
                - fontRenderer.getStringWidth("<") / 2;
        for (GuiButton b : buttons.keySet()) {
            if (b instanceof TooltipButton tooltipButton) {
                tooltipButton.yPosition = scaledSceneHeight + ICON_SIZE_Y;
                continue;
            }
            b.yPosition = scaledSceneHeight + ICON_SIZE_Y * b.id;
        }
    }

    public void refreshButtonText() {
        guiLayerButtonTitle = getLayerButtonTitle();
        guiTierButtonTitle = getTierButtonTitle();
        if (hasChannels()) {
            channelTitle = getChannelTitle();
            channelTierTitle = getChannelTierTitle();
        }
    }

    public void loadMultiblock(T multiblock, ItemStack stackForm) {
        setupColors();
        renderingController = multiblock;
        this.stackForm = stackForm;
        if (stackForm.stackSize == 0) stackForm.stackSize = 1;
        if (lastRenderingController != renderingController) {
            loadNewMultiblock();
        } else {
            loadPreviousMultiblockAgain();
        }
        setupButtonText();
    }

    protected void loadNewMultiblock() {
        trigger = getOriginalTriggerStack();
        channels.clear();
        layerIndex = -1;
        tierIndex = 1;
        channelIndex = 0;
        initializeSceneRenderer(true);
        lastRenderingController = renderingController;
        channelTier = new int[channels.size()];
        Arrays.fill(channelTier, 1);
        if (hasChannels()) {
            channelArray = channels.toArray(new String[0]);
        }
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
        refreshButtonText();
    }

    private void togglePreviousLayer() {
        int height = (int) ((TrackedDummyWorld) renderer.world).getSize().getY() - 1;
        if (layerIndex == -1) {
            layerIndex = height;
        } else if (--layerIndex < 0) {
            layerIndex = -1;
        }
        setNextLayer(layerIndex);
        refreshButtonText();
    }

    private void toggleNextChannel() {
        if (!hasChannels()) return;
        if (++channelIndex >= channels.size()) {
            channelIndex = 0;
        }
        useMasterChannel = false;
        initializeSceneRenderer(false);
        refreshButtonText();
    }

    private void togglePreviousChannel() {
        if (!hasChannels()) return;
        if (--channelIndex < 0) {
            channelIndex = channels.size() - 1;
        }
        useMasterChannel = false;
        initializeSceneRenderer(false);
        refreshButtonText();
    }

    private void toggleNextChannelTier() {
        if (!hasChannels()) return;
        channelTier[channelIndex] += 1;
        useMasterChannel = false;
        ChannelDataAccessor.setChannelData(trigger, channelArray[channelIndex], channelTier[channelIndex]);
        initializeSceneRenderer(false);
        refreshButtonText();
    }

    private void togglePreviousChannelTier() {
        if (!hasChannels()) return;
        channelTier[channelIndex] -= 1;
        if (channelTier[channelIndex] == 0) {
            channelTier[channelIndex] = 1;
            refreshButtonText();
            return;
        }
        useMasterChannel = false;
        ChannelDataAccessor.setChannelData(trigger, channelArray[channelIndex], channelTier[channelIndex]);
        initializeSceneRenderer(false);
        refreshButtonText();
    }

    protected void toggleNextTier() {
        tierIndex++;
        useMasterChannel = true;
        initializeSceneRenderer(false);
        refreshButtonText();
    }

    protected void togglePreviousTier() {
        if (tierIndex > 1) {
            useMasterChannel = true;
            tierIndex--;
            initializeSceneRenderer(false);
            refreshButtonText();
        }
    }

    protected boolean hasChannels() {
        return !channels.isEmpty();
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
        int guiLeft = NEIClientUtils.getGuiContainer().guiLeft;
        int guiTop = NEIClientUtils.getGuiContainer().guiTop;

        int guiHeight = NEIClientUtils.getGuiContainer().height;
        if (guiHeight != lastHeight) {
            scaledSceneHeight = Math.min(sceneHeight, sceneHeight * guiHeight / 500);
            refreshButtonPos();
        }
        renderer.render(
                RECIPE_LAYOUT_X + guiLeft,
                RECIPE_LAYOUT_Y + guiTop,
                RECIPE_WIDTH,
                scaledSceneHeight,
                lastGuiMouseX,
                lastGuiMouseY);
        drawMultiblockName();

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        tooltipBlockStack = null;

        MovingObjectPosition rayTraceResult = renderer.getLastTraceResult();
        boolean insideView = guiMouseX >= guiLeft + RECIPE_LAYOUT_X && guiMouseY >= guiTop + RECIPE_LAYOUT_Y
                && guiMouseX < guiLeft + RECIPE_LAYOUT_X + RECIPE_WIDTH
                && guiMouseY < guiTop + RECIPE_LAYOUT_Y + sceneHeight;
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
        int actualMouseX = guiMouseX - guiLeft - MOUSE_OFFSET_X;
        int actualMouseY = guiMouseY - guiTop - MOUSE_OFFSET_Y;
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        for (GuiButton button : buttons.keySet()) {
            button.drawButton(Minecraft.getMinecraft(), actualMouseX, actualMouseY);
        }
        drawButtonsTitle(fontRenderer);
        for (GuiButton button : buttons.keySet()) {
            if (button instanceof TooltipButton tooltipButton
                    && tooltipButton.isMouseOver(actualMouseX, actualMouseY)) {
                int textWidth = fontRenderer.getStringWidth(tooltipButton.hoverString);
                tooltipButton.drawTooltipBox(
                        fontRenderer,
                        actualMouseX - 3,
                        actualMouseY - 17,
                        textWidth + 3,
                        tooltipButton.height);
            }
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
                    Minecraft.getMinecraft().thePlayer);
        }

        lastHeight = guiHeight;
        lastGuiMouseX = guiMouseX;
        lastGuiMouseY = guiMouseY;

        // don't activate these
        // GlStateManager.disableRescaleNormal();
        // GlStateManager.disableLighting();
        // RenderHelper.disableStandardItemLighting();
    }

    protected String getMultiblockName() {
        return I18n.format(stackForm.getDisplayName());
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

    protected void drawButtonsTitle(FontRenderer fontRenderer) {
        fontRenderer.drawString(
                guiTierButtonTitle,
                BUTTON_LEFT + ICON_SIZE_X
                        + (initialTierButtonTitleWidth - fontRenderer.getStringWidth(guiTierButtonTitle)) / 2,
                scaledSceneHeight + ICON_SIZE_Y + 2,
                guiColorFont);
        fontRenderer.drawString(
                guiLayerButtonTitle,
                BUTTON_LEFT + ICON_SIZE_X
                        + (initialLayerButtonTitleWidth - fontRenderer.getStringWidth(guiLayerButtonTitle)) / 2,
                scaledSceneHeight + (ICON_SIZE_Y * 2) + 2,
                guiColorFont);
        if (hasChannels()) {
            fontRenderer.drawString(
                    StatCollector.translateToLocal("blockrenderer6343.nei.current_channel") + ": " + channelTitle,
                    BUTTON_LEFT + 6,
                    scaledSceneHeight + (ICON_SIZE_Y * 3) + 2,
                    guiColorFont);
            fontRenderer.drawString(
                    StatCollector.translateToLocal("blockrenderer6343.nei.channel"),
                    BUTTON_LEFT + ICON_SIZE_X,
                    scaledSceneHeight + (ICON_SIZE_Y * 4) + 2,
                    guiColorFont);
            fontRenderer.drawString(
                    channelTierTitle,
                    BUTTON_LEFT + ICON_SIZE_X
                            + (initialChannelTierButtonTitleWidth - fontRenderer.getStringWidth(channelTierTitle)) / 2,
                    scaledSceneHeight + (ICON_SIZE_Y * 5) + 2,
                    guiColorFont);
        }
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

        fakeMultiblockBuilder = createFakeBuilder(renderer.world, BlockRenderer6343.MOD_NAME);
        renderer.world.unloadEntities(Arrays.asList(fakeMultiblockBuilder));

        placeMultiblock();

        Vector3f size = ((TrackedDummyWorld) renderer.world).getSize();
        Vector3f minPos = ((TrackedDummyWorld) renderer.world).getMinPos();
        center = new Vector3f(minPos.x + size.x / 2, minPos.y + size.y / 2, minPos.z + size.z / 2);

        renderer.renderedBlocks.clear();
        renderer.addRenderedBlocks(((TrackedDummyWorld) renderer.world).placedBlocks);
        renderer.setOnLookingAt(ray -> {});

        renderer.setOnWorldRender(this::onRendererRender);

        blocksBelowController = MathHelper.floor_double(MB_PLACE_POS.y - minPos.y);
        selectedBlock = null;
        onBlockSelected();
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

    public void handleMouseScrollUp(int scrolled) {
        this.scrolled = scrolled;
    }

    protected String getTierButtonTitle() {
        return guiTextTier + ": " + tierIndex;
    }

    protected String getChannelTitle() {
        return useMasterChannel ? "All" : firstUpper(channelArray[channelIndex]);
    }

    protected String getChannelTierTitle() {
        return StatCollector.translateToLocal("blockrenderer6343.nei.channel_tier") + ": " + channelTier[channelIndex];
    }

    protected String getLayerButtonTitle() {
        return guiTextLayer + ": " + (layerIndex == -1 ? "A" : Integer.toString(layerIndex + 1));
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

    protected void projectMultiblock() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        World baseWorld = Minecraft.getMinecraft().theWorld;
        Vec3 lookVec = player.getLookVec();
        MovingObjectPosition lookingPos = player.rayTrace(10, 1);
        int blockX, blockY, blockZ;
        if (lookingPos == null || lookingPos.typeOfHit == MovingObjectPosition.MovingObjectType.MISS) {
            blockX = MathHelper.floor_double(player.posX + lookVec.xCoord * 2);
            blockZ = MathHelper.floor_double(player.posZ + lookVec.zCoord * 2);
            blockY = baseWorld.getPrecipitationHeight(blockX, blockZ) + blocksBelowController;
        } else {
            blockX = lookingPos.blockX;
            blockY = lookingPos.blockY + blocksBelowController + 1;
            blockZ = lookingPos.blockZ;
        }
        ItemStack copy = stackForm.copy();

        if (!baseWorld.isAirBlock(blockX, blockY, blockZ)) {
            player.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("blockrenderer6343.no_space")));
            return;
        }

        if (!copy.getItem().onItemUse(copy, player, baseWorld, blockX, blockY, blockZ, 0, blockX, blockY - 1, blockZ)) {
            player.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("blockrenderer6343.no_block")));
            return;
        }
        ConstructableUtility.handle(getBuildTriggerStack(), player, baseWorld, blockX, blockY, blockZ, 0);
        baseWorld.setBlockToAir(blockX, blockY, blockZ);
        baseWorld.removeTileEntity(blockX, blockY, blockZ);
    }

    protected void neiOverlay() {
        if (!BlockRenderer6343.isNEELoaded) return;
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

    protected void copyToHologram() {
        if (!hasChannels()) return;
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        ItemStack stack = player.getHeldItem();
        if (stack == null || !(stack.getItem() instanceof ItemConstructableTrigger)) {
            player.addChatMessage(
                    new ChatComponentText(StatCollector.translateToLocal("blockrenderer6343.no_projector")));
        } else {
            StructureLib.instance().proxy().uploadChannels(trigger);
        }
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
            int guiLeft = NEIClientUtils.getGuiContainer().guiLeft;
            int guiTop = NEIClientUtils.getGuiContainer().guiTop;
            if (buttons.getKey().mousePressed(
                    Minecraft.getMinecraft(),
                    guiMouseX - guiLeft - MOUSE_OFFSET_X,
                    guiMouseY - guiTop - MOUSE_OFFSET_Y)) {
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
    protected static ItemStack getOriginalTriggerStack() {
        return new ItemStack(StructureLibAPI.getDefaultHologramItem(), tierIndex);
    }

    protected EntityPlayer createFakeBuilder(World world, String name) {
        return new ClientFakePlayer(world, new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
    }

    @NotNull
    protected ItemStack getBuildTriggerStack() {
        return useMasterChannel ? getOriginalTriggerStack() : trigger;
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
                            getOriginalTriggerStack(),
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

    public String firstUpper(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    protected static class ClearGuiButton extends GuiButton {

        private int colorEnabled;
        private int colorDisabled;
        private int colorHovered;
        private BooleanSupplier booleanSupplier;

        public ClearGuiButton(int id, int x, int y, String displayString) {
            this(id, x, y, displayString, null);
        }

        public ClearGuiButton(int id, int x, int y, String displayString, BooleanSupplier booleanSupplier) {
            super(id, x, y, ICON_SIZE_X, ICON_SIZE_Y, displayString);
            this.booleanSupplier = booleanSupplier;
        }

        public void setColors(int clrEnabled, int clrDisabled, int clrHovered) {
            colorEnabled = clrEnabled;
            colorDisabled = clrDisabled;
            colorHovered = clrHovered;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (this.visible) {
                if (booleanSupplier != null && !booleanSupplier.getAsBoolean()) return;

                FontRenderer fontrenderer = mc.fontRenderer;
                this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition
                        && mouseX < this.xPosition + this.width
                        && mouseY < this.yPosition + this.height;
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
