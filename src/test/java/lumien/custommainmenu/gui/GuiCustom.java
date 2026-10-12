package lumien.custommainmenu.gui;

/** Minimal optional-mod fixture; no CMM dependency or Minecraft initialization required. */
public class GuiCustom {
    public Config guiConfig;
    public GuiCustom(String name) { guiConfig = new Config(name); }
    public static class Config {
        public String name;
        public Config(String name) { this.name = name; }
    }
}
