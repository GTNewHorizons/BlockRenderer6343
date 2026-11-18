package blockrenderer6343.client.world;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class PreviewChunk extends Chunk {

    public PreviewChunk(World world, int chunkX, int chunkZ) {
        super(world, chunkX, chunkZ);
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        return worldObj.getBlock((xPosition << 4) + x, y, (zPosition << 4) + z);
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        return worldObj.getBlockMetadata((xPosition << 4) + x, y, (zPosition << 4) + z);
    }

    @Override
    public boolean func_150807_a(int x, int y, int z, Block block, int metadata) {
        return worldObj.setBlock((xPosition << 4) + x, y, (zPosition << 4) + z, block, metadata, 0);
    }

    @Override
    public boolean setBlockMetadata(int x, int y, int z, int metadata) {
        return worldObj.setBlockMetadataWithNotify((xPosition << 4) + x, y, (zPosition << 4) + z, metadata, 0);
    }

    @Override
    public TileEntity func_150806_e(int x, int y, int z) {
        return worldObj.getTileEntity((xPosition << 4) + x, y, (zPosition << 4) + z);
    }

    @Override
    public TileEntity getTileEntityUnsafe(int x, int y, int z) {
        return func_150806_e(x, y, z);
    }

    @Override
    public void func_150812_a(int x, int y, int z, TileEntity tile) {
        worldObj.setTileEntity((xPosition << 4) + x, y, (zPosition << 4) + z, tile);
    }

    @Override
    public void removeTileEntity(int x, int y, int z) {
        worldObj.removeTileEntity((xPosition << 4) + x, y, (zPosition << 4) + z);
    }

    @Override
    public int getSavedLightValue(EnumSkyBlock type, int x, int y, int z) {
        return 15;
    }

    @Override
    public int getBlockLightValue(int x, int y, int z, int skyLightSubtracted) {
        return 15;
    }
}
