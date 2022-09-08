package blockrenderer6343.mixins;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;

@Mixin(GuiContainer.class)
public interface GuiContainerMixin {

    @Accessor
    int getXSize();

    @Accessor
    int getYSize();
}
