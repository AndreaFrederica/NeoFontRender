use cosmic_text::{Attrs, Buffer, Color, Family, FontSystem, Metrics, Shaping, Style, SwashCache, Weight};
use jni::objects::{JByteArray, JClass, JObjectArray, JString};
use jni::sys::{jbyteArray, jfloat, jint, jlong, jstring};
use jni::JNIEnv;
use std::ptr;
use std::sync::Mutex;

const ABI_VERSION: jint = 3;
const RASTER_MAGIC: i32 = 0x434F534D; // "COSM"
const HEADER_SIZE: usize = 32;
const MAX_TEXTURE_DIMENSION: i32 = 8192;
const RASTER_PADDING: i32 = 2;

struct Engine {
    font_system: Mutex<FontSystem>,
    swash_cache: Mutex<SwashCache>,
    primary_family: String,
    font_size: f32,
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
    primary_family: JString,
    font_size: jfloat,
    locale: JString,
) -> jlong {
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> Result<jlong, String> {
        let count = env.get_array_length(&fonts).map_err(|e| e.to_string())?;
        let mut font_data = Vec::with_capacity(count as usize);
        for index in 0..count {
            let object = env.get_object_array_element(&fonts, index).map_err(|e| e.to_string())?;
            let bytes = env.convert_byte_array(JByteArray::from(object)).map_err(|e| e.to_string())?;
            if !bytes.is_empty() {
                font_data.push(bytes);
            }
        }
        let requested_family: String = env.get_string(&primary_family).map_err(|e| e.to_string())?.into();

        // Load byte-backed resources first so bundled fallbacks retain priority over arbitrary
        // host fallback fonts. We still load system fonts because config values such as
        // "JetBrains Mono" are family names, not readable font paths.
        let mut db = cosmic_text::fontdb::Database::new();
        let mut byte_primary_id = None;
        for (index, bytes) in font_data.into_iter().enumerate() {
            let ids = db.load_font_source(cosmic_text::fontdb::Source::Binary(std::sync::Arc::new(bytes)));
            if index == 0 {
                byte_primary_id = ids.first().copied();
            }
        }
        db.load_system_fonts();

        // Never select the first face returned by a system scan. Its order is platform-specific
        // and previously made Cosmic pick Agency FB even when another family was configured.
        let requested_key = normalize_family_name(&requested_family);
        let system_primary_id = if requested_key.is_empty() || looks_like_font_location(&requested_family) {
            None
        } else {
            db.faces()
                .find(|face| face.families.iter().any(|family| {
                    normalize_family_name(&family.0) == requested_key
                }))
                .map(|face| face.id)
        };
        let primary_id = system_primary_id.or(byte_primary_id);
        let primary_family = primary_id
            .and_then(|id| db.face(id))
            .and_then(|face| face.families.first().map(|family| family.0.clone()));
        let primary_family = primary_family.ok_or_else(|| {
            format!("configured font family '{requested_family}' was not found and no font data was supplied")
        })?;
        db.set_sans_serif_family(primary_family.clone());
        let locale: String = env.get_string(&locale).map_err(|e| e.to_string())?.into();
        let font_system = FontSystem::new_with_locale_and_db(locale, db);

        let engine = Box::new(Engine {
            font_system: Mutex::new(font_system),
            swash_cache: Mutex::new(SwashCache::new()),
            primary_family,
            font_size: font_size.max(1.0),
        });
        Ok(Box::into_raw(engine) as jlong)
    }));

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

fn normalize_family_name(value: &str) -> String {
    value.chars().filter(|ch| ch.is_alphanumeric()).flat_map(char::to_lowercase).collect()
}

