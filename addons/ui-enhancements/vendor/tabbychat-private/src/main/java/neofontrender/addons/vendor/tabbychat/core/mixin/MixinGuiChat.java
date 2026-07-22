package neofontrender.addons.vendor.tabbychat.core.mixin;

import com.google.common.collect.Lists;
import neofontrender.addons.vendor.tabbychat.ChatManager;
import neofontrender.addons.vendor.tabbychat.TabbyChat;
import neofontrender.addons.vendor.tabbychat.api.Channel;
import neofontrender.addons.vendor.tabbychat.api.events.ChatScreenEvents.ChatInitEvent;
import neofontrender.addons.vendor.tabbychat.core.GuiNewChatTC;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiComponent;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiText;
import neofontrender.addons.vendor.tabbychat.util.Translation;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiChat.class)
public abstract class MixinGuiChat extends GuiScreen {

    private final GuiChat that = (GuiChat) (Object) this;

    protected List<GuiComponent> componentList = Lists.newArrayList();
    private GuiNewChatTC chatGui;
    private ChatManager chat;
    private GuiText textBox;

    @Shadow
    private String field_146410_g;
    @Shadow
    private int sentHistoryCursor;
    @Shadow
    protected GuiTextField inputField;
    @Shadow
    private String defaultInputFieldText;

    private boolean opened;

    private TabbyChat tc = TabbyChat.getInstance();

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void onInitialization(CallbackInfo ci) {

        this.chatGui = tc.getChatGui();
        this.sentHistoryCursor = chatGui.getSentMessages().size();
        this.chat = chatGui.getChatManager();
        this.textBox = chat.getChatBox().getChatInput().getTextField();

        Channel chan = chat.getActiveChannel();
        if (this.defaultInputFieldText.isEmpty()
                && !chan.isPrefixHidden()
                && !chan.getPrefix().isEmpty()) {
            defaultInputFieldText = chan.getPrefix() + " ";
        }

        this.componentList.add(chat.getChatBox());
    }

    @Inject(method = "initGui()V", at = @At("RETURN"))
    private void onInitGui(CallbackInfo ci) {
        if (this.textBox == null) this.onInitialization(null);
        this.inputField = this.textBox.getTextField();
        chatGui.getBus().post(new ChatInitEvent(that));
        if (!opened) {
            textBox.setValue("");
            textBox.getTextField().writeText(defaultInputFieldText);
            this.opened = true;
            updateScreen();
        }
    }

    @Inject(method = "updateScreen()V", at = @At("RETURN"))
    private void onUpdateScreen(CallbackInfo ci) {
        this.componentList.forEach(GuiComponent::updateComponent);
    }

    @Inject(method = "onGuiClosed()V", at = @At("RETURN"))
    private void onChatClosed(CallbackInfo ci) {
        this.field_146410_g = "";
        this.componentList.forEach(GuiComponent::onClosed);
    }

    @Override
    public void handleKeyboardInput() {
        super.handleKeyboardInput();
        this.componentList.forEach(GuiComponent::handleKeyboardInput);
    }

    @Inject(method = "handleMouseInput()V", at = @At("RETURN"))
    private void onHandleMouseInput(CallbackInfo ci) {
        this.componentList.forEach(GuiComponent::handleMouseInput);
    }

    @Inject(method = "func_146406_a", at = @At("HEAD"), cancellable = true)
    private void limitCompletionFlood(String[] completions, CallbackInfo ci) {
        if (completions.length <= 20) {
            return;
        }
        IChatComponent warning = new ChatComponentTranslation(
                Translation.WARN_COMPLETIONS,
                completions.length);
        chatGui.printChatMessageWithOptionalDeletion(warning, 1);
        ci.cancel();
    }

    @Inject(
            method = "keyTyped(CI)V",
            cancellable = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;displayGuiScreen(Lnet/minecraft/client/gui/GuiScreen;)V",
                    ordinal = 1))
    private void keepChatOpen(char key, int code, CallbackInfo ci) {
        this.chatGui.resetScroll();
        this.textBox.setValue(this.defaultInputFieldText);
        this.inputField.setText(this.defaultInputFieldText);
        if (tc.settings.advanced.keepChatOpen.get()) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "drawScreen(IIF)V",
            require = 1,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiChat;drawRect(IIIII)V"))
    private void onDrawScreen(int x1, int y1, int x2, int y2, int color) {
        // noop
    }

}
