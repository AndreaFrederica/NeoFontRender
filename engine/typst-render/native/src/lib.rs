//! JNI-backed Typst compiler and CPU rasterizer.

use std::collections::{HashMap, VecDeque};
use std::path::PathBuf;
use std::ptr;
use std::sync::{Arc, Mutex, OnceLock};

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jbyteArray, jfloat, jint, jlong, jobjectArray};
use typst::diag::{FileError, FileResult};
use typst::foundations::{Bytes, Datetime, Duration};
use typst::syntax::package::PackageSpec;
use typst::syntax::{FileId, Source};
use typst::text::{Font, FontBook};
use typst::utils::LazyHash;
use typst::{Library, LibraryExt, World};
use typst_kit::downloader::{
    Downloader, Progress, ProgressDownloader, ProgressReporter, SystemDownloader,
};
use typst_kit::files::FsRoot;
use typst_kit::packages::{FsPackages, SystemPackages, UniversePackages};
use typst_layout::PagedDocument;
use typst_render::{PngFormatOptions, RenderOptions};
use typst_utils::Scalar;

struct Core {
    library: LazyHash<Library>,
    fonts: Arc<Vec<Font>>,
    book: LazyHash<FontBook>,
    packages: PackageRuntime,
}

struct PackageRuntime {
    root: PathBuf,
    packages: SystemPackages,
    events: Events,
}

type Events = Arc<Mutex<VecDeque<[String; 5]>>>;

fn report(
    events: &Events,
    state: &str,
    package: &str,
    downloaded: usize,
    total: Option<usize>,
    detail: &str,
) {
    if let Ok(mut queue) = events.lock() {
        if queue.len() >= 128 {
            queue.pop_front();
        }
        queue.push_back([
            state.into(),
            package.into(),
            downloaded.to_string(),
            total.map(|n| n.to_string()).unwrap_or_default(),
            detail.into(),
        ]);
    }
}

struct DownloadProgress {
    events: Events,
    package: String,
}
impl ProgressReporter for DownloadProgress {
    fn start(&mut self, p: &Progress) {
        self.update(p);
    }
    fn update(&mut self, p: &Progress) {
        report(
            &self.events,
            "downloading",
            &self.package,
            p.downloaded,
            p.content_len,
            "",
        );
    }
    fn finish(&mut self, p: &Progress) {
        report(
            &self.events,
            "extracting",
            &self.package,
            p.downloaded,
            p.content_len,
            "",
        );
    }
}

struct ConnectingDownloader {
    inner: SystemDownloader,
    events: Events,
}
impl Downloader for ConnectingDownloader {
    fn stream(
        &self,
        key: &dyn std::any::Any,
        url: &str,
    ) -> std::io::Result<(Option<usize>, Box<dyn std::io::Read>)> {
        if let Some(spec) = key.downcast_ref::<PackageSpec>() {
            report(&self.events, "connecting", &spec.to_string(), 0, None, "");
        }
        self.inner.stream(key, url)
    }
}

impl PackageRuntime {
    fn new(root: PathBuf, events: Events) -> Result<Self, String> {
        let packages_root = root.join("packages");
        std::fs::create_dir_all(&packages_root)
            .map_err(|error| format!("could not create Typst package directory: {error}"))?;
        let progress_events = events.clone();
        let downloader = ProgressDownloader::new(
            ConnectingDownloader {
                inner: SystemDownloader::new("neofontrender-typst-renderer"),
                events: events.clone(),
            },
            move |key: &dyn std::any::Any| DownloadProgress {
                events: progress_events.clone(),
                package: key
                    .downcast_ref::<PackageSpec>()
                    .map(ToString::to_string)
                    .unwrap_or_default(),
            },
        );
        let packages = SystemPackages::from_parts(
            None,
            Some(FsPackages::new(packages_root)),
            UniversePackages::new(downloader),
        );
        Ok(Self {
            root,
            packages,
            events,
        })
    }

