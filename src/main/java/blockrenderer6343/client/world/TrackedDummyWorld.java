package blockrenderer6343.client.world;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import org.joml.Vector3f;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;

public class TrackedDummyWorld extends DummyWorld {

    public final Long2ObjectMap<Block> blockMap = new Long2ObjectOpenHashMap<>();
    public final Long2ObjectMap<TileEntity> tileMap = new Long2ObjectOpenHashMap<>();
    public final Long2IntMap blockMetaMap = new Long2IntOpenHashMap();

    private final Vector3f minPos = new Vector3f(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    private final Vector3f maxPos = new Vector3f(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
    private final Vector3f size = new Vector3f();
    private boolean hasChanged;

    private int visibleYLevel = -1;

    @Override
    public boolean setBlock(int x, int y, int z, Block block, int meta, int flags) {
        long pos = CoordinatePacker.pack(x, y, z);
        if (block == Blocks.air) {
            blockMap.remove(pos);
            blockMetaMap.remove(pos);
            if (block.hasTileEntity(meta)) {
                removeTileEntity(x, y, z);
            }
        } else {
            blockMap.put(pos, block);
            blockMetaMap.put(pos, meta);
            if (block.hasTileEntity(meta)) {
                TileEntity tile = block.createTileEntity(this, meta);
                if (tile != null) {
                    setTileEntity(x, y, z, tile);
                }
            }
            block.onBlockAdded(this, x, y, z);
        }

        hasChanged = true;
        minPos.x = Math.min(minPos.x, x);
        minPos.y = Math.min(minPos.y, y);
        minPos.z = Math.min(minPos.z, z);
        maxPos.x = Math.max(maxPos.x, x);
        maxPos.y = Math.max(maxPos.y, y);
        maxPos.z = Math.max(maxPos.z, z);

        return y >= 0 && y < 256;
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
    public boolean setBlockMetadataWithNotify(int x, int y, int z, int meta, int flag) {
        long pos = CoordinatePacker.pack(x, y, z);
        if (!blockMap.containsKey(pos)) return false;
        blockMetaMap.put(pos, meta);

        TileEntity tile = tileMap.get(pos);
        if (tile != null) {
            tile.updateContainingBlockInfo();
            tile.blockMetadata = meta;
        }

        return true;
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

    @Override
    public void updateEntitiesForNEI() {
        for (TileEntity tile : tileMap.values()) {
            if (tile.canUpdate()) {
                tile.updateEntity();
            }
        }
    }

    /**
     * Enable fullbright rendering
     */
    @SideOnly(Side.CLIENT)
    @Override
    public int getLightBrightnessForSkyBlocks(int p_72802_1_, int p_72802_2_, int p_72802_3_, int p_72802_4_) {
        return 15 << 20 | 15 << 4;
    }

    public Vector3f getSize() {
        return size.set(maxPos.x - minPos.x + 1, maxPos.y - minPos.y + 1, maxPos.z - minPos.z + 1);
    }

    public Vector3f getMinPos() {
        return minPos;
    }

    public Vector3f getMaxPos() {
        return maxPos;
    }

    public void setVisibleYLevel(int visibleYLevel) {
        this.visibleYLevel = visibleYLevel;
    }

    public MovingObjectPosition rayTraceBlocksWithTargetMap(Vec3 start, Vec3 end, LongSet targetedBlocks) {
        return rayTraceBlocksWithTargetMap(start, end, targetedBlocks, false, false, false);
    }

    public MovingObjectPosition rayTraceBlocksWithTargetMap(Vec3 start, Vec3 end, LongSet targetedBlocks,
            boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock) {
        if (!Double.isNaN(start.xCoord) && !Double.isNaN(start.yCoord) && !Double.isNaN(start.zCoord)) {
            if (!Double.isNaN(end.xCoord) && !Double.isNaN(end.yCoord) && !Double.isNaN(end.zCoord)) {
                int i = MathHelper.floor_double(end.xCoord);
                int j = MathHelper.floor_double(end.yCoord);
                int k = MathHelper.floor_double(end.zCoord);
                int l = MathHelper.floor_double(start.xCoord);
                int i1 = MathHelper.floor_double(start.yCoord);
                int j1 = MathHelper.floor_double(start.zCoord);
                Block block = this.getBlock(l, i1, j1);
                int k1 = this.getBlockMetadata(l, i1, j1);

                if (!ignoreBlockWithoutBoundingBox || block.getCollisionBoundingBoxFromPool(this, l, i1, j1) != null) {
                    if (block.canCollideCheck(k1, stopOnLiquid)) {
                        if (visibleYLevel == -1 || visibleYLevel == i1) {
                            MovingObjectPosition movingobjectposition = block
                                    .collisionRayTrace(this, l, i1, j1, start, end);

                            if (movingobjectposition != null && isBlockTargeted(movingobjectposition, targetedBlocks)) {
                                return movingobjectposition;
                            }
                        }
                    }
                }

                MovingObjectPosition movingobjectposition2 = null;
                k1 = 200;

                while (k1-- >= 0) {
                    if (Double.isNaN(start.xCoord) || Double.isNaN(start.yCoord) || Double.isNaN(start.zCoord)) {
                        return null;
                    }

                    if (l == i && i1 == j && j1 == k) {
                        return returnLastUncollidableBlock ? movingobjectposition2 : null;
                    }

                    boolean flag6 = true;
                    boolean flag3 = true;
                    boolean flag4 = true;
                    double d0 = 999.0D;
                    double d1 = 999.0D;
                    double d2 = 999.0D;

                    if (i > l) {
                        d0 = (double) l + 1.0D;
                    } else if (i < l) {
                        d0 = (double) l + 0.0D;
                    } else {
                        flag6 = false;
                    }

                    if (j > i1) {
                        d1 = (double) i1 + 1.0D;
                    } else if (j < i1) {
                        d1 = (double) i1 + 0.0D;
                    } else {
                        flag3 = false;
                    }

                    if (k > j1) {
                        d2 = (double) j1 + 1.0D;
                    } else if (k < j1) {
                        d2 = (double) j1 + 0.0D;
                    } else {
                        flag4 = false;
                    }

                    double d3 = 999.0D;
                    double d4 = 999.0D;
                    double d5 = 999.0D;
                    double d6 = end.xCoord - start.xCoord;
                    double d7 = end.yCoord - start.yCoord;
                    double d8 = end.zCoord - start.zCoord;

                    if (flag6) {
                        d3 = (d0 - start.xCoord) / d6;
                    }

                    if (flag3) {
                        d4 = (d1 - start.yCoord) / d7;
                    }

                    if (flag4) {
                        d5 = (d2 - start.zCoord) / d8;
                    }

                    boolean flag5 = false;
                    byte b0;

                    if (d3 < d4 && d3 < d5) {
                        if (i > l) {
                            b0 = 4;
                        } else {
                            b0 = 5;
                        }

                        start.xCoord = d0;
                        start.yCoord += d7 * d3;
                        start.zCoord += d8 * d3;
                    } else if (d4 < d5) {
                        if (j > i1) {
                            b0 = 0;
                        } else {
                            b0 = 1;
                        }

                        start.xCoord += d6 * d4;
                        start.yCoord = d1;
                        start.zCoord += d8 * d4;
                    } else {
                        if (k > j1) {
                            b0 = 2;
                        } else {
                            b0 = 3;
                        }

                        start.xCoord += d6 * d5;
                        start.yCoord += d7 * d5;
                        start.zCoord = d2;
                    }

                    Vec3 vec32 = Vec3.createVectorHelper(start.xCoord, start.yCoord, start.zCoord);
                    l = (int) (vec32.xCoord = MathHelper.floor_double(start.xCoord));

                    if (b0 == 5) {
                        --l;
                        ++vec32.xCoord;
                    }

                    i1 = (int) (vec32.yCoord = MathHelper.floor_double(start.yCoord));

                    if (b0 == 1) {
                        --i1;
                        ++vec32.yCoord;
                    }

                    j1 = (int) (vec32.zCoord = MathHelper.floor_double(start.zCoord));

                    if (b0 == 3) {
                        --j1;
                        ++vec32.zCoord;
                    }

                    Block block1 = this.getBlock(l, i1, j1);
                    int l1 = this.getBlockMetadata(l, i1, j1);

                    if (!ignoreBlockWithoutBoundingBox
                            || block1.getCollisionBoundingBoxFromPool(this, l, i1, j1) != null) {
                        if (block1.canCollideCheck(l1, stopOnLiquid)) {
                            if (visibleYLevel == -1 || visibleYLevel == i1) {
                                MovingObjectPosition movingobjectposition1 = block1
                                        .collisionRayTrace(this, l, i1, j1, start, end);

                                if (movingobjectposition1 != null
                                        && isBlockTargeted(movingobjectposition1, targetedBlocks)) {
                                    return movingobjectposition1;
                                }
                            }
                        } else {
                            movingobjectposition2 = new MovingObjectPosition(l, i1, j1, b0, start, false);
                        }
                    }
                }

                return returnLastUncollidableBlock ? movingobjectposition2 : null;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private boolean isBlockTargeted(MovingObjectPosition result, LongSet targetedBlocks) {
        return targetedBlocks.contains(CoordinatePacker.pack(result.blockX, result.blockY, result.blockZ));
    }

    public boolean hasChanged() {
        boolean changed = hasChanged;
        hasChanged = false;
        return changed;
    }
}
