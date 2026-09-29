//! Conservative character composition: fixed advances are necessary but not sufficient.
//! Compare the actual shaped run with the proposed independent pieces, including GSUB/GPOS.

use std::collections::{HashMap, VecDeque};
use std::sync::Mutex;
use unicode_script::{Script, UnicodeScript};

use cosmic_text::{Attrs, Buffer, FontSystem, LayoutGlyph, Metrics, Shaping};

/// Bounded LRU: reaching capacity must never discard every active size/style at once.
pub(crate) struct ProfileCache<T> {
    entries: VecDeque<((i32, u32), T)>,
    capacity: usize,
}

impl<T: Clone> ProfileCache<T> {
    pub(crate) fn new(capacity: usize) -> Self {
        Self { entries: VecDeque::new(), capacity: capacity.max(1) }
    }

    pub(crate) fn get_or_insert_with(&mut self, key: (i32, u32), create: impl FnOnce() -> T) -> T {
        let value = if let Some(index) = self.entries.iter().position(|(k, _)| *k == key) {
            self.entries.remove(index).unwrap().1
        } else {
            let value = create();
            if self.entries.len() >= self.capacity { self.entries.pop_front(); }
            value
        };
        self.entries.push_back((key, value.clone()));
        value
    }
}

#[derive(Clone)]
pub(crate) struct Shape {
    glyphs: Vec<LayoutGlyph>,
    width: f32,
    baseline: f32,
}

pub(crate) fn snapshot(buffer: &Buffer) -> Option<Shape> {
    let mut runs = buffer.layout_runs();
    let run = runs.next()?;
    if runs.next().is_some() || run.rtl { return None; }
    Some(Shape { glyphs: run.glyphs.to_vec(), width: run.line_w, baseline: run.line_y })
}

fn shape(fonts: &mut FontSystem, attrs: &Attrs, size: f32, text: &str) -> Option<Shape> {
    let metrics = Metrics::new(size, size * 1.4);
    let mut buffer = Buffer::new(fonts, metrics);
    buffer.set_size(None, Some(metrics.line_height * 2.0));
    buffer.set_text(text, attrs, Shaping::Advanced, None);
    buffer.shape_until_scroll(fonts, false);
    snapshot(&buffer)
}

fn near(a: f32, b: f32) -> bool {
    // Allow floating-point accumulation noise, not a percentage of a long line's width:
    // a relative visual tolerance could silently accept real kerning near the end of a run.
    let tolerance = (4.0 * f32::EPSILON * a.abs().max(b.abs())).max(0.00001);
    a.is_finite() && b.is_finite() && (a - b).abs() <= tolerance
}

pub(crate) struct CharacterProfile {
    singles: Vec<Option<Shape>>,
    ascii_monospace: bool,
    tabular_digits: bool,
    cjk_singles: Mutex<HashMap<char, Option<Shape>>>,
}

impl CharacterProfile {
    pub(crate) fn new(fonts: &mut FontSystem, attrs: &Attrs, size: f32) -> Self {
        let singles: Vec<_> = (b' '..=b'~')
            .map(|c| shape(fonts, attrs, size, &(c as char).to_string())).collect();
        let uniform = |start: u8, end: u8| {
            let Some(reference) = singles[(start - b' ') as usize].as_ref() else { return false; };
            if reference.width <= 0.0 || reference.glyphs.len() != 1 { return false; }
            (start..=end).all(|c| singles[(c - b' ') as usize].as_ref().is_some_and(|s| {
                s.glyphs.len() == 1 && s.glyphs[0].start == 0 && s.glyphs[0].end == 1
                    && s.glyphs[0].glyph_id != 0
                    && s.glyphs[0].font_id == reference.glyphs[0].font_id
                    && near(s.width, reference.width)
            }))
        };
        let ascii_monospace = uniform(b' ', b'~');
        let tabular_digits = uniform(b'0', b'9');
        Self { singles, ascii_monospace, tabular_digits, cjk_singles: Mutex::new(HashMap::new()) }
    }

    fn cjk_single(&self, fonts: &mut FontSystem, attrs: &Attrs, size: f32, c: char) -> Option<Shape> {
        let mut cache = self.cjk_singles.lock().ok()?;
        if cache.len() >= 2048 && !cache.contains_key(&c) {
            if let Some(victim) = cache.keys().next().copied() { cache.remove(&victim); }
        }
        cache.entry(c).or_insert_with(|| shape(fonts, attrs, size, &c.to_string())).clone()
    }

    fn fixed_cjk(&self, fonts: &mut FontSystem, attrs: &Attrs, size: f32, c: char) -> bool {
        let Some(reference) = cjk_reference(c) else { return false; };
        let Some(single) = self.cjk_single(fonts, attrs, size, c) else { return false; };
        let Some(reference) = self.cjk_single(fonts, attrs, size, reference) else { return false; };
        single.glyphs.len() == 1 && reference.glyphs.len() == 1 && reference.width > 0.0
            && single.glyphs[0].glyph_id != 0 && reference.glyphs[0].glyph_id != 0
            && single.glyphs[0].font_id == reference.glyphs[0].font_id
            && single.glyphs[0].start == 0 && single.glyphs[0].end == c.len_utf8()
            && near(single.width, reference.width)
    }

