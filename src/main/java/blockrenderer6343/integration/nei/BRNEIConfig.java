package blockrenderer6343.integration.nei;

import codechicken.lib.config.ConfigTagParent;
import codechicken.nei.NEIClientConfig;

public class BRNEIConfig {

    public static final String AUTO_FILL_PATTERN = "blockrenderer6343.auto_fill_pattern";
    public static final String FILTER_HATCH = "blockrenderer6343.filter_hatch";

    private static final ConfigTagParent tag = NEIClientConfig.global.config;

    public static boolean getConfigValue(String identifier) {
        return tag.getTag(identifier).getBooleanValue(true);
    }
}
