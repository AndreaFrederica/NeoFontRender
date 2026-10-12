package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * State bridge for {@code neofontrender.addons.worldcreation.CreateWorldScreen}.
 *
 * <p>MCP never assigned readable names to GuiCreateWorld's internals in 1.7.10, so the members
 * below keep their SRG names; each one is annotated with the role it has in vanilla.</p>
 */
@Mixin(GuiCreateWorld.class)
public interface GuiCreateWorldAccessor {
    @Accessor("field_146332_f") // parent screen
    GuiScreen nfrUi$getParentScreen();

    @Accessor("field_146333_g") // world name field
    GuiTextField nfrUi$getWorldNameField();

    @Accessor("field_146335_h") // world seed field
    GuiTextField nfrUi$getWorldSeedField();

    @Accessor("field_146330_J") // world name kept for re-display
    void nfrUi$setWorldName(String value);

    @Accessor("field_146329_I") // world seed kept for re-display
    void nfrUi$setWorldSeed(String value);

    @Accessor("field_146342_r") // game mode name ("survival" / "hardcore" / "creative")
    String nfrUi$getGameMode();

    @Accessor("field_146341_s") // map features
    boolean nfrUi$getGenerateStructuresEnabled();

    @Accessor("field_146340_t") // allow commands
    boolean nfrUi$getAllowCheats();

    @Accessor("field_146338_v") // bonus chest
    boolean nfrUi$getBonusChestEnabled();

    @Accessor("field_146337_w") // hardcore
    boolean nfrUi$getHardCoreMode();

    @Accessor("field_146331_K") // selected world type index
    int nfrUi$getSelectedIndex();

    @Invoker("actionPerformed")
    void nfrUi$performAction(GuiButton button);

    /** 1.7.10 name of the private re-derive-save-directory method. */
    @Invoker("func_146314_g")
    void nfrUi$calcSaveDirName();

    /** 1.7.10 name of the private toggleMoreWorldOptions method. */
    @Invoker("func_146315_i")
    void nfrUi$toggleMoreWorldOptions();
}
