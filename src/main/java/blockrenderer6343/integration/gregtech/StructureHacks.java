package blockrenderer6343.integration.gregtech;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureUtility;

import blockrenderer6343.client.world.DummyWorld;
import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.multitileentity.multiblock.casing.Glasses;
import gregtech.api.util.GTStructureUtility;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class StructureHacks {

    private static final int MAX_TIERS_TO_CHECK = 50;
    private static final List<String> TIERED_ELEMENTS;
    private static final MethodHandle LAZY_ELEMENT_GET;
    private static final String LAZY_ELEMENT = "com.gtnewhorizon.structurelib.structure.LazyStructureElement";
    private static final ItemStack HOLO_STACK = new ItemStack(StructureLibAPI.getDefaultHologramItem());

    static List<Item> validGlass;

    static {
        // This is so dumb but all elements are anonymous classes so this is the best way to only check the necessary
        // ones
        String coil = GTStructureUtility.ofCoil((a, c) -> true, b -> HeatingCoilLevel.LV).getClass().getName();
        String tiered = StructureUtility
                .ofBlocksTiered((a, b) -> 0, null, Collections.emptyList(), (c, d) -> {}, e -> -1).getClass().getName();
        IStructureElement<?> elem = StructureUtility.ofBlocksTiered((a, b) -> 0, null, (c, d) -> {}, e -> -1);
        String tieredCheck = elem.getClass().getName();
        String channel = StructureUtility.withChannel("blah", elem).getClass().getName();
        TIERED_ELEMENTS = ImmutableList.of(coil, tiered, tieredCheck, channel);
        try {
            // Class is package-private so Class.forName() it is
            Method method = Class.forName(LAZY_ELEMENT).getDeclaredMethod("get", Object.class);
            method.setAccessible(true);
            LAZY_ELEMENT_GET = MethodHandles.lookup().unreflect(method);
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    static @Nullable Iterable<ItemStack> getStacksForElement(IConstructable multi,
            IStructureElement<IConstructable> element, AutoPlaceEnvironment env) {
        if (validGlass == null) validGlass = getGlasses(env);

        String name = element.getClass().getName();
        if (name.equals(LAZY_ELEMENT)) {
            element = getUnderlyingElement(multi, element);
            name = element.getClass().getName();
        }

        IStructureElement.BlocksToPlace blocks = element
                .getBlocksToPlace(multi, DummyWorld.INSTANCE, 0, 0, 0, HOLO_STACK, env);
        if (blocks == null) return Collections.emptyList();
        if (TIERED_ELEMENTS.contains(name)) {
            return extractTieredBlocks(multi, element, env);
        }

        return blocks.getStacks();
    }

    private static ObjectSet<ItemStack> extractTieredBlocks(IConstructable multi,
            IStructureElement<IConstructable> element, AutoPlaceEnvironment env) {
        ObjectSet<ItemStack> result = new ObjectOpenHashSet<>();

        int tier = 0;
        LongSet addedItemHashes = new LongOpenHashSet();
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

            if (!addedItemHashes.add(hashStack(firstStack))) {
                break;
            }

            addWithSize(result, firstStack, tier);
            while (iterator.hasNext()) {
                ItemStack stack = iterator.next();
                if (!addedItemHashes.add(hashStack(stack))) continue;
                addWithSize(result, stack, tier);
            }
        } while (tier < MAX_TIERS_TO_CHECK);

        HOLO_STACK.stackSize = 1;
        return result;
    }

    private static void addWithSize(ObjectSet<ItemStack> set, ItemStack stack, int size) {
        if (!GTNEIMultiblockHandler.isValidItem(stack.getItem())) return;
        ItemStack copy = stack.copy();
        copy.stackSize = size;
        set.add(copy);
    }

    private static long hashStack(ItemStack stack) {
        // noinspection ConstantConditions
        return stack.getItem().hashCode() * 31L + stack.getItemDamage() * 31L;
    }

    private static List<Item> getGlasses(AutoPlaceEnvironment env) {
        List<Item> result = new ArrayList<>();
        // noinspection ConstantConditions
        Iterable<ItemStack> stacks = Glasses.chainAllGlasses()
                .getBlocksToPlace(null, DummyWorld.INSTANCE, 0, 0, 0, HOLO_STACK, env).getStacks();
        if (stacks == null) return result;
        stacks.forEach(stack -> result.add(stack.getItem()));
        return result;
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