fn looks_like_font_location(value: &str) -> bool {
    let lower = value.to_lowercase();
    value.contains(':') || value.contains('/') || value.contains('\\')
        || lower.ends_with(".ttf") || lower.ends_with(".otf") || lower.ends_with(".ttc")
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
pub extern "system" fn Java_neofontrender_core_font_cosmic_CosmicNative_measure(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    text: JString,
    style_flags: jint,
) -> jfloat {
    match with_text(&mut env, handle, text, style_flags, 1.0, |buffer, _, _| {
        Ok(buffer.layout_runs().map(|run| run.line_w).fold(0.0_f32, f32::max))
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
    let result = with_text(&mut env, handle, text, style_flags, raster_scale.max(1.0),
        |buffer, engine, scale| rasterize(buffer, engine, argb as u32, scale));
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
        let mut font_system = engine.font_system.lock().map_err(|_| "font system lock poisoned".to_string())?;
        let mut buffer = Buffer::new(&mut font_system, metrics);
        buffer.set_size(None, Some(metrics.line_height * 2.0));
        // Use the configured primary font by its parsed internal family name. Relying on the
        // generic SansSerif alias allowed fontdb fallback ordering to select a later font face.
        let mut attrs = Attrs::new().family(Family::Name(&engine.primary_family));
        if style_flags & 1 != 0 {
            attrs = attrs.weight(Weight::BOLD);
        }
        if style_flags & 2 != 0 {
            attrs = attrs.style(Style::Italic);
        }
        buffer.set_text(&value, &attrs, Shaping::Advanced, None);
        buffer.shape_until_scroll(&mut font_system, false);
        drop(font_system);
        operation(&mut buffer, engine, scale)
    }))
    .map_err(|_| "panic in cosmic-text operation".to_string())?
}

fn rasterize(buffer: &mut Buffer, engine: &Engine, argb: u32, scale: f32) -> Result<Vec<u8>, String> {
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
    let mut cache = engine.swash_cache.lock().map_err(|_| "swash cache lock poisoned".to_string())?;
    {
        let mut font_system = engine.font_system.lock().map_err(|_| "font system lock poisoned".to_string())?;
        buffer.draw(&mut font_system, &mut cache, color, |x, y, w, h, pixel| {
            if pixel.a() != 0 && w != 0 && h != 0 {
                rects.push(PixelRect { x, y, w, h, color: pixel });
            }
        });
    }

    if rects.is_empty() {
        return Ok(encode_raster(0, 0, 0, 0, advance_px / scale, baseline_px / scale, scale, &[]));
    }
    // Keep transparent texels around the tight glyph bounds. Java draws the complete 0..1 UV
    // range with linear minification; without this guard, edge taps can repeat top-row coverage at
    // the bottom of a line and show up as isolated bright pixels below otherwise normal text.
    let min_x = rects.iter().map(|r| r.x).min().unwrap_or(0).saturating_sub(RASTER_PADDING);
    let min_y = rects.iter().map(|r| r.y).min().unwrap_or(0).saturating_sub(RASTER_PADDING);
    let max_x = rects.iter().map(|r| r.x.saturating_add(r.w as i32)).max().unwrap_or(0)
        .saturating_add(RASTER_PADDING);
    let max_y = rects.iter().map(|r| r.y.saturating_add(r.h as i32)).max().unwrap_or(0)
        .saturating_add(RASTER_PADDING);
    let width = max_x.saturating_sub(min_x);
    let height = max_y.saturating_sub(min_y);
    if width <= 0 || height <= 0 || width > MAX_TEXTURE_DIMENSION || height > MAX_TEXTURE_DIMENSION {
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
    encode_raster(width, height, min_x, min_y, advance_px / scale, baseline_px / scale, scale, &pixels).pipe(Ok)
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

fn encode_raster(width: i32, height: i32, offset_x: i32, offset_y: i32,
                 advance: f32, baseline: f32, scale: f32, pixels: &[u32]) -> Vec<u8> {
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
    fn pipe<T>(self, f: impl FnOnce(Self) -> T) -> T { f(self) }
}
impl<T> Pipe for T {}
