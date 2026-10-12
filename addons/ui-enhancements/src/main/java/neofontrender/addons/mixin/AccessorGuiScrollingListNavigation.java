package neofontrender.addons.mixin;

import cpw.mods.fml.client.GuiScrollingList;
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
    // 1.7.10 keeps the header height in the unmapped private field field_27261_r.
    @Accessor("field_27261_r") int nfrUi$getHeaderHeight();
    @Accessor("scrollDistance") float nfrUi$getScrollDistance();
    @Accessor("scrollDistance") void nfrUi$setScrollDistance(float value);
    @Accessor("selectedIndex") void nfrUi$setSelectedIndex(int value);
}