    fn obtain(&self, spec: &PackageSpec) -> FileResult<PathBuf> {
        let cached = self
            .packages
            .cache()
            .and_then(|cache| cache.obtain(spec))
            .is_some();
        let result = self
            .packages
            .obtain(spec)
            .map(|root| root.path().to_path_buf());
        if !cached {
            match &result {
                Ok(_) => report(&self.events, "installed", &spec.to_string(), 0, None, ""),
                Err(error) => report(
                    &self.events,
                    "download_failed",
                    &spec.to_string(),
                    0,
                    None,
                    &error.to_string(),
                ),
            }
        }
        Ok(result?)
    }
}

impl Core {
    fn new(root: PathBuf, events: Events) -> Result<Self, String> {
        let fonts: Vec<Font> = typst_assets::fonts()
            .flat_map(|data| Font::iter(Bytes::new(data)))
            .collect();
        let book = FontBook::from_fonts(&fonts);
        Ok(Self {
            // Only the paged raster format is registered. HTML/PDF/SVG exporters and the
            // command-line watcher are deliberately left out.
            library: LazyHash::new(Library::builder([typst_render::FORMAT]).build()),
            fonts: Arc::new(fonts),
            book: LazyHash::new(book),
            packages: PackageRuntime::new(root, events)?,
        })
    }

    fn render(&self, source: String, pixel_per_pt: f64) -> Result<Raster, String> {
        let world = SourceWorld::new(self, source);
        let compiled = typst::compile::<PagedDocument>(&world);
        let document = compiled
            .output
            .map_err(|errors| format!("Typst compilation failed: {errors:?}"))?;
        if document.pages().is_empty() {
            return Err("Typst produced an empty document".into());
        }
        let ppi = pixel_per_pt.clamp(0.25, 16.0) as f32;
        let options = RenderOptions {
            render_bleed: false,
            format: PngFormatOptions {
                pixel_per_pt: Some(Scalar::new(ppi as f64)),
            },
        };
        let pixmap =
            typst_render::render_merged(&document, &options, typst::layout::Abs::pt(1.0), None);
        let width = pixmap.width();
        let height = pixmap.height();
        let pixels = u64::from(width) * u64::from(height);
        if width > 4096 || height > 4096 || pixels > 8 * 1024 * 1024 {
            return Err(format!(
                "Typst raster exceeds size budget: {width}x{height}"
            ));
        }
        Ok(Raster {
            width,
            height,
            rgba: pixmap.take_demultiplied(),
        })
    }
}

struct Raster {
    width: u32,
    height: u32,
    rgba: Vec<u8>,
}

struct SourceWorld<'a> {
    core: &'a Core,
    runtime: &'a PackageRuntime,
    source: Source,
    library: &'a LazyHash<Library>,
    root: PathBuf,
}

impl<'a> SourceWorld<'a> {
    fn new(core: &'a Core, text: String) -> Self {
        Self {
            core,
            runtime: &core.packages,
            source: Source::detached(text),
            library: &core.library,
            root: core.packages.root.clone(),
        }
    }
}

impl World for SourceWorld<'_> {
    fn library(&self) -> &LazyHash<Library> {
        self.library
    }
    fn book(&self) -> &LazyHash<FontBook> {
        &self.core.book
    }
    fn main(&self) -> FileId {
        self.source.id()
    }
    fn source(&self, id: FileId) -> FileResult<Source> {
        if id == self.source.id() {
            Ok(self.source.clone())
        } else {
            let path = self.resolve(id)?;
            let text = std::fs::read_to_string(&path)
                .map_err(|_| FileError::NotFound(id.vpath().get_without_slash().into()))?;
            if text.len() > 2 * 1024 * 1024 {
                return Err(FileError::Other(Some("imported file exceeds 2 MiB".into())));
            }
            Ok(Source::new(id, text))
        }
    }
    fn file(&self, id: FileId) -> FileResult<Bytes> {
        if id == self.source.id() {
            Ok(Bytes::from_string(self.source.text().to_owned()))
        } else {
            let path = self.resolve(id)?;
            let bytes = std::fs::read(&path)
                .map_err(|_| FileError::NotFound(id.vpath().get_without_slash().into()))?;
            if bytes.len() > 2 * 1024 * 1024 {
                return Err(FileError::Other(Some("imported file exceeds 2 MiB".into())));
            }
            Ok(Bytes::new(bytes))
        }
    }
    fn font(&self, index: usize) -> Option<Font> {
        self.core.fonts.get(index).cloned()
    }
    fn today(&self, _: Option<Duration>) -> Option<Datetime> {
        None
    }
}

