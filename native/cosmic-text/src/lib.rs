use cosmic_text::fontdb::{Family as DbFamily, Query, ID};
use cosmic_text::{
    Attrs, Buffer, Color, Fallback, Family, FontSystem, Metrics, PlatformFallback, Shaping,
    Style, SwashCache, Weight,
};
use jni::objects::{JByteArray, JClass, JObjectArray, JString};
use jni::sys::{jboolean, jbyteArray, jfloat, jint, jlong, jstring};
use jni::JNIEnv;
use skrifa::prelude::*;
use skrifa::string::StringId;
use std::collections::HashMap;
use std::ptr;
use std::sync::Mutex;
use unicode_script::Script;

const ABI_VERSION: jint = 7;
const RASTER_MAGIC: i32 = 0x434F534D; // "COSM"
const HEADER_SIZE: usize = 32;
const MAX_TEXTURE_DIMENSION: i32 = 8192;
const RASTER_PADDING: i32 = 2;

struct Engine {
    font_system: Mutex<FontSystem>,
    swash_cache: Mutex<SwashCache>,
    primary_family: String,
    faces: [ResolvedFace; 4],
    resolution_warnings: String,
    font_size: f32,
}

#[derive(Clone, Debug)]
struct ResolvedFace {
    #[cfg_attr(not(test), allow(dead_code))]
    id: ID,
    family: String,
    render_family: String,
    weight: Weight,
    style: Style,
    post_script_name: String,
}

#[derive(Clone, Debug)]
struct NamedWeight {
    key: String,
    weight: u16,
    ignored_axes: bool,
}

#[derive(Clone, Debug)]
struct FaceRecord {
    id: ID,
    family_keys: Vec<String>,
    face_keys: Vec<String>,
    named_weights: Vec<NamedWeight>,
    weight_range: Option<(u16, u16)>,
}

#[derive(Clone, Copy)]
struct PixelRect {
    x: i32,
    y: i32,
    w: u32,
    h: u32,
    color: Color,
}

#[no_mangle]
pub extern "system" fn Java_neofontrender_core_font_cosmic_CosmicNative_abiVersion(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    ABI_VERSION
}

