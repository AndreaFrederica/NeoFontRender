package speiger.src.salutation.client.gui.chat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.network.play.client.CPacketTabComplete;
import net.minecraft.util.TabCompleter;
import net.minecraft.util.text.TextFormatting;


public abstract class AdvancedTabCompleter extends TabCompleter {
	private static final Pattern WHITESPACE_PATTERN = Pattern.compile("(\\s+)");
	
	protected Minecraft mc = Minecraft.getMinecraft();
	protected ClickBox box = new ClickBox(0, 0, -1, -1);
	protected int offset;
	protected boolean cycle = false;
	protected boolean keepSuggestions = false;
	protected boolean wasFirst = false;
	
	public AdvancedTabCompleter(GuiTextField textField, boolean hasTargetBlock) {
		super(textField, hasTargetBlock);
	}
	
	public boolean onKeyPress(int key) {
		if(hasSuggestions()) {
			if(key == Keyboard.KEY_TAB) {
				complete();
				return true;
			}
			else if(key == Keyboard.KEY_UP) {
				previous();
				return true;
			}
			else if(key == Keyboard.KEY_DOWN) {
				next();
				return true;
			}
		}
		return false;
	}
	
	public boolean onClick(int mouseX, int mouseY) {
		if(box.isInsideBox(mouseX, mouseY)) {
			select(offset + ((mouseY - box.getY()) / 12));
			return true;
		}
		return false;
	}
	
	public boolean onScroll(int mouseX, int mouseY, int scroll) {
		if(box.isInsideBox(mouseX, mouseY)) {
			this.offset += scroll;
			if(offset < 0) offset = 0;
			else if(offset + 10 > completions.size()) offset = Math.max(0, completions.size() - 10);
			return true;
		}
		return false;
	}
	
	public void render(int mouseX, int mouseY, FontRenderer font) {
		int limit = Math.min(completions.size() - offset, 10);
		if(limit > 0) {
			String s = textField.getText();
			int x = Math.min(s.length() <= 0 ? 0 : font.getStringWidth(s.substring(0, Math.max(0, textField.getText().lastIndexOf(" ")) + 1)), textField.getWidth()) + textField.x - 0;
			int width = 0;
			for(int i = 0,m=completions.size();i<m;i++) {
				width = Math.max(width, font.getStringWidth(completions.get(i)));
			}
			int baseY = textField.y - (12 * limit) - 3;
			
			int index = mouseX >= x && mouseX <= x + width ? ((mouseY - baseY) / 12) : -1;
			
			Gui.drawRect(x, baseY, x + width + 5, textField.y - 3, -805306368);
			for(int i = 0;i < limit;i++) {
				font.drawStringWithShadow(completions.get(i + offset), x + 2, baseY + (i * 12) + 2, i == index || i + offset == completionIdx ? -256 : -5592406);
			}
			box.set(x, baseY, width, (textField.y-3) - baseY);
		}
	}
	
	public int getWordIndex(String text) {
		if(text != null && text.length() > 0) {
			int i = 0;
			Matcher matcher = WHITESPACE_PATTERN.matcher(text);
			while(matcher.find()) {
				i = matcher.end();
			}
			return i;
		}
		return 0;
	}
	
	public boolean hasSuggestions() {
		return completions.size() > 0;
	}
	
	@Override
	public void complete() {
		if(completions.isEmpty()) return;
		if(wasFirst) {
			select(completionIdx);
			wasFirst = false;
			return;
		}
		if(GuiScreen.isShiftKeyDown()) previous();
		else next();
	}
	
	public void previous() {
		select(completionIdx == 0 ? completions.size()-1 : completionIdx-1);
	}
	
	public void next() {
		select((++completionIdx) % completions.size());
	}
	
	public void select(int index) {
		if(index < 0 || index >= completions.size()) return;
		setSelection(index);
		updateWord(completions.get(completionIdx));
	}
	
	protected void setSelection(int index) {
		completionIdx = index;
		if(index-9 >= offset) offset = index-9;
		else if(index < offset) offset = index;
	}
	
	public void updateWord(String value) {
        this.textField.deleteFromCursor(0);
        int end = textField.getCursorPosition();
        String text = this.textField.getText().substring(0, end);
        int start = getWordIndex(text);
        if(start == 0 && textField.getText().length() > 1 && end >= 1) start = 1;
		cycle = true;
		StringBuilder builder = new StringBuilder(textField.getText());
		builder.replace(start, end, value);
		value = builder.toString();
		keepSuggestions = true;
		textField.setText(TextFormatting.getTextWithoutFormattingCodes(value));
		textField.setCursorPosition(start + value.length());
		keepSuggestions = false;
	}
	
	public void requestUpdate() {
		if(textField.getText().isEmpty() || textField.getCursorPosition() == 0) {
			completionIdx = 0;
			completions.clear();
			return;
		}
		String prefix = this.textField.getText().substring(0, this.textField.getCursorPosition());
		if(prefix.isEmpty()) {
			prefix = "/";
		}
        net.minecraftforge.client.ClientCommandHandler.instance.autoComplete(prefix);
        Minecraft.getMinecraft().player.connection.sendPacket(new CPacketTabComplete(prefix, this.getTargetBlockPos(), this.hasTargetBlock));
        cycle = false;
	}
	
	@Override
	public void setCompletions(String... newCompl) {
		boolean allStart = true;
		for(int i = 0,m=newCompl.length;i<m;i++) {
			if(newCompl[i].isEmpty()) continue;
			if(!newCompl[i].startsWith("/")) {
				allStart = false;
				break;
			}
		}
		if(allStart) {
			for(int i = 0,m=newCompl.length;i<m;i++) {
				if(newCompl[i].isEmpty()) continue;
				newCompl[i] = newCompl[i].substring(1);
			}
		}
		wasFirst = !cycle || completions.isEmpty();
		completions.clear();
		completionIdx = 0;
		String currentWord = "";
		if(!textField.getText().isEmpty()) {
			int end = textField.getCursorPosition();
			currentWord = textField.getText().substring(0, end);
			int start = getWordIndex(currentWord);
			if(start == 0) start = 1;
			if(start <= currentWord.length()) {
				currentWord = currentWord.substring(start);
			}
		}
		for(int i = 0,m=newCompl.length;i<m;i++) {
			String value = newCompl[i];
			if(value.isEmpty() || value.equals(currentWord)) continue;
			completions.add(value);
		}
		offset = 0;
	}
	
	public static class ClickBox {
		int x;
		int y;
		int width;
		int height;
		
		public ClickBox(int x, int y, int width, int height) {
			set(x, y, width, height);
		}
		
		public void set(int x, int y, int width, int height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}
		
		public boolean isInsideBox(int xPos, int yPos) { return xPos >= x && xPos <= x + width && yPos >= y && yPos <= y + height; }
		public int getX() { return x; }
		public int getY() { return y; }
		public int getWidth() { return width; }
		public int getHeight() { return height; }
	}
} 