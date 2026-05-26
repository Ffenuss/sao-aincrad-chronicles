const fs = require('fs');
const path = require('path');
const { PNG } = require('pngjs');

const root = path.resolve(__dirname, '..', '..');
const mapsDir = path.join(root, 'core', 'assets', 'maps');

const route = [
  { floor: 1, size: 40, tint: [1.00, 1.00, 1.00], sat: 1.0, dark: 1.0, style: 'town' },
  { floor: 2, size: 52, tint: [0.98, 0.98, 1.02], sat: 1.0, dark: 0.98, style: 'town' },
  { floor: 10, size: 64, tint: [0.92, 1.02, 0.90], sat: 0.95, dark: 0.92, style: 'forest' },
  { floor: 22, size: 72, tint: [0.90, 1.05, 0.92], sat: 0.92, dark: 0.90, style: 'forest' },
  { floor: 25, size: 74, tint: [0.82, 0.95, 0.86], sat: 0.82, dark: 0.82, style: 'ruins' },
  { floor: 35, size: 78, tint: [0.86, 0.98, 0.84], sat: 0.88, dark: 0.84, style: 'forest' },
  { floor: 47, size: 82, tint: [0.92, 0.92, 0.96], sat: 0.70, dark: 0.80, style: 'cathedral' },
  { floor: 50, size: 86, tint: [0.95, 0.90, 0.78], sat: 0.66, dark: 0.72, style: 'labyrinth' },
  { floor: 55, size: 90, tint: [0.92, 0.88, 0.84], sat: 0.62, dark: 0.68, style: 'city' },
  { floor: 67, size: 96, tint: [0.78, 0.80, 0.86], sat: 0.58, dark: 0.62, style: 'battlefield' },
  { floor: 74, size: 100, tint: [0.75, 0.70, 0.78], sat: 0.58, dark: 0.60, style: 'battlefield' },
  { floor: 75, size: 104, tint: [0.72, 0.70, 0.80], sat: 0.54, dark: 0.58, style: 'fortress' },
  { floor: 90, size: 110, tint: [0.70, 0.72, 0.88], sat: 0.56, dark: 0.56, style: 'skybridge' },
  { floor: 100, size: 116, tint: [0.72, 0.66, 0.90], sat: 0.52, dark: 0.52, style: 'ruby' },
];

const sourcePath = path.join(mapsDir, 'floor1_backdrop.png');
const src = PNG.sync.read(fs.readFileSync(sourcePath));

function clamp255(v) { return Math.max(0, Math.min(255, v)); }
function hash(v) { return Math.abs(Math.sin(v * 12.9898) * 43758.5453) % 1; }

function blend(r, g, b, tr, tg, tb, a) {
  return [
    clamp255((r * (1 - a) + tr * a) | 0),
    clamp255((g * (1 - a) + tg * a) | 0),
    clamp255((b * (1 - a) + tb * a) | 0),
  ];
}

function adjustBase(r, g, b, cfg, nx, ny) {
  const lum = 0.2126 * r + 0.7152 * g + 0.0722 * b;
  r = lum + (r - lum) * cfg.sat;
  g = lum + (g - lum) * cfg.sat;
  b = lum + (b - lum) * cfg.sat;

  const vignette = 1 - (Math.pow(nx - 0.5, 2) + Math.pow(ny - 0.5, 2)) * 0.85;
  const light = (0.90 + 0.10 * vignette) * cfg.dark;

  r = r * cfg.tint[0] * light;
  g = g * cfg.tint[1] * light;
  b = b * cfg.tint[2] * light;

  return [clamp255(r | 0), clamp255(g | 0), clamp255(b | 0)];
}

function styleOverlay(r, g, b, cfg, nx, ny, x, y) {
  if (cfg.style === 'forest') {
    const fog = Math.max(0, Math.sin((nx + ny) * 14 + cfg.floor * 0.3)) * 0.04;
    return blend(r, g, b, 208, 235, 205, fog);
  }
  if (cfg.style === 'ruins' || cfg.style === 'labyrinth' || cfg.style === 'cathedral' || cfg.style === 'fortress') {
    const gx = (x % 96) < 3;
    const gy = (y % 96) < 3;
    if (gx || gy) {
      return blend(r, g, b, 138, 138, 148, 0.20);
    }
  }
  if (cfg.style === 'battlefield') {
    const scar = hash(x * 0.005 + y * 0.003 + cfg.floor) > 0.997;
    if (scar) return blend(r, g, b, 88, 58, 58, 0.45);
  }
  if (cfg.style === 'skybridge') {
    const cloud = Math.sin(nx * 18 + ny * 25 + cfg.floor) * 0.5 + 0.5;
    if (cloud > 0.93) return blend(r, g, b, 224, 232, 255, 0.25);
  }
  if (cfg.style === 'ruby') {
    const rune = ((x + y) % 140) < 2 || ((x - y + 100000) % 170) < 2;
    if (rune) return blend(r, g, b, 210, 110, 190, 0.28);
  }
  if (cfg.style === 'city') {
    if ((x % 128) < 5 && (y % 128) < 80) {
      return blend(r, g, b, 118, 116, 112, 0.24);
    }
  }
  return [r, g, b];
}

for (const cfg of route) {
  const dim = cfg.size * 32;
  const out = new PNG({ width: dim, height: dim });
  for (let y = 0; y < dim; y++) {
    const sy = Math.floor((y / dim) * src.height);
    for (let x = 0; x < dim; x++) {
      const sx = Math.floor((x / dim) * src.width);
      const si = (src.width * sy + sx) << 2;
      const di = (out.width * y + x) << 2;
      let [r, g, b] = adjustBase(src.data[si], src.data[si + 1], src.data[si + 2], cfg, x / dim, y / dim);
      [r, g, b] = styleOverlay(r, g, b, cfg, x / dim, y / dim, x, y);
      out.data[di] = r;
      out.data[di + 1] = g;
      out.data[di + 2] = b;
      out.data[di + 3] = 255;
    }
  }

  const outPath = path.join(mapsDir, `floor${cfg.floor}_backdrop.png`);
  fs.writeFileSync(outPath, PNG.sync.write(out));
}

console.log('Generated biome-styled backdrops:', route.map((r) => r.floor).join(', '));
