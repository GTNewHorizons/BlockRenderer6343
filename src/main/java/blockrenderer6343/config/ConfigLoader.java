package blockrenderer6343.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.minecraftforge.common.config.Configuration;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

import blockrenderer6343.BlockRenderer6343;
import blockrenderer6343.integration.nei.BRNEIConfig;
import codechicken.lib.config.ConfigTag;
import codechicken.lib.config.ConfigTagParent;

public class ConfigLoader {

    public static void load(Path configDirectory) throws IOException {
        Path configPath = configDirectory.resolve(BlockRenderer6343.MOD_ID + ".cfg");
        Configuration existing = null;
        if (Files.exists(configPath)) {
            Path backup = configPath.resolveSibling(configPath.getFileName() + ".bak");
            if (!Files.exists(backup)) Files.copy(configPath, backup);
            existing = new Configuration(configPath.toFile());
        }

        boolean importLegacyControls = isMissing(existing, "legacyControls");
        boolean importSwapMouseButtons = isMissing(existing, "sceneSwapMouseButtons");
        boolean importAutoFillPattern = isMissing(existing, "autoFillPattern");
        boolean importFilterHatch = isMissing(existing, "filterHatch");
        Properties legacy = new Properties();
        Path legacyPath = configDirectory.resolve("blockrenderer6343-preview.properties");
        if ((importLegacyControls || importSwapMouseButtons) && Files.exists(legacyPath)) {
            try (Reader reader = Files.newBufferedReader(legacyPath, StandardCharsets.UTF_8)) {
                legacy.load(reader);
            }
        }

        ConfigTagParent legacyNEI = new LegacyNEIConfig();
        Path legacyNEIPath = configDirectory.resolve("NEI").resolve("client.cfg");
        if ((importAutoFillPattern || importFilterHatch) && Files.exists(legacyNEIPath)) {
            try (var reader = Files.newBufferedReader(legacyNEIPath, StandardCharsets.UTF_8)) {
                legacyNEI.loadChildren(reader);
            }
        }
        boolean autoFillPattern = readLegacyOption(legacyNEI, BRNEIConfig.AUTO_FILL_PATTERN);
        boolean filterHatch = readLegacyOption(legacyNEI, BRNEIConfig.FILTER_HATCH);
        ConfigurationManager.registerConfig(BlockRendererConfig.class);
        if (importLegacyControls)
            BlockRendererConfig.legacyControls = Boolean.parseBoolean(legacy.getProperty("legacyControls", "false"));
        if (importSwapMouseButtons) BlockRendererConfig.sceneSwapMouseButtons = Boolean
                .parseBoolean(legacy.getProperty("sceneSwapMouseButtons", "true"));
        if (importAutoFillPattern) BlockRendererConfig.autoFillPattern = autoFillPattern;
        if (importFilterHatch) BlockRendererConfig.filterHatch = filterHatch;
        if (importLegacyControls || importSwapMouseButtons || importAutoFillPattern || importFilterHatch)
            ConfigurationManager.save(BlockRendererConfig.class);
    }

    private static boolean isMissing(Configuration config, String name) {
        return config == null || !config.hasKey(Configuration.CATEGORY_GENERAL, name);
    }

    private static boolean readLegacyOption(ConfigTagParent config, String identifier) {
        ConfigTag tag = config.getTag(identifier, false);
        String value = tag == null ? null : tag.getValue();
        return !"false".equalsIgnoreCase(value) && !"no".equalsIgnoreCase(value);
    }

    public static class LegacyNEIConfig extends ConfigTagParent {

        @Override
        public void saveConfig() {}

        @Override
        public String getNameQualifier() {
            return "";
        }
    }
}
