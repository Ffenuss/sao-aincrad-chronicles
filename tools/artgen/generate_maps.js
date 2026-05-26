const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..', '..');
const mapsDir = path.join(root, 'core', 'assets', 'maps');
fs.mkdirSync(mapsDir, { recursive: true });

const route = [
  { floor: 1, name: 'Town of Beginnings', size: 40, bossType: null, npc: { id: 'klein_tutorial', name: 'Klein', role: 'ally', dialog: 'Kirito, get to Tolbana and clear the first raid.' } },
  { floor: 2, name: 'Tolbana Outskirts', size: 52, bossType: 'floor_boss', npc: { id: 'asuna_rookie', name: 'Asuna', role: 'ally', dialog: 'I will join the first raid. Let us clear this floor.' } },
  { floor: 10, name: 'Forest of Reflection', size: 64, bossType: 'dark_elf', npc: { id: 'argo_info', name: 'Argo', role: 'story', dialog: 'Information first, sword second. The elf route starts from floor 22.' } },
  { floor: 22, name: 'Elf Campaign Front', size: 72, bossType: 'dark_elf', npc: { id: 'kizmel_echo', name: 'Kizmel', role: 'ally', dialog: 'The dark elves hold the next gate. Break their captain.' } },
  { floor: 25, name: 'Dark Elf Castle', size: 74, bossType: 'dark_elf', npc: { id: 'dark_elf_scout', name: 'Dark Elf Scout', role: 'story', dialog: 'The castle route is hostile, but the labyrinth gate lies beyond the patrols.' } },
  { floor: 35, name: 'Forest of Wandering', size: 78, bossType: 'floor_boss', npc: { id: 'silica_35', name: 'Silica', role: 'ally', dialog: 'The event boss appears near the old cedar shrine at night.' } },
  { floor: 47, name: 'Cathedral District', size: 82, bossType: 'skeleton', npc: { id: 'heathcliff_47', name: 'Heathcliff', role: 'story', dialog: 'Discipline is survival. Keep your party formation tight.' } },
  { floor: 50, name: 'Algade Labyrinth', size: 86, bossType: 'floor_boss', npc: { id: 'agil_50', name: 'Agil', role: 'trader', dialog: 'This maze eats careless players. Stock potions before the boss room.' } },
  { floor: 55, name: 'Grandzam Lower Town', size: 90, bossType: 'knight', npc: { id: 'lisbeth_55', name: 'Lisbeth', role: 'trader', dialog: 'Your blade needs reinforcement before the upper floors.' } },
  { floor: 67, name: 'Ruined Battleground', size: 96, bossType: 'floor_boss', npc: { id: 'klein_67', name: 'Klein', role: 'ally', dialog: 'Frontline scouts saw a heavy guardian near the collapsed wall.' } },
  { floor: 74, name: 'Crimson Approach', size: 100, bossType: 'floor_boss', npc: { id: 'asuna_74', name: 'Asuna', role: 'ally', dialog: 'One more push. The assault squad assembles beyond this gate.' } },
  { floor: 75, name: 'Granzam Fortress', size: 104, bossType: 'floor_boss', npc: { id: 'asuna_75', name: 'Asuna', role: 'ally', dialog: 'The Knights of the Blood patrol this fortress. Break their guard, then strike.' } },
  { floor: 90, name: 'Sky Bridge Citadel', size: 110, bossType: 'floor_boss', npc: { id: 'agil_90', name: 'Agil', role: 'story', dialog: 'You are close to the top. Watch for burst attacks and delayed swings.' } },
  { floor: 100, name: 'Ruby Palace', size: 116, bossType: 'heathcliff', npc: { id: 'kayaba_echo', name: 'Kayaba', role: 'story', dialog: 'This is the final system boundary. Defeat Heathcliff and Aincrad will open.' } },
];

const nextByFloor = new Map();
for (let i = 0; i < route.length; i++) {
  nextByFloor.set(route[i].floor, i + 1 < route.length ? route[i + 1].floor : route[0].floor);
}

function objectWithProps(id, name, x, y, w, h, props) {
  const propXml = props.map((p) => `        <property name="${p.name}"${p.type ? ` type="${p.type}"` : ''} value="${p.value}"/>`).join('\n');
  return `    <object id="${id}" name="${name}" x="${x}" y="${y}" width="${w}" height="${h}">\n      <properties>\n${propXml}\n      </properties>\n    </object>`;
}

