package speiger.src.salutation.client.gui.chat;

import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.IChatComponent;
import speiger.src.salutation.common.utils.TranslateUtils;

public class MultilineChatScreen extends GuiNewChat {

	public MultilineChatScreen() {
		super(Minecraft.getMinecraft());
	}

	@Override
	public void printChatMessageWithOptionalDeletion(IChatComponent chatComponent, int chatLineId) {
		if(chatLineId != 0) {
			super.printChatMessageWithOptionalDeletion(chatComponent, chatLineId);
			return;
		}
		split(chatComponent, T -> super.printChatMessageWithOptionalDeletion(T, chatLineId));
	}

	public static void split(IChatComponent input, Consumer<IChatComponent> output) {
		IChatComponent currentOutput = TranslateUtils.empty();
		for(Object raw : input) {
			IChatComponent component = (IChatComponent)raw;
			boolean hasSplit = false;
			String originalText = component.getUnformattedText();
			if("\\n".equals(originalText)) {
				output.accept(currentOutput);
				currentOutput = TranslateUtils.empty();
			}
			else {
				for(String text : originalText.split("\\n")) {
					if(hasSplit) {
						output.accept(currentOutput);
						currentOutput = TranslateUtils.empty();
						hasSplit = false;
					}
					currentOutput.appendSibling(TranslateUtils.literal(text).setChatStyle(component.getChatStyle()));
				}
			}
		}
		if(currentOutput.getSiblings().size() > 0) {
			output.accept(currentOutput);
		}
	}
}
