package blockrenderer6343.integration.nei;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.reflect.Fields;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureUtility;

import blockrenderer6343.client.utils.BRUtil;
import blockrenderer6343.client.utils.ConstructableData;
import blockrenderer6343.client.world.DummyWorld;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class StructureHacks {

    private static final int MAX_TIERS_TO_CHECK = 50;
    private static final List<String> TIERED_ELEMENTS = new ArrayList<>();
    private static final String CHANNEL_ELEMENT;
    private static final MethodHandle LAZY_ELEMENT_GET;
    private static final String LAZY_ELEMENT = "com.gtnewhorizon.structurelib.structure.LazyStructureElement";
    public static final ItemStack HOLO_STACK = new ItemStack(StructureLibAPI.getDefaultHologramItem());
    private static Fields.ClassFields<IStructureElement<?>>.Field<String> channelGetter;

    public static void addTieredElement(String className) {
        TIERED_ELEMENTS.add(className);
    }

    static {
        // This is so dumb but all elements are anonymous classes so this is the best way to only check the necessary
        // ones
        addTieredElement(
                StructureUtility.ofBlocksTiered((a, b) -> 0, null, Collections.emptyList(), (c, d) -> {}, e -> -1)
                        .getClass().getName());
        IStructureElement<?> elem = StructureUtility.ofBlocksTiered((a, b) -> 0, null, (c, d) -> {}, e -> -1);
        addTieredElement(elem.getClass().getName());
        addTieredElement(CHANNEL_ELEMENT = StructureUtility.withChannel("blah", elem).getClass().getName());
        try {
            // Class is package-private so Class.forName() it is
            Method method = Class.forName(LAZY_ELEMENT).getDeclaredMethod("get", Object.class);
            method.setAccessible(true);
            LAZY_ELEMENT_GET = MethodHandles.lookup().unreflect(method);
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static @Nullable Iterable<ItemStack> getStacksForElement(IConstructable multi,
            IStructureElement<IConstructable> element) {
        String name = element.getClass().getName();
        if (name.equals(LAZY_ELEMENT)) {
            element = getUnderlyingElement(multi, element);
            name = element.getClass().getName();
        }

        AutoPlaceEnvironment env = BRUtil.getBuildEnvironment();
        IStructureElement.BlocksToPlace blocks = element
                .getBlocksToPlace(multi, DummyWorld.INSTANCE, 0, 0, 0, HOLO_STACK, env);
        if (TIERED_ELEMENTS.contains(name)) {
            return extractTieredBlocks(multi, element, env, getChannel(name, element));
        }

        if (blocks == null) return Collections.emptyList();

        return blocks.getStacks();
    }

    private static ObjectSet<ItemStack> extractTieredBlocks(IConstructable multi,
            IStructureElement<IConstructable> element, AutoPlaceEnvironment env, String channel) {
        ObjectSet<ItemStack> result = new ObjectOpenHashSet<>();

        int tier = 0;
        ConstructableData data = ConstructableData.getTierDataMap()
                .computeIfAbsent(multi, k -> new ConstructableData());
        do {
            HOLO_STACK.stackSize = tier++ + 1;
            IStructureElement.BlocksToPlace toPlace = element
                    .getBlocksToPlace(multi, DummyWorld.INSTANCE, 0, 0, 0, HOLO_STACK, env);
            if (toPlace == null || toPlace.getStacks() == null) break;
            Iterator<ItemStack> iterator = toPlace.getStacks().iterator();
            if (!iterator.hasNext()) break;
            ItemStack firstStack = iterator.next();
            // Some elements contained stacks that had a null getItem() so we need to check for that
            if (firstStack == null || firstStack.getItem() == null) break;

            if (!data.addItemTier(firstStack, channel, tier)) break;
            result.add(firstStack);

            while (iterator.hasNext()) {
                ItemStack stack = iterator.next();
                if (!data.addItemTier(stack, channel, tier)) break;
                result.add(stack);
            }
        } while (tier < MAX_TIERS_TO_CHECK);

        data.setMaxTier(tier - 1, channel);
        HOLO_STACK.stackSize = 1;
        return result;
    }

    private static String getChannel(String className, IStructureElement<?> element) {
        if (className.equals(CHANNEL_ELEMENT)) {
            if (channelGetter == null) {
                // noinspection unchecked
                channelGetter = (Fields.ClassFields<IStructureElement<?>>.Field<String>) Fields
                        .ofClass(element.getClass()).getField(Fields.LookupType.DECLARED, "val$channel", String.class);
            }
            return channelGetter.getValue(element);
        }

        return "";
    }

    private static IStructureElement<IConstructable> getUnderlyingElement(IConstructable multi,
            IStructureElement<?> element) {
        try {
            // noinspection unchecked
            return (IStructureElement<IConstructable>) LAZY_ELEMENT_GET.invokeWithArguments(element, multi);
        } catch (Throwable ignored) {
            // This should never happen
            throw new RuntimeException();
        }
    }
}