impl SourceWorld<'_> {
    fn resolve(&self, id: FileId) -> FileResult<PathBuf> {
        let package_root = match id.root() {
            typst::syntax::VirtualRoot::Project => self.root.clone(),
            typst::syntax::VirtualRoot::Package(spec) => self.runtime.obtain(spec)?,
        };
        let candidate = FsRoot::new(package_root.clone()).resolve(id.vpath())?;
        let canonical = candidate.canonicalize().unwrap_or(candidate);
        let canonical_root = package_root.canonicalize().unwrap_or(package_root);
        if canonical.starts_with(&canonical_root) {
            Ok(canonical)
        } else {
            Err(FileError::AccessDenied)
        }
    }
}

const ABI_VERSION: jint = 2;
const RASTER_MAGIC: u32 = 0x5459_5053;

type Engine = (Arc<Mutex<Core>>, Events);

struct Registry {
    next: jlong,
    engines: HashMap<jlong, Engine>,
}

static ENGINES: OnceLock<Mutex<Registry>> = OnceLock::new();

fn registry() -> &'static Mutex<Registry> {
    ENGINES.get_or_init(|| {
        Mutex::new(Registry {
            next: 1,
            engines: HashMap::new(),
        })
    })
}

fn guarded<T>(operation: impl FnOnce() -> Result<T, String>) -> Result<T, String> {
    std::panic::catch_unwind(std::panic::AssertUnwindSafe(operation))
        .map_err(|_| "panic in Typst native operation".to_string())?
}