function buildMap(floorCfg, nextFloor) {
  const tile = 32;
  const sizePx = floorCfg.size * tile;
  const border = tile;
  const center = Math.floor(sizePx / 2);

  const collisions = [];
  let oid = 1;
  collisions.push(`    <object id="${oid++}" x="0" y="0" width="${sizePx}" height="${border}"/>`);
  collisions.push(`    <object id="${oid++}" x="0" y="${sizePx - border}" width="${sizePx}" height="${border}"/>`);
  collisions.push(`    <object id="${oid++}" x="0" y="0" width="${border}" height="${sizePx}"/>`);
  collisions.push(`    <object id="${oid++}" x="${sizePx - border}" y="0" width="${border}" height="${sizePx}"/>`);
  collisions.push(`    <object id="${oid++}" x="${center - 192}" y="${center - 192}" width="384" height="64"/>`);
  collisions.push(`    <object id="${oid++}" x="${center - 192}" y="${center + 128}" width="384" height="64"/>`);
  collisions.push(`    <object id="${oid++}" x="${center - 192}" y="${center - 192}" width="64" height="384"/>`);
  collisions.push(`    <object id="${oid++}" x="${center + 128}" y="${center - 192}" width="64" height="384"/>`);
  collisions.push(`    <object id="${oid++}" x="${tile * 8}" y="${tile * 10}" width="${tile * 8}" height="${tile * 3}"/>`);
  collisions.push(`    <object id="${oid++}" x="${sizePx - tile * 16}" y="${tile * 18}" width="${tile * 10}" height="${tile * 3}"/>`);
  collisions.push(`    <object id="${oid++}" x="${tile * 12}" y="${sizePx - tile * 14}" width="${tile * 12}" height="${tile * 3}"/>`);

  const spawnId = oid++;
  const enemyNodes = [];
  const spawnPoints = [
    [tile * 10, tile * 8],
    [tile * 20, tile * 11],
    [tile * 28, tile * 18],
    [tile * 17, tile * 27],
  ];

  let eid = oid;
  for (let i = 0; i < spawnPoints.length; i++) {
    const [x, y] = spawnPoints[i];
    const type = i % 2 === 0 ? 'slime' : 'dark_elf';
    enemyNodes.push(objectWithProps(eid++, `spawn_${floorCfg.floor}_${i + 1}`, x, y, 28, 24, [
      { name: 'type', value: type },
      { name: 'respawn', type: 'float', value: `${10 + i}` },
    ]));
  }

  if (floorCfg.bossType) {
    enemyNodes.push(objectWithProps(eid++, `boss_${floorCfg.floor}`, center + tile * 6, center + tile * 6, 48, 48, [
      { name: 'type', value: floorCfg.bossType },
      { name: 'respawn', type: 'float', value: '90' },
    ]));
  }

  const npcId = eid++;
  const exitId = eid++;
  const img = `floor${floorCfg.floor}_backdrop.png`;

  return `<?xml version="1.0" encoding="UTF-8"?>
<map version="1.10" tiledversion="1.10.2" orientation="orthogonal" renderorder="right-down" width="${floorCfg.size}" height="${floorCfg.size}" tilewidth="32" tileheight="32" infinite="0" nextlayerid="7" nextobjectid="${exitId + 2}">
  <properties>
    <property name="floorName" value="${floorCfg.name}"/>
  </properties>
  <imagelayer id="6" name="visual_backdrop">
    <image source="${img}" width="${sizePx}" height="${sizePx}"/>
  </imagelayer>
  <objectgroup id="1" name="collision">
${collisions.join('\n')}
  </objectgroup>
  <objectgroup id="2" name="spawn">
    <object id="${spawnId}" x="96" y="96" width="32" height="32"/>
  </objectgroup>
  <objectgroup id="3" name="enemy_spawns">
${enemyNodes.join('\n')}
  </objectgroup>
  <objectgroup id="4" name="npcs">
${objectWithProps(npcId, floorCfg.npc.id, 192, 192, 28, 32, [
    { name: 'name', value: floorCfg.npc.name },
    { name: 'role', value: floorCfg.npc.role },
    { name: 'dialog', value: floorCfg.npc.dialog.replace(/"/g, '&quot;') },
  ])}
  </objectgroup>
  <objectgroup id="5" name="exits">
${objectWithProps(exitId, `to_floor_${nextFloor}`, sizePx - 192, sizePx - 192, 128, 128, [
    { name: 'toFloor', type: 'int', value: `${nextFloor}` },
  ])}
  </objectgroup>
</map>
`;
}

for (const floorCfg of route) {
  const xml = buildMap(floorCfg, nextByFloor.get(floorCfg.floor));
  fs.writeFileSync(path.join(mapsDir, `floor${floorCfg.floor}.tmx`), xml, 'utf8');
}

console.log('Regenerated TMX maps with image backdrops.');
