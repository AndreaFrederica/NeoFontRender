package neofontrender.client.gui;

import cpw.mods.fml.client.IModGuiFactory;
import cpw.mods.fml.client.IModGuiFactory.RuntimeOptionCategoryElement;
import cpw.mods.fml.client.IModGuiFactory.RuntimeOptionGuiHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import java.util.Collections;
import java.util.Set;

/** Shared Forge Mod List integration for NFR and its bundled components. */
public abstract class NfrConfigGuiFactory implements IModGuiFactory {
    private static final ThreadLocal<NfrConfigGuiFactory> ACTIVE_FACTORY = new ThreadLocal<>();

    @Override
    public final void initialize(Minecraft minecraftInstance) {}

    @Override
    public final Class<? extends GuiScreen> mainConfigGuiClass() {
        ACTIVE_FACTORY.set(this);
        return NfrConfigScreen.class;
    }

    @Override
    public final Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return Collections.emptySet();
    }

    @Override
    public final RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
        return null;
    }

    /** Kept as the subclass extension point used by NFR and bundled UIE factories. */
    public abstract GuiScreen createConfigGui(GuiScreen parentScreen);

    public static final class NfrConfigScreen extends GuiScreen {
        private final GuiScreen delegate;

        public NfrConfigScreen(GuiScreen parentScreen) {
            NfrConfigGuiFactory factory = ACTIVE_FACTORY.get();
            if (factory == null) {
                throw new IllegalStateException("No active NFR config GUI factory");
            }
            GuiScreen created;
            try {
                created = factory.createConfigGui(parentScreen);
            } finally {
                ACTIVE_FACTORY.remove();
            }
            if (created == null) {
                throw new IllegalStateException("NFR config GUI factory returned no screen");
            }
            this.delegate = created;
        }

        @Override
        public void initGui() {
            Minecraft.getMinecraft().displayGuiScreen(delegate);
        }
    }
}
