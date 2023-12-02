package blockrenderer6343.api.utils;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureElement;

public class PositionedIStructureElement {

    public final int x;
    public final int y;
    public final int z;
    public final IStructureElement<ISurvivalConstructable> element;

    public PositionedIStructureElement(int x, int y, int z, IStructureElement<ISurvivalConstructable> element) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.element = element;
    }
}
