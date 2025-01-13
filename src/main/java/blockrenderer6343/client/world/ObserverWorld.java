package blockrenderer6343.client.world;

import static blockrenderer6343.client.utils.BRUtil.FAKE_PLAYER;
import static blockrenderer6343.integration.nei.StructureHacks.HOLO_STACK;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.IMultiblockInfoContainer;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;

import blockrenderer6343.BlockRenderer6343;
import blockrenderer6343.client.utils.BRUtil;
import gregtech.api.interfaces.INEIPreviewModifier;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.threads.RunnableMachineUpdate;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class ObserverWorld extends DummyWorld {

    private static final int MAX_TRIES = 64;
    private final Long2ObjectMap<Block> blockMap = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<TileEntity> tileMap = new Long2ObjectOpenHashMap<>();
    private final Long2IntMap blockMetaMap = new Long2IntOpenHashMap();
    private final LongSet checkedBlocks = new LongOpenHashSet();
    private Consumer<ItemStack> stackConsumer;
    private boolean hasChanged = false;

    @Override
    public boolean setBlock(int x, int y, int z, Block block, int meta, int flags) {
        long pos = CoordinatePacker.pack(x, y, z);
        if (block == Blocks.air) {
            blockMap.remove(pos);
            if (block.hasTileEntity(meta)) {
                removeTileEntity(x, y, z);
            }
        } else {
            if (blockMap.containsKey(pos) && blockMap.get(pos) == block && blockMetaMap.get(pos) == meta) {
                return true;
            }

            hasChanged = true;
            blockMap.put(pos, block);
            blockMetaMap.put(pos, meta);
            addBlockToResult(block, pos);
            if (block.hasTileEntity(meta)) {
                TileEntity tile = block.createTileEntity(this, meta);
                if (tile != null) {
                    setTileEntity(x, y, z, tile);
                }
            }
        }

        return y >= 0 && y < 256;
    }

    @Override
    public boolean blockExists(int x, int y, int z) {
        return blockMap.containsKey(CoordinatePacker.pack(x, y, z));
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        Block block = blockMap.get(CoordinatePacker.pack(x, y, z));
        return block == null ? Blocks.air : block;
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        return blockMetaMap.get(CoordinatePacker.pack(x, y, z));
    }

    @Override
    public void setTileEntity(int x, int y, int z, TileEntity tile) {
        tile.setWorldObj(this);
        tile.xCoord = x;
        tile.yCoord = y;
        tile.zCoord = z;
        tile.validate();
        tileMap.put(CoordinatePacker.pack(x, y, z), tile);
    }

    @Override
    public void removeTileEntity(int x, int y, int z) {
        tileMap.remove(CoordinatePacker.pack(x, y, z)).invalidate();
    }

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        return tileMap.get(CoordinatePacker.pack(x, y, z));
    }

    private void addBlockToResult(Block block, long pos) {
        if (stackConsumer == null) return;
        if (!checkedBlocks.add(BRUtil.hashBlock(this, pos))) return;

        ItemStack stack = new ItemStack(block, 1, BRUtil.getDamageValue(this, block, pos));
        stackConsumer.accept(stack);
    }

    public @Nullable IConstructable getConstructableFromContainer(String className,
            IMultiblockInfoContainer<TileEntity> container) {
        TileEntity tile = getUnsafeTile(className);
        if (tile == null) return null;
        setTileEntity(0, 64, 0, tile);
        return container.toConstructable(tile, ExtendedFacing.DEFAULT);
    }

    private static @Nullable TileEntity getUnsafeTile(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return (TileEntity) clazz.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException
                | NoSuchMethodException e) {
            return null;
        }
    }

    public int estimateTierFromInfoContainer(Long2ObjectMap<ObjectSet<IConstructable>> result,
            Object2ObjectMap<IConstructable, ItemStack> multiBlockStack, IConstructable constructable) {
        stackConsumer = e -> result.computeIfAbsent(BRUtil.hashStack(e), k -> new ObjectOpenHashSet<>())
                .add(constructable);
        int tier = estimateTier(constructable);
        Block block = getBlock(0, 64, 0);
        long pos = CoordinatePacker.pack(0, 64, 0);
        ItemStack stack = new ItemStack(block, 1, BRUtil.getDamageValue(this, block, pos));
        multiBlockStack.put(constructable, stack);
        reset();
        return tier;
    }

    public int estimateTierFromConstructable(Consumer<ItemStack> result, IConstructable multi) {
        if (!BlockRenderer6343.isGTLoaded || !(multi instanceof IMetaTileEntity metaTile)) return 0;
        if (RunnableMachineUpdate.isCurrentThreadEnabled()) {
            RunnableMachineUpdate.setCurrentThreadEnabled(false);
        }

        ItemStack stack = metaTile.getStackForm(1);
        FAKE_PLAYER.setWorld(this);
        stack.getItem().onItemUse(stack, FAKE_PLAYER, this, 0, 64, 0, 0, 0, 64, 0);
        TileEntity tile = getTileEntity(0, 64, 0);
        IConstructable constructable = (IConstructable) ((IGregTechTileEntity) tile).getMetaTileEntity();
        stackConsumer = result;

        int tier = estimateTier(constructable);

        if (!RunnableMachineUpdate.isCurrentThreadEnabled()) {
            RunnableMachineUpdate.setCurrentThreadEnabled(true);
        }

        reset();

        return tier;
    }

    private int estimateTier(IConstructable multi) {
        int tier = 0;
        ItemStack holo = HOLO_STACK.copy();

        do {
            holo.stackSize = tier + 1;
            hasChanged = false;
            if (BlockRenderer6343.isGTLoaded && multi instanceof INEIPreviewModifier modifier) {
                modifier.onPreviewConstruct(holo);
            }
            multi.construct(holo, false);

        } while (tier++ < MAX_TRIES && hasChanged);

        return tier - 1;
    }

    public void reset() {
        checkedBlocks.clear();
        blockMap.clear();
        tileMap.clear();
        blockMetaMap.clear();
    }
}
