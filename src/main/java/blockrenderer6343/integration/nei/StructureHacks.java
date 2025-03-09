package blockrenderer6343.integration.nei;

import static blockrenderer6343.client.utils.BRUtil.AUTO_PLACE_ENVIRONMENT;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.IStructureElementChain;
import com.gtnewhorizon.structurelib.structure.StructureUtility;

import blockrenderer6343.client.utils.ConstructableData;
import blockrenderer6343.client.world.DummyWorld;
import cpw.mods.fml.relauncher.ReflectionHelper;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

@SuppressWarnings({ "unchecked" })
public class StructureHacks {

    private static final int MAX_TIERS_TO_CHECK = 50;
    private static final List<String> TIERED_ELEMENTS = new ArrayList<>();
    private static final String CHANNEL_ELEMENT;
    public static final String LAZY_ELEMENT = "com.gtnewhorizon.structurelib.structure.LazyStructureElement";
    private static final MethodHandle CHAIN_GETTER, CHANNEL_GETTER, LAZY_ELEMENT_GETTER;
    public static final ItemStack HOLO_STACK = new ItemStack(StructureLibAPI.getDefaultHologramItem());
    public static final Collection<String> SKIP_ELEMENTS = getClassNames(
            StructureUtility.isAir(),
            StructureUtility.notAir(),
            StructureUtility.error());

