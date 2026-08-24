package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiKeyBindingList;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiKeyBindingList.KeyEntry.class)
public interface AccessorGuiKeyBindingEntryNavigation {
    @Accessor("keybinding") KeyBinding nfrUi$getKeyBinding();
    @Accessor("btnChangeKeyBinding") GuiButton nfrUi$getChangeButton();
    @Accessor("btnReset") GuiButton nfrUi$getResetButton();
}
