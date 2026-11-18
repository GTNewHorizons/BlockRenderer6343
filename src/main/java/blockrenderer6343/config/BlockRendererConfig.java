package blockrenderer6343.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultBoolean;
import com.gtnewhorizon.gtnhlib.config.Config.LangKeyPattern;

import blockrenderer6343.BlockRenderer6343;

@Config(modid = BlockRenderer6343.MOD_ID, categoryCulling = false)
@LangKeyPattern(pattern = "blockrenderer6343.config.%field")
public class BlockRendererConfig {

    @Comment("Restore legacy controls and disable camera smoothing and local orbit picking.")
    @DefaultBoolean(false)
    public static boolean legacyControls = false;

    @Comment({ "Use GuideNH controls: left drag rotates and right drag pans. Disable to swap these buttons.",
            "Middle drag zooms. This setting is ignored when legacy controls are enabled." })
    @DefaultBoolean(true)
    public static boolean sceneSwapMouseButtons = true;

    @Comment("Automatically fill the fake pattern output when exporting through NEI Overlay.")
    @DefaultBoolean(true)
    public static boolean autoFillPattern = true;

    @Comment("Exclude GregTech hatches and buses when exporting through NEI Overlay.")
    @DefaultBoolean(true)
    public static boolean filterHatch = true;
}
