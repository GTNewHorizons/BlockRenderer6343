package blockrenderer6343.api.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.structure.IItemSource;

import codechicken.nei.ItemList;

public class CreativeItemSource implements IItemSource {

    public static final CreativeItemSource instance;

    static {
        instance = new CreativeItemSource();
    }

    @NotNull
    @Override
    public Map<ItemStack, Integer> take(Predicate<ItemStack> predicate, boolean b, int i) {
        Map<ItemStack, Integer> store = new HashMap<>();
        if (!ItemList.loadFinished) return store;

        for (ItemStack itemStack : ItemList.items) {
            if (predicate.test(itemStack)) {
                store.put(itemStack, Integer.MAX_VALUE);
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
