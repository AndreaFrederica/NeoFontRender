package neofontrender.addons.cursor;

import com.cleanroommc.modularui.api.navigation.NavigationAction;
import com.cleanroommc.modularui.api.navigation.NavigationInfo;
import com.cleanroommc.modularui.api.navigation.NavigationRole;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.AbstractScrollWidget;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widget.sizer.AreaResizer;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.menu.ContextMenuButton;
import com.cleanroommc.modularui.widgets.menu.Menu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import neofontrender.client.gui.component.base.NfrPreferredHeight;
import neofontrender.client.gui.component.base.NfrTextButton;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Cursor image dropdown whose current value and options render their actual PNG preview. */
final class CursorImageSelector extends ContextMenuButton<CursorImageSelector>
        implements NfrPreferredHeight {
    private static final int MENU_BACKGROUND = 0xE0080B10;
    private static final int OPTION_HOVER = 0xB8333333;
    private static final int ICON_SIZE = 22;

    private final Supplier<String> label;
    private final Supplier<String> getter;
    private final Consumer<String> setter;

    CursorImageSelector(String name, Supplier<String> label, Supplier<String> getter,
                        Consumer<String> setter) {
        super(name);
        this.label = label;
        this.getter = getter;
        this.setter = setter;
        navigationInfo(NavigationInfo.builder(NavigationRole.DROPDOWN)
                .id(name).label(() -> label.get() + ": " + selectedName())
                .actions(NavigationAction.ACTIVATE).build());
        requiresClick();
        openCustom();
    }

    @Override public int preferredHeight() { return 34; }

    @Override
    public void openMenu(boolean soft) {
        if (!isOpen()) setMenu(buildMenu());
        super.openMenu(soft);
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
        super.draw(context, theme);
        Minecraft minecraft = Minecraft.getMinecraft();
        CursorAsset selected = CursorAssetCatalog.INSTANCE.find(getter.get());
        if (selected != null) drawIcon(selected, 5, 6, ICON_SIZE);
        String left = minecraft.fontRenderer.trimStringToWidth(label.get(),
                Math.max(1, getArea().w() / 2 - 38));
        String right = minecraft.fontRenderer.trimStringToWidth(selectedName(),
                Math.max(1, getArea().w() / 2 - 16));
        int y = Math.max(0, (getArea().h() - minecraft.fontRenderer.FONT_HEIGHT) / 2);
        minecraft.fontRenderer.drawString(left, selected == null ? 6 : 32, y, 0xFFFFFF);
        minecraft.fontRenderer.drawString(right,
                Math.max(6, getArea().w() - minecraft.fontRenderer.getStringWidth(right) - 16),
                y, selected == null && !getter.get().isEmpty() ? 0xFF7777 : 0xD8E0E8);
        drawArrow();
    }

    private Menu<?> buildMenu() {
        ListWidget<IWidget, ?> list = new ListWidget<>().widthRel(1F).maxSize(210)
                .background(new Rectangle().color(MENU_BACKGROUND));
        list.child(option(null));
        List<CursorAsset> assets = CursorAssetCatalog.INSTANCE.assets();
        for (CursorAsset asset : assets) list.child(option(asset));
        Menu<?> menu = new Menu<>().widthRel(1F).topRel(1F).coverChildrenHeight()
                .background(new Rectangle().color(0xA064748B)).padding(1).child(list);
        menu.resizer().relative(new AreaResizer(scrolledAnchor()));
        return menu;
    }

    private IWidget option(CursorAsset asset) {
        String id = asset == null ? "" : asset.id();
        Supplier<String> text = () -> (id.equals(getter.get()) ? "✓ " : "")
                + (asset == null ? tr("system") : asset.displayName() + "  [" + source(asset) + "]");
        return new CursorOptionButton(text, asset).height(30).widthRel(1F)
                .background(new Rectangle().color(0x00000000))
                .hoverBackground(new Rectangle().color(OPTION_HOVER))
                .onMousePressed(button -> {
                    setter.accept(id);
                    CursorManager.releaseCustomHandles();
                    closeMenu(false);
                    return true;
                });
    }

    private String selectedName() {
        String id = getter.get();
        if (id == null || id.isEmpty()) return tr("system");
        CursorAsset asset = CursorAssetCatalog.INSTANCE.find(id);
        return asset == null ? tr("missing") : asset.displayName();
    }

    private static String source(CursorAsset asset) {
        return tr(asset.source() == CursorAsset.Source.LOCAL ? "source_local" : "source_resource");
    }

    private Area scrolledAnchor() {
        Area anchor = getArea().createCopy();
        IWidget ancestor = getParent();
        while (ancestor != null) {
            if (ancestor instanceof AbstractScrollWidget) {
                AbstractScrollWidget<?, ?> scroll = (AbstractScrollWidget<?, ?>) ancestor;
                anchor.offset(-scroll.getScrollX(), -scroll.getScrollY());
            }
            ancestor = ancestor.getParent();
        }
        return anchor;
    }

    private void drawArrow() {
        int x = Math.max(4, getArea().w() - 10);
        int y = Math.max(3, getArea().h() / 2 - 2);
        Gui.drawRect(x, y, x + 5, y + 1, 0xFFB8C2D0);
        Gui.drawRect(x + 1, y + 1, x + 4, y + 2, 0xFFB8C2D0);
        Gui.drawRect(x + 2, y + 2, x + 3, y + 3, 0xFFB8C2D0);
    }

    private static void drawIcon(CursorAsset asset, int x, int y, int box) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(asset.previewTexture());
        float scale = Math.min((float) box / asset.width(), (float) box / asset.height());
        int width = Math.max(1, Math.round(asset.width() * scale));
        int height = Math.max(1, Math.round(asset.height() * scale));
        Gui.drawModalRectWithCustomSizedTexture(x + (box - width) / 2, y + (box - height) / 2,
                0, 0, width, height, width, height);
    }

    private static String tr(String key) {
        return neofontrender.addons.tooltips.AddonI18n.tr(
                "neofontrender_ui_enhancements.gui.cursor.image." + key);
    }

    private static final class CursorOptionButton extends NfrTextButton {
        private final CursorAsset asset;

        private CursorOptionButton(Supplier<String> label, CursorAsset asset) {
            super(label, false);
            this.asset = asset;
        }

        @Override protected int textInset() { return asset == null ? 6 : 34; }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
            super.draw(context, theme);
            if (asset != null) drawIcon(asset, 5, 4, 22);
        }
    }
}
