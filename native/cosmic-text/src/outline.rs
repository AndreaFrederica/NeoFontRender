//! Small outline based signed-distance rasterizer.
//!
//! Cosmic's swash cache already exposes the shaped glyph outline.  Keeping this
//! conversion in native code means the Java ABI only has to carry the finished
//! raster and keeps variable-font and fallback selection in Cosmic.

use rayon::prelude::*;
use swash::zeno::Command;

#[derive(Clone, Copy, Debug)]
struct Point {
    x: f32,
    y: f32,
}

#[derive(Clone, Copy, Debug)]
struct Segment {
    a: Point,
    b: Point,
    min_x: f32,
    max_x: f32,
    min_y: f32,
    max_y: f32,
    dx: f32,
    dy: f32,
    inv_length_squared: f32,
}

impl Segment {
    fn new(a: Point, b: Point) -> Self {
        let dx = b.x - a.x;
        let dy = b.y - a.y;
        let length_squared = dx * dx + dy * dy;
        Self {
            a,
            b,
            min_x: a.x.min(b.x),
            max_x: a.x.max(b.x),
            min_y: a.y.min(b.y),
            max_y: a.y.max(b.y),
            dx,
            dy,
            inv_length_squared: if length_squared > 1.0e-8 {
                1.0 / length_squared
            } else {
                0.0
            },
        }
    }
}

#[derive(Clone, Copy, Debug)]
pub(crate) struct Bounds {
    pub min_x: f32,
    pub min_y: f32,
    pub max_x: f32,
    pub max_y: f32,
}

#[derive(Clone, Debug)]
pub(crate) struct OutlinePath {
    segments: Vec<Segment>,
    pub bounds: Bounds,
}

impl OutlinePath {
    pub(crate) fn from_commands(
        commands: &[Command],
        offset_x: f32,
        offset_y: f32,
    ) -> Option<Self> {
        let mut segments = Vec::new();
        let mut current = None;
        let mut start = None;
        for command in commands {
            match *command {
                Command::MoveTo(point) => {
                    let point = Point {
                        x: point.x + offset_x,
                        y: point.y + offset_y,
                    };
                    current = Some(point);
                    start = Some(point);
                }
                Command::LineTo(point) => {
                    let point = Point {
                        x: point.x + offset_x,
                        y: point.y + offset_y,
                    };
                    if let Some(previous) = current {
                        segments.push(Segment::new(previous, point));
                    }
                    current = Some(point);
                }
                Command::QuadTo(control, point) => {
                    let Some(previous) = current else { continue };
                    let control = Point {
                        x: control.x + offset_x,
                        y: control.y + offset_y,
                    };
                    let point = Point {
                        x: point.x + offset_x,
                        y: point.y + offset_y,
                    };
                    flatten_quad(previous, control, point, 0.08, &mut segments);
                    current = Some(point);
                }
                Command::CurveTo(control_a, control_b, point) => {
                    let Some(previous) = current else { continue };
                    let control_a = Point {
                        x: control_a.x + offset_x,
                        y: control_a.y + offset_y,
                    };
                    let control_b = Point {
                        x: control_b.x + offset_x,
                        y: control_b.y + offset_y,
                    };
                    let point = Point {
                        x: point.x + offset_x,
                        y: point.y + offset_y,
                    };
                    flatten_curve(previous, control_a, control_b, point, 0.08, &mut segments);
                    current = Some(point);
                }
                Command::Close => {
                    if let (Some(previous), Some(first)) = (current, start) {
                        if distance_squared(previous, first) > 1.0e-8 {
                            segments.push(Segment::new(previous, first));
                        }
                    }
                    current = start;
                }
            }
        }
        if segments.is_empty() {
            return None;
        }
        let mut bounds = Bounds {
            min_x: f32::INFINITY,
            min_y: f32::INFINITY,
            max_x: f32::NEG_INFINITY,
            max_y: f32::NEG_INFINITY,
        };
        for segment in &segments {
            bounds.min_x = bounds.min_x.min(segment.a.x).min(segment.b.x);
            bounds.min_y = bounds.min_y.min(segment.a.y).min(segment.b.y);
            bounds.max_x = bounds.max_x.max(segment.a.x).max(segment.b.x);
            bounds.max_y = bounds.max_y.max(segment.a.y).max(segment.b.y);
        }
        Some(Self { segments, bounds })
    }