#[no_mangle]
pub extern "system" fn Java_neofontrender_core_font_cosmic_CosmicNative_createEngine(
    mut env: JNIEnv,
    _class: JClass,
    fonts: JObjectArray,
    font_aliases: JObjectArray,
    primary_family: JString,
    fallback_families: JObjectArray,
    regular_override: JString,
    bold_override: JString,
    italic_override: JString,
    bold_italic_override: JString,
    variant_overrides_only_switch_font: jboolean,
    variable_weight: jint,
    font_size: jfloat,
    locale: JString,
) -> jlong {
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(
        || -> Result<jlong, String> {
            let count = env.get_array_length(&fonts).map_err(|e| e.to_string())?;
            let alias_count = env
                .get_array_length(&font_aliases)
                .map_err(|e| e.to_string())?;
            if alias_count != count {
                return Err(format!(
                    "font data/alias length mismatch: {count} != {alias_count}"
                ));
            }
            let mut font_data = Vec::with_capacity(count as usize);
            for index in 0..count {
                let object = env
                    .get_object_array_element(&fonts, index)
                    .map_err(|e| e.to_string())?;
                let bytes = env
                    .convert_byte_array(JByteArray::from(object))
                    .map_err(|e| e.to_string())?;
                let alias_object = env
                    .get_object_array_element(&font_aliases, index)
                    .map_err(|e| e.to_string())?;
                let alias: String = env
                    .get_string(&JString::from(alias_object))
                    .map_err(|e| e.to_string())?
                    .into();
                if !bytes.is_empty() {
                    font_data.push((alias, bytes));
                }
            }
            let requested_family: String = env
                .get_string(&primary_family)
                .map_err(|e| e.to_string())?
                .into();
            let requested_fallbacks = read_string_array(&mut env, &fallback_families)?;
            let overrides: [String; 4] = [
                env.get_string(&regular_override)
                    .map_err(|e| e.to_string())?
                    .into(),
                env.get_string(&bold_override)
                    .map_err(|e| e.to_string())?
                    .into(),
                env.get_string(&italic_override)
                    .map_err(|e| e.to_string())?
                    .into(),
                env.get_string(&bold_italic_override)
                    .map_err(|e| e.to_string())?
                    .into(),
            ];

            // Load byte-backed resources first so bundled fallbacks retain priority over arbitrary
            // host fallback fonts. We still load system fonts because config values such as
            // "JetBrains Mono" are family names, not readable font paths.
            let mut db = cosmic_text::fontdb::Database::new();
            let mut byte_primary_id = None;
            let mut source_aliases: HashMap<String, Vec<ID>> = HashMap::new();
            for (index, (alias, bytes)) in font_data.into_iter().enumerate() {
                let ids = db.load_font_source(cosmic_text::fontdb::Source::Binary(
                    std::sync::Arc::new(bytes),
                ));
                if index == 0 {
                    byte_primary_id = ids.first().copied();
                }
                source_aliases
                    .entry(normalize_font_name(&alias))
                    .or_default()
                    .extend(ids);
            }
            db.load_system_fonts();

            let catalog = build_face_catalog(&db);
            let mut warnings = Vec::new();
            let requested_key = normalize_font_name(&requested_family);
            let regular_weight = configured_weight(variable_weight, Weight::NORMAL);
            let primary_match = resolve_selector(
                &db,
                &catalog,
                &source_aliases,
                &requested_family,
                regular_weight,
                Style::Normal,
                &mut warnings,
            );
            // A core build deliberately omits the bundled default font. Its persisted config can still
            // point at that resource, so fall back to fontdb's platform sans-serif selection instead of
            // rejecting engine creation. load_system_fonts() also supplies Segoe UI Emoji, Apple Color
            // Emoji, or the installed Linux emoji font without shipping a second copy in the mod.
            let system_sans_id = db.query(&Query {
                families: &[DbFamily::SansSerif],
                ..Query::default()
            });
            let primary_id = primary_match
                .as_ref()
                .map(|face| face.0)
                .or(byte_primary_id)
                .or(system_sans_id);
            let fallback_primary = primary_id.and_then(|id| {
                selection_from_face(&db, &catalog, id, Some((regular_weight.0, Style::Normal)))
            });
            let base_face = primary_match.map(|face| face.1).or(fallback_primary).ok_or_else(|| {
            format!("configured font family '{requested_family}' was not found and no font data was supplied")
        })?;
            if !requested_key.is_empty()
                && resolve_selector_id(&catalog, &source_aliases, &requested_family).is_none()
            {
                warnings.push(format!(
                    "primary selector '{requested_family}' was not found; using '{}'",
                    base_face.post_script_name
                ));
            }
            let primary_family = base_face.family.clone();
            db.set_sans_serif_family(primary_family.clone());
            let configured_fallback = ConfiguredFallback::new(resolve_fallback_names(
                &db,
                &catalog,
                &source_aliases,
                &requested_fallbacks,
                &mut warnings,
            ));
            let automatic = build_automatic_faces(&db, &catalog, &base_face);
            let mut faces = automatic.clone();
            for (index, selector) in overrides.iter().enumerate() {
                if selector.trim().is_empty() {
                    continue;
                }
                let (desired_weight, desired_style) = override_request(
                    &automatic,
                    index,
                    variant_overrides_only_switch_font != 0,
                );
                match resolve_selector(
                    &db,
                    &catalog,
                    &source_aliases,
                    selector,
                    desired_weight,
                    desired_style,
                    &mut warnings,
                ) {
                    Some((_, resolved)) => faces[index] = resolved,
                    None => warnings.push(format!(
                        "{} override '{}' was not found; using '{}'",
                        style_slot_name(index),
                        selector,
                        automatic[index].post_script_name
                    )),
                }
            }
            let locale: String = env.get_string(&locale).map_err(|e| e.to_string())?.into();
            let font_system =
                FontSystem::new_with_locale_and_db_and_fallback(locale, db, configured_fallback);

            let engine = Box::new(Engine {
                font_system: Mutex::new(font_system),
                swash_cache: Mutex::new(SwashCache::new()),
                primary_family,
                faces,
                resolution_warnings: warnings.join("\n"),
                font_size: font_size.max(1.0),
            });
            Ok(Box::into_raw(engine) as jlong)
        },
    ));

    match result {
        Ok(Ok(handle)) => handle,
        Ok(Err(message)) => {
            throw(&mut env, &message);
            0
        }
        Err(_) => {
            throw(&mut env, "panic while creating cosmic-text engine");
            0
        }
    }
}

fn read_string_array(env: &mut JNIEnv, values: &JObjectArray) -> Result<Vec<String>, String> {
    let count = env.get_array_length(values).map_err(|e| e.to_string())?;
    let mut strings = Vec::with_capacity(count as usize);
    for index in 0..count {
        let object = env
            .get_object_array_element(values, index)
            .map_err(|e| e.to_string())?;
        let value: String = env
            .get_string(&JString::from(object))
            .map_err(|e| e.to_string())?
            .into();
        let value = value.trim();
        if !value.is_empty() {
            strings.push(value.to_string());
        }
    }
    Ok(strings)
}

struct ConfiguredFallback {
    configured: Vec<&'static str>,
    common: Vec<&'static str>,
    platform: PlatformFallback,
}

impl ConfiguredFallback {
    fn new(configured: Vec<String>) -> Self {
        let configured: Vec<&'static str> = configured
            .into_iter()
            .map(|name| Box::leak(name.into_boxed_str()) as &'static str)
            .collect();
        let platform = PlatformFallback;
        let mut common = configured.clone();
        common.extend(platform.common_fallback());
        Self {
            configured,
            common,
            platform,
        }
    }
}

impl Fallback for ConfiguredFallback {
    fn common_fallback(&self) -> &[&'static str] {
        &self.common
    }

    fn forbidden_fallback(&self) -> &[&'static str] {
        self.platform.forbidden_fallback()
    }

