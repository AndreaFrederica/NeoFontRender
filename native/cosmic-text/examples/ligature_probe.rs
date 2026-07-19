use cosmic_text::{Attrs, Buffer, Family, FontSystem, Metrics, Shaping};
use std::env;
use std::fs;
use std::sync::Arc;

fn main() {
    let font_path = env::args().nth(1).expect("usage: ligature_probe <font.ttf>");
    let font_data = fs::read(&font_path).expect("failed to read font");

    // Match the production engine: use an explicit database so system font fallback cannot
    // silently replace the font under test and make the shaping result non-reproducible.
    let mut db = cosmic_text::fontdb::Database::new();
    let ids = db.load_font_source(cosmic_text::fontdb::Source::Binary(Arc::new(font_data)));
    let primary_id = ids.first().copied().expect("font contains no faces");
    let family = db
        .face(primary_id)
        .and_then(|face| face.families.first())
        .map(|family| family.0.clone())
        .expect("font face has no family name");
    db.set_sans_serif_family(family.clone());

    let mut font_system = FontSystem::new_with_locale_and_db("en-US".to_string(), db);
    println!("font={family} path={font_path}");
    for text in ["!=", "->", "=>", "===", "ffi", "office"] {
        let metrics = Metrics::new(32.0, 44.8);
        let mut buffer = Buffer::new(&mut font_system, metrics);
        buffer.set_size(None, Some(metrics.line_height * 2.0));
        let attrs = Attrs::new().family(Family::Name(&family));
        buffer.set_text(text, &attrs, Shaping::Advanced, None);
        buffer.shape_until_scroll(&mut font_system, false);

        let glyphs = buffer
            .layout_runs()
            .flat_map(|run| run.glyphs.iter())
            .map(|glyph| format!("{}:{}-{}", glyph.glyph_id, glyph.start, glyph.end))
            .collect::<Vec<_>>();
        println!(
            "text={text:?} chars={} glyphs={} clusters=[{}]",
            text.chars().count(),
            glyphs.len(),
            glyphs.join(", ")
        );
    }
}
