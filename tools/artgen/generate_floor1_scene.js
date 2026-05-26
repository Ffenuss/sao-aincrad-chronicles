const fs = require('fs');
const path = require('path');
const { PNG } = require('pngjs');

const root = path.resolve(__dirname, '..', '..');
const mapsDir = path.join(root, 'core', 'assets', 'maps');
const outImage = path.join(mapsDir, 'floor1_scene.png');
const outMap = path.join(mapsDir, 'floor1.tmx');
const specPath = path.join(mapsDir, 'floor1_scene.json');

const spec = JSON.parse(fs.readFileSync(specPath, 'utf8'));
const artSource = spec.artSource ? path.join(mapsDir, spec.artSource) : null;

const MAP_TILES = spec.mapTiles;
const TILE = spec.tileSize;
const SIZE = MAP_TILES * TILE;

const ground = Array.from({ length: MAP_TILES }, () => Array.from({ length: MAP_TILES }, () => 0));
const blocked = Array.from({ length: MAP_TILES }, () => Array.from({ length: MAP_TILES }, () => 0));
const collisionZones = Array.isArray(spec.blockers) && spec.blockers.length > 0
    ? spec.blockers
    : [
        ...spec.houses,
        ...spec.stalls,
        ...spec.trees,
        spec.fountain,
        ...spec.walls,
    ];

function fill(layer, x0, y0, x1, y1, value) {
    for (let y = y0; y <= y1; y++) {
        for (let x = x0; x <= x1; x++) {
            if (x >= 0 && y >= 0 && x < MAP_TILES && y < MAP_TILES) {
                layer[y][x] = value;
            }
        }
    }
}

for (let y = 0; y < MAP_TILES; y++) {
    for (let x = 0; x < MAP_TILES; x++) {
        ground[y][x] = 1;
    }
}

fill(ground, 0, 18, 39, 22, 2);
fill(ground, 18, 0, 22, 39, 2);
fill(ground, 14, 14, 25, 25, 3);
fill(ground, 18, 18, 21, 21, 4);

spec.roads.forEach((road) => {
    const kind = road.kind === 'main' ? 2 : road.kind === 'plaza' ? 3 : 5;
    fill(ground, road.x0, road.y0, road.x1, road.y1, kind);
});

fill(ground, spec.fountain.x0, spec.fountain.y0, spec.fountain.x1, spec.fountain.y1, 4);

for (const zone of collisionZones) {
    fill(blocked, zone.x0, zone.y0, zone.x1, zone.y1, 1);
}

const houses = spec.houses;
const stalls = spec.stalls;
const trees = spec.trees;

const scene = new PNG({ width: SIZE, height: SIZE });
const backdropSize = { width: SIZE, height: SIZE };

function color(hex, a = 255) {
    const normalized = hex.replace('#', '');
    return {
        r: parseInt(normalized.slice(0, 2), 16),
        g: parseInt(normalized.slice(2, 4), 16),
        b: parseInt(normalized.slice(4, 6), 16),
        a,
    };
}

function putPixel(x, y, c) {
    if (x < 0 || y < 0 || x >= scene.width || y >= scene.height) return;
    const i = (scene.width * y + x) << 2;
    scene.data[i] = c.r;
    scene.data[i + 1] = c.g;
    scene.data[i + 2] = c.b;
    scene.data[i + 3] = c.a;
}

function rect(x, y, w, h, c) {
    for (let py = y; py < y + h; py++) {
        for (let px = x; px < x + w; px++) {
            putPixel(px, py, c);
        }
    }
}

function circle(cx, cy, radius, c) {
    for (let y = -radius; y <= radius; y++) {
        for (let x = -radius; x <= radius; x++) {
            if (x * x + y * y <= radius * radius) {
                putPixel(cx + x, cy + y, c);
            }
        }
    }
}

function tileNoise(x, y) {
    const raw = Math.sin(x * 12.11 + y * 3.73) + Math.cos(x * 5.17 - y * 9.41);
    return raw * 0.5 + 0.5;
}

