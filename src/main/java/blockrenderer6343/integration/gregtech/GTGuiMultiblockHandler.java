package blockrenderer6343.integration.gregtech;

import static blockrenderer6343.client.utils.BRUtil.FAKE_PLAYER;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import com.google.common.collect.ImmutableList;
import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructableProvider;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;

import blockrenderer6343.api.utils.CreativeItemSource;
import blockrenderer6343.client.renderer.WorldSceneRenderer;
import blockrenderer6343.client.utils.BRUtil;
import blockrenderer6343.client.utils.EnumColor;
import blockrenderer6343.integration.nei.GuiMultiblockHandler;
import blockrenderer6343.integration.nei.StructureHacks;
import cpw.mods.fml.relauncher.ReflectionHelper;
import gregtech.api.interfaces.INEIPreviewModifier;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.interfaces.tileentity.ITurnable;
import gregtech.api.threads.RunnableMachineUpdate;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.HatchElementBuilder;
import gregtech.common.misc.GTStructureChannels;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class GTGuiMultiblockHandler extends GuiMultiblockHandler {

    private static final Object2IntMap<IStructureElement<?>> cachedDots = new Object2IntOpenHashMap<>();
    private static final Int2ObjectMap<LongSet> hatchGroupPositions = new Int2ObjectLinkedOpenHashMap<>();
    private static final Int2ObjectMap<String> hintForDot = new Int2ObjectOpenHashMap<>();
    private static final Long2IntMap dotForPos = new Long2IntOpenHashMap();
    private static final List<String> hatchElements;
    private static final String HATCH_ELEMENT;
    private static final MethodHandle DOT_GETTER, HINT_FALLBACK;
    private static final Pattern hintPattern = Pattern.compile("Hint \\d+ dot:");

    private boolean highlightHatch = false;

    static {
        dotForPos.defaultReturnValue(9999);
        HatchElementBuilder<?> builder = GTStructureUtility.buildHatchAdder().adder((a, b, c) -> true).casingIndex(1)
                .hint(1);
        String hatchNoPlacement = builder.build().getClass().getName();
        IStructureElement<?> hatchEle = builder.hatchItemFilter((a, b) -> c -> true).build();
        HATCH_ELEMENT = hatchEle.getClass().getName();
        String chained = builder.buildAndChain().getClass().getName();
        hatchElements = ImmutableList.of(HATCH_ELEMENT, hatchNoPlacement, chained);

        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle hatchBuilder = lookup
                    .unreflectGetter(ReflectionHelper.findField(hatchEle.getClass(), "this$0"));
            MethodHandle hint = lookup.unreflectGetter(ReflectionHelper.findField(HatchElementBuilder.class, "mHint"));
            DOT_GETTER = MethodHandles.filterReturnValue(hatchBuilder, hint);
            HINT_FALLBACK = lookup
                    .unreflect(ReflectionHelper.findMethod(hatchEle.getClass(), null, new String[] { "getHint" }));

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void initGui() {
        super.initGui();
        addButtonInRow("H").setTooltip(I18n.format("blockrenderer6343.nei.hatch_highlight"))
                .setClickAction(() -> highlightHatch = !highlightHatch);
    }

    @Override
    protected void loadNewMultiblock() {
        hintForDot.clear();
        dotForPos.clear();
        hatchGroupPositions.clear();
        super.loadNewMultiblock();
        setChannelTier(GTStructureChannels.HATCH.get(), 1);
        findHints();
    }

    @Override
    public List<String> getHoveredTooltip(@NotNull ItemStack stack) {
        List<String> stackTooltip = super.getHoveredTooltip(stack);
        MovingObjectPosition mop = renderer.getLastTraceResult();
        int dot = dotForPos.get(CoordinatePacker.pack(mop.blockX, mop.blockY, mop.blockZ));
        if (dot == dotForPos.defaultReturnValue()) return stackTooltip;
        String hint = hintForDot.get(dot);
        if (hint != null) {
            stackTooltip.add("");
            EnumChatFormatting dotColor = EnumColor.VALUES[dot % EnumColor.VALUES.length].formatting;
            stackTooltip
                    .add(EnumChatFormatting.GOLD + I18n.format("blockrenderer6343.nei.hint_dot", dotColor + "" + dot));
            String hintStr = EnumChatFormatting.GOLD + I18n.format("blockrenderer6343.nei.valid_hatches", hint);
            stackTooltip.addAll(mc.fontRenderer.listFormattedStringToWidth(hintStr, 200));
        }

        return stackTooltip;
    }

    @Override
    protected void onPostBlocksRendered(WorldSceneRenderer renderer) {
        if (!highlightHatch || hatchGroupPositions.isEmpty()) return;
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        for (Int2ObjectMap.Entry<LongSet> entry : hatchGroupPositions.int2ObjectEntrySet()) {
            LongSet positions = entry.getValue();
            if (positions.isEmpty()) continue;

            int color = EnumColor.VALUES[entry.getIntKey() % EnumColor.VALUES.length].color;
            for (long pos : positions) {
                if (!renderer.renderedBlocks.contains(pos)) continue;
                BRUtil.renderOverlay(positions, pos, color, 150);
            }

        }

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    @Override
    protected void placeMultiblock() {
        if (RunnableMachineUpdate.isCurrentThreadEnabled()) {
            RunnableMachineUpdate.setCurrentThreadEnabled(false);
        }

        IConstructable constructable = null;
        ItemStack copy = stackForm.copy();
        copy.getItem().onItemUse(
                copy,
                FAKE_PLAYER,
                renderer.world,
                MB_PLACE_POS.x,
                MB_PLACE_POS.y,
                MB_PLACE_POS.z,
                0,
                MB_PLACE_POS.x,
                MB_PLACE_POS.y,
                MB_PLACE_POS.z);

        TileEntity tTileEntity = renderer.world.getTileEntity(MB_PLACE_POS.x, MB_PLACE_POS.y, MB_PLACE_POS.z);
        ((ITurnable) tTileEntity).setFrontFacing(ForgeDirection.SOUTH);
        IMetaTileEntity mte = ((IGregTechTileEntity) tTileEntity).getMetaTileEntity();

        if (mte instanceof INEIPreviewModifier modifier) {
            modifier.onPreviewConstruct(getBuildTriggerStack());
        }

        if (mte instanceof ISurvivalConstructable survivalConstructable) {
            int iterations = 0;
            do {
                survivalConstructable.survivalConstruct(
                        getBuildTriggerStack(),
                        Integer.MAX_VALUE,
                        ISurvivalBuildEnvironment.create(CreativeItemSource.instance, FAKE_PLAYER));
                iterations++;
            } while (renderer.world.hasChanged() && iterations < MAX_PLACE_ROUNDS);
        } else if (tTileEntity instanceof IConstructableProvider iConstructableProvider) {
            constructable = iConstructableProvider.getConstructable();
        } else if (tTileEntity instanceof IConstructable iConstructable) {
            constructable = iConstructable;
        }

        if (constructable != null) {
            constructable.construct(getBuildTriggerStack(), false);
        }

        if (mte instanceof INEIPreviewModifier modifier) {
            modifier.onPreviewStructureComplete(getBuildTriggerStack());
        }

        if (!RunnableMachineUpdate.isCurrentThreadEnabled()) {
            RunnableMachineUpdate.setCurrentThreadEnabled(true);
        }
    }

    @Override
    protected void onElementAdded(@NotNull IStructureElement<Object> element, long pos) {
        if (StructureHacks.anyElementMatches(hatchElements, renderingController, element)) {
            int dot = getDotForElement(element);
            if (dot == dotForPos.defaultReturnValue()) return;
            hatchGroupPositions.computeIfAbsent(dot, k -> new LongOpenHashSet()).add(pos);
            dotForPos.put(pos, dot);
        }
    }

    private void findHints() {
        for (String string : renderingController.getStructureDescription(getBuildTriggerStack())) {
            Matcher matcher = hintPattern.matcher(string);
            if (matcher.find()) {
                String match = matcher.group();
                int dot = Integer.parseInt(match.split(" ")[1]);
                String hint = string.replace(match, "");
                hintForDot.put(dot, hint);
            }
        }

        if (hintForDot.size() != hatchGroupPositions.size()) {
            IntList missing = new IntArrayList(hatchGroupPositions.keySet());
            missing.removeAll(hintForDot.keySet());

            for (int dot : missing) {
                for (long pos : hatchGroupPositions.get(dot)) {
                    String hint = getFallbackHint(structureElementMap.get(pos));
                    if (hint.isEmpty()) continue;
                    hintForDot.put(dot, hint);
                    break;
                }
            }
        }
    }

    private int getDotForElement(IStructureElement<Object> element) {
        return cachedDots.computeIfAbsent(element, a -> {
            IStructureElement<?> match = StructureHacks
                    .getFirstMatchingElement(HATCH_ELEMENT, renderingController, element);
            if (match == null) return dotForPos.defaultReturnValue();

            try {
                return (int) DOT_GETTER.invokeWithArguments(match);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String getFallbackHint(@Nullable IStructureElement<Object> element) {
        IStructureElement<Object> hatchEle = StructureHacks
                .getFirstMatchingElement(HATCH_ELEMENT, renderingController, element);
        if (hatchEle == null) return "";
        try {
            String hint = (String) HINT_FALLBACK.invokeWithArguments(hatchEle);
            StringBuilder builder = new StringBuilder(hint);
            int typeIndex = builder.indexOf("of type");
            if (typeIndex == -1) {
                return hint;
            }

            builder.replace(0, 7, "");
            int index;
            while ((index = builder.indexOf(" or")) != -1) {
                int last = builder.lastIndexOf(" or");
                String replacement = last == index ? " and" : ",";
                builder.replace(index, index + 3, replacement);
            }

            return builder.toString();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
