const fs = require('fs');
const path = require('path');
const { PNG } = require('pngjs');

const root = path.resolve(__dirname, '..', '..');
const spriteDir = path.join(root, 'core', 'assets', 'sprites');
const audioDir = path.join(root, 'core', 'assets', 'audio');
fs.mkdirSync(spriteDir, { recursive: true });
fs.mkdirSync(audioDir, { recursive: true });

const DIRS = [
  { name: 'UP', dx: 0, dy: 1 },
  { name: 'UP_RIGHT', dx: 1, dy: 1 },
  { name: 'RIGHT', dx: 1, dy: 0 },
  { name: 'DOWN_RIGHT', dx: 1, dy: -1 },
  { name: 'DOWN', dx: 0, dy: -1 },
  { name: 'DOWN_LEFT', dx: -1, dy: -1 },
  { name: 'LEFT', dx: -1, dy: 0 },
  { name: 'UP_LEFT', dx: -1, dy: 1 },
];

function rgba(hex, a = 255) {
  const h = hex.replace('#', '');
  return {
    r: parseInt(h.slice(0, 2), 16),
    g: parseInt(h.slice(2, 4), 16),
    b: parseInt(h.slice(4, 6), 16),
    a,
  };
}

function setPixel(img, x, y, c) {
  if (x < 0 || y < 0 || x >= img.width || y >= img.height) return;
  const idx = (img.width * y + x) << 2;
  img.data[idx] = c.r;
  img.data[idx + 1] = c.g;
  img.data[idx + 2] = c.b;
  img.data[idx + 3] = c.a;
}

function fillRect(img, x, y, w, h, c) {
  for (let iy = y; iy < y + h; iy++) {
    for (let ix = x; ix < x + w; ix++) setPixel(img, ix, iy, c);
  }
}

function drawShadow(img, ox, oy) {
  fillRect(img, ox + 10, oy + 27, 12, 3, rgba('#0A1016', 120));
}