    pub(crate) fn append(&mut self, other: Self) {
        self.segments.extend(other.segments);
        self.bounds.min_x = self.bounds.min_x.min(other.bounds.min_x);
        self.bounds.min_y = self.bounds.min_y.min(other.bounds.min_y);
        self.bounds.max_x = self.bounds.max_x.max(other.bounds.max_x);
        self.bounds.max_y = self.bounds.max_y.max(other.bounds.max_y);
    }

    pub(crate) fn rasterize_sdf(
        &self,
        min_x: i32,
        min_y: i32,
        width: usize,
        height: usize,
        range: f32,
    ) -> Vec<u32> {
        let mut pixels = vec![0_u32; width.saturating_mul(height)];
        let index = SegmentGrid::build(self, min_x, min_y, width, height, range);
        let segment_count = self.segments.len();
        pixels
            .par_chunks_mut(width.max(1))
            .enumerate()
            .with_min_len(4)
            .map_init(|| (vec![0_u32; segment_count], 0_u32), |state, (y, row)| {
                let (marks, stamp) = state;
                let scanline = scanline_crossings(self, min_y as f32 + y as f32 + 0.5);
                let mut crossing_index = 0;
                let mut winding = 0_i32;
                for (x, output) in row.iter_mut().enumerate() {
                    *stamp = stamp.wrapping_add(1).max(1);
                    let point = Point {
                        x: min_x as f32 + x as f32 + 0.5,
                        y: min_y as f32 + y as f32 + 0.5,
                    };
                    let mut distance_squared = f32::INFINITY;
                    while crossing_index < scanline.len() && scanline[crossing_index].0 < point.x {
                        winding += scanline[crossing_index].1;
                        crossing_index += 1;
                    }
                    index.for_each_near(point, range, marks, *stamp, |segment_index| {
                        let segment = self.segments[segment_index];
                        distance_squared =
                            distance_squared.min(distance_squared_to_segment(point, segment));
                    });
                    let distance = distance_squared.sqrt();
                    let distance = if winding != 0 { distance } else { -distance };
                    let encoded = (0.5 + distance / (2.0 * range.max(1.0))).clamp(0.0, 1.0);
                    *output = ((encoded * 255.0).round() as u32) << 24;
                }
            })
            .for_each(|_| {});
        pixels
    }

    /// Basic three-channel distance field. Edges are assigned cyclically to RGB while retaining
    /// the same winding sign in each channel. The shader's median reconstruction handles the
    /// common corner cases; this remains an experimental path until the atlas benchmark covers
    /// all bundled fonts.
    pub(crate) fn rasterize_msdf(
        &self,
        min_x: i32,
        min_y: i32,
        width: usize,
        height: usize,
        range: f32,
    ) -> Vec<u32> {
        let mut pixels = vec![0_u32; width.saturating_mul(height)];
        let index = SegmentGrid::build(self, min_x, min_y, width, height, range);
        let segment_count = self.segments.len();
        pixels
            .par_chunks_mut(width.max(1))
            .enumerate()
            .with_min_len(4)
            .map_init(|| (vec![0_u32; segment_count], 0_u32), |state, (y, row)| {
                let (marks, stamp) = state;
                let scanline = scanline_crossings(self, min_y as f32 + y as f32 + 0.5);
                let mut crossing_index = 0;
                let mut winding = 0_i32;
                for (x, output) in row.iter_mut().enumerate() {
                    *stamp = stamp.wrapping_add(1).max(1);
                    let point = Point {
                        x: min_x as f32 + x as f32 + 0.5,
                        y: min_y as f32 + y as f32 + 0.5,
                    };
                    let mut channels = [f32::INFINITY; 3];
                    while crossing_index < scanline.len() && scanline[crossing_index].0 < point.x {
                        winding += scanline[crossing_index].1;
                        crossing_index += 1;
                    }
                    index.for_each_near(point, range, marks, *stamp, |segment_index| {
                        let segment = self.segments[segment_index];
                        let channel = segment_index % 3;
                        channels[channel] =
                            channels[channel].min(distance_squared_to_segment(point, segment));
                    });
                    let inside = winding != 0;
                    let mut encoded = [0_u32; 3];
                    for channel in 0..3 {
                        // No candidate in the indexed radius means the true distance is outside the
                        // encoded range. Saturate at the range instead of treating it as the contour.
                        let distance = if channels[channel].is_finite() {
                            channels[channel].sqrt()
                        } else {
                            range.max(1.0)
                        };
                        let signed = if inside { distance } else { -distance };
                        encoded[channel] = ((0.5 + signed / (2.0 * range.max(1.0))).clamp(0.0, 1.0)
                            * 255.0)
                            .round() as u32;
                    }
                    *output = (255_u32 << 24) | (encoded[2] << 16) | (encoded[1] << 8) | encoded[0];
                }
            })
            .for_each(|_| {});
        pixels
    }
}

