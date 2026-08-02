package speiger.src.salutation.client.gui.chat;

import java.io.IOException;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class ChatScreen extends GuiChat implements ISaluationChat {
	Completor completer;
	
	public ChatScreen(GuiChat chat) {
		super(getChatString(chat));
	}
	
	private static String getChatString(GuiChat chat) {
		Minecraft mc = Minecraft.getMinecraft();
		if(mc.currentScreen == null && mc.gameSettings.keyBindCommand.isKeyDown()) return "/";
		return ReflectionHelper.getPrivateValue(GuiChat.class, chat, "defaultInputFieldText", "field_146410_g");
	}
	
	@Override
	public void initGui() {
		super.initGui();
		completer = new Completor(inputField);
	}
	
	@Override
	public void setCompletions(String... newCompletions) {
		completer.setCompletions(newCompletions);
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		completer.render(mouseX, mouseY, fontRenderer);
	}
	
	public void handleMouseInput() {
        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
		int scroll = Mouse.getDWheel() / 120;
		
		if(scroll != 0) {
			if(!completer.onScroll(mouseX, mouseY, -scroll)) {
				if(!isShiftKeyDown()) scroll *= 7;
				
				this.mc.ingameGUI.getChatGUI().scroll(scroll);
			}
		}
		super.handleMouseInput();
	}
	
	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		if(completer.onClick(mouseX, mouseY)) {
			return;
		}
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}
	
	@Override
	protected void keyTyped(char typedChar, int keyCode) {		
		boolean update = false;
		if(completer.onKeyPress(keyCode)) {
			return;
		}
		else {
			update = true;
		}
		
		if(keyCode == 1) {
			this.mc.displayGuiScreen((GuiScreen)null);
		}
		else if(keyCode != 28 && keyCode != 156) {
			if(keyCode == Keyboard.KEY_UP) {
				this.getSentHistory(-1);
			}
			else if(keyCode == Keyboard.KEY_DOWN) {
				this.getSentHistory(1);
			}
			else if(keyCode == Keyboard.KEY_PRIOR) {
				this.mc.ingameGUI.getChatGUI().scroll(this.mc.ingameGUI.getChatGUI().getLineCount() - 1);
			}
			else if(keyCode == Keyboard.KEY_NEXT) {
				this.mc.ingameGUI.getChatGUI().scroll(-this.mc.ingameGUI.getChatGUI().getLineCount() + 1);
			}
			else {
				this.inputField.textboxKeyTyped(typedChar, keyCode);
			}
			if(update) {
				completer.requestUpdate();
			}
		}
		else {
			String s = this.inputField.getText().trim();
			if(!s.isEmpty()) this.sendChatMessage(s);
			this.mc.displayGuiScreen(null);
		}
	}
	
	public static class Completor extends AdvancedTabCompleter {
		public Completor(GuiTextField textFieldIn) {
			super(textFieldIn, false);
		}
		
		@Override
		public BlockPos getTargetBlockPos() {
			return mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK ? mc.objectMouseOver.getBlockPos() : null;
		}
	}
}
