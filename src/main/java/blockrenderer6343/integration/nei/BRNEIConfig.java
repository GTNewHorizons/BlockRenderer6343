package blockrenderer6343.integration.nei;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

import blockrenderer6343.config.BlockRendererConfig;

public class BRNEIConfig {

    public static final String AUTO_FILL_PATTERN = "blockrenderer6343.auto_fill_pattern";
    public static final String FILTER_HATCH = "blockrenderer6343.filter_hatch";

    public static boolean getConfigValue(String identifier) {
        return switch (identifier) {
            case AUTO_FILL_PATTERN -> BlockRendererConfig.autoFillPattern;
            case FILTER_HATCH -> BlockRendererConfig.filterHatch;
            default -> throw new IllegalArgumentException("Unknown BlockRenderer6343 option: " + identifier);
        };
    }

    public static void setConfigValue(String identifier, boolean value) {
        switch (identifier) {
            case AUTO_FILL_PATTERN -> BlockRendererConfig.autoFillPattern = value;
            case FILTER_HATCH -> BlockRendererConfig.filterHatch = value;
            default -> throw new IllegalArgumentException("Unknown BlockRenderer6343 option: " + identifier);
        }
        ConfigurationManager.save(BlockRendererConfig.class);
    }
}
