package speiger.src.salutation.client.gui.chat;

import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.text.ITextComponent;
import speiger.src.salutation.common.utils.TranslateUtils;

public class MultilineChatScreen extends GuiNewChat {
	
	public MultilineChatScreen() {
		super(Minecraft.getMinecraft());
	}
	
	@Override
	public void printChatMessageWithOptionalDeletion(ITextComponent chatComponent, int chatLineId) {
		if(chatLineId != 0) {
			super.printChatMessageWithOptionalDeletion(chatComponent, chatLineId);
			return;
		}
		split(chatComponent, T -> super.printChatMessageWithOptionalDeletion(T, chatLineId));
	}
	
	public static void split(ITextComponent input, Consumer<ITextComponent> output) {
		ITextComponent currentOutput = TranslateUtils.empty();
		for(ITextComponent component : input) {
			boolean hasSplit = false;
			String originalText = component.getUnformattedComponentText();
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
					currentOutput.appendSibling(TranslateUtils.literal(text).setStyle(component.getStyle()));
				}
			}
		}
		if(currentOutput.getSiblings().size() > 0) {
			output.accept(currentOutput);
		}
	}
}
