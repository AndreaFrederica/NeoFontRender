package neofontrender.addons.chat;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

/** Persistent options for vanilla chat history and session restoration. */
final class EnhancedChatConfig {
    static boolean enabled = true;
    static boolean extendedHistory = true;
    static int maxMessages = 16384;
    static boolean persistence = true;
    static boolean persistReceived = true;
    static boolean persistSent = true;
    static boolean playerHeads = true;
    static boolean headShadow = true;
    static boolean itemIcons = true;
    static boolean copySelection = true;
    static boolean copyFormattingCodes = false;
    static boolean ampersandFormatting = false;
    static boolean tabbedChat = true;
    static boolean sourceClassification = true;
    static boolean blockPlayerMessages = false;
    static boolean blockServerMessages = false;
    static boolean blockPrivateMessages = false;
    static boolean mentionCompletion = true;
    static boolean mentionNotification = true;
    static boolean messageSearch = true;
    static boolean commandCompletion = true;
    static boolean privateCommandBlock = true;
    static boolean salutationForceServerTranslations = false;
    static boolean salutationDisableOverride = false;
    static boolean verticalTabs = false;
    static boolean persistentChatHud = false;
    static boolean closeChatOnDetach = true;
    static String playerSourcePattern = "";
    static String serverSourcePattern = "";
    static String privateSourcePattern = "";
    static String blockedMessagePattern = "";
    static String mutedPlayers = "";
    static String mentionSound = "random.orb";
    static String privateMessageCommand = "/msg";
    static String pinnedTabs = "";
    static boolean keepOpenPublic = false;
    static boolean keepOpenPlayer = false;
    static boolean keepOpenServer = false;
    static boolean keepOpenPrivate = false;
    static boolean keepOpenCustom = false;
    static boolean animateMessages = true;
    static int messageAnimationDuration = 150;
    static float messageAnimationDistance = 7.0F;
    static String messageAnimationEasing = "sine";
    static boolean animateInput = true;
    static int inputAnimationDuration = 170;
    static float inputAnimationDistance = 8.0F;
    static String inputAnimationEasing = "back";

