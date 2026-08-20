package neofontrender.addons.navigation.vanilla;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Finds standard controls directly owned by a vanilla-style composite list entry. */
final class VanillaControlIntrospector {
    private static final Map<Class<?>, List<FieldRef>> FIELDS = new HashMap<>();

    private VanillaControlIntrospector() {}

    static List<ControlRef> controls(Object owner) {
        if (owner == null) return Collections.emptyList();
        List<ControlRef> result = new ArrayList<>();
        Set<Gui> seen = Collections.newSetFromMap(new IdentityHashMap<Gui, Boolean>());
        for (FieldRef ref : fields(owner.getClass())) {
            Gui widget = ref.read(owner);
            if (widget != null && seen.add(widget)) result.add(new ControlRef(ref.path, widget));
        }
        return result;
    }

    private static synchronized List<FieldRef> fields(Class<?> ownerType) {
        List<FieldRef> cached = FIELDS.get(ownerType);
        if (cached != null) return cached;
        List<FieldRef> found = new ArrayList<>();
        for (Class<?> type = ownerType; type != null && type != Object.class;
             type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || !supported(field.getType())) continue;
                try {
                    field.setAccessible(true);
                    found.add(new FieldRef(type.getName() + "#" + field.getName(), field));
                } catch (RuntimeException ignored) {
                    // An inaccessible optional field does not invalidate the other row controls.
                }
            }
        }
        found.sort(Comparator.comparing(ref -> ref.path));
        List<FieldRef> immutable = Collections.unmodifiableList(found);
        FIELDS.put(ownerType, immutable);
        return immutable;
    }

    private static boolean supported(Class<?> type) {
        return Gui.class.isAssignableFrom(type);
    }

    static final class ControlRef {
        final String path;
        final Gui widget;

        private ControlRef(String path, Gui widget) {
            this.path = path;
            this.widget = widget;
        }
    }

    private static final class FieldRef {
        private final String path;
        private final Field field;

        private FieldRef(String path, Field field) {
            this.path = path;
            this.field = field;
        }

        private Gui read(Object owner) {
            try {
                Object value = field.get(owner);
                return value instanceof GuiButton || value instanceof GuiTextField
                        ? (Gui) value : null;
            } catch (IllegalAccessException | RuntimeException ignored) {
                return null;
            }
        }
    }
}
