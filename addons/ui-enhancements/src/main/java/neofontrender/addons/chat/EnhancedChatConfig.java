package neofontrender.addons.chat;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

final class EnhancedChatConfig {
    static boolean enabled = true;
    static boolean tabbedChat = true;
    static boolean extendedHistory = true;
    static int maxMessages = 16384;
    static boolean persistence = true;
    static boolean persistReceived = true;
    static boolean persistSent = true;
    static boolean messageSearch = true;
    static boolean commandCompletion = true;
    static boolean sourceClassification = true;
    static String playerSourcePattern = "";
    static String serverSourcePattern = "";
    static String privateSourcePattern = "";
    static String blockedMessagePattern = "";
    static String mutedPlayers = "";
    static boolean blockPlayerMessages = false;
    static boolean blockServerMessages = false;
    static boolean blockPrivateMessages = false;
    static boolean mentionCompletion = true;
    static boolean mentionNotification = true;
    static String mentionSound = "minecraft:entity.experience_orb.pickup";
    static String privateMessageCommand = "/msg {player}";
    static boolean privateCommandBlock = true;
    static boolean keepOpenPublic = false;
    static boolean keepOpenPlayer = false;
    static boolean keepOpenServer = false;
    static boolean keepOpenPrivate = false;
    static boolean keepOpenCustom = false;
    static boolean persistentChatHud = false;
    static boolean closeChatOnDetach = false;
    static boolean playerHeads = true;
    static boolean headShadow = true;
    static boolean itemIcons = true;
    static boolean copySelection = true;
    static boolean copyFormattingCodes = false;
    static boolean ampersandFormatting = false;
    static boolean animateMessages = true;
    static int messageAnimationDuration = 150;
    static float messageAnimationDistance = 7.0F;
    static String messageAnimationEasing = "sine";
    static boolean animateInput = true;
    static int inputAnimationDuration = 170;
    static float inputAnimationDistance = 8.0F;
    static String inputAnimationEasing = "back";
    static boolean salutationForceServerTranslations = false;
    static boolean salutationDisableOverride = false;

