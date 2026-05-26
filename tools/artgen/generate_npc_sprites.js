const fs = require('fs');
const path = require('path');
const { PNG } = require('pngjs');

const root = path.resolve(__dirname, '..', '..');
const spriteDir = path.join(root, 'core', 'assets', 'sprites');
fs.mkdirSync(spriteDir, { recursive: true });

const npcs = [
  { key: 'npc_klein', outfit: '#3B5E87', hair: '#141824', skin: '#D8B28C', accent: '#D9A34A' },
  { key: 'npc_asuna', outfit: '#F6F5F8', hair: '#A7642D', skin: '#E7C0A0', accent: '#C8303D' },
  { key: 'npc_agil', outfit: '#4E6B45', hair: '#101012', skin: '#7A4B34', accent: '#B88A3C' },
  { key: 'npc_lisbeth', outfit: '#7D4A90', hair: '#E388A8', skin: '#E5BA9A', accent: '#F0C8FF' },
  { key: 'npc_argo', outfit: '#3C526E', hair: '#8898A8', skin: '#DAB398', accent: '#D7E6FF' },
  { key: 'npc_kayaba', outfit: '#5A5968', hair: '#D8D8DD', skin: '#D0AF96', accent: '#A6B6E4' },
  { key: 'npc_kizmel', outfit: '#2F6A4A', hair: '#1C2D22', skin: '#CFA88D', accent: '#7BD6A2' },
  { key: 'npc_silica', outfit: '#734A61', hair: '#5A2E45', skin: '#E5BAA1', accent: '#F3A7C9' },
  { key: 'npc_heathcliff', outfit: '#8A2228', hair: '#2A2C34', skin: '#D7B299', accent: '#D8C6A0' },
  { key: 'npc_trader', outfit: '#6E5330', hair: '#3D281A', skin: '#D5B089', accent: '#F0D37A' },
  { key: 'npc_default', outfit: '#4C5348', hair: '#2A2A30', skin: '#D0AB8B', accent: '#9EC7A0' },
];

function c(hex, a = 255) {
  const h = hex.replace('#', '');
  return { r: parseInt(h.slice(0, 2), 16), g: parseInt(h.slice(2, 4), 16), b: parseInt(h.slice(4, 6), 16), a };
}

function px(img, x, y, col) {
  if (x < 0 || y < 0 || x >= img.width || y >= img.height) return;
  const i = (img.width * y + x) << 2;
  img.data[i] = col.r; img.data[i + 1] = col.g; img.data[i + 2] = col.b; img.data[i + 3] = col.a;
}

function rect(img, x, y, w, h, col) {
  for (let yy = y; yy < y + h; yy++) for (let xx = x; xx < x + w; xx++) px(img, xx, yy, col);
}

function drawNpcFrame(img, ox, oy, frame, def) {
  const outfit = c(def.outfit);
  const outfitHi = c(def.accent);
  const outline = c('#0A0D12');
  const hair = c(def.hair);
  const hairHi = c('#D8DCE7', 95);
  const skin = c(def.skin);
  const skinShade = c('#A97E63', 170);
  const boot = c('#0A0F17');
  const cape = c('#1A2533', 190);
  const shadow = c('#070A10', 120);

  const bob = frame % 2;
  const sway = frame % 2 === 0 ? -1 : 1;

  rect(img, ox + 10, oy + 27, 12, 3, shadow);
  rect(img, ox + 11, oy + 14 + bob, 10, 10, outfit);
  rect(img, ox + 12, oy + 16 + bob, 8, 6, outfitHi);
  rect(img, ox + 10, oy + 15 + bob, 2, 7, outfit);
  rect(img, ox + 20, oy + 15 + bob, 2, 7, outfit);
  rect(img, ox + 11, oy + 23 + bob, 10, 2, cape);

  rect(img, ox + 12 + sway, oy + 24, 3, 4, boot);
  rect(img, ox + 16 - sway, oy + 24, 3, 4, boot);
  rect(img, ox + 13 + sway, oy + 25, 2, 1, c('#334358'));
  rect(img, ox + 17 - sway, oy + 25, 2, 1, c('#334358'));

  rect(img, ox + 12, oy + 8 + bob, 8, 6, skin);
  rect(img, ox + 13, oy + 10 + bob, 6, 3, skinShade);
  rect(img, ox + 11, oy + 7 + bob, 10, 3, hair);
  rect(img, ox + 12, oy + 8 + bob, 8, 1, hairHi);
  rect(img, ox + 11 + sway, oy + 11 + bob, 1, 2, hair);
  rect(img, ox + 20 + sway, oy + 11 + bob, 1, 2, hair);

  rect(img, ox + 9 + sway, oy + 18 + bob, 2, 4, skin);
  rect(img, ox + 21 - sway, oy + 17 + bob, 2, 4, skin);
  rect(img, ox + 12, oy + 18 + bob, 8, 1, outfitHi);
  rect(img, ox + 11, oy + 14 + bob, 10, 1, outline);
}

for (const def of npcs) {
  const img = new PNG({ width: 128, height: 32 });
  for (let frame = 0; frame < 4; frame++) drawNpcFrame(img, frame * 32, 0, frame, def);
  fs.writeFileSync(path.join(spriteDir, `${def.key}.png`), PNG.sync.write(img));
}

console.log('Generated NPC sprite strips:', npcs.length);
