package blockrenderer6343.mixins;

import net.minecraft.client.gui.inventory.GuiContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiContainer.class)
public interface GuiContainerMixin {

    @Accessor
    int getXSize();

    @Accessor
    int getYSize();
}
