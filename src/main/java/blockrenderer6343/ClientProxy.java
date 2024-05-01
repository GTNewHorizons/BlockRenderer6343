package blockrenderer6343;

import net.minecraftforge.common.MinecraftForge;

import blockrenderer6343.client.renderer.TooltipsFrameRenderer;
import blockrenderer6343.integration.nei.IMCForNEI;
import blockrenderer6343.integration.nei.InputHandler;
import codechicken.nei.guihook.GuiContainerManager;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    // preInit "Run before anything else. Read your config, create blocks, items,
    // etc, and register them with the GameRegistry."
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        new TooltipsFrameRenderer();
        GuiContainerManager.addInputHandler(new InputHandler());
        GuiContainerManager.addTooltipHandler(new InputHandler());
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes."
    public void init(FMLInitializationEvent event) {
        super.init(event);
        IMCForNEI.IMCSender();
    }

    // postInit "Handle interaction with other mods, complete your setup based on this."
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
        MinecraftForge.EVENT_BUS.register(new EventHandler());
    }
}
