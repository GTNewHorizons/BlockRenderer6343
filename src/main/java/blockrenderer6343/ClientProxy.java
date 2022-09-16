package blockrenderer6343;

import blockrenderer6343.client.renderer.TooltipsFrameRenderer;
import blockrenderer6343.integration.gregtech.GT_GUI_MultiblocksHandler;
import blockrenderer6343.integration.nei.IMCForNEI;
import blockrenderer6343.integration.nei.NEI_Config;
import cpw.mods.fml.common.event.*;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy {

    public static GT_GUI_MultiblocksHandler guiMultiblocksHandler = new GT_GUI_MultiblocksHandler();

    // preInit "Run before anything else. Read your config, create blocks, items,
    // etc, and register them with the GameRegistry."
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        new TooltipsFrameRenderer();
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes."
    public void init(FMLInitializationEvent event) {
        super.init(event);
        IMCForNEI.IMCSender();
    }

    // postInit "Handle interaction with other mods, complete your setup based on this."
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
        MinecraftForge.EVENT_BUS.register(guiMultiblocksHandler);
    }

    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        super.serverAboutToStart(event);
    }

    // register server commands in this event handler
    public void serverStarting(FMLServerStartingEvent event) {
        super.serverStarting(event);
    }

    public void serverStarted(FMLServerStartedEvent event) {
        super.serverStarted(event);
    }

    public void serverStopping(FMLServerStoppingEvent event) {
        super.serverStopping(event);
    }

    public void serverStopped(FMLServerStoppedEvent event) {
        super.serverStopped(event);
    }
}
