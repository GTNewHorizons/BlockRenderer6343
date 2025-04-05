package blockrenderer6343.client.utils;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;

import blockrenderer6343.integration.nei.StructureHacks;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class ConstructableData {

    private static final Object2ObjectMap<IConstructable, ConstructableData> constructableData = new Object2ObjectOpenHashMap<>();
    private static final ConstructableData EMPTY = new ConstructableData();

    private final Object2IntMap<String> channelMaxTierMap = new Object2IntOpenHashMap<>();
    private final Long2IntMap itemTiers = new Long2IntOpenHashMap();
    private final Long2ObjectMap<String> itemChannels = new Long2ObjectOpenHashMap<>();
    private String currentChannel = "";
    private int maxTotalTier = 1;
    private int currentTier;
    private boolean hasData = false;

    public static @NotNull ConstructableData getTierData(IConstructable constructable) {
        return constructableData.getOrDefault(constructable, EMPTY);
    }

    public static void addConstructableData(Object2ObjectMap<IConstructable, ConstructableData> data) {
        synchronized (constructableData) {
            constructableData.putAll(data);
        }
    }

    public boolean addItemTier(@NotNull ItemStack item, @NotNull String channel, int tier) {
        return addItemTier(item, null, channel, tier);
    }

    public boolean addItemTier(@NotNull ItemStack item, ItemStack lastItem, @NotNull String channel, int tier) {
        if (this == EMPTY || !StructureHacks.isSafeStack(item)) return false;
        hasData = true;
        long hash = BRUtil.hashStack(item);
        if (lastItem != null) {
            long lastHash = BRUtil.hashStack(lastItem);
            if (lastHash == hash) return false;
        }
        if (!channel.isEmpty()) {
            itemChannels.put(hash, channel);
        }
        itemTiers.put(hash, tier);
        return true;
    }

    public void setMaxTier(int tier, @NotNull String channel) {
        if (this == EMPTY) return;
        hasData = true;
        maxTotalTier = Math.max(maxTotalTier, tier);
        if (!channel.isEmpty() && channelMaxTierMap.getInt(channel) < tier) {
            channelMaxTierMap.put(channel, tier);
        }
    }

    public boolean hasData() {
        return hasData;
    }

    public ConstructableData setTierFromStack(ItemStack stack) {
        long hash = BRUtil.hashStack(stack);
        currentTier = itemTiers.getOrDefault(hash, 1);
        currentChannel = itemChannels.getOrDefault(hash, "");
        return this;
    }

    public int getMaxTotalTier() {
        return maxTotalTier;
    }

    public int getCurrentTier() {
        return currentTier;
    }

    public @NotNull String getCurrentChannel() {
        return currentChannel;
    }

    public Object2IntMap<String> getChannelData() {
        return channelMaxTierMap;
    }
}