    fn script_fallback(&self, _script: Script, _locale: &str) -> &[&'static str] {
        &self.configured
    }
}

fn override_request(
    automatic: &[ResolvedFace; 4],
    index: usize,
    variant_overrides_only_switch_font: bool,
) -> (Weight, Style) {
    if variant_overrides_only_switch_font && index != 0 {
        (Weight::NORMAL, Style::Normal)
    } else {
        (automatic[index].weight, automatic[index].style)
    }
}

fn normalize_font_name(value: &str) -> String {
    value
        .chars()
        .filter(|ch| ch.is_alphanumeric())
        .flat_map(char::to_lowercase)
        .collect()
}

fn configured_weight(value: jint, fallback: Weight) -> Weight {
    if value <= 0 {
        fallback
    } else {
        Weight((value as u16).clamp(1, 1000))
    }
}

fn build_face_catalog(db: &cosmic_text::fontdb::Database) -> Vec<FaceRecord> {
    db.faces()
        .map(|face| {
            let family_keys = face
                .families
                .iter()
                .map(|family| normalize_font_name(&family.0))
                .collect::<Vec<_>>();
            let mut face_keys = vec![normalize_font_name(&face.post_script_name)];
            let mut named_weights = Vec::new();
            let mut weight_range = None;
            let _ = db.with_face_data(face.id, |data, index| {
                let Ok(font) = skrifa::FontRef::from_index(data, index) else {
                    return;
                };
                for id in [StringId::FULL_NAME, StringId::POSTSCRIPT_NAME] {
                    for value in font.localized_strings(id) {
                        let key = normalize_font_name(&value.to_string());
                        if !key.is_empty() && !face_keys.contains(&key) {
                            face_keys.push(key);
                        }
                    }
                }
                // Some Windows fonts expose each non-RIBBI weight as a legacy family (name ID 1)
                // while also providing a shared typographic family (name ID 16). Treat only the
                // differing legacy value as a face alias; the shared family must keep CSS-like
                // weight selection semantics.
                for value in font.localized_strings(StringId::FAMILY_NAME) {
                    let key = normalize_font_name(&value.to_string());
                    if !key.is_empty() && !family_keys.contains(&key) && !face_keys.contains(&key) {
                        face_keys.push(key);
                    }
                }
                let typographic_families = font
                    .localized_strings(StringId::TYPOGRAPHIC_FAMILY_NAME)
                    .map(|value| value.to_string())
                    .collect::<Vec<_>>();
                let typographic_subfamilies = font
                    .localized_strings(StringId::TYPOGRAPHIC_SUBFAMILY_NAME)
                    .map(|value| value.to_string())
                    .collect::<Vec<_>>();
                for family in &typographic_families {
                    for subfamily in &typographic_subfamilies {
                        let key = normalize_font_name(&format!("{family} {subfamily}"));
                        if !key.is_empty() && !face_keys.contains(&key) {
                            face_keys.push(key);
                        }
                    }
                }
                let axes = font.axes().iter().collect::<Vec<_>>();
                if let Some(axis) = axes
                    .iter()
                    .find(|axis| axis.tag() == skrifa::Tag::new(b"wght"))
                {
                    weight_range = Some((
                        axis.min_value().round().clamp(1.0, u16::MAX as f32) as u16,
                        axis.max_value().round().clamp(1.0, u16::MAX as f32) as u16,
                    ));
                    for instance in font.named_instances().iter() {
                        let coords = instance.user_coords().collect::<Vec<_>>();
                        let Some(weight_index) = axes
                            .iter()
                            .position(|axis| axis.tag() == skrifa::Tag::new(b"wght"))
                        else {
                            continue;
                        };
                        let Some(weight) = coords.get(weight_index) else {
                            continue;
                        };
                        let ignored_axes = axes.iter().zip(coords.iter()).enumerate().any(
                            |(axis_index, (axis, value))| {
                                axis_index != weight_index
                                    && (*value - axis.default_value()).abs() > f32::EPSILON
                            },
                        );
                        let subfamily = font
                            .localized_strings(instance.subfamily_name_id())
                            .english_or_first()
                            .map(|value| value.to_string());
                        if let Some(subfamily) = subfamily {
                            let family = face
                                .families
                                .first()
                                .map(|family| family.0.as_str())
                                .unwrap_or("");
                            for value in [subfamily.clone(), format!("{family} {subfamily}")] {
                                let key = normalize_font_name(&value);
                                if !key.is_empty() {
                                    named_weights.push(NamedWeight {
                                        key,
                                        weight: weight.round().clamp(1.0, u16::MAX as f32) as u16,
                                        ignored_axes,
                                    });
                                }
                            }
                        }
                        if let Some(postscript_id) = instance.postscript_name_id() {
                            if let Some(value) =
                                font.localized_strings(postscript_id).english_or_first()
                            {
                                named_weights.push(NamedWeight {
                                    key: normalize_font_name(&value.to_string()),
                                    weight: weight.round().clamp(1.0, u16::MAX as f32) as u16,
                                    ignored_axes,
                                });
                            }
                        }
                    }
                }
            });
            FaceRecord {
                id: face.id,
                family_keys,
                face_keys,
                named_weights,
                weight_range,
            }
        })
        .collect()
}

fn resolve_selector_id(
    catalog: &[FaceRecord],
    source_aliases: &HashMap<String, Vec<ID>>,
    selector: &str,
) -> Option<ID> {
    let key = normalize_font_name(selector);
    source_aliases
        .get(&key)
        .and_then(|ids| ids.first().copied())
        .or_else(|| {
            catalog
                .iter()
                .find(|record| {
                    record.face_keys.contains(&key)
                        || record
                            .named_weights
                            .iter()
                            .any(|instance| instance.key == key)
                })
                .map(|record| record.id)
        })
        .or_else(|| {
            catalog
                .iter()
                .find(|record| record.family_keys.contains(&key))
                .map(|record| record.id)
        })
}

fn resolve_selector(
    db: &cosmic_text::fontdb::Database,
    catalog: &[FaceRecord],
    source_aliases: &HashMap<String, Vec<ID>>,
    selector: &str,
    desired_weight: Weight,
    desired_style: Style,
    warnings: &mut Vec<String>,
) -> Option<(ID, ResolvedFace)> {
    let key = normalize_font_name(selector);
    if key.is_empty() {
        return None;
    }
    if let Some(ids) = source_aliases.get(&key) {
        let id = best_id(
            db,
            catalog,
            ids.iter().copied(),
            desired_weight,
            desired_style,
        )?;
        return selection_from_face(db, catalog, id, Some((desired_weight.0, desired_style)))
            .map(|face| (id, face));
    }
    for record in catalog {
        if let Some(instance) = record
            .named_weights
            .iter()
            .find(|instance| instance.key == key)
        {
            if instance.ignored_axes {
                warnings.push(format!(
                    "variable instance '{selector}' uses non-wght axes; only wght={} is applied",
                    instance.weight
                ));
            }
            let face = selection_from_face(
                db,
                catalog,
                record.id,
                Some((instance.weight, desired_style)),
            )?;
            return Some((record.id, face));
        }
    }
    if let Some(family) = catalog
        .iter()
        .find(|record| record.family_keys.contains(&key))
        .and_then(|record| db.face(record.id))
        .and_then(|face| {
            face.families
                .iter()
                .find(|family| normalize_font_name(&family.0) == key)
        })
        .map(|family| family.0.clone())
    {
        return selection_for_family(db, catalog, &family, desired_weight, desired_style)
            .map(|face| (face.0, face.1));
    }
    catalog
        .iter()
        .find(|record| record.face_keys.contains(&key))
        .and_then(|record| {
            selection_from_face(
                db,
                catalog,
                record.id,
                Some((desired_weight.0, desired_style)),
            )
            .map(|face| (record.id, face))
        })
}

fn resolve_fallback_names(
    db: &cosmic_text::fontdb::Database,
    catalog: &[FaceRecord],
    source_aliases: &HashMap<String, Vec<ID>>,
    selectors: &[String],
    warnings: &mut Vec<String>,
) -> Vec<String> {
    let mut names = Vec::new();
    for selector in selectors {
        if selector.trim().is_empty() {
            continue;
        }
        match resolve_selector(
            db,
            catalog,
            source_aliases,
            selector,
            Weight::NORMAL,
            Style::Normal,
            warnings,
        ) {
            Some((_, face)) => {
                if !names.iter().any(|name| name == &face.family) {
                    names.push(face.family);
                }
            }
            None => warnings.push(format!("fallback selector '{selector}' was not found")),
        }
    }
    names
}

fn best_id(
    db: &cosmic_text::fontdb::Database,
    catalog: &[FaceRecord],
    ids: impl Iterator<Item = ID>,
    desired_weight: Weight,
    desired_style: Style,
) -> Option<ID> {
    ids.filter_map(|id| {
        let face = db.face(id)?;
        let record = catalog.iter().find(|record| record.id == id)?;
        let weight_diff = if let Some((min, max)) = record.weight_range {
            desired_weight.0.abs_diff(desired_weight.0.clamp(min, max))
        } else {
            face.weight.0.abs_diff(desired_weight.0)
        };
        let style_diff = if face.style == desired_style { 0 } else { 1 };
        Some(((style_diff, weight_diff), id))
    })
    .min_by_key(|entry| entry.0)
    .map(|entry| entry.1)
}

fn selection_from_face(
    db: &cosmic_text::fontdb::Database,
    catalog: &[FaceRecord],
    id: ID,
    requested: Option<(u16, Style)>,
) -> Option<ResolvedFace> {
    let face = db.face(id)?;
    let record = catalog.iter().find(|record| record.id == id)?;
    let (weight, style) = requested.unwrap_or((face.weight.0, face.style));
    let weight = record
        .weight_range
        .map(|(min, max)| weight.clamp(min, max))
        .unwrap_or(weight);
    let family = face.families.first()?.0.clone();
    let post_script_name = face.post_script_name.clone();
    let render_family = render_family_for_face(db, id, &family, &post_script_name, Weight(weight), style);
    Some(ResolvedFace {
        id,
        family,
        render_family,
        weight: Weight(weight),
        style,
        post_script_name,
    })
}

fn render_family_for_face(
    db: &cosmic_text::fontdb::Database,
    id: ID,
    family: &str,
    post_script_name: &str,
    weight: Weight,
    style: Style,
) -> String {
    let postscript_match = db.query(&Query {
        families: &[DbFamily::Name(post_script_name)],
        weight,
        style,
        ..Query::default()
    });
    if postscript_match == Some(id) {
        post_script_name.to_string()
    } else {
        family.to_string()
    }
}

fn selection_for_family(
    db: &cosmic_text::fontdb::Database,
    catalog: &[FaceRecord],
    family: &str,
    desired_weight: Weight,
    desired_style: Style,
) -> Option<(ID, ResolvedFace)> {
    let family_key = normalize_font_name(family);
    let ids = catalog
        .iter()
        .filter(|record| record.family_keys.contains(&family_key))
        .map(|record| record.id);
    let id = best_id(db, catalog, ids, desired_weight, desired_style)?;
    let mut resolved =
        selection_from_face(db, catalog, id, Some((desired_weight.0, desired_style)))?;
    resolved.family = family.to_string();
    Some((id, resolved))
}

fn build_automatic_faces(
    db: &cosmic_text::fontdb::Database,
    catalog: &[FaceRecord],
    base: &ResolvedFace,
) -> [ResolvedFace; 4] {
    let desired = [
        (base.weight, base.style),
        (Weight::BOLD, base.style),
        (base.weight, Style::Italic),
        (Weight::BOLD, Style::Italic),
    ];
    desired.map(|(weight, style)| {
        selection_for_family(db, catalog, &base.family, weight, style)
            .map(|entry| entry.1)
            .unwrap_or_else(|| {
                let mut fallback = base.clone();
                fallback.weight = weight;
                fallback.style = style;
                fallback
            })
    })
}

fn style_slot_name(index: usize) -> &'static str {
    ["regular", "bold", "italic", "boldItalic"]
        .get(index)
        .copied()
        .unwrap_or("unknown")
}

