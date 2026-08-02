package speiger.src.salutation.common.utils;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.event.HoverEvent.Action;
import speiger.src.salutation.Salutation;

public class TranslateUtils {
	private static boolean forceServerTranslate() {
		return Salutation.FORCE_SERVER_TRANSLATIONS.get();
	}
	
	public static ITextComponent empty() {
		return new TextComponentString("");
	}
	
	public static ITextComponent literal(String text) {
		return new TextComponentString(text);
	}
	
	public static ITextComponent translate(String text, Object...args) {
		return forceServerTranslate() ? serverTranslate(new TextComponentTranslation(text, args)) : new TextComponentTranslation(text, args);
	}
	
	public static ITextComponent serverTranslate(ITextComponent input) {
		ITextComponent output = empty();
		input.forEach(T -> {
			output.appendSibling(literal(T.getUnformattedComponentText()).setStyle(validateStyle(T.getStyle())));
		});
		return output;
	}
	
	public static ITextComponent applyTextStyle(ITextComponent text, TextFormatting...formatting) {
		Style style = text.getStyle();
		for(TextFormatting format : formatting) {
			if(format.isColor()) {
				style.setColor(format);
				continue;
			}
			else if(format == TextFormatting.OBFUSCATED) {
				style.setObfuscated(true);
			}
			else if(format == TextFormatting.BOLD) {
				style.setBold(true);
			}
			else if(format == TextFormatting.STRIKETHROUGH) {
				style.setStrikethrough(true);
			}
			else if(format == TextFormatting.UNDERLINE) {
				style.setUnderlined(true);
			}
			else if(format == TextFormatting.ITALIC) {
				style.setItalic(true);
			}
			if(format == TextFormatting.RESET) {
				text.setStyle(new Style());
				style = text.getStyle();
			}
		}
		return text;
	}
	
	private static Style validateStyle(Style style) {
		HoverEvent event = style.getHoverEvent();
		if(event != null) {
			ITextComponent value = event.getValue();
			if(value != null) return style.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, serverTranslate(value)));
		}
		return style;
	}
}
