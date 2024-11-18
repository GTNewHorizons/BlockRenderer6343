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
    private IConstructable currentConstructable;

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
        if (!StructureLibAPI.isInstrumentEnabled()) {
            StructureLibAPI.enableInstrument(IDENTIFIER);
        }

        Object2ObjectMap<IConstructable, ItemStack> stacks = new Object2ObjectOpenHashMap<>();
        MinecraftForge.EVENT_BUS.register(this);
        ObserverWorld world = new ObserverWorld();
        for (Map.Entry<String, IMultiblockInfoContainer<TileEntity>> entry : infoContainers.entrySet()) {
            currentConstructable = world.getConstructableFromContainer(entry.getKey(), entry.getValue());
            int tier = world.estimateTierFromInfoContainer(result, stacks, currentConstructable);
            if (tier > 1) {
                ConstructableData data = ConstructableData.getTierDataMap()
                        .computeIfAbsent(currentConstructable, k -> new ConstructableData());
                data.setMaxTier(tier, "");
            }
        }

        stackCallback.accept(stacks);
        resultCallback.accept(result);

        MinecraftForge.EVENT_BUS.unregister(this);

        if (StructureLibAPI.isInstrumentEnabled()) {
            StructureLibAPI.disableInstrument();
        }
    }

    @SubscribeEvent
    @SuppressWarnings({ "unused", "unchecked" })
    public void OnStructureEvent(StructureEvent.StructureElementVisitedEvent event) {
        // TODO use instrument identifier when fix is merged
        // There is no way to get the structure definition of an IMultiblockInfoContainer so this is a hacky workaround
        // for that
        if (!StructureLibAPI.isInstrumentEnabled() || currentConstructable == null
                || !checkedElements.add(event.getElement())) {
            return;
        }

        Iterable<ItemStack> stacks = StructureHacks
                .getStacksForElement(currentConstructable, (IStructureElement<IConstructable>) event.getElement());
        if (stacks == null || Iterables.isEmpty(stacks)) return;
        for (ItemStack stack : stacks) {
            if (!StructureHacks.isSafeStack(stack)) continue;
            result.computeIfAbsent(BRUtil.hashStack(stack), k -> new ObjectOpenHashSet<>()).add(currentConstructable);
        }
    }
}
