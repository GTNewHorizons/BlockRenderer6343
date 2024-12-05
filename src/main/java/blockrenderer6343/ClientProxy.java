package blockrenderer6343;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.StatCollector;

import blockrenderer6343.client.renderer.WorldSceneRenderer;
import blockrenderer6343.client.world.ObserverWorld;
import blockrenderer6343.client.world.TrackedDummyWorld;
import blockrenderer6343.integration.nei.InputHandler;
import codechicken.nei.guihook.GuiContainerManager;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import gregtech.api.GregTechAPI;

public class ClientProxy extends CommonProxy implements IResourceManagerReloadListener {

    // preInit "Run before anything else. Read your config, create blocks, items,
    // etc, and register them with the GameRegistry."
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        GuiContainerManager.addInputHandler(new InputHandler());
        GuiContainerManager.addTooltipHandler(new InputHandler());
        if (BlockRenderer6343.isGTLoaded) {
            GregTechAPI.addDummyWorld(TrackedDummyWorld.class);
            GregTechAPI.addDummyWorld(ObserverWorld.class);
        }
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(this);
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes."
    public void init(FMLInitializationEvent event) {
        super.init(event);
    }

    // postInit "Handle interaction with other mods, complete your setup based on this."
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }

    @Override
    public void onResourceManagerReload(IResourceManager p_110549_1_) {
        String hex = StatCollector.translateToLocal("gui.blockrenderer6343.BgColor");
        if (hex.length() <= 6) {
            try {
                WorldSceneRenderer.backgroundColor = Integer.parseUnsignedInt(hex, 16);
            } catch (final NumberFormatException e) {
                BlockRenderer6343.LOG.warn("Couldn't format background color!", e);
            }
        }
    }
}