const grassA = color('#5f9f58');
const grassB = color('#6eae61');
const grassShade = color('#4e8747');
const roadStone = color('#bfae90');
const roadShade = color('#a19279');
const plazaStone = color('#cdc0a6');
const fountainStone = color('#8f8b91');
const waterA = color('#4fa9d5');
const waterB = color('#8de4fb');
const roofDark = color('#6c3f29');
const roofLight = color('#986041');
const wallLight = color('#d7c6a8');
const wallDark = color('#b79e7c');
const wood = color('#7d5a33');
const woodDark = color('#664626');
const treeDark = color('#275d33');
const treeLight = color('#3f8750');
const shadow = color('#20321f', 110);
const lampGold = color('#f4e099');
const fence = color('#79664a');

function drawProceduralScene() {
    rect(0, 0, SIZE, SIZE, grassA);
    for (let ty = 0; ty < MAP_TILES; ty++) {
        for (let tx = 0; tx < MAP_TILES; tx++) {
            const baseX = tx * TILE;
            const baseY = ty * TILE;
            const n = tileNoise(tx * 0.3, ty * 0.2);
            const grass = n > 0.52 ? grassB : grassA;
            rect(baseX, baseY, TILE, TILE, grass);
            rect(baseX + 2, baseY + 2, 12, 10, grassShade);
            rect(baseX + 17, baseY + 15, 9, 8, color(n > 0.52 ? '#78b96b' : '#5a954f'));
        }
    }

    function drawRoadTile(tx, ty, plaza = false, dirt = false) {
        const px = tx * TILE;
        const py = ty * TILE;
        const base = plaza ? plazaStone : dirt ? color('#b8996f') : roadStone;
        const edge = plaza ? color('#aa9b84') : dirt ? color('#9d805b') : roadShade;
        rect(px, py, TILE, TILE, base);
        for (let y = 0; y < TILE; y += 8) {
            rect(px, py + y, TILE, 1, edge);
        }
        for (let x = 0; x < TILE; x += 8) {
            rect(px + x, py, 1, TILE, edge);
        }
        rect(px + 2, py + 2, TILE - 4, TILE - 4, color(plaza ? '#d8ccb2' : dirt ? '#c5aa81' : '#c9b89a'));
    }

    for (let ty = 0; ty < MAP_TILES; ty++) {
        for (let tx = 0; tx < MAP_TILES; tx++) {
            const v = ground[ty][tx];
            if (v === 2) drawRoadTile(tx, ty);
            if (v === 3) drawRoadTile(tx, ty, true, false);
            if (v === 4) {
                const cx = tx * TILE + TILE / 2;
                const cy = ty * TILE + TILE / 2;
                rect(tx * TILE, ty * TILE, TILE, TILE, fountainStone);
                circle(cx, cy, 14, waterA);
                circle(cx, cy, 9, waterB);
            }
            if (v === 5) drawRoadTile(tx, ty, false, true);
        }
    }
    
    function drawHouse(tileRect) {
        const x = tileRect.x0 * TILE;
        const y = tileRect.y0 * TILE;
        const w = (tileRect.x1 - tileRect.x0 + 1) * TILE;
        const h = (tileRect.y1 - tileRect.y0 + 1) * TILE;
        rect(x + 10, y + 22, w - 20, h - 28, wallLight);
        rect(x + 16, y + 28, w - 32, h - 40, wallDark);
        rect(x, y, w, 26, roofDark);
        rect(x + 6, y + 8, w - 12, 16, roofLight);
        rect(x + 12, y + 34, 18, h - 48, wood);
        rect(x + w - 30, y + 34, 18, h - 48, wood);
        rect(x + Math.floor(w * 0.45), y + h - 40, 34, 28, woodDark);
        rect(x + Math.floor(w * 0.45) + 4, y + h - 34, 26, 22, wood);
        rect(x + 30, y + 40, 26, 20, color('#8fbdd8'));
        rect(x + w - 56, y + 40, 26, 20, color('#8fbdd8'));
        rect(x + 10, y + h - 6, w - 20, 6, shadow);
    }

    for (const house of houses) {
        drawHouse(house);
    }

    function drawStall(stall, awningA, awningB) {
        const x = stall.x0 * TILE;
        const y = stall.y0 * TILE;
        const w = (stall.x1 - stall.x0 + 1) * TILE;
        const h = (stall.y1 - stall.y0 + 1) * TILE;
        rect(x + 10, y + 18, w - 20, h - 24, wood);
        rect(x + 6, y + 4, w - 12, 18, awningA);
        for (let stripe = 0; stripe < w - 12; stripe += 18) {
            rect(x + 6 + stripe, y + 4, 9, 18, awningB);
        }
        rect(x + 14, y + h - 16, 20, 12, woodDark);
        rect(x + w - 34, y + h - 16, 20, 12, woodDark);
    }

    const stallPalettes = {
        blue: [color('#4a83c8'), color('#ecf2fb')],
        gold: [color('#d7c58e'), color('#f4ebcb')],
        orange: [color('#cd7543'), color('#f4e4d0')],
        green: [color('#88b26d'), color('#ebf4dd')],
    };
    stalls.forEach((stall) => {
        const palette = stallPalettes[stall.palette] || stallPalettes.blue;
        drawStall(stall, palette[0], palette[1]);
    });

    function drawTree(tileX, tileY) {
        const x = tileX * TILE;
        const y = tileY * TILE;
        rect(x + 12, y + 18, 8, 14, color('#5a3d20'));
        circle(x + 16, y + 12, 16, treeDark);
        circle(x + 10, y + 16, 10, treeLight);
        circle(x + 22, y + 16, 10, treeLight);
    }

    for (const tree of trees) {
        for (let y = tree.y0; y <= tree.y1; y++) {
            for (let x = tree.x0; x <= tree.x1; x++) {
                drawTree(x, y);
            }
        }
    }

    function drawFenceRun(x0, y0, x1) {
        for (let x = x0; x <= x1; x++) {
            const px = x * TILE + 10;
            const py = y0 * TILE + 10;
            rect(px, py + 8, 12, 4, fence);
            rect(px + 1, py, 3, 20, woodDark);
            rect(px + 8, py, 3, 20, woodDark);
        }
    }

    drawFenceRun(7, 13, 10);
    drawFenceRun(29, 13, 32);
    drawFenceRun(7, 27, 10);
    drawFenceRun(29, 27, 32);

    function drawLamp(tileX, tileY) {
        const px = tileX * TILE + 11;
        const py = tileY * TILE + 6;
        rect(px + 5, py + 8, 4, 18, color('#363941'));
        rect(px + 2, py + 2, 10, 8, lampGold);
        rect(px + 3, py, 8, 3, color('#fff0b8'));
    }

    spec.lamps.forEach((lamp) => drawLamp(lamp.x, lamp.y));

    function drawFountain() {
        circle(640, 640, 86, color('#6f747d'));
        circle(640, 640, 72, color('#99c7d7'));
        circle(640, 640, 58, waterA);
        circle(640, 640, 36, waterB);
        rect(628, 584, 24, 56, color('#7a7f88'));
        circle(640, 580, 18, color('#8a9098'));
    }

    drawFountain();

    function drawGate() {
        const x = 1032;
        const y = 1068;
        rect(x + 10, y + 46, 96, 18, woodDark);
        rect(x + 20, y, 14, 64, wood);
        rect(x + 82, y, 14, 64, wood);
        rect(x, y + 58, 116, 12, color('#b69858'));
    }

    drawGate();
}

