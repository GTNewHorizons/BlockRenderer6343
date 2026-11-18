package blockrenderer6343.client.world;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import blockrenderer6343.api.utils.world.DummyChunkProvider;

public class PreviewChunkProvider extends DummyChunkProvider {

    private final World previewWorld;

    public PreviewChunkProvider(World world) {
        super(world);
        previewWorld = world;
    }

    @Override
    protected Chunk createChunk(int x, int z) {
        return new PreviewChunk(previewWorld, x, z);
    }
}
