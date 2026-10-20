package neofontrender.addons.mainmenu;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * Creates a CMM button only through reflection, so CMM remains an optional runtime dependency.
 * The continue button copies the configured skin from the nearest CMM button in the title menu.
 */
final class CustomMainMenuButtonSkin {
    private static final String CMM_BUTTON = "lumien.custommainmenu.gui.GuiCustomButton";
    private static final String CMM_CONFIG = "lumien.custommainmenu.configuration.GuiConfig";
    private static final String CMM_ELEMENT = "lumien.custommainmenu.configuration.elements.Button";
    private static final String CMM_TEXT = "lumien.custommainmenu.lib.texts.TextString";
    private static final String CMM_ITEXT = "lumien.custommainmenu.lib.texts.IText";
    private static final String CMM_ALIGNMENT = "lumien.custommainmenu.configuration.Alignment";

    private CustomMainMenuButtonSkin() {}

    static boolean isCmmButton(GuiButton button) {
        return button != null && CMM_BUTTON.equals(button.getClass().getName());
    }

    /**
     * Copies CMM's configured texture/colors/sounds from a real CMM button. Returning null means
     * the optional class changed or no usable CMM style source was available, so the caller can
     * safely fall back to the normal UIE button.
     */
    static GuiButton create(GuiScreen screen, LastPlayedTarget target, int x, int y, int width,
                            GuiButton styleSource) {
        if (screen == null || target == null || !isCmmButton(styleSource)) return null;
        try {
            FontRenderer font = screen.mc == null ? null : screen.mc.fontRenderer;
            if (font == null) return null;
            String label = MainMenuContinueButton.fit(font,
                    MainMenuContinueButton.fullLabel(target), width - 8);
            Class<?> configType = Class.forName(CMM_CONFIG);
            Class<?> elementType = Class.forName(CMM_ELEMENT);
            Class<?> textType = Class.forName(CMM_TEXT);
            Class<?> iTextType = Class.forName(CMM_ITEXT);
            Class<?> alignmentType = Class.forName(CMM_ALIGNMENT);
            Object config = field(screen, "guiConfig");
            Object text = textType.getConstructor(String.class).newInstance(label);
            Object alignment = alignmentType.getConstructor(float.class, float.class)
                    .newInstance(0.0F, 0.0F);
            Constructor<?> elementConstructor = elementType.getConstructor(
                    configType, iTextType, int.class, int.class, int.class, int.class, alignmentType);
            Object element = elementConstructor.newInstance(
                    config, text, x, y, width, 20, alignment);
            field(element, "name").set(element, "neofontrender_continue");
            copyStyle(styleSource, element);

            Constructor<?> buttonConstructor = Class.forName(CMM_BUTTON).getConstructor(
                    int.class, elementType);
            return (GuiButton) buttonConstructor.newInstance(MainMenuContinueButton.BUTTON_ID, element);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    private static void copyStyle(GuiButton source, Object destination) throws ReflectiveOperationException {
        Object sourceElement = field(source, "b").get(source);
        for (String name : new String[]{
                "texture", "normalTextColor", "hoverTextColor", "shadow", "pressSound",
                "hoverSound", "textOffsetX", "textOffsetY", "imageWidth", "imageHeight"}) {
            Field sourceField = field(sourceElement, name);
            field(destination, name).set(destination, sourceField.get(sourceElement));
        }
    }

    private static Field field(Object owner, String name) throws ReflectiveOperationException {
        if (owner == null) throw new NoSuchFieldException(name);
        return field(owner.getClass(), name);
    }

    private static Field field(Class<?> owner, String name) throws ReflectiveOperationException {
        for (Class<?> type = owner; type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                // Optional CMM versions move fields between the button and element superclass.
            }
        }
        throw new NoSuchFieldException(name);
    }
}
