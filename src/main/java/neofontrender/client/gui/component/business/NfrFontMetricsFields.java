package neofontrender.client.gui.component.business;

import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.widget.ParentWidget;
import neofontrender.client.gui.component.base.NfrLabeledTextField;
import neofontrender.client.gui.component.base.NfrLayout;

/** Size, variable weight, and baseline controls presented as one business field group. */
public final class NfrFontMetricsFields extends ParentWidget<NfrFontMetricsFields> implements ILayoutWidget {
    private final NfrLabeledTextField size, weight, baseline;
    public NfrFontMetricsFields(NfrLabeledTextField size,NfrLabeledTextField weight,NfrLabeledTextField baseline){
        this.size=size;this.weight=weight;this.baseline=baseline;child(size);child(weight);child(baseline);
    }
    @Override public boolean layoutWidgets(){
        int width=getArea().w(),gap=10,item=Math.max(0,(width-gap*2)/3);
        NfrLayout.place(size,0,0,item,getArea().h());NfrLayout.place(weight,item+gap,0,item,getArea().h());
        NfrLayout.place(baseline,(item+gap)*2,0,Math.max(0,width-(item+gap)*2),getArea().h());return true;
    }
}
