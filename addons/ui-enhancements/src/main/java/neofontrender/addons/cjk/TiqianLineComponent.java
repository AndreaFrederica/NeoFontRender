package neofontrender.addons.cjk;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import neofontrender.api.text.CjkParagraphLayoutProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A normal text component with non-serialized Tiqian line geometry. */
public final class TiqianLineComponent extends TextComponentString implements PositionedTextLine {
    private final List<CjkParagraphLayoutProvider.Run> runs;
    private final float width;
    private final int visibleLength;
    private final List<Cell> cells = new ArrayList<>();
    private final List<ComponentSpan> components = new ArrayList<>();

    public TiqianLineComponent(List<CjkParagraphLayoutProvider.Run> runs,
                               float width, int visibleLength) {
        super("");
        this.runs = Collections.unmodifiableList(new ArrayList<>(runs));
        this.width = Math.max(0.0F, width);
        this.visibleLength = Math.max(0, visibleLength);
    }

    public void nfrUi$addCell(int start, int end, float left, float right) {
        cells.add(new Cell(Math.max(0, start), Math.max(start, end), left, Math.max(left, right)));
    }

    public void nfrUi$addComponentSpan(int start, int end, ITextComponent component) {
        components.add(new ComponentSpan(Math.max(0, start), Math.max(start, end), component));
    }

    @Override
    public List<CjkParagraphLayoutProvider.Run> nfrUi$runs() {
        return runs;
    }

    @Override
    public float nfrUi$width() {
        return width;
    }

    @Override
    public int nfrUi$visibleOffsetAt(float x) {
        if (cells.isEmpty()) return x <= 0.0F ? 0 : visibleLength;
        if (x <= cells.get(0).left) return cells.get(0).start;
        for (Cell cell : cells) {
            if (x < cell.left) return cell.start;
            if (x <= cell.right) {
                int length = cell.end - cell.start;
                if (length <= 0 || cell.right <= cell.left) return cell.start;
                float ratio = (x - cell.left) / (cell.right - cell.left);
                return Math.max(cell.start, Math.min(cell.end,
                        cell.start + Math.round(length * ratio)));
            }
        }
        return visibleLength;
    }

    @Override
    public float nfrUi$xAtVisibleOffset(int offset) {
        int target = Math.max(0, Math.min(visibleLength, offset));
        if (cells.isEmpty()) return target == 0 ? 0.0F : width;
        for (Cell cell : cells) {
            if (target < cell.start) return cell.left;
            if (target <= cell.end) {
                int length = cell.end - cell.start;
                if (length <= 0) return cell.left;
                float ratio = (target - cell.start) / (float) length;
                return cell.left + (cell.right - cell.left) * ratio;
            }
        }
        return width;
    }

    @Override
    public ITextComponent nfrUi$componentAt(float x) {
        int offset = nfrUi$visibleOffsetAt(x);
        for (ComponentSpan span : components) {
            if (offset >= span.start && (offset < span.end
                    || offset == visibleLength && offset == span.end)) return span.component;
        }
        return null;
    }

    @Override
    public float nfrUi$componentLeft(ITextComponent component) {
        for (ComponentSpan span : components) {
            if (span.component == component) return nfrUi$xAtVisibleOffset(span.start);
        }
        return 0.0F;
    }

    @Override
    public float nfrUi$componentRight(ITextComponent component) {
        for (ComponentSpan span : components) {
            if (span.component == component) return nfrUi$xAtVisibleOffset(span.end);
        }
        return 0.0F;
    }

    private static final class Cell {
        private final int start;
        private final int end;
        private final float left;
        private final float right;

        private Cell(int start, int end, float left, float right) {
            this.start = start;
            this.end = end;
            this.left = left;
            this.right = right;
        }
    }

    private static final class ComponentSpan {
        private final int start;
        private final int end;
        private final ITextComponent component;

        private ComponentSpan(int start, int end, ITextComponent component) {
            this.start = start;
            this.end = end;
            this.component = component;
        }
    }
}
