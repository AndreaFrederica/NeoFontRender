package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.io.IOException;

@Mixin(GuiCreateWorld.class)
public interface GuiCreateWorldAccessor {
    @Accessor("parentScreen")
    GuiScreen nfrUi$getParentScreen();

    @Accessor("worldNameField")
    GuiTextField nfrUi$getWorldNameField();

    @Accessor("worldSeedField")
    GuiTextField nfrUi$getWorldSeedField();

    @Accessor("worldName")
    void nfrUi$setWorldName(String value);

    @Accessor("worldSeed")
    void nfrUi$setWorldSeed(String value);

    @Accessor("gameMode")
    String nfrUi$getGameMode();

    @Accessor("generateStructuresEnabled")
    boolean nfrUi$getGenerateStructuresEnabled();

    @Accessor("allowCheats")
    boolean nfrUi$getAllowCheats();

    @Accessor("bonusChestEnabled")
    boolean nfrUi$getBonusChestEnabled();

    @Accessor("hardCoreMode")
    boolean nfrUi$getHardCoreMode();

    @Accessor("selectedIndex")
    int nfrUi$getSelectedIndex();

    @Invoker("actionPerformed")
    void nfrUi$performAction(GuiButton button) throws IOException;

    @Invoker("calcSaveDirName")
    void nfrUi$calcSaveDirName();

    @Invoker("toggleMoreWorldOptions")
    void nfrUi$toggleMoreWorldOptions();

    @Invoker("showMoreWorldOptions")
    void nfrUi$showMoreWorldOptions(boolean show);
}
