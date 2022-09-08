package blockrenderer6343.mixinplugin;

import static blockrenderer6343.mixinplugin.TargetedMod.*;

import cpw.mods.fml.relauncher.FMLLaunchHandler;
import java.util.Arrays;
import java.util.List;

public enum Mixin {

    //
    // IMPORTANT: Do not make any references to any mod from this file. This file is loaded quite early on and if
    // you refer to other mods you load them as well. The consequence is: You can't inject any previously loaded
    // classes!
    // Exception: Tags.java, as long as it is used for Strings only!
    //

    // Replace with your own mixins:
    // waiting for GTNH fix
    InventoryEffectRendererMixin("InventoryEffectRendererMixin", Side.CLIENT, VANILLA),
    GuiScreenMixin("GuiScreenMixin", Side.CLIENT, VANILLA),
    // You may also require multiple mods to be loaded if your mixin requires both
    //    GT_Block_Ores_AbstractMixin("gregtech.GT_Block_Ores_AbstractMixin", GREGTECH, VANILLA);
    CCLGuiDrawMixin("CCLGuiDrawMixin", Side.CLIENT, CODECHICKENLIB, VANILLA),
    GuiContainerMixin("GuiContainerMixin", Side.CLIENT, VANILLA);

    public final String mixinClass;
    public final List<TargetedMod> targetedMods;
    private final Side side;

    Mixin(String mixinClass, Side side, TargetedMod... targetedMods) {
        this.mixinClass = mixinClass;
        this.targetedMods = Arrays.asList(targetedMods);
        this.side = side;
    }

    Mixin(String mixinClass, TargetedMod... targetedMods) {
        this.mixinClass = mixinClass;
        this.targetedMods = Arrays.asList(targetedMods);
        this.side = Side.BOTH;
    }

    public boolean shouldLoad(List<TargetedMod> loadedMods) {
        return (side == Side.BOTH
                        || side == Side.SERVER && FMLLaunchHandler.side().isServer()
                        || side == Side.CLIENT && FMLLaunchHandler.side().isClient())
                && loadedMods.containsAll(targetedMods);
    }
}

enum Side {
    BOTH,
    CLIENT,
    SERVER;
}
