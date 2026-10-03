package blockrenderer6343.integration.gregtech;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import net.minecraft.item.ItemStack;

import com.google.common.collect.Iterables;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.structure.IChannelDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import blockrenderer6343.BlockRenderer6343;
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
            try {
                // noinspection unchecked
                IStructureDefinition<IConstructable> structure = (IStructureDefinition<IConstructable>) multi
                        .getStructureDefinition();
                if (structure instanceof StructureDefinition) {
                    result.add(Pair.of(multi, ((StructureDefinition<IConstructable>) structure).getStructures().values()));
                } else {
                    result.add(Pair.of(multi, Collections.emptySet()));
                }
            } catch (Exception e) {
                BlockRenderer6343.LOG.error("Failed to load multiblock structure {}", multi, e);
            }
        }

        return result;
    }

    @Override
    public void run() {
        Long2ObjectMap<ObjectSet<IConstructable>> result = new Long2ObjectOpenHashMap<>();
        ObjectSet<IConstructable> secondScan = new ObjectOpenHashSet<>();
        Object2ObjectMap<IConstructable, ConstructableData> secondScanData = new Object2ObjectOpenHashMap<>();
        Object2ObjectMap<IConstructable, ConstructableData> pendingTierEstimates = new Object2ObjectOpenHashMap<>();
        Object2ObjectMap<IConstructable, ConstructableData> constructableData = new Object2ObjectOpenHashMap<>();
        ObserverWorld world = new ObserverWorld();

        for (Pair<IConstructable, Collection<IStructureElement<IConstructable>[]>> pair : constructables) {
            IConstructable multi = pair.left();
            try {
                Collection<IStructureElement<IConstructable>[]> structures = pair.right();
                ConstructableData data = new ConstructableData();
                addChannelDefinitions(multi, data);
                ObjectSet<IStructureElement<IConstructable>> checkedElements = new ObjectOpenHashSet<>();

                if (structures.isEmpty()) {
                    secondScan.add(multi);
                    secondScanData.put(multi, data);
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
                    if (data.hasTierData()) pendingTierEstimates.put(multi, data);
                } else if (structures.size() > 1) {
                    secondScan.add(multi);
                    secondScanData.put(multi, data);
                }
            } catch (Exception e) {
                BlockRenderer6343.LOG.error("Failed to scan multiblock {}", multi, e);
                result.values().forEach(multiblocks -> multiblocks.remove(multi));
            }
        }

        ConstructableData.addConstructableData(constructableData);

        for (Object2ObjectMap.Entry<IConstructable, ConstructableData> entry : pendingTierEstimates
                .object2ObjectEntrySet()) {
            try {
                int estimatedTier = world.estimateTierFromConstructable(e -> {}, entry.getKey());
                if (estimatedTier > 1) entry.getValue().setMaxTier(estimatedTier, "");
            } catch (Exception e) {
                BlockRenderer6343.LOG.error("Failed to estimate multiblock tier {}", entry.getKey(), e);
            }
        }

        for (IConstructable multi : secondScan) {
            ConstructableData data = secondScanData.get(multi);
            if (data == null) data = new ConstructableData();
            try {
                int tier = world.estimateTierFromConstructable(
                        stack -> result.computeIfAbsent(BRUtil.hashStack(stack), k -> new ObjectOpenHashSet<>()).add(multi),
                        multi);
                if (tier > 1) data.setMaxTier(tier, "");
                if (data.hasData()) ConstructableData.addConstructableData(multi, data);
            } catch (Exception e) {
                BlockRenderer6343.LOG.error("Failed to estimate multiblock tier {}", multi, e);
                result.values().forEach(multiblocks -> multiblocks.remove(multi));
            }
        }

        resultCallback.accept(result);
    }

    private static void addChannelDefinitions(IConstructable multi, ConstructableData data) {
        IStructureDefinition<?> structure = multi.getStructureDefinition();
        if (structure == null) return;
        for (IChannelDefinition definition : structure.getExplicitChannelDefinitions()) {
            data.addChannelDefinition(definition);
        }
    }
}
