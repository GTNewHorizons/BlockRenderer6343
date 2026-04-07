package blockrenderer6343.integration.nei;

import codechicken.lib.config.ConfigTagParent;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.config.OptionToggleButton;

public class BROptionToggleButton extends OptionToggleButton {

    private static final ConfigTagParent tag = NEIClientConfig.global.config;

    public BROptionToggleButton(String name) {
        this(name, true);
    }

    public BROptionToggleButton(String name, boolean defaultValue) {
        super(name, true);
        // Initializes the config tag with the default value if not yet set.
        // This is the standard NEI config initialization pattern used by OptionToggleButton.
        tag.getTag(name).getBooleanValue(defaultValue);
    }
}
