package blockrenderer6343.integration.structurelib;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.IMultiblockInfoContainer;

import blockrenderer6343.client.utils.ConstructableData;
import blockrenderer6343.client.world.ObserverWorld;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class MultiblockInfoContainerScan implements Runnable {

    private final Consumer<Long2ObjectMap<ObjectSet<IConstructable>>> resultCallback;
    private final Consumer<Object2ObjectMap<IConstructable, ItemStack>> stackCallback;
    private final Map<String, IMultiblockInfoContainer<TileEntity>> infoContainers;

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
        Long2ObjectMap<ObjectSet<IConstructable>> result = new Long2ObjectOpenHashMap<>();
        Object2ObjectMap<IConstructable, ItemStack> stacks = new Object2ObjectOpenHashMap<>();
        ObserverWorld world = new ObserverWorld();
        for (Map.Entry<String, IMultiblockInfoContainer<TileEntity>> entry : infoContainers.entrySet()) {
            IConstructable constructable = world.getConstructableFromContainer(entry.getKey(), entry.getValue());
            int tier = world.estimateTierFromInfoContainer(result, stacks, constructable);
            if (tier > 1) {
                ConstructableData data = ConstructableData.getTierDataMap()
                        .computeIfAbsent(constructable, k -> new ConstructableData());
                data.setMaxTier(tier, "");
            }
        }

        stackCallback.accept(stacks);
        resultCallback.accept(result);
    }
}
