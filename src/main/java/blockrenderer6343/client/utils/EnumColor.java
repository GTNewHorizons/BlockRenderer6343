package blockrenderer6343.client.utils;

import net.minecraft.util.EnumChatFormatting;

public enum EnumColor {

    // There's 12 possible structure hint dots so we need 12 colors to represent them
    RED(0xFF0000, EnumChatFormatting.RED),
    GREEN(0x00FF00, EnumChatFormatting.GREEN),
    BLUE(0x0000FF, EnumChatFormatting.BLUE),
    YELLOW(0xFFFF00, EnumChatFormatting.YELLOW),
    MAGENTA(0xFF00FF, EnumChatFormatting.LIGHT_PURPLE),
    CYAN(0x00FFFF, EnumChatFormatting.AQUA),
    ORANGE(0xFFA500, EnumChatFormatting.GOLD),
    PURPLE(0x800080, EnumChatFormatting.DARK_PURPLE),
    DARK_GREEN(0x006400, EnumChatFormatting.DARK_GREEN),
    DARK_RED(0x8B0000, EnumChatFormatting.DARK_RED),
    DARK_BLUE(0x00008B, EnumChatFormatting.DARK_BLUE),
    DARK_AQUA(0x008B8B, EnumChatFormatting.DARK_AQUA);

    public static final EnumColor[] VALUES = values();

    public final int color;
    public final EnumChatFormatting formatting;

    EnumColor(int color, EnumChatFormatting formatting) {
        this.color = color;
        this.formatting = formatting;
    }
}