function clamp(value, min, max) {
    return Math.max(min, Math.min(max, value));
}

function sampleBilinear(png, x, y) {
    const x0 = clamp(Math.floor(x), 0, png.width - 1);
    const y0 = clamp(Math.floor(y), 0, png.height - 1);
    const x1 = clamp(x0 + 1, 0, png.width - 1);
    const y1 = clamp(y0 + 1, 0, png.height - 1);
    const tx = x - x0;
    const ty = y - y0;

    function pixel(px, py) {
        const i = (png.width * py + px) << 2;
        return [
            png.data[i],
            png.data[i + 1],
            png.data[i + 2],
            png.data[i + 3],
        ];
    }

    const p00 = pixel(x0, y0);
    const p10 = pixel(x1, y0);
    const p01 = pixel(x0, y1);
    const p11 = pixel(x1, y1);

    const out = [0, 0, 0, 0];
    for (let c = 0; c < 4; c++) {
        const top = p00[c] * (1 - tx) + p10[c] * tx;
        const bottom = p01[c] * (1 - tx) + p11[c] * tx;
        out[c] = Math.round(top * (1 - ty) + bottom * ty);
    }
    return out;
}

function writeScaledArt(sourcePath, destinationPath) {
    const source = PNG.sync.read(fs.readFileSync(sourcePath));
    const scaled = new PNG({ width: SIZE, height: SIZE });

    for (let y = 0; y < SIZE; y++) {
        const sy = (y / (SIZE - 1)) * (source.height - 1);
        for (let x = 0; x < SIZE; x++) {
            const sx = (x / (SIZE - 1)) * (source.width - 1);
            const [r, g, b, a] = sampleBilinear(source, sx, sy);
            const i = (scaled.width * y + x) << 2;
            scaled.data[i] = r;
            scaled.data[i + 1] = g;
            scaled.data[i + 2] = b;
            scaled.data[i + 3] = a;
        }
    }

    fs.writeFileSync(destinationPath, PNG.sync.write(scaled));
    backdropSize.width = SIZE;
    backdropSize.height = SIZE;
}

