package neofontrender.client;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import neofontrender.client.gui.NeofontrenderEmojiTestScreen;
import neofontrender.build.BuildFeatures;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.FontManager;
import neofontrender.core.font.awt.FontSet;
import neofontrender.core.font.backend.BackendTextSegmenter;
import neofontrender.core.font.backend.TextRenderBackend;
import neofontrender.core.font.cosmic.CosmicTextRenderer;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Client diagnostics and font-management command. */
public final class NeofontrenderCommand extends CommandBase {
    private static final List<String> SUBCOMMANDS = Arrays.asList("fonts", "info", "reload", "test", "gui");

    @Override
    public String getName() { return "neofontrender"; }

    @Override
    public String getUsage(ICommandSender sender) { return "/neofontrender fonts|info|reload|test|gui"; }

    @Override
    public int getRequiredPermissionLevel() { return 0; }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length == 0) {
            message(sender, TextFormatting.GOLD, NeofontrenderBranding.displayName() + ": " + getUsage(sender));
            return;
        }
        switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
            case "fonts": fonts(sender); break;
            case "info": info(sender); break;
            case "reload": reload(sender); break;
            case "test": test(sender); break;
            case "gui": gui(sender); break;
            default: message(sender, TextFormatting.RED, "Unknown subcommand: " + args[0]);
        }
    }

    private static void fonts(ICommandSender sender) {
        message(sender, TextFormatting.GOLD, "Configured font: " + NeofontrenderConfig.fontFamily());
        TextRenderBackend backend = FontManager.INSTANCE.getTextRenderBackend();
        if (backend == null || !backend.isReady()) {
            message(sender, TextFormatting.YELLOW, "Active backend does not expose font families.");
            return;
        }
        String[] families = backend.getFontFamilies();
        message(sender, TextFormatting.AQUA, "Backend families: " + families.length);
        for (String family : families) message(sender, TextFormatting.WHITE, "  " + family);
    }

    private static void info(ICommandSender sender) {
        FontManager manager = FontManager.INSTANCE;
        String engine = manager.isCosmicActive() ? "cosmic" : manager.isSfrActive() ? "sfr" : "vanilla";
        message(sender, TextFormatting.GOLD, NeofontrenderBranding.displayName() + " engine: " + engine);
        message(sender, TextFormatting.WHITE, "  configured: " + NeofontrenderConfig.renderingEngine());
        message(sender, TextFormatting.WHITE, "  backend: " + manager.getBackendVersion());
        message(sender, TextFormatting.WHITE, "  advanced string mode: " + NeofontrenderConfig.advancedStringMode());
        message(sender, TextFormatting.WHITE, "  oversample: " + NeofontrenderConfig.fontOversample());

        CosmicTextRenderer cosmic = manager.getCosmicTextRenderer();
        if (cosmic != null) {
            CosmicTextRenderer.DebugState state = cosmic.debugState();
            message(sender, TextFormatting.WHITE, "  text cache: " + state.renderCacheSize + "/" + state.renderCacheMax
                    + " h/m/e=" + state.renderHits + "/" + state.renderMisses + "/" + state.renderEvictions);
            message(sender, TextFormatting.WHITE, "  measure cache: " + state.measureCacheSize + "/" + state.measureCacheMax
                    + " h/m/e=" + state.measureHits + "/" + state.measureMisses + "/" + state.measureEvictions);
            if (BuildFeatures.RENDER_STATS) {
                BackendTextSegmenter.DebugState segments = BackendTextSegmenter.debugState();
                message(sender, TextFormatting.WHITE, "  segments: " + (segments.enabled() ? "on" : "off")
                        + " attempts=" + segments.attempts() + " runs=" + segments.segmentedRuns()
                        + " rejected=" + segments.rejectedRuns());
            }
        } else if (manager.isSfrActive()) {
            FontSet.DebugState state = manager.getSfrDebugState();
            if (state != null) message(sender, TextFormatting.WHITE,
                    "  glyph cache: " + state.glyphInfoCacheSize() + "/" + state.bakedGlyphCacheSize());
        }
    }

    private static void reload(ICommandSender sender) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.addScheduledTask(() -> FontManager.INSTANCE.reload(minecraft.getResourceManager()));
        message(sender, TextFormatting.GREEN, "Font reload scheduled.");
    }

    private static void test(ICommandSender sender) {
        TextRenderBackend backend = FontManager.INSTANCE.getModernTextBackend();
        if (backend == null || !backend.isReady()) {
            message(sender, TextFormatting.RED, "No modern text backend is available.");
            return;
        }
        String sample = NeofontrenderBranding.displayName() + " fi العربية 😀";
        message(sender, TextFormatting.AQUA, "Measured sample width: " + backend.measureFormatted(sample, 0xFFFFFFFF, false));
    }

    private static void gui(ICommandSender sender) {
        Minecraft.getMinecraft().addScheduledTask(NeofontrenderEmojiTestScreen::open);
        message(sender, TextFormatting.GREEN, "Opening text test screen.");
    }

    private static void message(ICommandSender sender, TextFormatting color, String text) {
        sender.sendMessage(new TextComponentString(color + text));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                                          @Nullable BlockPos targetPos) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, SUBCOMMANDS) : Collections.emptyList();
    }
}
