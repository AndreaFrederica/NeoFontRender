package neofontrender.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.client.IModGuiFactory;

import java.util.Collections;
import java.util.Set;

/** Shared Forge Mod List integration for NFR and its bundled components. */
public abstract class NfrConfigGuiFactory implements IModGuiFactory {
    @Override
    public final void initialize(Minecraft minecraftInstance) {}

    @Override
    public final boolean hasConfigGui() {
        return true;
    }

    @Override
    public final Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return Collections.emptySet();
    }
}