function drawKiritoFrame(img, ox, oy, dir, frame, action) {
  const dark = rgba('#0D131C');
  const outline = rgba('#090E14');
  const coat = rgba('#171E29');
  const coatHi = rgba('#2C3C50');
  const coatMid = rgba('#1F2A39');
  const coatTrim = rgba('#47617C');
  const skin = rgba('#E0B38F');
  const skinShade = rgba('#B98668');
  const hair = rgba('#0A0E13');
  const hairHi = rgba('#273447');
  const steel = rgba('#C3D5EA');
  const glow = rgba('#6EE8FF', 180);
  const boot = rgba('#080B10');
  const belt = rgba('#5BC0FF');
  const cape = rgba('#0F1420');
  const capeHi = rgba('#223249');

  const side = dir.dx;
  const front = dir.dy < 0;
  const back = dir.dy > 0;
  const bob = action === 'run' ? (frame % 2 === 0 ? 0 : 1) : 0;

  drawShadow(img, ox, oy);

  let bodyX = ox + 11 + side;
  let bodyY = oy + 14 + bob;

  // coat torso + chest highlight
  fillRect(img, bodyX, bodyY, 10, 10, coat);
  fillRect(img, bodyX + 1, bodyY + 1, 8, 8, coatMid);
  fillRect(img, bodyX + 2, bodyY + 2, 6, 6, coatHi);
  fillRect(img, bodyX + 4, bodyY + 3, 2, 5, coatTrim);
  fillRect(img, bodyX, bodyY, 10, 1, outline);

  // legs
  const stride = action === 'run' ? (frame % 2 === 0 ? 1 : -1) : 0;
  fillRect(img, bodyX + 1 + stride, bodyY + 10, 3, 5, boot);
  fillRect(img, bodyX + 6 - stride, bodyY + 10, 3, 5, boot);

  // shoulders + cape base
  fillRect(img, bodyX - 2, bodyY + 1, 3, 7, coatHi);
  fillRect(img, bodyX + 9, bodyY + 1, 3, 7, coatHi);
  fillRect(img, bodyX + 1, bodyY + 9, 8, 4, cape);
  fillRect(img, bodyX + 2, bodyY + 10, 6, 2, capeHi);

  // head + hair orientation
  let headX = ox + 12 + Math.sign(side);
  let headY = oy + 8 + bob;
  fillRect(img, headX, headY, 8, 6, skin);
  fillRect(img, headX + 1, headY + 3, 6, 2, skinShade);
  if (back) {
    fillRect(img, headX - 1, headY - 1, 10, 5, hair);
    fillRect(img, headX + 1, headY + 4, 6, 1, hair);
  } else if (front) {
    fillRect(img, headX - 1, headY - 1, 10, 3, hair);
    fillRect(img, headX - 1, headY + 1, 2, 3, hair);
    fillRect(img, headX + 7, headY + 1, 2, 3, hair);
  } else {
    fillRect(img, headX - 1, headY - 1, 10, 4, hair);
    if (side >= 0) fillRect(img, headX + 6, headY + 1, 2, 4, hair);
    else fillRect(img, headX, headY + 1, 2, 4, hair);
  }
  fillRect(img, headX + 2, headY, 4, 1, hairHi);

  // belt accent
  fillRect(img, bodyX + 2, bodyY + 8, 6, 1, belt);

  // hands
  const armSwing = action === 'run' ? (frame % 2 === 0 ? 1 : -1) : 0;
  fillRect(img, bodyX - 1, bodyY + 5 + armSwing, 2, 3, skin);
  fillRect(img, bodyX + 9, bodyY + 5 - armSwing, 2, 3, skin);

  // sword + attack effects
  const slash = action === 'attack';
  if (slash) {
    const swing = frame % 6;
    const sx = ox + 19 + Math.round(side * 2 + dir.dx * 2);
    const sy = oy + 14 - Math.round(dir.dy * 2);
    fillRect(img, sx, sy, 9, 2, steel);
    fillRect(img, sx - 1, sy + 1, 10, 1, outline);
    fillRect(img, sx + 1, sy - 1, 6, 1, rgba('#E3F2FF'));
    if (swing >= 1 && swing <= 4) {
      fillRect(img, sx + dir.dx * 2, sy + dir.dy * 2, 6, 2, glow);
      fillRect(img, sx + dir.dx * 4, sy + dir.dy * 4, 4, 1, glow);
    }
  } else {
    // sheathed sword
    const backShift = back ? -1 : 0;
    fillRect(img, bodyX + 8 + side + backShift, bodyY + 2, 2, 8, steel);
  }

  // dodge trail
  if (action === 'dodge') {
    const alpha = frame % 2 === 0 ? 110 : 70;
    fillRect(img, ox + 7 - side * 2, oy + 16, 8, 6, rgba('#8CB5D8', alpha));
    fillRect(img, ox + 5 - side * 2, oy + 17, 4, 4, rgba('#A8D7FF', alpha - 20));
  }

  if (action === 'death') {
    // lay body down regardless of direction
    fillRect(img, ox + 9, oy + 20, 14, 4, coat);
    fillRect(img, ox + 18, oy + 18, 5, 2, hair);
  }
}

