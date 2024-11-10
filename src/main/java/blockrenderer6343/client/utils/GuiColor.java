package blockrenderer6343.client.utils;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.StatCollector;

import blockrenderer6343.BlockRenderer6343;

public enum GuiColor implements IResourceManagerReloadListener {

    // UI Text
    FontColor(0x333333),
    BgColor(0xC6C6C6),
    ButtonEnabledColor(0x202020),
    ButtonDisabledColor(0xA0A0A0),
    ButtonHoveredColor(0xFFFFA0);

    private final String root;
    private int color;

    GuiColor(final int hex) {
        this.root = "gui.blockrenderer6343";
        this.color = hex;
    }

    public int getColor() {
        return color;
    }

    public String getUnlocalized() {
        return this.root + '.' + this;
    }

    @Override
    public void onResourceManagerReload(IResourceManager p_110549_1_) {
        for (GuiColor text : values()) {
            String hex = StatCollector.translateToLocal(text.getUnlocalized());
            if (hex.length() <= 6) {
                try {
                    text.color = Integer.parseUnsignedInt(hex, 16);
                } catch (final NumberFormatException e) {
                    BlockRenderer6343.warn("Couldn't format color correctly for: " + text.root + " -> " + hex);
                }
            }
        }
    }
}
