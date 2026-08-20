package neofontrender.addons.mixin;

import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import neofontrender.addons.api.ui.navigation.CreativeTabNavigation;
import neofontrender.addons.api.ui.navigation.UiNavigationApi;
import neofontrender.addons.api.ui.navigation.UiRect;
import neofontrender.addons.scrolling.SmoothScrollConfigAccess;
import neofontrender.addons.scrolling.SmoothScrollController;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mixin(GuiContainerCreative.class)
public abstract class MixinGuiContainerCreativeSmoothScroll implements CreativeTabNavigation {
    @Shadow private static int selectedTabIndex;
    @Shadow(remap = false) private static int tabPage;
    @Shadow private float currentScroll;
    @Shadow private boolean isScrolling;
    @Shadow(remap = false) private int maxPages;
    @Shadow protected abstract void setCurrentCreativeTab(CreativeTabs tab);
    @Unique private final SmoothScrollController nfrUi$scroller = new SmoothScrollController();

    @Redirect(method = "handleMouseInput", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I"))
    private int nfrUi$captureWheel() {
        int wheel = Mouse.getEventDWheel();
        if (!SmoothScrollConfigAccess.creativeInventoryEnabled() || wheel == 0) return wheel;
        int hiddenRows = nfrUi$hiddenRows();
        if (hiddenRows <= 0) {
            nfrUi$scroller.sync(0.0F);
            currentScroll = 0.0F;
            return 0;
        }
        float rowsPerNotch = SmoothScrollConfigAccess.wheelStep() / 18.0F;
        float delta = (wheel > 0 ? -rowsPerNotch : rowsPerNotch) / hiddenRows;
        nfrUi$scroller.scrollBy(delta, 1.0F, currentScroll);
        return 0;
    }

    @Redirect(method = "drawScreen", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/input/Mouse;isButtonDown(I)Z", remap = false))
    private boolean nfrUi$syntheticButtonState(int button) {
        return UiNavigationApi.isPointerButtonDown(button);
    }

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void nfrUi$update(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!SmoothScrollConfigAccess.creativeInventoryEnabled()) {
            nfrUi$scroller.sync(currentScroll);
            return;
        }
        if (nfrUi$hiddenRows() <= 0) {
            currentScroll = 0.0F;
            nfrUi$scroller.sync(0.0F);
            nfrUi$container().scrollTo(0.0F);
            return;
        }
        if (!isScrolling) {
            currentScroll = nfrUi$scroller.update(currentScroll, 1.0F);
            nfrUi$container().scrollTo(currentScroll);
        }
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void nfrUi$syncDrag(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (isScrolling) nfrUi$scroller.sync(currentScroll);
    }

    @Inject(method = "setCurrentCreativeTab", at = @At("RETURN"))
    private void nfrUi$tabChanged(CallbackInfo ci) {
        nfrUi$scroller.sync(currentScroll);
    }

    @Unique
    private GuiContainerCreative.ContainerCreative nfrUi$container() {
        return (GuiContainerCreative.ContainerCreative)
                ((AccessorGuiContainer) (Object) this).nfrUi$getInventorySlots();
    }

    @Unique
    private int nfrUi$hiddenRows() {
        return Math.max(0, (nfrUi$container().itemList.size() + 8) / 9 - 5);
    }

    @Override
    public List<CreativeTabNavigation.Tab> nfrUi$getVisibleCreativeTabs() {
        GuiContainer screen = (GuiContainer) (Object) this;
        List<CreativeTabNavigation.Tab> result = new ArrayList<>();
        int start = tabPage * 10;
        int end = Math.min(CreativeTabs.CREATIVE_TAB_ARRAY.length, (tabPage + 1) * 10 + 2);
        if (tabPage != 0) start += 2;
        for (int index = start; index < end; index++) {
            nfrUi$addCreativeTab(result, CreativeTabs.CREATIVE_TAB_ARRAY[index], screen);
        }
        if (tabPage != 0) {
            nfrUi$addCreativeTab(result, CreativeTabs.SEARCH, screen);
            nfrUi$addCreativeTab(result, CreativeTabs.INVENTORY, screen);
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public int nfrUi$getSelectedCreativeTab() {
        return selectedTabIndex;
    }

    @Override
    public boolean nfrUi$selectCreativeTab(int tabIndex) {
        CreativeTabs tab = nfrUi$findCreativeTab(tabIndex);
        if (tab == null) return false;
        if (selectedTabIndex == tab.getIndex()) return false;
        setCurrentCreativeTab(tab);
        return true;
    }

    @Override
    public boolean nfrUi$changeCreativeTab(int direction) {
        if (direction == 0 || CreativeTabs.CREATIVE_TAB_ARRAY.length == 0) return false;
        int step = direction < 0 ? -1 : 1;
        int start = -1;
        for (int index = 0; index < CreativeTabs.CREATIVE_TAB_ARRAY.length; index++) {
            CreativeTabs tab = CreativeTabs.CREATIVE_TAB_ARRAY[index];
            if (tab != null && tab.getIndex() == selectedTabIndex) {
                start = index;
                break;
            }
        }
        if (start < 0) start = 0;
        for (int offset = 1; offset <= CreativeTabs.CREATIVE_TAB_ARRAY.length; offset++) {
            int index = Math.floorMod(start + step * offset, CreativeTabs.CREATIVE_TAB_ARRAY.length);
            CreativeTabs tab = CreativeTabs.CREATIVE_TAB_ARRAY[index];
            if (tab == null) continue;
            tabPage = Math.max(0, Math.min(maxPages, tab.getTabPage()));
            setCurrentCreativeTab(tab);
            return true;
        }
        return false;
    }

    @Unique
    private static CreativeTabs nfrUi$findCreativeTab(int tabIndex) {
        for (CreativeTabs tab : CreativeTabs.CREATIVE_TAB_ARRAY) {
            if (tab != null && tab.getIndex() == tabIndex) return tab;
        }
        return null;
    }

    @Unique
    private static void nfrUi$addCreativeTab(List<CreativeTabNavigation.Tab> result,
                                             CreativeTabs tab, GuiContainer screen) {
        if (tab == null) return;
        for (CreativeTabNavigation.Tab existing : result) {
            if (existing.index() == tab.getIndex()) return;
        }
        int x = 28 * tab.getColumn();
        if (tab.isAlignedRight()) {
            x = screen.getXSize() - 28 * (6 - tab.getColumn()) + 2;
        } else if (tab.getColumn() > 0) {
            x += tab.getColumn();
        }
        int y = tab.isOnTopRow() ? -32 : screen.getYSize();
        UiRect bounds = new UiRect(screen.getGuiLeft() + x, screen.getGuiTop() + y,
                screen.getGuiLeft() + x + 29, screen.getGuiTop() + y + 33);
        result.add(new CreativeTabNavigation.Tab(tab.getIndex(),
                I18n.format(tab.getTranslationKey()), bounds));
    }
}
