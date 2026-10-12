package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiKeyBindingList;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiKeyBindingList.KeyEntry.class)
public interface AccessorGuiKeyBindingEntryNavigation {
    // 1.7.10 left the bound key in the unmapped private field field_148282_b.
    @Accessor("field_148282_b") KeyBinding nfrUi$getKeyBinding();
    @Accessor("btnChangeKeyBinding") GuiButton nfrUi$getChangeButton();
    @Accessor("btnReset") GuiButton nfrUi$getResetButton();
}
