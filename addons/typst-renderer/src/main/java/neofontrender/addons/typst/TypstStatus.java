package neofontrender.addons.typst;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import neofontrender.addons.api.flight.FlightApi;
import neofontrender.addons.api.flight.FlightHudCanvas;
import neofontrender.addons.hud.compositor.HudSurface;
import neofontrender.typst.TypstEvent;
import neofontrender.typst.TypstPackages;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/** Client-thread status presentation. Download/compile work stays on the engine worker. */
final class TypstStatus implements HudSurface {
    static final TypstStatus INSTANCE = new TypstStatus();
    private final LinkedHashMap<String, Notice> notices = new LinkedHashMap<>();
    private List<TypstPackages.Entry> packages = List.of();
    private boolean busy;
    private long revision;
    private String lastError = "";

    record Notice(TypstEvent event, long time) {}
    private TypstStatus() {}

    static String tr(String key, Object... args) {
        return TypstI18n.tr("neofontrender_typst_renderer." + key, args);
    }

    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || TypstMiddleware.plugin() == null) return;
        boolean changed = false;
        for (TypstEvent status : TypstMiddleware.plugin().pollEvents()) {
            accept(status);
            changed |= status.state().equals("installed") || status.state().equals("deleted");
        }
        if (changed) refresh();
    }

    private void accept(TypstEvent event) {
        String key = event.packageSpec().isEmpty() ? "compiler" : event.packageSpec();
        notices.remove(key);
        notices.put(key, new Notice(event, System.nanoTime()));
        while (notices.size() > 16) notices.remove(notices.keySet().iterator().next());
        if (event.failed() && !event.detail().equals(lastError)) {
            lastError = event.detail();
            revision++;
            LogManager.getLogger("NFR Typst").error("{} {}: {}", event.state(), event.packageSpec(), event.detail());
        }
    }

    boolean busy() { return busy; }
    long revision() { return revision; }
    List<TypstPackages.Entry> packages() { return packages; }
    String lastError() { return lastError; }

    void refresh() {
        if (busy) return;
        busy = true;
        TypstMiddleware.plugin().packages().whenComplete((entries, error) ->
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    busy = false;
                    if (error == null) packages = entries;
                    else accept(new TypstEvent("cache_failed", "", 0, -1, error.toString()));
                    revision++;
                }));
    }

    void manage(String spec, boolean delete) {
        if (busy) return;
        try {
            String validated = TypstPackages.validate(spec);
            busy = true;
            accept(new TypstEvent(delete ? "deleting" : "queued", validated, 0, -1, ""));
            TypstMiddleware.plugin().managePackage(validated, delete).whenComplete((unused, error) ->
                    Minecraft.getMinecraft().addScheduledTask(() -> {
                        busy = false;
                        if (error != null) accept(new TypstEvent(delete ? "delete_failed" : "download_failed",
                                validated, 0, -1, error.toString()));
                        refresh();
                    }));
        } catch (IllegalArgumentException error) {
            accept(new TypstEvent("download_failed", "", 0, -1, error.getMessage()));
        }
    }

    void retry() {
        if (busy) return;
        List<String> failed = notices.values().stream().map(Notice::event)
                .filter(e -> e.state().equals("download_failed") && !e.packageSpec().isEmpty())
                .map(TypstEvent::packageSpec).toList();
        if (failed.isEmpty()) {
            TypstMiddleware.plugin().retryFailed();
            return;
        }
        busy = true;
        var jobs = new ArrayList<java.util.concurrent.CompletableFuture<Void>>();
        for (String spec : failed) {
            try { jobs.add(TypstMiddleware.plugin().managePackage(spec, false)); }
            catch (IllegalArgumentException ignored) { /* Invalid imports are retried by the compiler. */ }
        }
        java.util.concurrent.CompletableFuture.allOf(jobs.toArray(new java.util.concurrent.CompletableFuture<?>[0]))
                .whenComplete((unused, error) -> Minecraft.getMinecraft().addScheduledTask(() -> {
                    busy = false;
                    TypstMiddleware.plugin().retryFailed();
                    refresh();
                }));
    }

    void dismiss() { notices.clear(); lastError = ""; revision++; }

    List<Notice> notices() { return new ArrayList<>(notices.values()); }

    static String describe(TypstEvent event) {
        String label = tr("status." + event.state());
        if (!event.packageSpec().isEmpty()) label += " " + event.packageSpec();
        if (event.state().equals("downloading")) {
            label += " " + bytes(event.downloaded());
            if (event.total() > 0) label += " / " + bytes(event.total()) + " ("
                    + Math.min(100, event.downloaded() * 100 / event.total()) + "%)";
        }
        return label;
    }

    static String bytes(long count) {
        return count < 1024 ? count + " B" : count < 1024 * 1024
                ? String.format(Locale.ROOT, "%.1f KiB", count / 1024.0)
                : String.format(Locale.ROOT, "%.1f MiB", count / (1024.0 * 1024.0));
    }

    @Override public String id() { return "neofontrender_typst_renderer:status"; }

    @Override public java.awt.Rectangle bounds() {
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        int width = Math.min(280, resolution.getScaledWidth() - 16);
        int height = Math.min(260, Math.max(24, notices.size() * 46));
        return new java.awt.Rectangle(resolution.getScaledWidth() - width - 8, 8, width, height);
    }

    @Override public boolean visible() {
        Minecraft mc = Minecraft.getMinecraft();
        return TypstConfig.downloadHud() && !mc.gameSettings.hideGUI
                && mc.currentScreen == null
                && !notices.isEmpty();
    }

    @Override public void render(float partialTicks) {
        FlightHudCanvas canvas = FlightApi.getHudCanvas();
        if (canvas == null) return;
        java.awt.Rectangle area = bounds();
        List<Notice> visible = notices.values().stream()
                .filter(n -> n.event.active() || n.event.failed()
                        || System.nanoTime() - n.time < 8_000_000_000L).toList();
        float y = 0;
        for (int i = Math.max(0, visible.size() - 4); i < visible.size(); i++) {
            Notice notice = visible.get(i);
            List<String> lines = Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(
                    describe(notice.event), area.width - 14);
            int count = Math.min(3, lines.size());
            float height = count * 10 + 12;
            int color = notice.event.failed() ? 0xFFFF7272 : notice.event.active() ? 0xFF69BAE8 : 0xFF75D89D;
            canvas.fill(0, y, area.width, y + height, 0xEB202428);
            canvas.fill(0, y, 2, y + height, color);
            for (int line = 0; line < count; line++)
                canvas.text(lines.get(line), 7, y + 5 + line * 10, 1.0F, 0xFFF3F4F5, 0xB0202428);
            if (notice.event.state().equals("downloading") && notice.event.total() > 0) {
                float length = (area.width - 14) * Math.min(1.0F,
                        notice.event.downloaded() / (float) notice.event.total());
                canvas.fill(7, y + height - 4, 7 + length, y + height - 2, color);
            }
            y += height + 4;
        }
    }

    @Override public boolean acceptsPointer() { return false; }

}
