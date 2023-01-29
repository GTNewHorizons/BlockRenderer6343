package blockrenderer6343.api.utils;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_MultiBlockBase;

public class PositionedIStructureElement {

    public final int x;
    public final int y;
    public final int z;
    public final IStructureElement<GT_MetaTileEntity_MultiBlockBase> element;

    public PositionedIStructureElement(int x, int y, int z,
            IStructureElement<GT_MetaTileEntity_MultiBlockBase> element) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.element = element;
    }
}
