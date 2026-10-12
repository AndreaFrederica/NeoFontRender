package neofontrender.addons.cursor;

import javax.annotation.Nullable;

/** Extension point for Mod-specific cursor hit testing. */
@FunctionalInterface
public interface CursorRule {
    @Nullable CursorRequest resolve(CursorContext context);
}
