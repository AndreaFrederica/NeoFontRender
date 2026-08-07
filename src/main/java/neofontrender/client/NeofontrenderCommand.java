package neofontrender.client;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import neofontrender.client.gui.NeofontrenderEmojiTestScreen;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.FontManager;
import neofontrender.core.font.awt.FontSet;
import neofontrender.core.font.backend.BackendTextSegmenter;
import neofontrender.core.font.backend.TextRenderBackend;
import neofontrender.core.font.cosmic.CosmicTextRenderer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Client diagnostics and font-management command. */
public final class NeofontrenderCommand extends CommandBase {
    private static final List<String> SUBCOMMANDS =
            Arrays.asList("fonts", "info", "reload", "test", "gui");

    @Override
    public String getCommandName() {
        return "neofontrender";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/neofontrender fonts|info|reload|test|gui";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            message(sender, EnumChatFormatting.GOLD,
                    NeofontrenderBranding.displayName() + ": " + getCommandUsage(sender));
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "fonts":
                fonts(sender);
                break;
            case "info":
                info(sender);
                break;
            case "reload":
                reload(sender);
                break;
            case "test":
                test(sender);
                break;
            case "gui":
                gui(sender);
                break;
            default:
                message(sender, EnumChatFormatting.RED,
                        "Unknown subcommand: " + args[0]);
        }
    }

    private static void fonts(ICommandSender sender) {
        message(sender, EnumChatFormatting.GOLD,
                "Configured font: " + NeofontrenderConfig.fontName());
        TextRenderBackend backend = FontManager.INSTANCE.getTextRenderBackend();
        if (backend == null || !backend.isReady()) {
            message(sender, EnumChatFormatting.YELLOW,
                    "Active backend does not expose font families.");
            return;
        }
        String[] families = backend.getFontFamilies();
        message(sender, EnumChatFormatting.AQUA,
                "Backend families: " + families.length);
        for (String family : families) {
            message(sender, EnumChatFormatting.WHITE, "  " + family);
        }
    }

    private static void info(ICommandSender sender) {
        FontManager manager = FontManager.INSTANCE;
        String engine = manager.isCosmicActive()
                ? "cosmic" : manager.isSfrActive() ? "sfr" : "vanilla";
        message(sender, EnumChatFormatting.GOLD,
                NeofontrenderBranding.displayName() + " engine: " + engine);
        message(sender, EnumChatFormatting.WHITE,
                "  configured: " + NeofontrenderConfig.renderingEngine());
        message(sender, EnumChatFormatting.WHITE,
                "  backend: " + manager.getBackendVersion());
        message(sender, EnumChatFormatting.WHITE,
                "  advanced string mode: " + NeofontrenderConfig.advancedStringMode());
        message(sender, EnumChatFormatting.WHITE,
                "  oversample: " + NeofontrenderConfig.fontOversample());

        CosmicTextRenderer cosmic = manager.getCosmicTextRenderer();
        if (cosmic != null) {
            CosmicTextRenderer.DebugState state = cosmic.debugState();
            message(sender, EnumChatFormatting.WHITE,
                    "  text cache: " + state.renderCacheSize + "/" + state.renderCacheMax
                            + " h/m/e=" + state.renderHits + "/" + state.renderMisses
                            + "/" + state.renderEvictions);
            message(sender, EnumChatFormatting.WHITE,
                    "  measure cache: " + state.measureCacheSize + "/" + state.measureCacheMax
                            + " h/m/e=" + state.measureHits + "/" + state.measureMisses
                            + "/" + state.measureEvictions);
            BackendTextSegmenter.DebugState segments = BackendTextSegmenter.debugState();
            message(sender, EnumChatFormatting.WHITE,
                    "  segments: " + (segments.enabled() ? "on" : "off")
                            + " attempts=" + segments.attempts()
                            + " runs=" + segments.segmentedRuns()
                            + " rejected=" + segments.rejectedRuns());
        } else if (manager.isSfrActive()) {
            FontSet.DebugState state = manager.getSfrDebugState();
            if (state != null) {
                message(sender, EnumChatFormatting.WHITE,
                        "  glyph cache: " + state.glyphInfoCacheSize()
                                + "/" + state.bakedGlyphCacheSize());
            }
        }
    }

    private static void reload(ICommandSender sender) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.func_152344_a(
                () -> FontManager.INSTANCE.reload(minecraft.getResourceManager()));
        message(sender, EnumChatFormatting.GREEN, "Font reload scheduled.");
    }

    private static void test(ICommandSender sender) {
        TextRenderBackend backend = FontManager.INSTANCE.getModernTextBackend();
        if (backend == null || !backend.isReady()) {
            message(sender, EnumChatFormatting.RED,
                    "No modern text backend is available.");
            return;
        }
        String sample = NeofontrenderBranding.displayName() + " fi \u0627\u0644\u0639\u0631\u0628\u064a\u0629 "
                + "\uD83D\uDE00";
        message(sender, EnumChatFormatting.AQUA,
                "Measured sample width: "
                        + backend.measureFormatted(sample, 0xFFFFFFFF, false));
    }

    private static void gui(ICommandSender sender) {
        Minecraft.getMinecraft().func_152344_a(NeofontrenderEmojiTestScreen::open);
        message(sender, EnumChatFormatting.GREEN, "Opening text test screen.");
    }

    private static void message(
            ICommandSender sender, EnumChatFormatting color, String text) {
        sender.addChatMessage(new ChatComponentText(color + text));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        return args.length == 1
                ? getListOfStringsMatchingLastWord(
                        args, SUBCOMMANDS.toArray(new String[SUBCOMMANDS.size()]))
                : Collections.emptyList();
    }
}
