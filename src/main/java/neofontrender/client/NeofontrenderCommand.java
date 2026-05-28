package neofontrender.client;

import io.github.humbleui.skija.FontMgr;
import io.github.humbleui.skija.FontStyle;
import io.github.humbleui.skija.Typeface;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import neofontrender.client.gui.NeofontrenderEmojiTestScreen;
import neofontrender.core.font.FontManager;
import neofontrender.core.font.skia.SkijaTextRenderer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NeofontrenderCommand extends CommandBase {

    @Override
    public String getName() {
        return "neofontrender";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/" + getName() + " fonts|info|reload";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Neo Font Render Commands:"));
            sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  /neofontrender fonts - Show current font families"));
            sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  /neofontrender info - Show engine status"));
            sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  /neofontrender reload - Reload fonts"));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "fonts":
                executeFonts(sender);
                break;
            case "info":
                executeInfo(sender);
                break;
            case "reload":
                executeReload(sender);
                break;
            case "test":
                executeTest(sender);
                break;
            case "gui":
                executeGui(sender);
                break;
            default:
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "Unknown subcommand: " + args[0]));
        }
    }

    private void executeFonts(ICommandSender sender) {
        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== Font Families ==="));

        if (!FontManager.INSTANCE.isSkiaActive()) {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Skia engine not active. Current engine: "
                    + (FontManager.INSTANCE.isSfrActive() ? "SFR" : "vanilla")));
            return;
        }

        SkijaTextRenderer renderer = FontManager.INSTANCE.getSkijaTextRenderer();
        if (renderer == null) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "SkijaTextRenderer is null"));
            return;
        }

        String[] families = renderer.getFontFamilies();
        if (families == null) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "fontFamilies is null"));
            return;
        }

        sender.sendMessage(new TextComponentString(TextFormatting.AQUA + "fontFamilies count: " + families.length));
        for (int i = 0; i < families.length; i++) {
            String family = families[i];
            sender.sendMessage(new TextComponentString(
                    TextFormatting.WHITE + "  [" + i + "] " + TextFormatting.GREEN + family));
        }

        // Check what FontMgr.getDefault() can find
        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== System FontMgr Resolution ==="));
        try {
            FontMgr defaultMgr = FontMgr.getDefault();

            // Check each configured family against system fonts
            sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== Configured Family Resolution ==="));
            for (String family : families) {
                try {
                    Typeface tf = defaultMgr.matchFamilyStyle(family, FontStyle.NORMAL);
                    if (tf != null) {
                        String resolvedName = tf.getFamilyName();
                        sender.sendMessage(new TextComponentString(
                                TextFormatting.WHITE + "  " + family + " -> "
                                        + TextFormatting.GREEN + resolvedName));
                        tf.close();
                    } else {
                        sender.sendMessage(new TextComponentString(
                                TextFormatting.WHITE + "  " + family + " -> "
                                        + TextFormatting.RED + "NOT FOUND"));
                    }
                } catch (Throwable e) {
                    sender.sendMessage(new TextComponentString(
                            TextFormatting.WHITE + "  " + family + " -> "
                                    + TextFormatting.RED + "ERROR: " + e.getMessage()));
                }
            }
        } catch (Throwable t) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Error querying FontMgr: " + t.getMessage()));
        }
    }

    private void executeInfo(ICommandSender sender) {
        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== Engine Status ==="));
        sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  SFR active: " + FontManager.INSTANCE.isSfrActive()));
        sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  Skia active: " + FontManager.INSTANCE.isSkiaActive()));
        sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  Engine config: " + neofontrender.core.config.NeofontrenderConfig.renderingEngine()));
        sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  Enabled: " + neofontrender.core.config.NeofontrenderConfig.enabled()));
        sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  Font name: " + neofontrender.core.config.NeofontrenderConfig.fontName()));
        sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  Font size: " + neofontrender.core.config.NeofontrenderConfig.fontSize()));
        sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  Oversample: " + neofontrender.core.config.NeofontrenderConfig.fontOversample()));
    }

    private void executeReload(ICommandSender sender) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getResourceManager() != null) {
            FontManager.INSTANCE.reload(mc.getResourceManager());
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Fonts reloaded."));
        } else {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Resource manager not available."));
        }
    }

    private void executeTest(ICommandSender sender) {
        if (!FontManager.INSTANCE.isSkiaActive()) {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Skia engine not active."));
            return;
        }
        SkijaTextRenderer renderer = FontManager.INSTANCE.getSkijaTextRenderer();
        if (renderer == null) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "SkijaTextRenderer is null"));
            return;
        }

        String[] testCases = {
                "Hello",
                "😀",
                "Hello 😀 World",
                "❤️",
                "Test 😀 ❤️ 🎉"
        };

        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== Emoji Render Test ==="));
        for (String test : testCases) {
            float width = renderer.measure(test, false, false);
            SkijaTextRenderer.RenderedText rendered = renderer.render(test, 0xFFFFFFFF, false, false);
            boolean success = rendered.advance() > 0;
            sender.sendMessage(new TextComponentString(
                    TextFormatting.WHITE + "  \"" + test + "\""
                            + TextFormatting.AQUA + " width=" + String.format("%.1f", width)
                            + (success ? TextFormatting.GREEN + " OK" : TextFormatting.RED + " EMPTY")));
        }
    }

    private void executeGui(ICommandSender sender) {
        Minecraft.getMinecraft().addScheduledTask(() -> NeofontrenderEmojiTestScreen.open());
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            if ("fonts".startsWith(prefix)) completions.add("fonts");
            if ("info".startsWith(prefix)) completions.add("info");
            if ("reload".startsWith(prefix)) completions.add("reload");
            return completions;
        }
        return Collections.emptyList();
    }
}
