package neofontrender.client.gui.pages;

/** Stable route metadata for every settings content view. */
public enum NfrSettingsRoute {
    FONT(0,"neofontrender.gui.category.font"),
    GENERAL(1,"neofontrender.gui.category.general"),
    RENDERING(2,"neofontrender.gui.category.rendering"),
    PERFORMANCE(3,"neofontrender.gui.category.performance"),
    ADVANCED(4,"neofontrender.gui.category.advanced"),
    CACHE(5,"neofontrender.gui.category.cache"),
    SHADOW(8,"neofontrender.gui.category.shadow"),
    FIXES(9,"neofontrender.gui.category.fixes"),
    LABORATORY(10,"neofontrender.gui.category.laboratory"),
    ABOUT(6,"neofontrender.gui.category.about"),
    LICENSES(7,"neofontrender.gui.category.licenses");
    public final int id; public final String titleKey;
    NfrSettingsRoute(int id,String titleKey){this.id=id;this.titleKey=titleKey;}
    public static NfrSettingsRoute byId(int id){for(NfrSettingsRoute route:values())if(route.id==id)return route;return FONT;}
}