/// A coarse uniform grid keeps each pixel from testing every outline segment. The grid stores
/// segment ids rather than copies, so building it is cheap compared with rasterizing the field.
struct SegmentGrid {
    origin_x: f32,
    origin_y: f32,
    cell_size: f32,
    width: usize,
    height: usize,
    buckets: Vec<Vec<usize>>,
}

impl SegmentGrid {
    fn build(
        path: &OutlinePath,
        min_x: i32,
        min_y: i32,
        width: usize,
        height: usize,
        range: f32,
    ) -> Self {
        let cell_size = range.max(4.0);
        let origin_x = min_x as f32 - cell_size;
        let origin_y = min_y as f32 - cell_size;
        let extent_x = width as f32 + cell_size * 2.0;
        let extent_y = height as f32 + cell_size * 2.0;
        let grid_width = (extent_x / cell_size).ceil().max(1.0) as usize;
        let grid_height = (extent_y / cell_size).ceil().max(1.0) as usize;
        let mut grid = Self {
            origin_x,
            origin_y,
            cell_size,
            width: grid_width,
            height: grid_height,
            buckets: (0..grid_width * grid_height).map(|_| Vec::new()).collect(),
        };
        for (index, segment) in path.segments.iter().enumerate() {
            let x0 = grid.clamp_x(grid.x_cell(segment.min_x));
            let x1 = grid.clamp_x(grid.x_cell(segment.max_x));
            let y0 = grid.clamp_y(grid.y_cell(segment.min_y));
            let y1 = grid.clamp_y(grid.y_cell(segment.max_y));
            for y in y0..=y1 {
                for x in x0..=x1 {
                    grid.buckets[y * grid.width + x].push(index);
                }
            }
        }
        grid
    }

    fn x_cell(&self, x: f32) -> isize {
        ((x - self.origin_x) / self.cell_size).floor() as isize
    }
    fn y_cell(&self, y: f32) -> isize {
        ((y - self.origin_y) / self.cell_size).floor() as isize
    }
    fn clamp_x(&self, value: isize) -> usize {
        value.clamp(0, self.width as isize - 1) as usize
    }
    fn clamp_y(&self, value: isize) -> usize {
        value.clamp(0, self.height as isize - 1) as usize
    }

    fn for_each_near<F: FnMut(usize)>(
        &self,
        point: Point,
        range: f32,
        marks: &mut [u32],
        stamp: u32,
        mut callback: F,
    ) {
        let x0 = self.clamp_x(self.x_cell(point.x - range));
        let x1 = self.clamp_x(self.x_cell(point.x + range));
        let y0 = self.clamp_y(self.y_cell(point.y - range));
        let y1 = self.clamp_y(self.y_cell(point.y + range));
        for y in y0..=y1 {
            for x in x0..=x1 {
                for &segment_index in &self.buckets[y * self.width + x] {
                    if marks[segment_index] == stamp {
                        continue;
                    }
                    marks[segment_index] = stamp;
                    callback(segment_index);
                }
            }
        }
    }
}