#[no_mangle]
pub extern "system" fn Java_neofontrender_core_font_cosmic_CosmicNative_destroyEngine(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle != 0 {
        unsafe { drop(Box::from_raw(handle as *mut Engine)) };
    }
}

#[no_mangle]
pub extern "system" fn Java_neofontrender_core_font_cosmic_CosmicNative_primaryFamily(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jstring {
    if handle == 0 {
        throw(&mut env, "cosmic-text engine is closed");
        return ptr::null_mut();
    }
    let engine = unsafe { &*(handle as *const Engine) };
    match env.new_string(&engine.primary_family) {
        Ok(value) => value.into_raw(),
        Err(error) => {
            throw(&mut env, &error.to_string());
            ptr::null_mut()
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_neofontrender_core_font_cosmic_CosmicNative_resolvedFace(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    style_flags: jint,
) -> jstring {
    if handle == 0 {
        throw(&mut env, "cosmic-text engine is closed");
        return ptr::null_mut();
    }
    let engine = unsafe { &*(handle as *const Engine) };
    let face = &engine.faces[(style_flags & 3) as usize];
    let style = match face.style {
        Style::Normal => "normal",
        Style::Italic => "italic",
        Style::Oblique => "oblique",
    };
    let description = format!(
        "{}|{}|{}|{}",
        face.post_script_name, face.family, face.weight.0, style
    );
    match env.new_string(description) {
        Ok(value) => value.into_raw(),
        Err(error) => {
            throw(&mut env, &error.to_string());
            ptr::null_mut()
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_neofontrender_core_font_cosmic_CosmicNative_resolutionWarnings(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jstring {
    if handle == 0 {
        throw(&mut env, "cosmic-text engine is closed");
        return ptr::null_mut();
    }
    let engine = unsafe { &*(handle as *const Engine) };
    match env.new_string(&engine.resolution_warnings) {
        Ok(value) => value.into_raw(),
        Err(error) => {
            throw(&mut env, &error.to_string());
            ptr::null_mut()
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_neofontrender_core_font_cosmic_CosmicNative_measure(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    text: JString,
    style_flags: jint,
) -> jfloat {
    match with_text(&mut env, handle, text, style_flags, 1.0, |buffer, _, _| {
        Ok(buffer
            .layout_runs()
            .map(|run| run.line_w)
            .fold(0.0_f32, f32::max))
    }) {
        Ok(width) => width,
        Err(message) => {
            throw(&mut env, &message);
            0.0
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_neofontrender_core_font_cosmic_CosmicNative_render(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    text: JString,
    argb: jint,
    style_flags: jint,
    raster_scale: jfloat,
) -> jbyteArray {
    let result = with_text(
        &mut env,
        handle,
        text,
        style_flags,
        raster_scale.max(1.0),
        |buffer, engine, scale| rasterize(buffer, engine, argb as u32, scale),
    );
    match result {
        Ok(bytes) => match env.byte_array_from_slice(&bytes) {
            Ok(array) => array.into_raw(),
            Err(error) => {
                throw(&mut env, &error.to_string());
                ptr::null_mut()
            }
        },
        Err(message) => {
            throw(&mut env, &message);
            ptr::null_mut()
        }
    }
}

fn with_text<T, F>(
    env: &mut JNIEnv,
    handle: jlong,
    text: JString,
    style_flags: jint,
    raster_scale: f32,
    operation: F,
) -> Result<T, String>
where
    F: FnOnce(&mut Buffer, &Engine, f32) -> Result<T, String>,
{
    std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        if handle == 0 {
            return Err("cosmic-text engine is closed".to_string());
        }
        let engine = unsafe { &*(handle as *const Engine) };
        let value: String = env.get_string(&text).map_err(|e| e.to_string())?.into();
        let scale = raster_scale.max(1.0);
        let font_size = engine.font_size * scale;
        let metrics = Metrics::new(font_size, font_size * 1.4);
        let mut font_system = engine
            .font_system
            .lock()
            .map_err(|_| "font system lock poisoned".to_string())?;
        let mut buffer = Buffer::new(&mut font_system, metrics);
        buffer.set_size(None, Some(metrics.line_height * 2.0));
        // Each style slot is resolved once at engine creation. Keeping the requested weight here
        // is essential for variable fonts whose OS/2 default weight differs from the desired
        // `wght` coordinate.
        let face = &engine.faces[(style_flags & 3) as usize];
        let attrs = Attrs::new()
            .family(Family::Name(&face.render_family))
            .weight(face.weight)
            .style(face.style);
        buffer.set_text(&value, &attrs, Shaping::Advanced, None);
        buffer.shape_until_scroll(&mut font_system, false);
        drop(font_system);
        operation(&mut buffer, engine, scale)
    }))
    .map_err(|_| "panic in cosmic-text operation".to_string())?
}

fn rasterize(
    buffer: &mut Buffer,
    engine: &Engine,
    argb: u32,
    scale: f32,
) -> Result<Vec<u8>, String> {
    let mut advance_px = 0.0_f32;
    let mut baseline_px = 0.0_f32;
    for (index, run) in buffer.layout_runs().enumerate() {
        advance_px = advance_px.max(run.line_w);
        if index == 0 {
            baseline_px = run.line_y;
        }
    }
    let color = Color::rgba(
        ((argb >> 16) & 0xFF) as u8,
        ((argb >> 8) & 0xFF) as u8,
        (argb & 0xFF) as u8,
        ((argb >> 24) & 0xFF) as u8,
    );
    let mut rects = Vec::new();
    let mut cache = engine
        .swash_cache
        .lock()
        .map_err(|_| "swash cache lock poisoned".to_string())?;
    {
        let mut font_system = engine
            .font_system
            .lock()
            .map_err(|_| "font system lock poisoned".to_string())?;
        buffer.draw(&mut font_system, &mut cache, color, |x, y, w, h, pixel| {
            if pixel.a() != 0 && w != 0 && h != 0 {
                rects.push(PixelRect {
                    x,
                    y,
                    w,
                    h,
                    color: pixel,
                });
            }
        });
    }

    if rects.is_empty() {
        return Ok(encode_raster(
            0,
            0,
            0,
            0,
            advance_px / scale,
            baseline_px / scale,
            scale,
            &[],
        ));
    }
    // Keep transparent texels around the tight glyph bounds. Java draws the complete 0..1 UV
    // range with linear minification; without this guard, edge taps can repeat top-row coverage at
    // the bottom of a line and show up as isolated bright pixels below otherwise normal text.
    let min_x = rects
        .iter()
        .map(|r| r.x)
        .min()
        .unwrap_or(0)
        .saturating_sub(RASTER_PADDING);
    let min_y = rects
        .iter()
        .map(|r| r.y)
        .min()
        .unwrap_or(0)
        .saturating_sub(RASTER_PADDING);
    let max_x = rects
        .iter()
        .map(|r| r.x.saturating_add(r.w as i32))
        .max()
        .unwrap_or(0)
        .saturating_add(RASTER_PADDING);
    let max_y = rects
        .iter()
        .map(|r| r.y.saturating_add(r.h as i32))
        .max()
        .unwrap_or(0)
        .saturating_add(RASTER_PADDING);
    let width = max_x.saturating_sub(min_x);
    let height = max_y.saturating_sub(min_y);
    if width <= 0 || height <= 0 || width > MAX_TEXTURE_DIMENSION || height > MAX_TEXTURE_DIMENSION
    {
        return Err(format!("invalid cosmic-text raster size {width}x{height}"));
    }
    let mut pixels = vec![0_u32; width as usize * height as usize];
    for rect in rects {
        let x0 = (rect.x - min_x).max(0);
        let y0 = (rect.y - min_y).max(0);
        let x1 = (x0 + rect.w as i32).min(width);
        let y1 = (y0 + rect.h as i32).min(height);
        let src = ((rect.color.a() as u32) << 24)
            | ((rect.color.r() as u32) << 16)
            | ((rect.color.g() as u32) << 8)
            | rect.color.b() as u32;
        for y in y0..y1 {
            for x in x0..x1 {
                let index = y as usize * width as usize + x as usize;
                pixels[index] = blend_src_over(pixels[index], src);
            }
        }
    }
    // Swash returns straight RGB plus coverage alpha. Texture minification is only mathematically
    // correct when those channels are premultiplied before bilinear sampling.
    for pixel in &mut pixels {
        *pixel = premultiply(*pixel);
    }
    encode_raster(
        width,
        height,
        min_x,
        min_y,
        advance_px / scale,
        baseline_px / scale,
        scale,
        &pixels,
    )
    .pipe(Ok)
}

fn premultiply(pixel: u32) -> u32 {
    let alpha = (pixel >> 24) & 0xFF;
    let channel = |shift: u32| (((pixel >> shift) & 0xFF) * alpha + 127) / 255;
    (alpha << 24) | (channel(16) << 16) | (channel(8) << 8) | channel(0)
}

fn blend_src_over(dst: u32, src: u32) -> u32 {
    let sa = (src >> 24) & 0xFF;
    if sa == 255 || dst == 0 {
        return src;
    }
    let da = (dst >> 24) & 0xFF;
    let out_a = sa + da * (255 - sa) / 255;
    if out_a == 0 {
        return 0;
    }
    let blend = |shift: u32| {
        let sc = (src >> shift) & 0xFF;
        let dc = (dst >> shift) & 0xFF;
        (sc * sa + dc * da * (255 - sa) / 255) / out_a
    };
    (out_a << 24) | (blend(16) << 16) | (blend(8) << 8) | blend(0)
}

#[allow(clippy::too_many_arguments)]
fn encode_raster(
    width: i32,
    height: i32,
    offset_x: i32,
    offset_y: i32,
    advance: f32,
    baseline: f32,
    scale: f32,
    pixels: &[u32],
) -> Vec<u8> {
    let mut bytes = Vec::with_capacity(HEADER_SIZE + pixels.len() * 4);
    bytes.extend_from_slice(&RASTER_MAGIC.to_le_bytes());
    bytes.extend_from_slice(&width.to_le_bytes());
    bytes.extend_from_slice(&height.to_le_bytes());
    bytes.extend_from_slice(&offset_x.to_le_bytes());
    bytes.extend_from_slice(&offset_y.to_le_bytes());
    bytes.extend_from_slice(&advance.to_le_bytes());
    bytes.extend_from_slice(&baseline.to_le_bytes());
    bytes.extend_from_slice(&scale.to_le_bytes());
    for pixel in pixels {
        bytes.extend_from_slice(&pixel.to_le_bytes());
    }
    bytes
}

fn throw(env: &mut JNIEnv, message: &str) {
    let _ = env.throw_new("java/lang/IllegalStateException", message);
}

trait Pipe: Sized {
    fn pipe<T>(self, f: impl FnOnce(Self) -> T) -> T {
        f(self)
    }
}
impl<T> Pipe for T {}

#[cfg(test)]
mod tests {
    use super::*;
    use std::path::Path;
    use std::sync::Arc;

    fn load_test_font(path: &str) -> Option<(cosmic_text::fontdb::Database, Vec<FaceRecord>)> {
        let bytes = std::fs::read(Path::new(path)).ok()?;
        let mut db = cosmic_text::fontdb::Database::new();
        db.load_font_source(cosmic_text::fontdb::Source::Binary(Arc::new(bytes)));
        let catalog = build_face_catalog(&db);
        Some((db, catalog))
    }

    #[test]
    fn normalizes_face_names_without_style_punctuation() {
        assert_eq!(normalize_font_name("MiSans Demi-Bold"), "misansdemibold");
        assert_eq!(normalize_font_name("  Noto Sans SC  "), "notosanssc");
    }

    #[test]
    fn variant_override_can_only_switch_font() {
        let face = |weight, style| ResolvedFace {
            id: ID::dummy(),
            family: "Example".to_string(),
            render_family: "Example".to_string(),
            weight,
            style,
            post_script_name: "Example".to_string(),
        };
        let automatic = [
            face(Weight::NORMAL, Style::Normal),
            face(Weight::BOLD, Style::Normal),
            face(Weight::NORMAL, Style::Italic),
            face(Weight::BOLD, Style::Italic),
        ];

        assert_eq!(
            override_request(&automatic, 0, true),
            (Weight::NORMAL, Style::Normal)
        );
        assert_eq!(
            override_request(&automatic, 1, true),
            (Weight::NORMAL, Style::Normal)
        );
        assert_eq!(
            override_request(&automatic, 2, true),
            (Weight::NORMAL, Style::Normal)
        );
        assert_eq!(
            override_request(&automatic, 3, true),
            (Weight::NORMAL, Style::Normal)
        );
        assert_eq!(
            override_request(&automatic, 3, false),
            (Weight::BOLD, Style::Italic)
        );
    }

    #[test]
    fn resolves_misans_typographic_subfamily_when_installed() {
        let Some((db, catalog)) = load_test_font(r"C:\Windows\Fonts\MiSans-Demibold.ttf") else {
            return;
        };
        let mut warnings = Vec::new();
        let resolved = resolve_selector(
            &db,
            &catalog,
            &HashMap::new(),
            "MiSans DemiBold",
            Weight::NORMAL,
            Style::Normal,
            &mut warnings,
        )
        .expect("MiSans Demibold face should resolve")
        .1;
        assert_eq!(resolved.family, "MiSans");
        assert_eq!(resolved.weight, Weight::SEMIBOLD);
        assert_eq!(resolved.style, Style::Normal);
    }

    #[test]
    fn keeps_requested_weight_for_variable_family_when_installed() {
        let Some((db, catalog)) = load_test_font(r"C:\Windows\Fonts\NotoSansSC-VF.ttf") else {
            return;
        };
        let aliases = HashMap::new();
        let mut warnings = Vec::new();
        let regular = resolve_selector(
            &db,
            &catalog,
            &aliases,
            "Noto Sans SC",
            Weight::NORMAL,
            Style::Normal,
            &mut warnings,
        )
        .expect("variable family should resolve")
        .1;
        assert_eq!(regular.weight, Weight::NORMAL);

        let faces = build_automatic_faces(&db, &catalog, &regular);
        assert_eq!(faces[0].weight, Weight::NORMAL);
        assert_eq!(faces[1].weight, Weight::BOLD);

        let medium = resolve_selector(
            &db,
            &catalog,
            &aliases,
            "Noto Sans SC Medium",
            Weight::NORMAL,
            Style::Normal,
            &mut warnings,
        )
        .expect("variable named instance should resolve")
        .1;
        assert_eq!(medium.weight, Weight::MEDIUM);
    }

    #[test]
    fn configured_weight_selects_variable_wght_when_installed() {
        let Some((db, catalog)) = load_test_font(r"C:\Windows\Fonts\NotoSansSC-VF.ttf") else {
            return;
        };
        let mut warnings = Vec::new();
        let resolved = resolve_selector(
            &db,
            &catalog,
            &HashMap::new(),
            "Noto Sans SC",
            configured_weight(350, Weight::NORMAL),
            Style::Normal,
            &mut warnings,
        )
        .expect("variable family should resolve")
        .1;
        assert_eq!(resolved.weight, Weight(350));
    }

    #[test]
    fn render_family_prefers_queryable_postscript_name_when_installed() {
        let Some((db, catalog)) = load_test_font(r"C:\Windows\Fonts\MiSans-Demibold.ttf") else {
            return;
        };
        let mut warnings = Vec::new();
        let resolved = resolve_selector(
            &db,
            &catalog,
            &HashMap::new(),
            "MiSans DemiBold",
            Weight::NORMAL,
            Style::Normal,
            &mut warnings,
        )
        .expect("MiSans Demibold face should resolve")
        .1;
        assert_eq!(resolved.render_family, resolved.post_script_name);
    }

    #[test]
    fn buffer_layout_uses_resolved_render_family_when_installed() {
        let Some((db, catalog)) = load_test_font(r"C:\Windows\Fonts\MiSans-Demibold.ttf") else {
            return;
        };
        let mut warnings = Vec::new();
        let resolved = resolve_selector(
            &db,
            &catalog,
            &HashMap::new(),
            "MiSans DemiBold",
            Weight::NORMAL,
            Style::Normal,
            &mut warnings,
        )
        .expect("MiSans Demibold face should resolve")
        .1;
        let mut font_system = FontSystem::new_with_locale_and_db("zh-CN".to_string(), db);
        let metrics = Metrics::new(16.0, 22.0);
        let mut buffer = Buffer::new(&mut font_system, metrics);
        let attrs = Attrs::new()
            .family(Family::Name(&resolved.render_family))
            .weight(resolved.weight)
            .style(resolved.style);
        buffer.set_text("abc", &attrs, Shaping::Advanced, None);
        buffer.shape_until_scroll(&mut font_system, false);

        let used_id = buffer
            .layout_runs()
            .flat_map(|run| run.glyphs.iter())
            .map(|glyph| glyph.font_id)
            .next();
        assert_eq!(used_id, Some(resolved.id));
    }

    #[test]
    fn times_new_roman_layout_uses_resolved_render_family_when_installed() {
        let mut db = cosmic_text::fontdb::Database::new();
        db.load_system_fonts();
        let catalog = build_face_catalog(&db);
        let mut warnings = Vec::new();
        let Some((_, resolved)) = resolve_selector(
            &db,
            &catalog,
            &HashMap::new(),
            "Times New Roman",
            Weight::NORMAL,
            Style::Normal,
            &mut warnings,
        ) else {
            return;
        };
        let mut font_system = FontSystem::new_with_locale_and_db("en-US".to_string(), db);
        let metrics = Metrics::new(16.0, 22.0);
        let mut buffer = Buffer::new(&mut font_system, metrics);
        let attrs = Attrs::new()
            .family(Family::Name(&resolved.render_family))
            .weight(resolved.weight)
            .style(resolved.style);
        buffer.set_text("Times", &attrs, Shaping::Advanced, None);
        buffer.shape_until_scroll(&mut font_system, false);

        let used_id = buffer
            .layout_runs()
            .flat_map(|run| run.glyphs.iter())
            .map(|glyph| glyph.font_id)
            .next();
        assert_eq!(used_id, Some(resolved.id));
    }
}
