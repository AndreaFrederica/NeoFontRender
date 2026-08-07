package neofontrender.addons.mixin.tabbychat;

import com.google.common.collect.Lists;
import neofontrender.addons.chat.ChatAnimationController;
import neofontrender.addons.chat.ChatInlineImageInteraction;
import neofontrender.addons.chat.ChatKeepOpenPolicy;
import neofontrender.addons.chat.ChatKeyBindings;
import neofontrender.addons.chat.EnhancedChatConfigAccess;
import neofontrender.addons.chat.ExternalChatCompat;
import neofontrender.addons.vendor.tabbychat.ChatManager;
import neofontrender.addons.vendor.tabbychat.TabbyChat;
import neofontrender.addons.vendor.tabbychat.api.Channel;
import neofontrender.addons.vendor.tabbychat.api.events.ChatScreenEvents.ChatInitEvent;
import neofontrender.addons.vendor.tabbychat.core.GuiNewChatTC;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiComponent;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiText;
import neofontrender.addons.vendor.tabbychat.foundation.ILocation;
import neofontrender.addons.vendor.tabbychat.util.Translation;
import neofontrender.addons.input.TextCursorManager;
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
import org.lwjgl.input.Keyboard;

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
    private boolean nfrUi$explicitInitialText;
    private String nfrUi$historyDraft;
    private Channel nfrUi$historyChannel;

    // Resolved lazily inside the tabbed-chat gate: the vendored TabbyChat is only started when
    // the embedded backend is active, and getInstance() throws before that.
    private TabbyChat tc;

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void onInitialization(CallbackInfo ci) {
        if (!EnhancedChatConfigAccess.tabbedChatEnabled()) return;
        this.nfrUi$explicitInitialText = !this.defaultInputFieldText.isEmpty();

        this.tc = TabbyChat.getInstance();
        this.chatGui = tc.getChatGui();
        this.sentHistoryCursor = chatGui.getSentMessages().size();
        this.chat = chatGui.getChatManager();
        this.nfrUi$historyChannel = this.chat.getActiveChannel();
        this.textBox = chat.getChatBox().getChatInput().getTextField();

        this.componentList.add(chat.getChatBox());
    }

    @Inject(method = "initGui()V", at = @At("RETURN"))
    private void onInitGui(CallbackInfo ci) {
        if (!EnhancedChatConfigAccess.tabbedChatEnabled()) return;
        if (this.textBox == null) this.onInitialization(null);
        this.inputField = this.textBox.getTextField();
        // Re-point lwjgl3ify's TextFieldHandler at the substituted field: vanilla initGui
        // registered the short-lived vanilla field as the IME focus, and replacing it here
        // leaves only a WeakReference that GC collects, starving textBuffer and silently
        // discarding every writeText call. setFocused(true) is a no-op state-wise but still
        // re-registers this long-lived dummy as the focused input.
        this.inputField.setFocused(true);
        chatGui.getBus().post(new ChatInitEvent(that));
        if (!opened) {
            boolean preservePrivateInput = nfrUi$explicitInitialText
                    && "/".equals(defaultInputFieldText)
                    && chat.hasActivePrivateCommandBlock();
            chat.restoreActiveInput(preservePrivateInput ? "" :
                    (nfrUi$explicitInitialText ? defaultInputFieldText : ""));
            this.opened = true;
            updateScreen();
        }
    }

    @Inject(method = "updateScreen()V", at = @At("RETURN"))
    private void onUpdateScreen(CallbackInfo ci) {
        this.componentList.forEach(GuiComponent::updateComponent);
    }

    @Inject(method = "drawScreen(IIF)V", at = @At("HEAD"))
    private void nfrUi$updateTextCursor(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (this.chat == null) return;
        ILocation bounds = this.chat.getChatBox().getChatInput().getActualLocation();
        TextCursorManager.textFieldDrawn(bounds.getXPos(), bounds.getYPos(),
                bounds.getWidth(), bounds.getHeight(), true, true);
        ExternalChatCompat.updateSalutationInput(this.inputField,
                bounds.getXPos(), bounds.getYPos() + Math.round(ChatAnimationController.inputOffset()),
                bounds.getWidth(), bounds.getHeight(),
                bounds.getWidth()
                        / (float) this.chat.getChatBox().getChatInput().getLocation().getWidth());
    }

    @Inject(method = "onGuiClosed()V", at = @At("RETURN"))
    private void onChatClosed(CallbackInfo ci) {
        if (this.chat != null) this.chat.captureActiveDraft();
        ChatInlineImageInteraction.clearTabbyHover();
        this.field_146410_g = "";
        ExternalChatCompat.removeSalutationInput(this.inputField);
        this.componentList.forEach(GuiComponent::onClosed);
    }

    @Override
    public void handleKeyboardInput() {
        super.handleKeyboardInput();
        if (ChatKeyBindings.handledCurrentEvent()) return;
        int key = Keyboard.getEventKey();
        if (key == Keyboard.KEY_UP || key == Keyboard.KEY_DOWN) return;
        // Salutation's ChatScreen already writes this event into our substituted inputField and
        // immediately requests Brigadier completions for that value. Sending the same LWJGL event
        // through TabbyChat's GuiText afterwards types it a second time and makes the completion
        // request stale. Keep our self-drawn chat, but let Salutation own keyboard input while its
        // wrapper is open. Mouse/component drawing remains on the normal TabbyChat path.
        if (ExternalChatCompat.isSalutationChatScreen(that)) return;
        this.componentList.forEach(GuiComponent::handleKeyboardInput);
    }

    @Inject(method = "keyTyped(CI)V", at = @At("HEAD"), cancellable = true)
    private void nfrUi$removePrivateCommandBlock(char key, int code, CallbackInfo ci) {
        if (!EnhancedChatConfigAccess.tabbedChatEnabled() || code != Keyboard.KEY_BACK
                || this.chat == null || this.inputField == null) return;
        if (this.inputField.getCursorPosition() == 0
                && this.inputField.getSelectionEnd() == 0
                && this.chat.removeActivePrivateCommandBlock()) {
            ci.cancel();
        }
    }

    @Inject(method = "keyTyped(CI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiChat;func_146403_a(Ljava/lang/String;)V",
            shift = At.Shift.AFTER), require = 1)
    private void nfrUi$resetPrivateInputAfterSend(char key, int code, CallbackInfo ci) {
        ChatKeyBindings.resetPrivateInputAfterSend(this.inputField);
    }

    @Inject(method = "keyTyped(CI)V", at = @At("HEAD"), cancellable = true)
    private void nfrUi$terminalHistory(char key, int code, CallbackInfo ci) {
        if (!EnhancedChatConfigAccess.tabbedChatEnabled()
                || code != Keyboard.KEY_UP && code != Keyboard.KEY_DOWN) return;
        List<String> history = this.chatGui.getSentMessages();
        int end = history.size();
        Channel activeChannel = this.chat.getActiveChannel();
        if (activeChannel != this.nfrUi$historyChannel) {
            this.nfrUi$historyChannel = activeChannel;
            this.sentHistoryCursor = end;
            this.nfrUi$historyDraft = null;
        }
        this.sentHistoryCursor = Math.max(0, Math.min(this.sentHistoryCursor, end));
        if (code == Keyboard.KEY_UP) {
            if (this.sentHistoryCursor == end) {
                this.nfrUi$historyDraft = this.inputField.getText();
            }
            if (this.sentHistoryCursor > 0) {
                this.sentHistoryCursor--;
                this.inputField.setText(this.chat.activeInputText(history.get(this.sentHistoryCursor)));
                this.inputField.setCursorPositionEnd();
            }
        } else if (this.sentHistoryCursor < end) {
            this.sentHistoryCursor++;
            this.inputField.setText(this.sentHistoryCursor == end
                    ? (this.nfrUi$historyDraft == null ? "" : this.nfrUi$historyDraft)
                    : this.chat.activeInputText(history.get(this.sentHistoryCursor)));
            this.inputField.setCursorPositionEnd();
        }
        ci.cancel();
    }

    @Inject(method = "handleMouseInput()V", at = @At("RETURN"))
    private void onHandleMouseInput(CallbackInfo ci) {
        this.componentList.forEach(GuiComponent::handleMouseInput);
    }

    @Inject(method = "func_146406_a", at = @At("HEAD"), cancellable = true)
    private void limitCompletionFlood(String[] completions, CallbackInfo ci) {
        if (!EnhancedChatConfigAccess.tabbedChatEnabled() || this.chatGui == null) return;
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
        if (!EnhancedChatConfigAccess.tabbedChatEnabled()) return;
        this.chatGui.resetScroll();
        this.chat.clearActiveDraft();
        this.sentHistoryCursor = this.chatGui.getSentMessages().size();
        this.nfrUi$historyDraft = null;
        this.nfrUi$historyChannel = this.chat.getActiveChannel();
        if (ChatKeepOpenPolicy.shouldKeepOpen(this.chat.getActiveChannel())) {
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
        if (!EnhancedChatConfigAccess.tabbedChatEnabled()) drawRect(x1, y1, x2, y2, color);
    }

}
