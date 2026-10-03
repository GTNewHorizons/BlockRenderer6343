package blockrenderer6343.integration.structurelib;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;

import com.google.common.collect.Iterables;
import com.gtnewhorizon.structurelib.StructureEvent;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.IMultiblockInfoContainer;
import com.gtnewhorizon.structurelib.structure.IStructureElement;

import blockrenderer6343.BlockRenderer6343;
import blockrenderer6343.client.utils.BRUtil;
import blockrenderer6343.client.utils.ConstructableData;
import blockrenderer6343.client.world.ObserverWorld;
import blockrenderer6343.integration.nei.StructureHacks;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class MultiblockInfoContainerScan implements Runnable {

    private static final String IDENTIFIER = "IMultiBlockInfoContainerScan";
    private final Consumer<Long2ObjectMap<ObjectSet<IConstructable>>> resultCallback;
    private final Consumer<Object2ObjectMap<IConstructable, ItemStack>> stackCallback;
    private final Map<String, IMultiblockInfoContainer<TileEntity>> infoContainers;
    private final Long2ObjectMap<ObjectSet<IConstructable>> result = new Long2ObjectOpenHashMap<>();
    private final ObjectSet<IStructureElement<?>> checkedElements = new ObjectOpenHashSet<>();
    private final ObserverWorld world = new ObserverWorld();
    private IConstructable currentConstructable;
    private ConstructableData currentData = new ConstructableData();

    public MultiblockInfoContainerScan(Consumer<Long2ObjectMap<ObjectSet<IConstructable>>> resultCallback,
            Consumer<Object2ObjectMap<IConstructable, ItemStack>> stackCallback,
            Map<String, IMultiblockInfoContainer<?>> infoContainers) {
        this.resultCallback = resultCallback;
        this.stackCallback = stackCallback;
        // noinspection unchecked
        this.infoContainers = infoContainers.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (IMultiblockInfoContainer<TileEntity>) e.getValue()));

    }

    @Override
    public void run() {
        MinecraftForge.EVENT_BUS.register(this);
        boolean instrumentEnabled = false;
        try {
            StructureLibAPI.enableInstrument(IDENTIFIER);
            instrumentEnabled = true;
            scan();
        } finally {
            if (instrumentEnabled) StructureLibAPI.disableInstrument();
            MinecraftForge.EVENT_BUS.unregister(this);
        }
    }

    private void scan() {
        Object2ObjectMap<IConstructable, ItemStack> stacks = new Object2ObjectOpenHashMap<>();

        for (Map.Entry<String, IMultiblockInfoContainer<TileEntity>> entry : infoContainers.entrySet()) {
            currentData = new ConstructableData();
            checkedElements.clear();
            currentConstructable = null;
            try {
                currentConstructable = world.getConstructableFromContainer(entry.getKey(), entry.getValue());
                if (currentConstructable == null) {
                    world.reset();
                    continue;
                }
                int tier = world.estimateTierFromInfoContainer(result, stacks, currentConstructable);
                if (tier > 1) currentData.setMaxTier(tier, "");
                if (currentData.hasData()) {
                    ConstructableData.addConstructableData(currentConstructable, currentData);
                }
            } catch (Exception e) {
                BlockRenderer6343.LOG.error("Failed to scan multiblock info container {}", entry.getKey(), e);
                if (currentConstructable != null) {
                    result.values().forEach(multiblocks -> multiblocks.remove(currentConstructable));
                    stacks.remove(currentConstructable);
                }
                world.reset();
            }
        }

        stackCallback.accept(stacks);
        resultCallback.accept(result);
    }

    @SubscribeEvent
    @SuppressWarnings({ "unused", "unchecked" })
    public void OnStructureEvent(StructureEvent.StructureElementVisitedEvent event) {
        // There is no way to get the structure definition of an IMultiblockInfoContainer so this is a hacky workaround
        // for that
        if (!IDENTIFIER.equals(event.getInstrumentIdentifier()) || !checkedElements.add(event.getElement())) {
            return;
        }

        TileEntity tile = world.getTileEntity(0, 64, 0);
        Iterable<ItemStack> stacks = StructureHacks
                .getStacksForElement(tile, (IStructureElement<Object>) event.getElement(), currentData);
        if (stacks == null || Iterables.isEmpty(stacks)) return;
        for (ItemStack stack : stacks) {
            if (!StructureHacks.isSafeStack(stack)) continue;
            result.computeIfAbsent(BRUtil.hashStack(stack), k -> new ObjectOpenHashSet<>()).add(currentConstructable);
        }
    }
}
