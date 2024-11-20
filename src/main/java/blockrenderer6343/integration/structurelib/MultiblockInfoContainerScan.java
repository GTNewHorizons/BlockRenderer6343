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
        if (!StructureLibAPI.isInstrumentEnabled()) {
            StructureLibAPI.enableInstrument(IDENTIFIER);
        }

        Object2ObjectMap<IConstructable, ItemStack> stacks = new Object2ObjectOpenHashMap<>();
        Object2ObjectMap<IConstructable, ConstructableData> constructableData = new Object2ObjectOpenHashMap<>();

        for (Map.Entry<String, IMultiblockInfoContainer<TileEntity>> entry : infoContainers.entrySet()) {
            currentConstructable = world.getConstructableFromContainer(entry.getKey(), entry.getValue());
            int tier = world.estimateTierFromInfoContainer(result, stacks, currentConstructable);
            if (tier > 1) {
                currentData.setMaxTier(tier, "");
            }

            if (currentData.hasData()) {
                constructableData.put(currentConstructable, currentData);
                currentData = new ConstructableData();
            }
        }

        ConstructableData.addConstructableData(constructableData);

        stackCallback.accept(stacks);
        resultCallback.accept(result);

        if (StructureLibAPI.isInstrumentEnabled()) {
            StructureLibAPI.disableInstrument();
        }

        MinecraftForge.EVENT_BUS.unregister(this);
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
