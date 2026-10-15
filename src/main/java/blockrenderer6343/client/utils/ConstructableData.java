package blockrenderer6343.client.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.structure.IChannelDefinition;

import blockrenderer6343.BlockRenderer6343;
import blockrenderer6343.integration.nei.StructureHacks;
import gregtech.api.util.GlassTier;
import gregtech.common.misc.GTStructureChannels;
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
    private final Object2ObjectMap<String, IChannelDefinition> channelDefinitions = new Object2ObjectOpenHashMap<>();
    private final Long2IntMap itemTiers = new Long2IntOpenHashMap();
    private final Long2ObjectMap<Collection<String>> itemChannels = new Long2ObjectOpenHashMap<>();
    private Collection<String> currentChannels = Collections.emptySet();
    private volatile int maxTotalTier = 1;
    private int currentTier = 1;
    private boolean hasData = false;
    private volatile boolean hasTierData = false;

    public static @NotNull ConstructableData getTierData(IConstructable constructable) {
        synchronized (constructableData) {
            return constructableData.getOrDefault(constructable, EMPTY);
        }
    }

    public static void addConstructableData(Object2ObjectMap<IConstructable, ConstructableData> data) {
        synchronized (constructableData) {
            constructableData.putAll(data);
        }
    }

    public static void addConstructableData(IConstructable constructable, ConstructableData data) {
        synchronized (constructableData) {
            constructableData.put(constructable, data);
        }
    }

    public boolean addItemTier(@NotNull ItemStack item, @NotNull Collection<String> channels, int tier) {
        return addItemTier(item, null, channels, tier);
    }

    public boolean addItemTier(@NotNull ItemStack item, ItemStack lastItem, @NotNull Collection<String> channels,
            int tier) {
        if (this == EMPTY || !StructureHacks.isSafeStack(item)) return false;
        hasData = true;
        hasTierData = true;
        long hash = BRUtil.hashStack(item);
        if (lastItem != null) {
            long lastHash = BRUtil.hashStack(lastItem);
            if (lastHash == hash) return false;
        }
        if (!channels.isEmpty()) {
            Collection<String> existingChannels = itemChannels.get(hash);
            if (existingChannels == null) {
                itemChannels.put(hash, immutableChannels(channels));
            } else {
                Set<String> mergedChannels = new LinkedHashSet<>(existingChannels);
                mergedChannels.addAll(channels);
                itemChannels.put(hash, Collections.unmodifiableSet(mergedChannels));
            }
        }
        itemTiers.put(hash, tier);
        return true;
    }

    public void setMaxTier(int tier, @NotNull Collection<String> channels) {
        if (this == EMPTY) return;
        hasData = true;
        hasTierData = true;
        // I'm sorry for this code but too many glass channels
        if (BlockRenderer6343.isGT5uNHLoaded && channels.contains(GTStructureChannels.BOROGLASS.get())) {
            maxTotalTier = Math.max(maxTotalTier, GlassTier.getMaxTierIndex());
        } else {
            maxTotalTier = Math.max(maxTotalTier, tier);
        }
        for (String channel : channels) {
            if (!channelDefinitions.containsKey(channel) && channelMaxTierMap.getInt(channel) < tier) {
                channelMaxTierMap.put(channel, tier);
            }
        }
    }

    public void addChannelDefinition(@NotNull IChannelDefinition definition) {
        if (this == EMPTY) return;
        channelDefinitions.put(definition.getChannel(), definition);
        channelMaxTierMap.put(definition.getChannel(), definition.getMaximumValue());
        hasData = true;
    }

    public boolean hasData() {
        return hasData;
    }

    public boolean hasTierData() {
        return hasTierData;
    }

    public ConstructableData setTierFromStack(ItemStack stack) {
        long hash = BRUtil.hashStack(stack);
        currentTier = itemTiers.getOrDefault(hash, 1);
        currentChannels = itemChannels.getOrDefault(hash, Collections.emptySet());
        return this;
    }

    public static Object2ObjectMap<IConstructable, ConstructableData> getConstructableData() {
        return constructableData;
    }

    public static ConstructableData getEmptyConstructableData() {
        return EMPTY;
    }

    public int getMaxTotalTier() {
        return maxTotalTier;
    }

    public int getCurrentTier() {
        return currentTier;
    }

    public @NotNull Collection<String> getCurrentChannels() {
        return currentChannels;
    }

    public Object2IntMap<String> getChannelMaxTierMap() {
        return channelMaxTierMap;
    }

    public Long2IntMap getItemTiers() {
        return itemTiers;
    }

    public Long2ObjectMap<Collection<String>> getItemChannels() {
        return itemChannels;
    }

    public Object2ObjectMap<String, IChannelDefinition> getChannelDefinitions() {
        return channelDefinitions;
    }

    private static Collection<String> immutableChannels(Collection<String> channels) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(channels));
    }
}
