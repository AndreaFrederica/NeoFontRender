package speiger.src.salutation.client.gui.chat;

import java.io.IOException;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.minecraft.client.gui.GuiSleepMP;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;

public class MPChatScreen extends GuiSleepMP implements ISaluationChat {
	Completor completer;
	
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
				this.mc.ingameGUI.getChatGUI().scroll(this.mc.ingameGUI.getChatGUI().getLineCount() - 1);
			}
			else if(keyCode == Keyboard.KEY_NEXT) {
				this.mc.ingameGUI.getChatGUI().scroll(-this.mc.ingameGUI.getChatGUI().getLineCount() + 1);
			}
			else {
				this.inputField.textboxKeyTyped(typedChar, keyCode);
			}
			if(update) { completer.requestUpdate(); }
		}
		else {
			String s = this.inputField.getText().trim();
			if(!s.isEmpty()) this.sendChatMessage(s);
			this.mc.displayGuiScreen(null);
		}
	}
	
	private void wakeFromSleep() {
		NetHandlerPlayClient nethandlerplayclient = this.mc.player.connection;
		nethandlerplayclient.sendPacket(new CPacketEntityAction(this.mc.player, CPacketEntityAction.Action.STOP_SLEEPING));
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
