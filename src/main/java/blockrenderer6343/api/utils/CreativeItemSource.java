package blockrenderer6343.api.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.structure.IItemSource;

import codechicken.nei.ItemList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceLinkedOpenHashMap;

public class CreativeItemSource implements IItemSource {

    public static final CreativeItemSource instance = new CreativeItemSource();

    private Reference2ReferenceLinkedOpenHashMap<ItemStack, ItemStack> itemList;

    @NotNull
    @Override
    public Map<ItemStack, Integer> take(Predicate<ItemStack> predicate, boolean b, int i) {
        Map<ItemStack, Integer> store = new HashMap<>();
        if (!ItemList.loadFinished) return store;

        if (itemList == null) {
            itemList = new Reference2ReferenceLinkedOpenHashMap<>();

            for (ItemStack stack : ItemList.items) {
                itemList.put(stack, stack);
            }
        }

        for (var p : itemList.reference2ReferenceEntrySet()) {
            ItemStack itemStack = p.getValue();

            if (predicate.test(itemStack)) {
                store.put(itemStack, Integer.MAX_VALUE);

                itemList.putAndMoveToFirst(itemStack, itemStack);

                return store;
            }
        }

        return store;
    }

    public Map<ItemStack, Integer> takeEverythingMatches(Predicate<ItemStack> predicate, boolean b, int i) {
        Map<ItemStack, Integer> store = new HashMap<>();
        if (!ItemList.loadFinished) return store;

        for (ItemStack itemStack : ItemList.items) {
            if (predicate.test(itemStack)) {
                store.put(itemStack, Integer.MAX_VALUE);
            }
        }

        return store;
    }

    @Override
    public boolean takeOne(ItemStack stack, boolean simulate) {
        return true;
    }

    @Override
    public boolean takeAll(ItemStack stack, boolean simulate) {
        return true;
    }
}
