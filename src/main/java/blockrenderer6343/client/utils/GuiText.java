package blockrenderer6343.client.utils;

import net.minecraft.util.StatCollector;

import blockrenderer6343.BlockRenderer6343;

public enum GuiText {

    // UI Text
    Tier,
    Layer,
    // UI Colors
    FontColor(0x333333),
    BgColor(0xC6C6C6),
    ButtonEnabledColor(0x202020),
    ButtonDisabledColor(0xA0A0A0),
    ButtonHoveredColor(0xFFFFA0);

    private final String root;
    private final int color;

    GuiText() {
        this.root = "gui.blockrenderer6343";
        this.color = 0x000000;
    }

    GuiText(final int hex) {
        this.root = "gui.blockrenderer6343";
        this.color = hex;
    }

    public int getColor() {
        String hex = StatCollector.translateToLocal(this.getUnlocalized());
        int color = this.color;
        if (hex.length() <= 6) {
            try {
                color = Integer.parseUnsignedInt(hex, 16);
            } catch (final NumberFormatException e) {
                BlockRenderer6343.warn("Couldn't format color correctly for: " + this.root + " -> " + hex);
            }
        }
        return color;
    }

    public String getLocal() {
        return StatCollector.translateToLocal(this.getUnlocalized());
    }

    public String getUnlocalized() {
        return this.root + '.' + this.toString();
    }
}
