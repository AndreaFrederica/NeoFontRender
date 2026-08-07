package speiger.src.salutation.common.utils;

import net.minecraft.event.HoverEvent;
import net.minecraft.event.HoverEvent.Action;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import speiger.src.salutation.Salutation;

public class TranslateUtils {
	private static boolean forceServerTranslate() {
		return Salutation.FORCE_SERVER_TRANSLATIONS.get();
	}

	public static IChatComponent empty() {
		return new ChatComponentText("");
	}

	public static IChatComponent literal(String text) {
		return new ChatComponentText(text);
	}

	public static IChatComponent translate(String text, Object...args) {
		return forceServerTranslate() ? serverTranslate(new ChatComponentTranslation(text, args)) : new ChatComponentTranslation(text, args);
	}

	public static IChatComponent serverTranslate(IChatComponent input) {
		IChatComponent output = empty();
		for(Object raw : input) {
			IChatComponent T = (IChatComponent)raw;
			output.appendSibling(literal(T.getUnformattedText()).setChatStyle(validateStyle(T.getChatStyle())));
		}
		return output;
	}

	public static IChatComponent applyTextStyle(IChatComponent text, EnumChatFormatting...formatting) {
		ChatStyle style = text.getChatStyle();
		for(EnumChatFormatting format : formatting) {
			if(format.isColor()) {
				style.setColor(format);
				continue;
			}
			else if(format == EnumChatFormatting.OBFUSCATED) {
				style.setObfuscated(true);
			}
			else if(format == EnumChatFormatting.BOLD) {
				style.setBold(true);
			}
			else if(format == EnumChatFormatting.STRIKETHROUGH) {
				style.setStrikethrough(true);
			}
			else if(format == EnumChatFormatting.UNDERLINE) {
				style.setUnderlined(true);
			}
			else if(format == EnumChatFormatting.ITALIC) {
				style.setItalic(true);
			}
			if(format == EnumChatFormatting.RESET) {
				text.setChatStyle(new ChatStyle());
				style = text.getChatStyle();
			}
		}
		return text;
	}

	private static ChatStyle validateStyle(ChatStyle style) {
		HoverEvent event = style.getChatHoverEvent();
		if(event != null) {
			IChatComponent value = event.getValue();
			if(value != null) return style.setChatHoverEvent(new HoverEvent(Action.SHOW_TEXT, serverTranslate(value)));
		}
		return style;
	}
}
