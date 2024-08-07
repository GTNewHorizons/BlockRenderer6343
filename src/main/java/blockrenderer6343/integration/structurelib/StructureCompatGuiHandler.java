package blockrenderer6343.integration.structurelib;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.IMultiblockInfoContainer;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;

import blockrenderer6343.BlockRenderer6343;
import blockrenderer6343.api.utils.CreativeItemSource;
import blockrenderer6343.client.world.TrackedDummyWorld;
import blockrenderer6343.integration.nei.GUI_MultiblockHandler;

public class StructureCompatGuiHandler extends GUI_MultiblockHandler<IConstructable> {

    public StructureCompatGuiHandler() {
        super();
    }

    @Override
    protected void placeMultiblock() {
        Block stackBlock = ((ItemBlock) stackForm.getItem()).field_150939_a;
        renderer.world
                .setBlock(MB_PLACE_POS.x, MB_PLACE_POS.y, MB_PLACE_POS.z, stackBlock, stackForm.getItemDamage(), 3);

        TileEntity tTileEntity = renderer.world.getTileEntity(MB_PLACE_POS.x, MB_PLACE_POS.y, MB_PLACE_POS.z);
        IMultiblockInfoContainer<TileEntity> t = IMultiblockInfoContainer.get(tTileEntity.getClass());
        ISurvivalConstructable multi = t.toConstructable(tTileEntity, ExtendedFacing.DEFAULT);

        if (!StructureLibAPI.isInstrumentEnabled()) {
            StructureLibAPI.enableInstrument(BlockRenderer6343.MOD_ID);
        }
        structureElements.clear();

        int result, iterations = 0;
        boolean tryContruct = false;
        do {
            result = multi.survivalConstruct(
                    getBuildTriggerStack(),
                    Integer.MAX_VALUE,
                    ISurvivalBuildEnvironment.create(CreativeItemSource.instance, fakeMultiblockBuilder));
            iterations++;
            if (result == -2) {
                tryContruct = true;
                break;
            }
        } while (result > 0 && iterations < MAX_PLACE_ROUNDS);

        if (tryContruct) {
            multi.construct(getBuildTriggerStack(), false);
        }

        // A single tick is needed for some non GT multiblocks to complete
        ((TrackedDummyWorld) renderer.world).updateEntitiesForNEI();

        if (StructureLibAPI.isInstrumentEnabled()) {
            StructureLibAPI.disableInstrument();
        }

    }

    public void loadTileMulti(TileEntity multiblock, ItemStack stackForm) {
        IMultiblockInfoContainer<TileEntity> m = IMultiblockInfoContainer.get(multiblock.getClass());
        IConstructable constructable = m.toConstructable(multiblock, ExtendedFacing.DEFAULT);
        super.loadMultiblock(constructable, stackForm);
    }
}