    pub(crate) fn boundaries(&self, fonts: &mut FontSystem, attrs: &Attrs, size: f32, text: &str) -> Vec<usize> {
        if text.chars().count() < 2 || text.chars().count() > 256
                || !text.chars().all(|c| (' '..='~').contains(&c) || cjk_reference(c).is_some()) {
            return vec![];
        }
        let mut points = vec![0];
        for (i, c) in text.char_indices() {
            let eligible = if c.is_ascii() {
                self.ascii_monospace || (self.tabular_digits && c.is_ascii_digit())
            } else {
                self.fixed_cjk(fonts, attrs, size, c)
            };
            if eligible {
                if *points.last().unwrap() != i { points.push(i); }
                points.push(i + c.len_utf8());
            }
        }
        if *points.last().unwrap() != text.len() { points.push(text.len()); }
        if points.len() <= 2 { vec![] } else { points }
    }

}

/// Fullwidth punctuation uses the Han cell. Exclude halfwidth kana and combining/variation
/// sequences from the candidate alphabet; full shaping remains the final authority.
fn cjk_reference(c: char) -> Option<char> {
    if ('\u{ff01}'..='\u{ff60}').contains(&c) || ('\u{ffe0}'..='\u{ffe6}').contains(&c)
            || ('\u{3000}'..='\u{303f}').contains(&c) { return Some('一'); }
    if ('\u{ff61}'..='\u{ffdc}').contains(&c) || c == '\u{3099}' || c == '\u{309a}' { return None; }
    match c.script() {
        Script::Han => Some('一'),
        Script::Hiragana => Some('あ'),
        Script::Katakana => Some('ア'),
        Script::Hangul => Some('가'),
        Script::Bopomofo => Some('ㄅ'),
        _ => None,
    }
}

fn same_glyph(full: &LayoutGlyph, part: &LayoutGlyph, byte_offset: usize, x: f32) -> bool {
    full.start == part.start + byte_offset && full.end == part.end + byte_offset
        && full.font_id == part.font_id && full.glyph_id == part.glyph_id
        && full.font_weight == part.font_weight && full.level == part.level
        && full.cache_key_flags == part.cache_key_flags && full.color_opt == part.color_opt
        && full.line_height_opt == part.line_height_opt && full.metadata == part.metadata
        && near(full.font_size, part.font_size) && near(full.x, part.x + x)
        && near(full.y, part.y) && near(full.w, part.w)
        && near(full.x_offset, part.x_offset) && near(full.y_offset, part.y_offset)
}

/// Checks whole-run clusters, substitutions and positioning, not just total width or glyph count.
fn first_mismatch(fonts: &mut FontSystem, attrs: &Attrs, size: f32, text: &str,
                      full: &Shape, profile: &CharacterProfile, points: &[usize]) -> Option<usize> {
    let mut glyph_index = 0;
    let mut x = 0.0;
    for (index, range) in points.windows(2).enumerate() {
        let piece = &text[range[0]..range[1]];
        let owned;
        let part = if piece.len() == 1 {
            profile.singles[(piece.as_bytes()[0] - b' ') as usize].as_ref()
        } else if piece.chars().count() == 1 {
            owned = profile.cjk_single(fonts, attrs, size, piece.chars().next().unwrap());
            owned.as_ref()
        } else {
            owned = shape(fonts, attrs, size, piece);
            owned.as_ref()
        };
        let Some(part) = part else { return Some(index); };
        if !near(full.baseline, part.baseline) { return Some(index); }
        for glyph in &part.glyphs {
            let Some(original) = full.glyphs.get(glyph_index) else { return Some(index); };
            if !same_glyph(original, glyph, range[0], x) { return Some(index); }
            glyph_index += 1;
        }
        x += part.width;
    }
    if glyph_index == full.glyphs.len() && near(x, full.width) { None }
    else { Some(points.len().saturating_sub(2)) }
}

pub(crate) fn matches(fonts: &mut FontSystem, attrs: &Attrs, size: f32, text: &str,
                      full: &Shape, profile: &CharacterProfile, points: &[usize]) -> bool {
    first_mismatch(fonts, attrs, size, text, full, profile, points).is_none()
}

/// Merge around a changed cluster/position, preserving reusable characters elsewhere.
/// Bound retries so an adversarial contextual font cannot turn planning quadratic.
pub(crate) fn refine(fonts: &mut FontSystem, attrs: &Attrs, size: f32, text: &str,
                     full: &Shape, profile: &CharacterProfile, mut points: Vec<usize>) -> Vec<usize> {
    for _ in 0..8 {
        if points.len() <= 2 { return vec![]; }
        let Some(index) = first_mismatch(fonts, attrs, size, text, full, profile, &points)
            else { return points; };
        if index + 1 < points.len() - 1 { points.remove(index + 1); }
        if index > 0 { points.remove(index); }
    }
    if points.len() > 2 && matches(fonts, attrs, size, text, full, profile, &points) { points }
    else { vec![] }
}

