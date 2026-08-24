package neofontrender.addons.mixin;

import net.minecraftforge.fml.client.GuiScrollingList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = GuiScrollingList.class, remap = false)
public interface AccessorGuiScrollingListNavigation {
    @Invoker("getSize") int nfrUi$invokeNavigationSize();
    @Invoker("elementClicked") void nfrUi$invokeElementClicked(int index, boolean doubleClick);
    @Invoker("applyScrollLimits") void nfrUi$invokeApplyScrollLimits();
    @Accessor("left") int nfrUi$getLeft();
    @Accessor("right") int nfrUi$getRight();
    @Accessor("top") int nfrUi$getTop();
    @Accessor("bottom") int nfrUi$getBottom();
    @Accessor("slotHeight") int nfrUi$getSlotHeight();
    @Accessor("headerHeight") int nfrUi$getHeaderHeight();
    @Accessor("scrollDistance") float nfrUi$getScrollDistance();
    @Accessor("scrollDistance") void nfrUi$setScrollDistance(float value);
    @Accessor("selectedIndex") void nfrUi$setSelectedIndex(int value);
}
