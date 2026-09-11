package blockrenderer6343.api.utils;

import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.fluid.IFluidSource;

/**
 * A fluid source that always holds as much of the requested fluid as asked for.
 * <p>
 * Structure previews and guide book scenes are built with a creative item source, and this is its fluid counterpart: it
 * lets a preview fill the positions a multiblock wants a fluid at, e.g. the water an ore washing plant needs, without
 * anybody having to carry that fluid.
 */
public class CreativeFluidSource implements IFluidSource {

    public static final CreativeFluidSource instance = new CreativeFluidSource();

    @NotNull
    @Override
    public FluidStack take(FluidStack resource, boolean simulate) {
        if (resource == null || resource.getFluid() == null) throw new IllegalArgumentException();
        return new FluidStack(resource, resource.amount);
    }
}