    static {
        // This is so dumb but all elements are anonymous classes so we have to get the classes through "creative" means
        addTieredElement(
                StructureUtility.ofBlocksTiered((a, b) -> 0, null, Collections.emptyList(), (c, d) -> {}, e -> -1)
                        .getClass().getName());
        IStructureElement<?> elem = StructureUtility.ofBlocksTiered((a, b) -> 0, null, (c, d) -> {}, e -> -1);
        addTieredElement(elem.getClass().getName());
        IStructureElement<?> channelElem = StructureUtility.withChannel("blah", elem);
        addTieredElement(CHANNEL_ELEMENT = channelElem.getClass().getName());
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            LAZY_ELEMENT_GETTER = lookup.unreflect(
                    ReflectionHelper
                            .findMethod(Class.forName(LAZY_ELEMENT), null, new String[] { "get" }, Object.class));
            CHAIN_GETTER = lookup.findVirtual(
                    IStructureElementChain.class,
                    "fallbacks",
                    MethodType.methodType(IStructureElement[].class));
            CHANNEL_GETTER = lookup.unreflectGetter(ReflectionHelper.findField(channelElem.getClass(), "val$channel"));
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Add a tiered element that should be checked in
     * {@link #extractTieredBlocks(Object, IStructureElement, ConstructableData, String)}
     *
     * @param className The class name of the element
     */
    public static void addTieredElement(String className) {
        TIERED_ELEMENTS.add(className);
    }

    public static <T> @Nullable Iterable<ItemStack> getStacksForElement(T multi, IStructureElement<T> element,
            ConstructableData data) {
        String name = element.getClass().getName();
        if (name.equals(LAZY_ELEMENT)) {
            element = getUnderlyingElement(multi, element);
            if (element == null) return Collections.emptyList();
            name = element.getClass().getName();
        }

        if (SKIP_ELEMENTS.contains(name)) return Collections.emptyList();

        if (element instanceof IStructureElementChain) {
            IStructureElement<T>[] elements = unwrapChainElement(element);
            if (elements == null) return Collections.emptyList();
            ObjectSet<ItemStack> chainStacks = new ObjectOpenHashSet<>();
            for (IStructureElement<T> e : elements) {
                Iterable<ItemStack> stacks = getStacksForElement(multi, e, data);
                if (stacks != null) {
                    stacks.forEach(chainStacks::add);
                }
            }
            return chainStacks;
        }

        IStructureElement.BlocksToPlace blocks = element
                .getBlocksToPlace(multi, DummyWorld.INSTANCE, 0, 0, 0, HOLO_STACK, AUTO_PLACE_ENVIRONMENT);
        if (TIERED_ELEMENTS.contains(name)) {
            return extractTieredBlocks(multi, element, data, getChannel(name, element));
        }

        if (blocks == null) return Collections.emptyList();

        return blocks.getStacks();
    }

    private static <T> ObjectSet<ItemStack> extractTieredBlocks(T multi, IStructureElement<T> element,
            ConstructableData data, String channel) {
        ObjectSet<ItemStack> result = new ObjectOpenHashSet<>();
        ItemStack holo = HOLO_STACK.copy();
        int tier = 0;
        boolean doBreak = false;

        do {
            holo.stackSize = tier++ + 1;
            IStructureElement.BlocksToPlace toPlace = element
                    .getBlocksToPlace(multi, DummyWorld.INSTANCE, 0, 0, 0, holo, AUTO_PLACE_ENVIRONMENT);
            if (toPlace == null || toPlace.getStacks() == null) break;
            Iterator<ItemStack> iterator = toPlace.getStacks().iterator();
            if (!iterator.hasNext()) break;
            ItemStack firstStack = iterator.next();

            if (!data.addItemTier(firstStack, channel, tier)) doBreak = true;
            result.add(firstStack);

            while (iterator.hasNext()) {
                ItemStack stack = iterator.next();
                data.addItemTier(stack, channel, tier);
                result.add(stack);
            }
        } while (!doBreak && tier < MAX_TIERS_TO_CHECK);

        data.setMaxTier(tier - 1, channel);
        return result;
    }

    private static String getChannel(String className, IStructureElement<?> element) {
        if (className.equals(CHANNEL_ELEMENT)) {
            try {
                return (String) CHANNEL_GETTER.invokeWithArguments(element);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        return "";
    }

    public static <T> boolean anyElementMatches(@NotNull Collection<String> elementsToFind, @NotNull T multi,
            @Nullable IStructureElement<T> element) {
        if (element == null) return false;
        String name = element.getClass().getName();

        if (elementsToFind.contains(name)) {
            return true;
        }

        for (String ele : elementsToFind) {
            IStructureElement<?> result = getFirstMatchingElement(ele, multi, element);
            if (result != null) {
                return true;
            }
        }

        return false;
    }

    public static <T> @Nullable IStructureElement<T> getFirstMatchingElement(String elementToFind, T multi,
            IStructureElement<T> element) {
        if (element == null) return null;
        String name = element.getClass().getName();
        if (elementToFind.equals(name)) {
            return element;
        }

        if (LAZY_ELEMENT.equals(name)) {
            return getFirstMatchingElement(elementToFind, multi, getUnderlyingElement(multi, element));
        }

        if (element instanceof IStructureElementChain) {
            IStructureElement<T>[] elements = StructureHacks.unwrapChainElement(element);
            if (elements == null) return null;
            for (IStructureElement<T> e : elements) {
                IStructureElement<T> result = getFirstMatchingElement(elementToFind, multi, e);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    public static <T> @Nullable IStructureElement<T>[] unwrapChainElement(IStructureElement<T> element) {
        if (!(element instanceof IStructureElementChain)) return null;

        try {
            return (IStructureElement<T>[]) CHAIN_GETTER.invokeWithArguments(element);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> IStructureElement<T> getUnderlyingElement(T multi, IStructureElement<?> element) {
        if (!LAZY_ELEMENT.equals(element.getClass().getName())) return (IStructureElement<T>) element;
        try {
            return (IStructureElement<T>) LAZY_ELEMENT_GETTER.invokeWithArguments(element, multi);
        } catch (Throwable ignored) {
            // This should never happen
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isSafeStack(ItemStack stack) {
        return stack != null && stack.getItem() != null;
    }

    private static ObjectSet<String> getClassNames(Object... elements) {
        ObjectSet<String> classNames = new ObjectOpenHashSet<>();
        for (Object elem : elements) {
            classNames.add(elem.getClass().getName());
        }
        return classNames;
    }
}
