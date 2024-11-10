package blockrenderer6343.client.utils;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class ConstructableData {

    private static final Object2ObjectMap<IConstructable, ConstructableData> tierDataMap = new Object2ObjectOpenHashMap<>();
    private static final ConstructableData EMPTY = new ConstructableData();

    private final Object2IntMap<String> channelMaxTierMap = new Object2IntOpenHashMap<>();
    private final Long2IntMap itemTiers = new Long2IntOpenHashMap();
    private final Long2ObjectMap<String> itemChannels = new Long2ObjectOpenHashMap<>();
    private String currentChannel = "";
    private int maxTotalTier = 1;
    private int currentTier;

    public boolean addItemTier(@NotNull ItemStack item, @NotNull String channel, int tier) {
        long hash = BRUtil.hashStack(item);
        if (!channel.isEmpty()) {
            itemChannels.put(hash, channel);
        }
        return itemTiers.putIfAbsent(hash, tier) == itemTiers.defaultReturnValue();
    }

    public void setMaxTier(int tier, @NotNull String channel) {
        maxTotalTier = Math.max(maxTotalTier, tier);
        if (!channel.isEmpty()) {
            channelMaxTierMap.put(channel, tier);
        }
    }

    public ConstructableData setTierFromStack(ItemStack stack) {
        long hash = BRUtil.hashStack(stack);
        currentTier = itemTiers.getOrDefault(hash, 0);
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

    public static @NotNull ConstructableData getTierData(IConstructable constructable) {
        return tierDataMap.getOrDefault(constructable, EMPTY);
    }

    public static Object2ObjectMap<IConstructable, ConstructableData> getTierDataMap() {
        return tierDataMap;
    }

}