    private EnhancedChatConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("chat.enabled", true, "Master switch for vanilla chat enhancements.")
                .define("chat.extendedHistory", true, "Increase vanilla's 100-message history limit.")
                .define("chat.maxMessages", 16384, "Maximum retained received and sent messages (100-32767).")
                .define("chat.persistence", true, "Restore chat for the same world or server across reconnects and game restarts.")
                .define("chat.persistReceived", true, "Persist received components with formatting and events.")
                .define("chat.persistSent", true, "Persist sent-message and command history.")
                .define("chat.playerHeads", true, "Display cached player heads next to chat messages.")
                .define("chat.playerHeadShadow", true, "Draw a one-pixel shadow behind chat heads.")
                .define("chat.itemIcons", true, "Display item icons beside SHOW_ITEM chat components.")
                .define("chat.copySelection", true, "Copy chat text by dragging over it while chat is open.")
                .define("chat.copyFormattingCodes", false, "Include Minecraft formatting codes in copied text.")
                .define("chat.copyAmpersandFormatting", false, "Write copied formatting codes with & instead of section signs.")
                .define("chat.tabbedChat", true, "Use the embedded TabbyChat interface when no external TabbyChat is loaded.")
                .define("chat.rules.sourceClassification", true, "Classify received messages into player, server, private and group tabs.")
                .define("chat.rules.blockPlayerMessages", false, "Block messages classified as player chat.")
                .define("chat.rules.blockServerMessages", false, "Block messages classified as server/system chat.")
                .define("chat.rules.blockPrivateMessages", false, "Block messages classified as private chat.")
                .define("chat.rules.mentionCompletion", true, "Suggest online player names after @ in chat.")
                .define("chat.rules.mentionNotification", true, "Play a sound when your name is mentioned.")
                .define("chat.rules.messageSearch", true, "Enable the in-chat message search panel.")
                .define("chat.rules.commandCompletion", true, "Enable command completion suggestions for the embedded chat.")
                .define("chat.rules.privateCommandBlock", true, "Show the private command as a removable fixed block in PM inputs.")
                .define("chat.rules.salutationForceServerTranslations", false,
                        "Use server-side Salutation translations even when UIE is installed.")
                .define("chat.rules.salutationDisableOverride", false,
                        "Keep vanilla GuiChat instead of Salutation's original chat screen when TabbyChat is disabled.")
                .define("chat.tabs.vertical", false, "Use an Edge-style vertical tab tray on the left edge of the chat window.")
                .define("chat.rules.playerSourcePattern", "", "Regex used to classify player chat.")
                .define("chat.rules.serverSourcePattern", "", "Regex used to classify server/system chat.")
                .define("chat.rules.privateSourcePattern", "", "Regex used to classify private chat.")
                .define("chat.rules.blockedMessagePattern", "", "Regex that blocks matching messages.")
                .define("chat.rules.mutedPlayers", "", "Comma-separated player names whose messages are muted.")
                .define("chat.rules.mentionSound", "random.orb", "Sound played for mention notifications.")
                .define("chat.rules.privateMessageCommand", "/msg", "Command template used for private messages.")
                .define("chat.rules.pinnedTabs", "", "Comma-separated TabbyChat channel names pinned in the tray.")
                .define("chat.rules.keepOpenPublic", false, "Keep chat open after sending to public channels.")
                .define("chat.rules.keepOpenPlayer", false, "Keep chat open after sending to player channels.")
                .define("chat.rules.keepOpenServer", false, "Keep chat open after sending to server channels.")
                .define("chat.rules.keepOpenPrivate", false, "Keep chat open after sending to private channels.")
                .define("chat.rules.keepOpenCustom", false, "Keep chat open after sending to custom channels.")
                .define("chat.hud.persistent", false, "Keep the embedded chat rendered as a detached HUD window.")
                .define("chat.hud.closeOnDetach", true, "Close the chat screen when the detached HUD is enabled.")
                .define("chat.animation.messages", true, "Animate newly received messages.")
                .define("chat.animation.messageDuration", 150, "Message entrance duration in milliseconds.")
                .define("chat.animation.messageDistance", 7.0D, "Message entrance distance in GUI pixels.")
                .define("chat.animation.messageEasing", "sine", "Message entrance easing.")
                .define("chat.animation.input", true, "Animate the chat input when opening chat.")
                .define("chat.animation.inputDuration", 170, "Input entrance duration in milliseconds.")
                .define("chat.animation.inputDistance", 8.0D, "Input entrance distance in GUI pixels.")
                .define("chat.animation.inputEasing", "back", "Input entrance easing.");
        enabled = file.getBoolean("chat.enabled", true);
        extendedHistory = file.getBoolean("chat.extendedHistory", true);
        maxMessages = file.getInt("chat.maxMessages", 16384, 100, 32767);
        persistence = file.getBoolean("chat.persistence", true);
        persistReceived = file.getBoolean("chat.persistReceived", true);
        persistSent = file.getBoolean("chat.persistSent", true);
        playerHeads = file.getBoolean("chat.playerHeads", true);
        headShadow = file.getBoolean("chat.playerHeadShadow", true);
        itemIcons = file.getBoolean("chat.itemIcons", true);
        copySelection = file.getBoolean("chat.copySelection", true);
        copyFormattingCodes = file.getBoolean("chat.copyFormattingCodes", false);
        ampersandFormatting = file.getBoolean("chat.copyAmpersandFormatting", false);
        tabbedChat = file.getBoolean("chat.tabbedChat", true);
        sourceClassification = file.getBoolean("chat.rules.sourceClassification", true);
        blockPlayerMessages = file.getBoolean("chat.rules.blockPlayerMessages", false);
        blockServerMessages = file.getBoolean("chat.rules.blockServerMessages", false);
        blockPrivateMessages = file.getBoolean("chat.rules.blockPrivateMessages", false);
        mentionCompletion = file.getBoolean("chat.rules.mentionCompletion", true);
        mentionNotification = file.getBoolean("chat.rules.mentionNotification", true);
        messageSearch = file.getBoolean("chat.rules.messageSearch", true);
        commandCompletion = file.getBoolean("chat.rules.commandCompletion", true);
        privateCommandBlock = file.getBoolean("chat.rules.privateCommandBlock", true);
        salutationForceServerTranslations =
                file.getBoolean("chat.rules.salutationForceServerTranslations", false);
        salutationDisableOverride = file.getBoolean("chat.rules.salutationDisableOverride", false);
        verticalTabs = file.getBoolean("chat.tabs.vertical", false);
        persistentChatHud = file.getBoolean("chat.hud.persistent", false);
        closeChatOnDetach = file.getBoolean("chat.hud.closeOnDetach", true);
        playerSourcePattern = file.getString("chat.rules.playerSourcePattern", "");
        serverSourcePattern = file.getString("chat.rules.serverSourcePattern", "");
        privateSourcePattern = file.getString("chat.rules.privateSourcePattern", "");
        blockedMessagePattern = file.getString("chat.rules.blockedMessagePattern", "");
        mutedPlayers = file.getString("chat.rules.mutedPlayers", "");
        mentionSound = file.getString("chat.rules.mentionSound", "random.orb");
        privateMessageCommand = file.getString("chat.rules.privateMessageCommand", "/msg");
        pinnedTabs = file.getString("chat.rules.pinnedTabs", "");
        keepOpenPublic = file.getBoolean("chat.rules.keepOpenPublic", false);
        keepOpenPlayer = file.getBoolean("chat.rules.keepOpenPlayer", false);
        keepOpenServer = file.getBoolean("chat.rules.keepOpenServer", false);
        keepOpenPrivate = file.getBoolean("chat.rules.keepOpenPrivate", false);
        keepOpenCustom = file.getBoolean("chat.rules.keepOpenCustom", false);
        animateMessages = file.getBoolean("chat.animation.messages", true);
        messageAnimationDuration = file.getInt("chat.animation.messageDuration", 150, 10, 1000);
        messageAnimationDistance = (float) file.getDouble("chat.animation.messageDistance", 7.0D, 0.0D, 32.0D);
        messageAnimationEasing = file.getString("chat.animation.messageEasing", "sine");
        animateInput = file.getBoolean("chat.animation.input", true);
        inputAnimationDuration = file.getInt("chat.animation.inputDuration", 170, 10, 1000);
        inputAnimationDistance = (float) file.getDouble("chat.animation.inputDistance", 8.0D, 0.0D, 32.0D);
        inputAnimationEasing = file.getString("chat.animation.inputEasing", "back");
        file.save();
    }

    static void save() {
        UiEnhancementsConfig.file()
                .set("chat.enabled", enabled)
                .set("chat.extendedHistory", extendedHistory)
                .set("chat.maxMessages", maxMessages)
                .set("chat.persistence", persistence)
                .set("chat.persistReceived", persistReceived)
                .set("chat.persistSent", persistSent)
                .set("chat.playerHeads", playerHeads)
                .set("chat.playerHeadShadow", headShadow)
                .set("chat.itemIcons", itemIcons)
                .set("chat.copySelection", copySelection)
                .set("chat.copyFormattingCodes", copyFormattingCodes)
                .set("chat.copyAmpersandFormatting", ampersandFormatting)
                .set("chat.tabbedChat", tabbedChat)
                .set("chat.rules.sourceClassification", sourceClassification)
                .set("chat.rules.blockPlayerMessages", blockPlayerMessages)
                .set("chat.rules.blockServerMessages", blockServerMessages)
                .set("chat.rules.blockPrivateMessages", blockPrivateMessages)
                .set("chat.rules.mentionCompletion", mentionCompletion)
                .set("chat.rules.mentionNotification", mentionNotification)
                .set("chat.rules.messageSearch", messageSearch)
                .set("chat.rules.commandCompletion", commandCompletion)
                .set("chat.rules.privateCommandBlock", privateCommandBlock)
                .set("chat.rules.salutationForceServerTranslations", salutationForceServerTranslations)
                .set("chat.rules.salutationDisableOverride", salutationDisableOverride)
                .set("chat.tabs.vertical", verticalTabs)
                .set("chat.hud.persistent", persistentChatHud)
                .set("chat.hud.closeOnDetach", closeChatOnDetach)
                .set("chat.rules.playerSourcePattern", playerSourcePattern)
                .set("chat.rules.serverSourcePattern", serverSourcePattern)
                .set("chat.rules.privateSourcePattern", privateSourcePattern)
                .set("chat.rules.blockedMessagePattern", blockedMessagePattern)
                .set("chat.rules.mutedPlayers", mutedPlayers)
                .set("chat.rules.mentionSound", mentionSound)
                .set("chat.rules.privateMessageCommand", privateMessageCommand)
                .set("chat.rules.pinnedTabs", pinnedTabs)
                .set("chat.rules.keepOpenPublic", keepOpenPublic)
                .set("chat.rules.keepOpenPlayer", keepOpenPlayer)
                .set("chat.rules.keepOpenServer", keepOpenServer)
                .set("chat.rules.keepOpenPrivate", keepOpenPrivate)
                .set("chat.rules.keepOpenCustom", keepOpenCustom)
                .set("chat.animation.messages", animateMessages)
                .set("chat.animation.messageDuration", messageAnimationDuration)
                .set("chat.animation.messageDistance", messageAnimationDistance)
                .set("chat.animation.messageEasing", messageAnimationEasing)
                .set("chat.animation.input", animateInput)
                .set("chat.animation.inputDuration", inputAnimationDuration)
                .set("chat.animation.inputDistance", inputAnimationDistance)
                .set("chat.animation.inputEasing", inputAnimationEasing)
                .save();
        speiger.src.salutation.Salutation.initialize();
        ChatHistoryManager.INSTANCE.configChanged();
        ChatRuntimeController.sync();
        ChatRuntimeController.refreshLayout();
    }
}
