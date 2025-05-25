package blockrenderer6343.integration.gregtech;

import java.util.Collection;
import java.util.Collections;
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
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
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
            if (structure instanceof StructureDefinition) {
                result.add(Pair.of(multi, ((StructureDefinition<IConstructable>) structure).getStructures().values()));
            } else {
                result.add(Pair.of(multi, Collections.emptySet()));
            }
        }

        return result;
    }

    @Override
    public void run() {
        Long2ObjectMap<ObjectSet<IConstructable>> result = new Long2ObjectOpenHashMap<>();
        ObjectSet<IConstructable> secondScan = new ObjectOpenHashSet<>();
        Object2ObjectMap<IConstructable, ConstructableData> constructableData = new Object2ObjectOpenHashMap<>();
        ConstructableData data = new ConstructableData();

        for (Pair<IConstructable, Collection<IStructureElement<IConstructable>[]>> pair : constructables) {
            IConstructable multi = pair.left();
            Collection<IStructureElement<IConstructable>[]> structures = pair.right();
            ObjectSet<IStructureElement<IConstructable>> checkedElements = new ObjectOpenHashSet<>();

            if (structures.isEmpty()) {
                secondScan.add(multi);
                continue;
            }

            for (IStructureElement<IConstructable>[] elementArray : structures) {
                for (IStructureElement<IConstructable> element : elementArray) {
                    if (!checkedElements.add(element)) continue;
                    Iterable<ItemStack> stacks = StructureHacks.getStacksForElement(multi, element, data);
                    if (stacks == null || Iterables.isEmpty(stacks)) continue;

                    for (ItemStack stack : stacks) {
                        if (!StructureHacks.isSafeStack(stack)) continue;
                        result.computeIfAbsent(BRUtil.hashStack(stack), k -> new ObjectOpenHashSet<>()).add(multi);
                    }
                }
            }

            if (data.hasData()) {
                constructableData.put(multi, data);
                data = new ConstructableData();
            } else if (structures.size() > 1) {
                secondScan.add(multi);
            }
        }

        ObserverWorld world = new ObserverWorld();
        for (IConstructable multi : secondScan) {
            int tier = world.estimateTierFromConstructable(
                    stack -> result.computeIfAbsent(BRUtil.hashStack(stack), k -> new ObjectOpenHashSet<>()).add(multi),
                    multi);
            if (tier > 1) {
                data.setMaxTier(tier, "");
                constructableData.put(multi, data);
                data = new ConstructableData();
            }
        }

        ConstructableData.addConstructableData(constructableData);
        resultCallback.accept(result);
    }
}
