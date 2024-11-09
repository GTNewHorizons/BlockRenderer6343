package blockrenderer6343;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.relauncher.Side;

@Mod(
        modid = BlockRenderer6343.MOD_ID,
        version = Tags.VERSION,
        name = BlockRenderer6343.MOD_NAME,
        acceptedMinecraftVersions = "[1.7.10]",
        dependencies = "required-after:NotEnoughItems;required-after:structurelib;required-after:gtnhlib;")
public class BlockRenderer6343 {

    public static final String MOD_ID = "blockrenderer6343";
    public static final String MOD_NAME = "BlockRenderer6343";
    public static final Logger LOG = LogManager.getLogger(MOD_ID);

    @SidedProxy(clientSide = MOD_ID + ".ClientProxy", serverSide = MOD_ID + ".CommonProxy")
    public static CommonProxy proxy;

    public static boolean isGTLoaded;
    public static boolean isBartworksLoaded;
    public static boolean isNEELoaded;

    @Mod.EventHandler
    // preInit "Run before anything else. Read your config, create blocks, items,
    // etc, and register them with the GameRegistry."
    public void preInit(FMLPreInitializationEvent event) {
        isGTLoaded = Loader.isModLoaded("gregtech");
        isBartworksLoaded = Loader.isModLoaded("bartworks");
        isNEELoaded = Loader.isModLoaded("neenergistics");
        proxy.preInit(event);
    }

    @Mod.EventHandler
    // load "Do your mod setup. Build whatever data structures you care about. Register recipes."
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    // postInit "Handle interaction with other mods, complete your setup based on this."
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    public static void debug(String message) {
        LOG.debug(message);
    }

    public static void info(String message) {
        LOG.info(message);
    }

    public static void warn(String message) {
        LOG.warn(message);
    }

    public static void error(String message) {
        LOG.error(message);
    }

    @NetworkCheckHandler
    public final boolean networkCheck(Map<String, String> remoteVersions, Side side) {
        return true;
    }
}