pub(crate) fn refine_scaled(fonts: &mut FontSystem, attrs: &Attrs, size: f32,
                             text: &str, profile: &CharacterProfile, points: Vec<usize>) -> Vec<usize> {
    shape(fonts, attrs, size, text)
        .map_or_else(Vec::new, |full| refine(fonts, attrs, size, text, &full, profile, points))
}

#[cfg(test)]
mod tests {
    use super::*;
    use cosmic_text::{fontdb, Family};
    use std::sync::Arc;

    #[test]
    fn adaptive_sizes_and_styles_survive_repeated_passes_without_rebuilding_the_cache() {
        let mut cache = ProfileCache::new(128);
        let mut built = 0;
        for _ in 0..3 {
            for style in 0..4 {
                for bucket in 0..28 {
                    cache.get_or_insert_with((style, bucket), || { built += 1; built });
                }
            }
        }
        assert_eq!(built, 112);
    }

    #[test]
    fn full_cache_retains_recent_entries_and_evicts_only_the_oldest() {
        let mut cache = ProfileCache::new(2);
        cache.get_or_insert_with((0, 1), || 1);
        cache.get_or_insert_with((0, 2), || 2);
        assert_eq!(cache.get_or_insert_with((0, 1), || panic!("a full cache hit must not rebuild")), 1);
        cache.get_or_insert_with((0, 3), || 3);
        assert_eq!(cache.get_or_insert_with((0, 1), || panic!("recent entry was flushed")), 1);
        assert_eq!(cache.get_or_insert_with((0, 2), || 22), 22);
    }

    fn fixture() -> (FontSystem, String) {
        let bytes = std::fs::read("../../src/main/resources/assets/neofontrender/fonts/noto_sans_sc-regular.otf")
            .expect("bundled test font");
        let mut db = fontdb::Database::new();
        let ids = db.load_font_source(fontdb::Source::Binary(Arc::new(bytes)));
        let family = db.face(ids[0]).unwrap().families[0].0.clone();
        (FontSystem::new_with_locale_and_db("en-US".into(), db), family)
    }

    #[test]
    fn rejects_cluster_merges_contextual_substitutions_and_position_changes_even_at_equal_total_width() {
        let (mut fonts, family) = fixture();
        let attrs = Attrs::new().family(Family::Name(&family));
        let profile = CharacterProfile::new(&mut fonts, &attrs, 12.0);
        let full = shape(&mut fonts, &attrs, 12.0, "123").unwrap();
        let points = profile.boundaries(&mut fonts, &attrs, 12.0, "123");
        assert!(matches(&mut fonts, &attrs, 12.0, "123", &full, &profile, &points));
        for mutation in 0..5 {
            let mut changed = full.clone();
            match mutation {
                0 => changed.glyphs[0].end = 2, // a ligature cluster crossing the cut
                1 => changed.glyphs[1].glyph_id += 1, // contextual GSUB, same advance
                2 => changed.glyphs[1].x_offset += 0.1, // GPOS, same total advance
                3 => changed.glyphs[1].y_offset += 0.1,
                _ => changed.glyphs[1].font_id = fontdb::ID::dummy(), // fallback change
            }
            assert!(!matches(&mut fonts, &attrs, 12.0, "123", &changed, &profile, &points));
        }
    }

    #[test]
    fn cjk_candidates_exclude_variation_selectors_combining_marks_and_halfwidth_forms() {
        for c in ['中', '𠀀', 'あ', 'ア', '한', 'ㄅ', '，', '！'] {
            assert!(cjk_reference(c).is_some(), "{c}");
        }
        for c in ['\u{3099}', '\u{fe00}', '\u{e0100}', 'ｱ', '😀'] {
            assert!(cjk_reference(c).is_none(), "{c}");
        }
    }

    #[test]
    fn rejects_latin_context_digit_substitution_despite_equal_advances() {
        let (mut fonts, family) = fixture();
        let attrs = Attrs::new().family(Family::Name(&family));
        let profile = CharacterProfile::new(&mut fonts, &attrs, 9.0);
        let text = "123.45MB";
        let full = shape(&mut fonts, &attrs, 9.0, text).unwrap();
        let points = profile.boundaries(&mut fonts, &attrs, 9.0, text);
        assert!(near(full.glyphs[0].w, profile.singles[(b'1' - b' ') as usize].as_ref().unwrap().width));
        assert_ne!(full.glyphs[0].glyph_id,
            profile.singles[(b'1' - b' ') as usize].as_ref().unwrap().glyphs[0].glyph_id);
        let refined = refine(&mut fonts, &attrs, 9.0, text, &full, &profile, points);
        assert!(refined.is_empty());
    }
}
