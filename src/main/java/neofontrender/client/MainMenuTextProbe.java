package neofontrender.client;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Reads the live CMM/FancyMenu 2.x layout; no JSON parsing or disk access during rendering. */
final class MainMenuTextProbe {
    private MainMenuTextProbe() {}

    static List<MainMenuBrandingLayout.Bounds> cmm(GuiScreen screen, FontRenderer font) {
        List<MainMenuBrandingLayout.Bounds> bounds = new ArrayList<>();
        try {
            Object labels = field(screen, "textLabels");
            if (!(labels instanceof Iterable<?>)) return bounds;
            for (Object label : (Iterable<?>) labels) {
                Object config = field(label, "text");
                Object source = field(config, Boolean.TRUE.equals(field(label, "hovered")) ? "hoverText" : "text");
                if (source == null) continue;
                String text = (String) source.getClass().getMethod("get").invoke(source);
                if (text == null || text.isEmpty()) continue;
                float scale = ((Number) field(config, "fontSize")).floatValue();
                if (!Float.isFinite(scale) || scale <= 0) continue;
                int x = ((Number) field(label, "posX")).intValue();
                int y = ((Number) field(label, "posY")).intValue();
                String anchor = String.valueOf(field(config, "anchor"));
                int row = 0;
                for (String line : text.split("\n")) {
                    String rendered = (String) Class.forName("lumien.custommainmenu.lib.StringReplacer")
                            .getMethod("replacePlaceholders", String.class).invoke(null, line);
                    int w = font.getStringWidth(rendered);
                    int offset = "END".equals(anchor) ? -w : "MIDDLE".equals(anchor) ? -(w / 2) : 0;
                    if (w > 0) bounds.add(new MainMenuBrandingLayout.Bounds(
                            (int) Math.floor(x + offset * scale), (int) Math.floor(y + row * font.FONT_HEIGHT * scale),
                            (int) Math.ceil(w * scale), (int) Math.ceil(font.FONT_HEIGHT * scale)));
                    row++;
                }
            }
        } catch (ReflectiveOperationException | LinkageError | ClassCastException ignored) {
            // Unsupported optional-mod versions fall back to the bottom margin.
        }
        return bounds;
    }

    static List<MainMenuBrandingLayout.Bounds> fancy(GuiScreen screen, FontRenderer font) {
        List<MainMenuBrandingLayout.Bounds> bounds = new ArrayList<>();
        try {
            Object handler = Class.forName("de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerRegistry")
                    .getMethod("getHandlerFor", GuiScreen.class).invoke(null, screen);
            if (handler != null && Boolean.TRUE.equals(field(handler, "showBranding"))) {
                List<String> lines = FMLCommonHandler.instance().getBrandings(true);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(lines.size() - 1 - i);
                    if (line != null && !line.isEmpty()) bounds.add(new MainMenuBrandingLayout.Bounds(
                            2, screen.height - (10 + i * (font.FONT_HEIGHT + 1)),
                            font.getStringWidth(line), font.FONT_HEIGHT));
                }
            }
            Object config = Class.forName("de.keksuccino.fancymenu.FancyMenu").getField("config").get(null);
            String position = String.valueOf(config.getClass().getMethod("getOrDefault", String.class, Object.class)
                    .invoke(config, "copyrightposition", "bottom-right")).toLowerCase(Locale.ROOT);
            int w = font.getStringWidth("Copyright Mojang AB. Do not distribute!");
            int x = position.endsWith("left") ? 2 : position.endsWith("centered")
                    ? screen.width / 2 - w / 2 : screen.width - w - 2;
            int y = position.startsWith("top-") ? 2 : screen.height - 12;
            bounds.add(new MainMenuBrandingLayout.Bounds(x, y, w, 10));
        } catch (ReflectiveOperationException | LinkageError | ClassCastException ignored) {
            // Keep any successfully collected branding rows.
        }
        return bounds;
    }

    private static Object field(Object owner, String name) throws ReflectiveOperationException {
        if (owner == null) throw new NoSuchFieldException(name);
        for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(owner);
            } catch (NoSuchFieldException ignored) {
                // Optional mods keep some layout properties on a superclass.
            }
        }
        throw new NoSuchFieldException(name);
    }
}
