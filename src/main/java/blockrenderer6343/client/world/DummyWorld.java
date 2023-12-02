package blockrenderer6343.client.world;

import javax.annotation.Nonnull;

import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;

import blockrenderer6343.api.utils.world.DummyChunkProvider;
import blockrenderer6343.api.utils.world.DummySaveHandler;

public class DummyWorld extends World {

    private static final WorldSettings DEFAULT_SETTINGS = new WorldSettings(
            1L,
            WorldSettings.GameType.SURVIVAL,
            true,
            false,
            WorldType.DEFAULT);

    public static final DummyWorld INSTANCE = new DummyWorld();

    public DummyWorld() {
        super(new DummySaveHandler(), "DummyServer", DEFAULT_SETTINGS, new WorldProviderSurface(), new Profiler());
        // Guarantee the dimension ID was not reset by the provider
        this.provider.setDimension(Integer.MAX_VALUE);
        int providerDim = this.provider.dimensionId;
        this.provider.worldObj = this;
        this.provider.setDimension(providerDim);
        this.chunkProvider = this.createChunkProvider();
        this.calculateInitialSkylight();
        this.calculateInitialWeatherBody();
    }

    @Override
    public void updateEntities() {}

    public void updateEntitiesForNEI() {
        super.updateEntities();
    }

    @Override
    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {}

    @Override
    protected int func_152379_p() {
        return 0;
    }

    @Override
    public Entity getEntityByID(int p_73045_1_) {
        return null;
    }

    @Nonnull
    @Override
    protected IChunkProvider createChunkProvider() {
        return new DummyChunkProvider(this);
    }

    @Override
    protected boolean chunkExists(int x, int z) {
        return chunkProvider.chunkExists(x, z);
    }

    @Override
    public boolean updateLightByType(EnumSkyBlock p_147463_1_, int p_147463_2_, int p_147463_3_, int p_147463_4_) {
        return true;
    }
}