fn flatten_quad(a: Point, b: Point, c: Point, tolerance: f32, output: &mut Vec<Segment>) {
    let chord = distance(a, c);
    let control = distance_to_line(b, a, c);
    if chord + control <= tolerance || chord <= 0.001 {
        output.push(Segment::new(a, c));
        return;
    }
    let ab = midpoint(a, b);
    let bc = midpoint(b, c);
    let abc = midpoint(ab, bc);
    flatten_quad(a, ab, abc, tolerance, output);
    flatten_quad(abc, bc, c, tolerance, output);
}

fn flatten_curve(
    a: Point,
    b: Point,
    c: Point,
    d: Point,
    tolerance: f32,
    output: &mut Vec<Segment>,
) {
    let control = distance_to_line(b, a, d).max(distance_to_line(c, a, d));
    let chord = distance(a, d);
    if chord + control <= tolerance || chord <= 0.001 {
        output.push(Segment::new(a, d));
        return;
    }
    let ab = midpoint(a, b);
    let bc = midpoint(b, c);
    let cd = midpoint(c, d);
    let abc = midpoint(ab, bc);
    let bcd = midpoint(bc, cd);
    let abcd = midpoint(abc, bcd);
    flatten_curve(a, ab, abc, abcd, tolerance, output);
    flatten_curve(abcd, bcd, cd, d, tolerance, output);
}

fn distance_squared_to_segment(point: Point, segment: Segment) -> f32 {
    if segment.inv_length_squared == 0.0 {
        return distance_squared(point, segment.a);
    }
    let t = (((point.x - segment.a.x) * segment.dx) + ((point.y - segment.a.y) * segment.dy))
        * segment.inv_length_squared;
    let t = t.clamp(0.0, 1.0);
    distance_squared(
        point,
        Point {
            x: segment.a.x + segment.dx * t,
            y: segment.a.y + segment.dy * t,
        },
    )
}

fn scanline_crossings(path: &OutlinePath, y: f32) -> Vec<(f32, i32)> {
    let mut crossings = Vec::new();
    for segment in &path.segments {
        let a = segment.a;
        let b = segment.b;
        if (a.y <= y && b.y > y) || (a.y > y && b.y <= y) {
            let x = a.x + (y - a.y) * (b.x - a.x) / (b.y - a.y);
            crossings.push((x, if b.y > a.y { 1 } else { -1 }));
        }
    }
    crossings.sort_unstable_by(|left, right| left.0.total_cmp(&right.0));
    crossings
}

fn midpoint(a: Point, b: Point) -> Point {
    Point {
        x: (a.x + b.x) * 0.5,
        y: (a.y + b.y) * 0.5,
    }
}
fn distance(a: Point, b: Point) -> f32 {
    distance_squared(a, b).sqrt()
}
fn distance_squared(a: Point, b: Point) -> f32 {
    let x = a.x - b.x;
    let y = a.y - b.y;
    x * x + y * y
}

fn distance_to_line(point: Point, a: Point, b: Point) -> f32 {
    let length = distance(a, b);
    if length <= 0.001 {
        return distance(point, a);
    }
    ((b.x - a.x) * (a.y - point.y) - (a.x - point.x) * (b.y - a.y)).abs() / length
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn outline_sdf_has_positive_inside_and_negative_outside_distance() {
        let commands = vec![
            Command::MoveTo(swash::zeno::Point::new(0.0, 0.0)),
            Command::LineTo(swash::zeno::Point::new(10.0, 0.0)),
            Command::LineTo(swash::zeno::Point::new(10.0, 10.0)),
            Command::LineTo(swash::zeno::Point::new(0.0, 10.0)),
            Command::Close,
        ];
        let path = OutlinePath::from_commands(&commands, 0.0, 0.0).expect("rectangle outline");
        let pixels = path.rasterize_sdf(-2, -2, 14, 14, 4.0);
        let inside = (pixels[5 * 14 + 5] >> 24) as f32 / 255.0;
        let outside = (pixels[0] >> 24) as f32 / 255.0;
        assert!(
            inside > 0.5,
            "inside sample should encode a positive distance"
        );
        assert!(
            outside < 0.5,
            "outside sample should encode a negative distance"
        );
    }
}
