package blockrenderer6343;

import blockrenderer6343.client.renderer.TooltipsFrameRenderer;
import blockrenderer6343.integration.nei.IMCForNEI;
import blockrenderer6343.integration.nei.NEI_config;
import cpw.mods.fml.common.event.*;

public class ClientProxy extends CommonProxy {

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
        new NEI_config().loadConfig();
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