if (artSource && fs.existsSync(artSource)) {
    writeScaledArt(artSource, outImage);
} else {
    drawProceduralScene();
    fs.writeFileSync(outImage, PNG.sync.write(scene));
    backdropSize.width = SIZE;
    backdropSize.height = SIZE;
}

function csv(layer) {
    return layer.map((row) => row.map((cell) => (cell === 0 ? 0 : 7)).join(',')).join(',\n');
}

const xml = `<?xml version="1.0" encoding="UTF-8"?>
<map version="1.10" tiledversion="1.10.2" orientation="orthogonal" renderorder="right-down" width="${MAP_TILES}" height="${MAP_TILES}" tilewidth="${TILE}" tileheight="${TILE}" infinite="0" nextlayerid="6" nextobjectid="30">
  <properties>
    <property name="floorName" value="${spec.floorName}"/>
  </properties>
  <tileset firstgid="1" name="town_tiles" tilewidth="32" tileheight="32" tilecount="12" columns="4">
    <image source="town_tiles.png" width="128" height="96"/>
  </tileset>
  <imagelayer id="1" name="visual_backdrop">
    <image source="floor1_scene.png" width="${backdropSize.width}" height="${backdropSize.height}"/>
  </imagelayer>
  <layer id="2" name="blocked" width="40" height="40" visible="0">
    <data encoding="csv">
${csv(blocked)}
    </data>
  </layer>
  <objectgroup id="3" name="spawn">
    <object id="10" x="${spec.spawn.x}" y="${spec.spawn.y}" width="${spec.spawn.width}" height="${spec.spawn.height}"/>
  </objectgroup>
  <objectgroup id="4" name="enemy_spawns">
    ${spec.enemySpawns.map((enemy, index) => `\
<object id="${11 + index}" name="${enemy.id}" x="${enemy.x}" y="${enemy.y}" width="${enemy.width}" height="${enemy.height}">
      <properties>
        <property name="type" value="${enemy.type}"/>
        <property name="respawn" type="float" value="${enemy.respawn}"/>
      </properties>
    </object>`).join('\n    ')}
  </objectgroup>
  <objectgroup id="5" name="exits">
    <object id="15" name="${spec.exit.name}" x="${spec.exit.x}" y="${spec.exit.y}" width="${spec.exit.width}" height="${spec.exit.height}">
      <properties>
        <property name="toFloor" type="int" value="${spec.exit.toFloor}"/>
      </properties>
    </object>
  </objectgroup>
  <objectgroup id="6" name="npcs">
    ${spec.npcs.map((npc, index) => `\
<object id="${16 + index}" name="${npc.id}" x="${npc.x}" y="${npc.y}" width="${npc.width}" height="${npc.height}">
      <properties>
        <property name="name" value="${npc.name}"/>
        <property name="role" value="${npc.role}"/>
        <property name="dialog" value="${npc.dialog}"/>
      </properties>
    </object>`).join('\n    ')}
  </objectgroup>
</map>
`;

fs.writeFileSync(outMap, xml, 'utf8');
console.log('Generated floor1 scene + map');
