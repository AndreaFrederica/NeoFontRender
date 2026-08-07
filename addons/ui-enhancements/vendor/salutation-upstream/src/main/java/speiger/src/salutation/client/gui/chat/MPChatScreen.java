package speiger.src.salutation.client.gui.chat;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.minecraft.client.gui.GuiSleepMP;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.client.C0BPacketEntityAction;

public class MPChatScreen extends GuiSleepMP implements ISaluationChat {
	Completor completer;

	@Override
	public void initGui() {
		super.initGui();
		completer = new Completor(inputField);
	}

	@Override
	public void func_146406_a(String... newCompletions) {
		completer.setCompletions(newCompletions);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		completer.render(mouseX, mouseY, fontRendererObj);
	}

	public void handleMouseInput() {
		int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
		int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
		int scroll = Mouse.getEventDWheel() / 120;

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
		if(completer.onClick(mouseX, mouseY)) return;
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
			wakeFromSleep();
		}
		else if(keyCode != 28 && keyCode != 156) {
			if(keyCode == Keyboard.KEY_UP) {
				this.getSentHistory(-1);
			}
			else if(keyCode == Keyboard.KEY_DOWN) {
				this.getSentHistory(1);
			}
			else if(keyCode == Keyboard.KEY_PRIOR) {
				this.mc.ingameGUI.getChatGUI().scroll(this.mc.ingameGUI.getChatGUI().func_146232_i() - 1);
			}
			else if(keyCode == Keyboard.KEY_NEXT) {
				this.mc.ingameGUI.getChatGUI().scroll(-this.mc.ingameGUI.getChatGUI().func_146232_i() + 1);
			}
			else {
				this.inputField.textboxKeyTyped(typedChar, keyCode);
			}
			if(update) { completer.requestUpdate(); }
		}
		else {
			String s = this.inputField.getText().trim();
			if(!s.isEmpty()) this.func_146403_a(s);
			this.inputField.setText("");
			this.mc.ingameGUI.getChatGUI().resetScroll();
		}
	}

	private void wakeFromSleep() {
		NetHandlerPlayClient nethandlerplayclient = this.mc.thePlayer.sendQueue;
		nethandlerplayclient.addToSendQueue(new C0BPacketEntityAction(this.mc.thePlayer, 3));
	}

	public static class Completor extends AdvancedTabCompleter {
		public Completor(GuiTextField textFieldIn) {
			super(textFieldIn);
		}
	}
}
