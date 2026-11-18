package blockrenderer6343.integration.nei;

import codechicken.nei.config.OptionToggleButton;

public class BROptionToggleButton extends OptionToggleButton {

    public BROptionToggleButton(String name) {
        super(name, true);
    }

    @Override
    public boolean state() {
        return BRNEIConfig.getConfigValue(name);
    }

    @Override
    public boolean onClick(int button) {
        BRNEIConfig.setConfigValue(name, !state());
        return true;
    }
}