function drawMonsterFrame(img, ox, oy, dir, frame, palette, kind) {
  const outline = rgba(palette.outline);
  const base = rgba(palette.base);
  const mid = rgba(palette.mid || palette.base);
  const hi = rgba(palette.highlight);
  const eye = rgba(palette.eye);
  const weapon = rgba(palette.weapon);

  const bob = frame % 2;
  const side = dir.dx;

  if (kind === 'slime') {
    fillRect(img, ox + 8, oy + 15 + bob, 16, 9, base);
    fillRect(img, ox + 9, oy + 16 + bob, 14, 7, mid);
    fillRect(img, ox + 10, oy + 12 + bob, 12, 4, hi);
    fillRect(img, ox + 9, oy + 24, 14, 2, outline);
    fillRect(img, ox + 12 + side, oy + 17 + bob, 2, 2, eye);
    fillRect(img, ox + 18 + side, oy + 17 + bob, 2, 2, eye);
    if (frame >= 2) fillRect(img, ox + 6 + side, oy + 13, 20, 1, hi);
    return;
  }

  fillRect(img, ox + 10 + side, oy + 13 + bob, 10, 11, base);
  fillRect(img, ox + 11 + side, oy + 14 + bob, 8, 9, mid);
  fillRect(img, ox + 9 + side, oy + 12 + bob, 12, 2, hi);
  fillRect(img, ox + 11 + side, oy + 8 + bob, 8, 6, rgba(palette.skin || '#B28769'));
  fillRect(img, ox + 10 + side, oy + 7 + bob, 10, 3, outline);
  fillRect(img, ox + 8 + side, oy + 14 + bob, 2, 7, base);
  fillRect(img, ox + 20 + side, oy + 14 + bob, 2, 7, base);
  fillRect(img, ox + 12 + side, oy + 24, 3, 4, outline);
  fillRect(img, ox + 16 + side, oy + 24, 3, 4, outline);

  if (kind !== 'boss') {
    fillRect(img, ox + 21 + side, oy + 13 + bob, 2, 10, weapon);
  } else {
    fillRect(img, ox + 6 + side, oy + 13 + bob, 4, 12, weapon);
    fillRect(img, ox + 22 + side, oy + 8 + bob, 3, 18, rgba('#DDE9F8'));
  }

  fillRect(img, ox + 13 + side, oy + 10 + bob, 1, 1, eye);
  fillRect(img, ox + 17 + side, oy + 10 + bob, 1, 1, eye);
}

function writeSheet(name, frames, drawFn) {
  const size = 32;
  const img = new PNG({ width: size * frames, height: size * DIRS.length });
  for (let row = 0; row < DIRS.length; row++) {
    for (let col = 0; col < frames; col++) {
      drawFn(img, col * size, row * size, DIRS[row], col);
    }
  }
  fs.writeFileSync(path.join(spriteDir, `${name}.png`), PNG.sync.write(img));
}

writeSheet('player_idle', 4, (img, ox, oy, dir, frame) => drawKiritoFrame(img, ox, oy, dir, frame, 'idle'));
writeSheet('player_run', 6, (img, ox, oy, dir, frame) => drawKiritoFrame(img, ox, oy, dir, frame, 'run'));
writeSheet('player_attack', 6, (img, ox, oy, dir, frame) => drawKiritoFrame(img, ox, oy, dir, frame, 'attack'));
writeSheet('player_dodge', 4, (img, ox, oy, dir, frame) => drawKiritoFrame(img, ox, oy, dir, frame, 'dodge'));
writeSheet('player_death', 4, (img, ox, oy, dir, frame) => drawKiritoFrame(img, ox, oy, dir, frame, 'death'));

writeSheet('slime', 4, (img, ox, oy, dir, frame) => drawMonsterFrame(img, ox, oy, dir, frame, {
  outline: '#0A2C15', base: '#2A923C', mid: '#3EAE4C', highlight: '#7AF582', eye: '#06130A', weapon: '#8FFF9B',
}, 'slime'));

writeSheet('dark_elf', 4, (img, ox, oy, dir, frame) => drawMonsterFrame(img, ox, oy, dir, frame, {
  outline: '#0E1912', base: '#2D5A3E', mid: '#3A6D4F', highlight: '#4D8763', eye: '#79F3B2', weapon: '#9AE8C7', skin: '#A48268',
}, 'elf'));

writeSheet('skeleton', 4, (img, ox, oy, dir, frame) => drawMonsterFrame(img, ox, oy, dir, frame, {
  outline: '#3A4148', base: '#8D98A2', mid: '#A3AFB9', highlight: '#CDD5DE', eye: '#F4F8FF', weapon: '#B4C7D8', skin: '#C3C8D0',
}, 'skeleton'));