fn throw(env: &mut JNIEnv, message: &str) {
    let _ = env.throw_new("java/lang/IllegalStateException", message);
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_neofontrender_typst_internal_TypstNative_abiVersion(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    ABI_VERSION
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_neofontrender_typst_internal_TypstNative_createEngine(
    mut env: JNIEnv,
    _class: JClass,
    directory: JString,
) -> jlong {
    let result = guarded(|| {
        let value: String = env
            .get_string(&directory)
            .map_err(|e| e.to_string())?
            .into();
        let root = PathBuf::from(value);
        std::fs::create_dir_all(&root)
            .map_err(|error| format!("could not create Typst library directory: {error}"))?;
        let root = root
            .canonicalize()
            .map_err(|error| format!("invalid Typst library directory: {error}"))?;
        let events = Events::default();
        let engine = (
            Arc::new(Mutex::new(Core::new(root, events.clone())?)),
            events,
        );
        let mut engines = registry()
            .lock()
            .map_err(|_| "Typst engine registry lock is poisoned".to_string())?;
        let handle = engines.next;
        engines.next = engines.next.checked_add(1).unwrap_or(1);
        engines.engines.insert(handle, engine);
        Ok(handle)
    });
    match result {
        Ok(handle) => handle,
        Err(error) => {
            throw(&mut env, &error);
            0
        }
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_neofontrender_typst_internal_TypstNative_render(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    source: JString,
    scale: jfloat,
) -> jbyteArray {
    let result = guarded(|| {
        if handle <= 0 {
            return Err("Typst engine is closed".to_string());
        }
        if !scale.is_finite() {
            return Err("Typst raster scale is not finite".to_string());
        }
        let source: String = env.get_string(&source).map_err(|e| e.to_string())?.into();
        let engine = {
            let engines = registry()
                .lock()
                .map_err(|_| "Typst engine registry lock is poisoned".to_string())?;
            engines
                .engines
                .get(&handle)
                .cloned()
                .ok_or_else(|| "Typst engine is closed".to_string())?
        };
        report(&engine.1, "compiling", "", 0, None, "");
        let rendered = engine
            .0
            .lock()
            .map_err(|_| "Typst engine lock is poisoned".to_string())?
            .render(source, f64::from(scale));
        match &rendered {
            Ok(_) => report(&engine.1, "ready", "", 0, None, ""),
            Err(error) => report(&engine.1, "compile_failed", "", 0, None, error),
        }
        let raster = rendered?;
        let expected = u64::from(raster.width) * u64::from(raster.height) * 4;
        if expected != raster.rgba.len() as u64 {
            return Err("Typst produced an invalid RGBA raster".to_string());
        }
        let mut bytes = Vec::with_capacity(12 + raster.rgba.len());
        bytes.extend_from_slice(&RASTER_MAGIC.to_le_bytes());
        bytes.extend_from_slice(&raster.width.to_le_bytes());
        bytes.extend_from_slice(&raster.height.to_le_bytes());
        bytes.extend_from_slice(&raster.rgba);
        Ok(bytes)
    });
    match result {
        Ok(bytes) => match env.byte_array_from_slice(&bytes) {
            Ok(array) => array.into_raw(),
            Err(error) => {
                throw(&mut env, &error.to_string());
                ptr::null_mut()
            }
        },
        Err(error) => {
            throw(&mut env, &error);
            ptr::null_mut()
        }
    }
}

// Poll only the event lock: a stalled network request must never stall the render thread.
#[unsafe(no_mangle)]
pub extern "system" fn Java_neofontrender_typst_internal_TypstNative_pollEvents(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jobjectArray {
    let result = guarded(|| {
        let events = registry()
            .lock()
            .map_err(|e| e.to_string())?
            .engines
            .get(&handle)
            .map(|engine| engine.1.clone());
        let rows: Vec<_> = match events {
            Some(events) => events
                .lock()
                .map_err(|e| e.to_string())?
                .drain(..)
                .collect(),
            None => Vec::new(),
        };
        let array = env
            .new_object_array(
                (rows.len() * 5) as jint,
                "java/lang/String",
                jni::objects::JObject::null(),
            )
            .map_err(|e| e.to_string())?;
        for (index, field) in rows.iter().flatten().enumerate() {
            let value = env.new_string(field).map_err(|e| e.to_string())?;
            env.set_object_array_element(&array, index as jint, value)
                .map_err(|e| e.to_string())?;
        }
        Ok(array.into_raw())
    });
    match result {
        Ok(array) => array,
        Err(error) => {
            throw(&mut env, &error);
            ptr::null_mut()
        }
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_neofontrender_typst_internal_TypstNative_installPackage(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    package: JString,
) {
    let result = guarded(|| {
        let value: String = env.get_string(&package).map_err(|e| e.to_string())?.into();
        let spec: PackageSpec = value.parse().map_err(|e| format!("{e}"))?;
        if spec.namespace != "preview" {
            return Err("Only @preview packages can be downloaded".into());
        }
        let engine = registry()
            .lock()
            .map_err(|e| e.to_string())?
            .engines
            .get(&handle)
            .cloned()
            .ok_or("Typst engine is closed")?;
        engine
            .0
            .lock()
            .map_err(|e| e.to_string())?
            .packages
            .obtain(&spec)
            .map_err(|e| e.to_string())?;
        Ok(())
    });
    if let Err(error) = result {
        throw(&mut env, &error);
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_neofontrender_typst_internal_TypstNative_destroyEngine(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if let Err(error) = guarded(|| {
        let mut engines = registry()
            .lock()
            .map_err(|_| "Typst engine registry lock is poisoned".to_string())?;
        engines.engines.remove(&handle);
        Ok(())
    }) {
        throw(&mut env, &error);
    }
}
