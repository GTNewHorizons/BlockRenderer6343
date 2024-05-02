package blockrenderer6343.integration.gregtech;

import java.util.Arrays;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructableProvider;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.mojang.authlib.GameProfile;

import blockrenderer6343.BlockRenderer6343;
import blockrenderer6343.api.utils.CreativeItemSource;
import blockrenderer6343.client.world.ClientFakePlayer;
import blockrenderer6343.integration.nei.GUI_MultiblocksHandler;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.interfaces.tileentity.ITurnable;
import gregtech.api.threads.GT_Runnable_MachineBlockUpdate;

public class GT_GUI_MultiblocksHandler extends GUI_MultiblocksHandler<IConstructable> {

    public GT_GUI_MultiblocksHandler() {
        super();
    }

    @Override
    protected void placeMultiblock() {
        if (GT_Runnable_MachineBlockUpdate.isCurrentThreadEnabled())
            GT_Runnable_MachineBlockUpdate.setCurrentThreadEnabled(false);

        fakeMultiblockBuilder = new ClientFakePlayer(
                renderer.world,
                new GameProfile(UUID.fromString("518FDF18-EC2A-4322-832A-58ED1721309B"), "[GregTech]"));
        renderer.world.unloadEntities(Arrays.asList(fakeMultiblockBuilder));

        IConstructable constructable = null;
        ItemStack copy = stackForm.copy();
        copy.getItem().onItemUse(
                copy,
                fakeMultiblockBuilder,
                renderer.world,
                MB_PLACE_POS.x,
                MB_PLACE_POS.y,
                MB_PLACE_POS.z,
                0,
                MB_PLACE_POS.x,
                MB_PLACE_POS.y,
                MB_PLACE_POS.z);

        TileEntity tTileEntity = renderer.world.getTileEntity(MB_PLACE_POS.x, MB_PLACE_POS.y, MB_PLACE_POS.z);
        ((ITurnable) tTileEntity).setFrontFacing(ForgeDirection.SOUTH);
        IMetaTileEntity mte = ((IGregTechTileEntity) tTileEntity).getMetaTileEntity();

        if (!StructureLibAPI.isInstrumentEnabled()) StructureLibAPI.enableInstrument(BlockRenderer6343.MOD_ID);
        structureElements.clear();

        if (mte instanceof ISurvivalConstructable) {
            int result, iterations = 0;
            do {
                result = ((ISurvivalConstructable) mte).survivalConstruct(
                        getTriggerStack(),
                        Integer.MAX_VALUE,
                        ISurvivalBuildEnvironment.create(CreativeItemSource.instance, fakeMultiblockBuilder));
                iterations++;
            } while (result > 0 && iterations < MAX_PLACE_ROUNDS);
        } else if (tTileEntity instanceof IConstructableProvider) {
            constructable = ((IConstructableProvider) tTileEntity).getConstructable();
        } else if (tTileEntity instanceof IConstructable) {
            constructable = (IConstructable) tTileEntity;
        }
        if (constructable != null) {
            constructable.construct(getTriggerStack(), false);
        }

        if (StructureLibAPI.isInstrumentEnabled()) StructureLibAPI.disableInstrument();

        if (!GT_Runnable_MachineBlockUpdate.isCurrentThreadEnabled())
            GT_Runnable_MachineBlockUpdate.setCurrentThreadEnabled(true);
    }
}