writeSheet('knight', 4, (img, ox, oy, dir, frame) => drawMonsterFrame(img, ox, oy, dir, frame, {
  outline: '#1A1F2B', base: '#495A73', mid: '#5B6F8C', highlight: '#6E85A7', eye: '#E8F4FF', weapon: '#A3C4E5', skin: '#B09076',
}, 'knight'));

writeSheet('boss', 4, (img, ox, oy, dir, frame) => drawMonsterFrame(img, ox, oy, dir, frame, {
  outline: '#2A1B2A', base: '#6C3B64', mid: '#83507A', highlight: '#9A5A8D', eye: '#FFD2D8', weapon: '#F2C8D8', skin: '#C89B87',
}, 'boss'));

// Generate simple chiptune-like wav loops to guarantee playable audio assets.
function writeWav(filePath, durationSec, makeSample) {
  const sampleRate = 22050;
  const samples = Math.floor(durationSec * sampleRate);
  const data = Buffer.alloc(samples * 2);
  for (let i = 0; i < samples; i++) {
    const t = i / sampleRate;
    let s = makeSample(t);
    if (s > 1) s = 1;
    if (s < -1) s = -1;
    data.writeInt16LE((s * 32767) | 0, i * 2);
  }
  const header = Buffer.alloc(44);
  header.write('RIFF', 0);
  header.writeUInt32LE(36 + data.length, 4);
  header.write('WAVE', 8);
  header.write('fmt ', 12);
  header.writeUInt32LE(16, 16);
  header.writeUInt16LE(1, 20);
  header.writeUInt16LE(1, 22);
  header.writeUInt32LE(sampleRate, 24);
  header.writeUInt32LE(sampleRate * 2, 28);
  header.writeUInt16LE(2, 32);
  header.writeUInt16LE(16, 34);
  header.write('data', 36);
  header.writeUInt32LE(data.length, 40);
  fs.writeFileSync(filePath, Buffer.concat([header, data]));
}

function tone(freq, t) {
  return Math.sin(2 * Math.PI * freq * t);
}

function floorLoop(baseFreq) {
  return (t) => {
    const beat = Math.floor(t * 2) % 8;
    const seq = [1, 1.25, 1.5, 2, 1.5, 1.25, 1, 0.75];
    const f = baseFreq * seq[beat];
    const lead = tone(f, t) * 0.28;
    const bass = tone(baseFreq * 0.5, t) * 0.14;
    const pulse = ((Math.sin(2 * Math.PI * 8 * t) > 0 ? 1 : -1) * 0.04);
    return lead + bass + pulse;
  };
}

writeWav(path.join(audioDir, 'floor1.wav'), 7.5, floorLoop(220));
writeWav(path.join(audioDir, 'floor25.wav'), 7.5, floorLoop(196));
writeWav(path.join(audioDir, 'floor50.wav'), 7.5, floorLoop(174));
writeWav(path.join(audioDir, 'floor75.wav'), 7.5, floorLoop(155));
writeWav(path.join(audioDir, 'floor100.wav'), 7.5, floorLoop(130));

writeWav(path.join(audioDir, 'sword.wav'), 0.22, (t) => tone(880 - t * 1200, t) * Math.exp(-t * 16));
writeWav(path.join(audioDir, 'hit.wav'), 0.20, (t) => (Math.random() * 2 - 1) * Math.exp(-t * 20));
writeWav(path.join(audioDir, 'pickup.wav'), 0.26, (t) => (tone(660, t) + tone(990, t) * 0.6) * Math.exp(-t * 8));
writeWav(path.join(audioDir, 'level_up.wav'), 0.45, (t) => {
  const f = t < 0.15 ? 392 : t < 0.30 ? 523.25 : 659.25;
  return tone(f, t) * Math.exp(-t * 3);
});
writeWav(path.join(audioDir, 'boss_roar.wav'), 0.8, (t) => {
  const mod = tone(28, t) * 35;
  return (tone(110 + mod, t) * 0.55 + tone(55, t) * 0.3) * Math.exp(-t * 1.2);
});

console.log('Art + audio generated');