    private EnhancedChatConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("chat.enabled", true, "Master switch for integrated chat enhancements.")
                .define("chat.tabbedChat", true, "Enable the embedded TabbyChat channel and filter interface.")
                .define("chat.extendedHistory", true, "Increase vanilla's 100-message chat limit.")
                .define("chat.maxMessages", 16384, "Maximum received and sent messages retained (100-32767).")
                .define("chat.persistence", true, "Restore chat for the same world or server across reconnects and game restarts.")
                .define("chat.persistReceived", true, "Persist received chat components with formatting and events.")
                .define("chat.persistSent", true, "Persist sent-message command history.")
                .define("chat.search", true, "Search and filter the current chat history with Ctrl+F.")
                .define("chat.commandCompletion", true,
                        "Show command completions in Salutation and embedded TabbyChat inputs.")
                .define("chat.sources.enabled", true, "Classify messages as player, server or private messages.")
                .define("chat.sources.playerPattern", "", "Regex overriding messages to the player source.")
                .define("chat.sources.serverPattern", "", "Regex overriding messages to the server source.")
                .define("chat.sources.privatePattern", "", "Regex overriding messages to the private source.")
                .define("chat.block.pattern", "", "Regex matching messages that should be hidden.")
                .define("chat.block.players", "", "Comma-separated player names whose messages should be hidden.")
                .define("chat.block.allPlayers", false, "Hide all messages classified as player messages.")
                .define("chat.block.server", false, "Hide all messages classified as server messages.")
                .define("chat.block.private", false, "Hide all messages classified as private messages.")
                .define("chat.mentions.completion", true, "Complete @player tokens from the online player list.")
                .define("chat.mentions.notification", true, "Play a sound when the local player is mentioned.")
                .define("chat.mentions.sound", "minecraft:entity.experience_orb.pickup", "Mention notification sound event.")
                .define("chat.privateMessageCommand", "/msg {player}", "Command template used by the private-message action.")
                .define("chat.privateCommandBlock", true, "Show the private command as a removable fixed block in TabbyChat PM inputs.")
                .define("chat.keepOpen.public", false, "Keep the chat screen open after sending from the public channel.")
                .define("chat.keepOpen.player", false, "Keep the chat screen open after sending from the player-source channel.")
                .define("chat.keepOpen.server", false, "Keep the chat screen open after sending from the server-source channel.")
                .define("chat.keepOpen.private", false, "Keep the chat screen open after sending from private-message channels.")
                .define("chat.keepOpen.custom", false, "Keep the chat screen open after sending from custom channels.")
                .define("chat.hud.persistent", false, "Keep the expanded TabbyChat window visible in the HUD.")
                .define("chat.hud.closeOnDetach", false, "Close the current chat screen immediately after detaching it to the HUD.")
                .define("chat.playerHeads", true, "Display cached player heads next to chat messages.")
                .define("chat.playerHeadShadow", true, "Draw a one-pixel shadow behind chat heads.")
                .define("chat.itemIcons", true, "Display item icons beside SHOW_ITEM chat components.")
                .define("chat.copySelection", true, "Copy chat text by dragging over it while chat is open.")
                .define("chat.copyFormattingCodes", false, "Include Minecraft formatting codes in copied text.")
                .define("chat.copyAmpersandFormatting", false, "Write copied formatting codes with & instead of section signs.")
                .define("chat.animation.messages", true, "Animate newly received messages.")
                .define("chat.animation.messageDuration", 150, "Message entrance duration in milliseconds.")
                .define("chat.animation.messageDistance", 7.0D, "Message entrance distance in GUI pixels.")
                .define("chat.animation.messageEasing", "sine", "Message entrance easing.")
                .define("chat.animation.input", true, "Animate the chat input when opening chat.")
                .define("chat.animation.inputDuration", 170, "Input entrance duration in milliseconds.")
                .define("chat.animation.inputDistance", 8.0D, "Input entrance distance in GUI pixels.")
                .define("chat.animation.inputEasing", "back", "Input entrance easing.")
                .define("chat.salutation.forceServerTranslations", false,
                        "Use server-side Salutation translations even when UIE is installed.")
                .define("chat.salutation.disableOverride", false,
                        "Keep vanilla GuiChat instead of Salutation's original chat screen when TabbyChat is disabled.");
        enabled = file.getBoolean("chat.enabled", true);
        tabbedChat = file.getBoolean("chat.tabbedChat", true);
        extendedHistory = file.getBoolean("chat.extendedHistory", true);
        maxMessages = file.getInt("chat.maxMessages", 16384, 100, 32767);
        persistence = file.getBoolean("chat.persistence", true);
        persistReceived = file.getBoolean("chat.persistReceived", true);
        persistSent = file.getBoolean("chat.persistSent", true);
        messageSearch = file.getBoolean("chat.search", true);
        commandCompletion = file.getBoolean("chat.commandCompletion", true);
        sourceClassification = file.getBoolean("chat.sources.enabled", true);
        playerSourcePattern = file.getString("chat.sources.playerPattern", "");
        serverSourcePattern = file.getString("chat.sources.serverPattern", "");
        privateSourcePattern = file.getString("chat.sources.privatePattern", "");
        blockedMessagePattern = file.getString("chat.block.pattern", "");
        mutedPlayers = file.getString("chat.block.players", "");
        blockPlayerMessages = file.getBoolean("chat.block.allPlayers", false);
        blockServerMessages = file.getBoolean("chat.block.server", false);
        blockPrivateMessages = file.getBoolean("chat.block.private", false);
        mentionCompletion = file.getBoolean("chat.mentions.completion", true);
        mentionNotification = file.getBoolean("chat.mentions.notification", true);
        mentionSound = file.getString("chat.mentions.sound", "minecraft:entity.experience_orb.pickup");
        privateMessageCommand = file.getString("chat.privateMessageCommand", "/msg {player}");
        privateCommandBlock = file.getBoolean("chat.privateCommandBlock", true);
        boolean legacyKeepOpen = file.getBoolean("chat.tabby.layout.keepChatOpen", false);
        keepOpenPublic = file.getBoolean("chat.keepOpen.public", legacyKeepOpen);
        keepOpenPlayer = file.getBoolean("chat.keepOpen.player", legacyKeepOpen);
        keepOpenServer = file.getBoolean("chat.keepOpen.server", legacyKeepOpen);
        keepOpenPrivate = file.getBoolean("chat.keepOpen.private", legacyKeepOpen);
        keepOpenCustom = file.getBoolean("chat.keepOpen.custom", legacyKeepOpen);
        persistentChatHud = file.getBoolean("chat.hud.persistent", false);
        closeChatOnDetach = file.getBoolean("chat.hud.closeOnDetach", false);
        playerHeads = file.getBoolean("chat.playerHeads", true);
        headShadow = file.getBoolean("chat.playerHeadShadow", true);
        itemIcons = file.getBoolean("chat.itemIcons", true);
        copySelection = file.getBoolean("chat.copySelection", true);
        copyFormattingCodes = file.getBoolean("chat.copyFormattingCodes", false);
        ampersandFormatting = file.getBoolean("chat.copyAmpersandFormatting", false);
        animateMessages = file.getBoolean("chat.animation.messages", true);
        messageAnimationDuration = file.getInt("chat.animation.messageDuration", 150, 10, 1000);
        messageAnimationDistance = (float) file.getDouble("chat.animation.messageDistance", 7.0D, 0.0D, 32.0D);
        messageAnimationEasing = file.getString("chat.animation.messageEasing", "sine");
        animateInput = file.getBoolean("chat.animation.input", true);
        inputAnimationDuration = file.getInt("chat.animation.inputDuration", 170, 10, 1000);
        inputAnimationDistance = (float) file.getDouble("chat.animation.inputDistance", 8.0D, 0.0D, 32.0D);
        inputAnimationEasing = file.getString("chat.animation.inputEasing", "back");
        salutationForceServerTranslations = file.getBoolean("chat.salutation.forceServerTranslations", false);
        salutationDisableOverride = file.getBoolean("chat.salutation.disableOverride", false);
        file.save();
    }

    static void save() {
        UiEnhancementsConfig.file().set("chat.enabled", enabled)
                .set("chat.tabbedChat", tabbedChat)
                .set("chat.extendedHistory", extendedHistory)
                .set("chat.maxMessages", maxMessages)
                .set("chat.persistence", persistence)
                .set("chat.persistReceived", persistReceived)
                .set("chat.persistSent", persistSent)
                .set("chat.search", messageSearch)
                .set("chat.commandCompletion", commandCompletion)
                .set("chat.sources.enabled", sourceClassification)
                .set("chat.sources.playerPattern", playerSourcePattern)
                .set("chat.sources.serverPattern", serverSourcePattern)
                .set("chat.sources.privatePattern", privateSourcePattern)
                .set("chat.block.pattern", blockedMessagePattern)
                .set("chat.block.players", mutedPlayers)
                .set("chat.block.allPlayers", blockPlayerMessages)
                .set("chat.block.server", blockServerMessages)
                .set("chat.block.private", blockPrivateMessages)
                .set("chat.mentions.completion", mentionCompletion)
                .set("chat.mentions.notification", mentionNotification)
                .set("chat.mentions.sound", mentionSound)
                .set("chat.privateMessageCommand", privateMessageCommand)
                .set("chat.privateCommandBlock", privateCommandBlock)
                .set("chat.keepOpen.public", keepOpenPublic)
                .set("chat.keepOpen.player", keepOpenPlayer)
                .set("chat.keepOpen.server", keepOpenServer)
                .set("chat.keepOpen.private", keepOpenPrivate)
                .set("chat.keepOpen.custom", keepOpenCustom)
                .set("chat.hud.persistent", persistentChatHud)
                .set("chat.hud.closeOnDetach", closeChatOnDetach)
                .set("chat.playerHeads", playerHeads)
                .set("chat.playerHeadShadow", headShadow)
                .set("chat.itemIcons", itemIcons)
                .set("chat.copySelection", copySelection)
                .set("chat.copyFormattingCodes", copyFormattingCodes)
                .set("chat.copyAmpersandFormatting", ampersandFormatting)
                .set("chat.animation.messages", animateMessages)
                .set("chat.animation.messageDuration", messageAnimationDuration)
                .set("chat.animation.messageDistance", messageAnimationDistance)
                .set("chat.animation.messageEasing", messageAnimationEasing)
                .set("chat.animation.input", animateInput)
                .set("chat.animation.inputDuration", inputAnimationDuration)
                .set("chat.animation.inputDistance", inputAnimationDistance)
                .set("chat.animation.inputEasing", inputAnimationEasing)
                .set("chat.salutation.forceServerTranslations", salutationForceServerTranslations)
                .set("chat.salutation.disableOverride", salutationDisableOverride)
                .save();
        speiger.src.salutation.Salutation.initialize();
        ChatHistoryManager.INSTANCE.configChanged();
        ChatRuntimeController.sync();
        ChatSourceChannels.sync();
        ChatRuntimeController.refreshLayout();
    }
}
