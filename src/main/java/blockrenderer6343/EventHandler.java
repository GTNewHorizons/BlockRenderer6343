package blockrenderer6343;

import com.gtnewhorizon.structurelib.StructureEvent;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureElement;

import blockrenderer6343.api.utils.PositionedIStructureElement;
import blockrenderer6343.common.GUI_MultiblocksHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class EventHandler {

    @SubscribeEvent
    public void OnStructureEvent(StructureEvent.StructureElementVisitedEvent event) {
        GUI_MultiblocksHandler.structureElements.add(
                new PositionedIStructureElement(
                        event.getX(),
                        event.getY(),
                        event.getZ(),
                        (IStructureElement<IConstructable>) event.getElement()));
    }
}
