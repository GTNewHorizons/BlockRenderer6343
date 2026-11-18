package blockrenderer6343.client.renderer;

import blockrenderer6343.config.BlockRendererConfig;

public class PreviewInteractionConfig {

    public static boolean isLegacyControls() {
        return BlockRendererConfig.legacyControls;
    }

    public static int getRotateButton() {
        return BlockRendererConfig.legacyControls || BlockRendererConfig.sceneSwapMouseButtons ? 0 : 1;
    }

    public static int getPanButton() {
        return BlockRendererConfig.legacyControls ? 2 : BlockRendererConfig.sceneSwapMouseButtons ? 1 : 0;
    }

    public static int getZoomButton() {
        return BlockRendererConfig.legacyControls ? 1 : 2;
    }
}
