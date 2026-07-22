package neofontrender.addons.vendor.tabbychat.foundation.text;

import neofontrender.addons.vendor.tabbychat.foundation.Color;

public class FancyTextStyle {

    private Color color;
    private Color underline;
    private Color highlight;
    private String insertion;

    public Color getColor() {
        if (color == null)
            return Color.WHITE;
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Color getUnderline() {
        if (underline == null)
            return Color.of(0);
        return underline;
    }

    public void setUnderline(Color underline) {
        this.underline = underline;
    }

    public Color getHighlight() {
        if (highlight == null)
            return Color.of(0);
        return highlight;
    }

    public void setHighlight(Color highlight) {
        this.highlight = highlight;
    }

    public String getInsertion() {
        return insertion;
    }

    public void setInsertion(String insertion) {
        this.insertion = insertion;
    }

    public FancyTextStyle createCopy() {
        FancyTextStyle fcs = new FancyTextStyle();
        fcs.color = color;
        fcs.highlight = highlight;
        fcs.underline = underline;
        fcs.insertion = insertion;
        return fcs;
    }

    @Override
    public String toString() {
        return String.format("FancyStyle{color=%s, underline=%s, highlight=%s, insertion=%s}",
                color, underline, highlight, insertion);
    }

}
