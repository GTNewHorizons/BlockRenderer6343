package blockrenderer6343;

import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.SimpleReloadableResourceManager;

import blockrenderer6343.client.renderer.PreviewGeometryCache;
import blockrenderer6343.client.utils.BRUtil;
import blockrenderer6343.client.world.ObserverWorld;
import blockrenderer6343.client.world.TrackedDummyWorld;
import blockrenderer6343.config.ConfigLoader;
import blockrenderer6343.integration.gregtech.GTNEIUtil;
import blockrenderer6343.integration.nei.InputHandler;
import codechicken.nei.guihook.GuiContainerManager;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import gregtech.api.GregTechAPI;

public class ClientProxy extends CommonProxy {

    // preInit "Run before anything else. Read your config, create blocks, items,
    // etc, and register them with the GameRegistry."
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        try {
            ConfigLoader.load(event.getModConfigurationDirectory().toPath());
        } catch (IOException failure) {
            BlockRenderer6343.LOG.error("Unable to migrate legacy BlockRenderer6343 configuration", failure);
        }
        InputHandler inputHandler = new InputHandler();
        GuiContainerManager.addInputHandler(inputHandler);
        GuiContainerManager.addTooltipHandler(inputHandler);
        GuiContainerManager.addObjectHandler(inputHandler);
        if (BlockRenderer6343.isGT5uNHLoaded) {
            GregTechAPI.addDummyWorld(TrackedDummyWorld.class);
            GregTechAPI.addDummyWorld(ObserverWorld.class);
            BRUtil.hatchFilter = GTNEIUtil::isHatchItem;
        }
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes."
    public void init(FMLInitializationEvent event) {
        super.init(event);
        ((SimpleReloadableResourceManager) Minecraft.getMinecraft().getResourceManager())
                .registerReloadListener(PreviewGeometryCache.getInstance());
    }

    // postInit "Handle interaction with other mods, complete your setup based on this."
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }
}
