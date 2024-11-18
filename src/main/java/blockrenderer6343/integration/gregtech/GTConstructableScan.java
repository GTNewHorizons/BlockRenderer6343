package blockrenderer6343.integration.gregtech;

import java.util.Collection;
import java.util.function.Consumer;

import net.minecraft.item.ItemStack;

import com.google.common.collect.Iterables;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import blockrenderer6343.client.utils.BRUtil;
import blockrenderer6343.client.utils.ConstructableData;
import blockrenderer6343.client.world.ObserverWorld;
import blockrenderer6343.integration.nei.StructureHacks;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class GTConstructableScan implements Runnable {

    private final Consumer<Long2ObjectMap<ObjectSet<IConstructable>>> resultCallback;
    private final ObjectSet<Pair<IConstructable, Collection<IStructureElement<IConstructable>[]>>> constructables;

    public GTConstructableScan(Consumer<Long2ObjectMap<ObjectSet<IConstructable>>> resultCallback,
            Collection<IConstructable> multiblocks) {
        this.resultCallback = resultCallback;
        // This should be safe to run in a thread as long as the structures are loaded on main thread
        this.constructables = getStructurePairs(multiblocks);

    }

    private static ObjectSet<Pair<IConstructable, Collection<IStructureElement<IConstructable>[]>>> getStructurePairs(
            Collection<IConstructable> constructables) {
        ObjectSet<Pair<IConstructable, Collection<IStructureElement<IConstructable>[]>>> result = new ObjectOpenHashSet<>();
        for (IConstructable multi : constructables) {
            // noinspection unchecked
            IStructureDefinition<IConstructable> structure = (IStructureDefinition<IConstructable>) multi
                    .getStructureDefinition();
            if (!(structure instanceof StructureDefinition)) continue;
            result.add(Pair.of(multi, ((StructureDefinition<IConstructable>) structure).getStructures().values()));
        }

        return result;
    }

    @Override
    public void run() {
        Long2ObjectMap<ObjectSet<IConstructable>> result = new Long2ObjectOpenHashMap<>();
        ObjectSet<IConstructable> secondScan = new ObjectOpenHashSet<>();
        constructables.forEach(pair -> {
            ObjectSet<IStructureElement<IConstructable>> checkedElements = new ObjectOpenHashSet<>();
            IConstructable multi = pair.left();
            for (IStructureElement<IConstructable>[] elementArray : pair.right()) {
                for (IStructureElement<IConstructable> element : elementArray) {
                    if (!checkedElements.add(element)) continue;
                    Iterable<ItemStack> stacks = StructureHacks.getStacksForElement(multi, element);
                    if (stacks == null || Iterables.isEmpty(stacks)) continue;

                    for (ItemStack stack : stacks) {
                        if (!StructureHacks.isSafeStack(stack)) continue;
                        result.computeIfAbsent(BRUtil.hashStack(stack), k -> new ObjectOpenHashSet<>()).add(multi);
                    }
                }
            }

            if (pair.right().size() > 1) {
                ConstructableData data = ConstructableData.getTierData(multi);
                if (data.getMaxTotalTier() == 1) secondScan.add(multi);
            }
        });

        ObserverWorld world = new ObserverWorld();
        for (IConstructable multi : secondScan) {
            int tier = world.estimateTierFromConstructable(
                    stack -> result.computeIfAbsent(BRUtil.hashStack(stack), k -> new ObjectOpenHashSet<>()).add(multi),
                    multi);
            if (tier > 1) {
                ConstructableData data = ConstructableData.getTierDataMap()
                        .computeIfAbsent(multi, k -> new ConstructableData());
                data.setMaxTier(tier, "");
            }
        }

        resultCallback.accept(result);
    }
}
