using System.IO;
using UnityEngine;

namespace Sao
{
    [ExecuteAlways]
    public class SaoMapBootstrap : MonoBehaviour
    {
        [Header("Source")]
        public string specFileName = "floor1_scene.json";
        public string artFileName = "floor1_scene_master.png";

        [Header("Render")]
        public int sortingOrder = -50;
        public float cameraPaddingTiles = 1.5f;

        [Header("Build")]
        public bool rebuildOnStart = true;
        public bool clearBeforeBuild = true;

        private const string RootName = "FloorRoot";
        private const string BackdropName = "Backdrop";
        private const string CollidersName = "Colliders";
        private const string MarkersName = "Markers";
        private const string EnemiesName = "Enemies";
        private const string PlayerName = "Player";

        private FloorSceneSpec _spec;

        private void Start()
        {
            if (Application.isPlaying && rebuildOnStart)
            {
                Build();
            }
        }

        [ContextMenu("Build Floor")]
        public void Build()
        {
            _spec = LoadSpec();
            if (_spec == null)
            {
                Debug.LogError("[SaoMapBootstrap] Failed to load floor spec.");
                return;
            }

            var root = GetOrCreateChild(transform, RootName);
            if (clearBeforeBuild)
            {
                ClearChildren(root);
            }

            BuildBackdrop(root, _spec);
            BuildColliders(root, _spec);
            BuildMarkers(root, _spec);
            BuildEnemies(root, _spec);
            BuildPlayer(root, _spec);
            SetupCamera(_spec);
        }

        [ContextMenu("Clear Floor")]
        public void ClearBuiltFloor()
        {
            var root = transform.Find(RootName);
            if (root != null)
            {
                ClearChildren(root);
            }
        }

        private FloorSceneSpec LoadSpec()
        {
            var path = Path.Combine(Application.streamingAssetsPath, "Sao", specFileName);
            if (!File.Exists(path))
            {
                Debug.LogError("[SaoMapBootstrap] Spec not found: " + path);
                return null;
            }

            var raw = File.ReadAllText(path);
            return JsonUtility.FromJson<FloorSceneSpec>(raw);
        }

        private void BuildBackdrop(Transform root, FloorSceneSpec spec)
        {
            var texture = LoadTexture(artFileName);
            if (texture == null)
            {
                return;
            }

            texture.filterMode = FilterMode.Bilinear;
            texture.wrapMode = TextureWrapMode.Clamp;

            var pixelsPerUnit = Mathf.Max(1, spec.tileSize);
            var sprite = Sprite.Create(
                texture,
                new Rect(0f, 0f, texture.width, texture.height),
                new Vector2(0.5f, 0.5f),
                pixelsPerUnit
            );

            var backdropTf = GetOrCreateChild(root, BackdropName);
            backdropTf.position = new Vector3(spec.mapTiles * 0.5f, spec.mapTiles * 0.5f, 0f);

            var renderer = backdropTf.GetComponent<SpriteRenderer>();
            if (renderer == null)
            {
                renderer = backdropTf.gameObject.AddComponent<SpriteRenderer>();
            }
            renderer.sprite = sprite;
            renderer.sortingOrder = sortingOrder;
        }

        private void BuildColliders(Transform root, FloorSceneSpec spec)
        {
            var collidersRoot = GetOrCreateChild(root, CollidersName);
            if (spec.blockers == null)
            {
                return;
            }

            foreach (var blocker in spec.blockers)
            {
                var go = new GameObject(string.IsNullOrWhiteSpace(blocker.name) ? "blocker" : blocker.name);
                go.transform.SetParent(collidersRoot, false);

                var center = TileRectCenter(blocker.x0, blocker.y0, blocker.x1, blocker.y1);
                var size = TileRectSize(blocker.x0, blocker.y0, blocker.x1, blocker.y1);
                go.transform.localPosition = new Vector3(center.x, center.y, 0f);

                var collider = go.AddComponent<BoxCollider2D>();
                collider.size = size;
            }
        }

        private void BuildMarkers(Transform root, FloorSceneSpec spec)
        {
            var markersRoot = GetOrCreateChild(root, MarkersName);
            BuildSpawnMarker(markersRoot, "PlayerSpawn", spec.spawn, Color.cyan, spec.tileSize);

            if (spec.npcs != null)
            {
                foreach (var npc in spec.npcs)
                {
                    BuildSpawnMarker(markersRoot, "NPC_" + npc.id, npc, new Color(1f, 0.87f, 0.2f, 1f), spec.tileSize);
                }
            }

            if (spec.enemySpawns != null)
            {
                foreach (var enemy in spec.enemySpawns)
                {
                    BuildSpawnMarker(markersRoot, "Enemy_" + enemy.id, enemy, new Color(1f, 0.35f, 0.35f, 1f), spec.tileSize);
                }
            }

            if (spec.exit != null)
            {
                BuildSpawnMarker(markersRoot, "Exit_" + spec.exit.name, spec.exit, new Color(0.35f, 1f, 0.45f, 1f), spec.tileSize);
            }
        }

        private static void BuildSpawnMarker(Transform parent, string name, RectSpec rect, Color color, int tileSize)
        {
            if (rect == null) return;
            var go = new GameObject(name);
            go.transform.SetParent(parent, false);
            var ts = Mathf.Max(1, tileSize);
            var center = new Vector3((rect.x / ts) + (rect.width / (ts * 2f)), (rect.y / ts) + (rect.height / (ts * 2f)), 0f);
            go.transform.localPosition = center;

            var gizmo = go.AddComponent<SaoMarkerGizmo>();
            gizmo.size = new Vector2(rect.width / ts, rect.height / ts);
            gizmo.color = color;
        }

        private void SetupCamera(FloorSceneSpec spec)
        {
            var cam = Camera.main;
            if (cam == null) return;
            cam.orthographic = true;
            cam.transform.position = new Vector3(spec.mapTiles * 0.5f, spec.mapTiles * 0.5f, -10f);

            var mapHeight = spec.mapTiles;
            var mapWidth = spec.mapTiles;
            var targetOrtho = 7.5f;
            if (cam.aspect > 0.01f)
            {
                var widthBased = (mapWidth * 0.5f + cameraPaddingTiles) / cam.aspect;
                targetOrtho = Mathf.Min(targetOrtho, widthBased);
            }
            cam.orthographicSize = targetOrtho;

            var follow = cam.GetComponent<SaoCameraFollow>();
            if (follow == null)
            {
                follow = cam.gameObject.AddComponent<SaoCameraFollow>();
            }
            var player = rootPlayerTransform();
            if (player != null) follow.target = player;
            if (cam.GetComponent<SaoHudTouch>() == null)
            {
                cam.gameObject.AddComponent<SaoHudTouch>();
            }
            if (cam.GetComponent<SaoVirtualJoystick>() == null)
            {
                cam.gameObject.AddComponent<SaoVirtualJoystick>();
            }
        }

        private void BuildPlayer(Transform root, FloorSceneSpec spec)
        {
            if (spec.spawn == null) return;

            var playerTf = GetOrCreateChild(root, PlayerName);
            var ts = Mathf.Max(1, spec.tileSize);
            var spawn = new Vector3((spec.spawn.x / ts) + 0.5f, (spec.spawn.y / ts) + 0.5f, 0f);
            playerTf.localPosition = spawn;

            var sr = playerTf.GetComponent<SpriteRenderer>();
            if (sr == null)
            {
                sr = playerTf.gameObject.AddComponent<SpriteRenderer>();
            }
            sr.sprite = BuildPlayerSprite();
            sr.sortingOrder = 100;

            var collider = playerTf.GetComponent<BoxCollider2D>();
            if (collider == null)
            {
                collider = playerTf.gameObject.AddComponent<BoxCollider2D>();
            }
            collider.size = new Vector2(0.72f, 0.88f);
            collider.offset = new Vector2(0f, -0.06f);

            var rb = playerTf.GetComponent<Rigidbody2D>();
            if (rb == null)
            {
                rb = playerTf.gameObject.AddComponent<Rigidbody2D>();
            }
            rb.bodyType = RigidbodyType2D.Dynamic;
            rb.constraints = RigidbodyConstraints2D.FreezeRotation;
            rb.gravityScale = 0f;

            if (playerTf.GetComponent<SaoPlayerController>() == null)
            {
                playerTf.gameObject.AddComponent<SaoPlayerController>();
            }
            var stats = playerTf.GetComponent<SaoPlayerStats>();
            if (stats == null)
            {
                stats = playerTf.gameObject.AddComponent<SaoPlayerStats>();
            }
            stats.SetSpawnPoint(spawn);
            if (playerTf.GetComponent<SaoPlayerCombat>() == null)
            {
                playerTf.gameObject.AddComponent<SaoPlayerCombat>();
            }
            if (playerTf.GetComponent<SaoPlayerSpriteAnimator>() == null)
            {
                playerTf.gameObject.AddComponent<SaoPlayerSpriteAnimator>();
            }
        }

        private void BuildEnemies(Transform root, FloorSceneSpec spec)
        {
            var enemiesRoot = GetOrCreateChild(root, EnemiesName);
            if (spec.enemySpawns == null) return;
            var ts = Mathf.Max(1, spec.tileSize);

            foreach (var enemySpawn in spec.enemySpawns)
            {
                var go = new GameObject("Enemy_" + enemySpawn.id);
                go.transform.SetParent(enemiesRoot, false);
                go.transform.localPosition = new Vector3(
                    (enemySpawn.x / ts) + 0.5f,
                    (enemySpawn.y / ts) + 0.5f,
                    0f
                );

                var sr = go.AddComponent<SpriteRenderer>();
                sr.sprite = BuildEnemySprite();
                sr.sortingOrder = 90;

                var col = go.AddComponent<BoxCollider2D>();
                col.size = new Vector2(0.7f, 0.7f);
                col.offset = new Vector2(0f, -0.05f);

                var enemy = go.AddComponent<SaoEnemy>();
                enemy.Init(enemySpawn.id, 35);
                enemy.moveSpeed = 1.9f;
                enemy.aggroRange = 3.8f;
                enemy.attackRange = 0.82f;
                enemy.attackCooldown = 1.05f;
                enemy.contactDamage = 7;
                go.AddComponent<SaoEnemyHpBar>();
            }
        }

        private static Sprite BuildPlayerSprite()
        {
            var tex = new Texture2D(16, 16, TextureFormat.RGBA32, false);
            tex.filterMode = FilterMode.Point;
            var clear = new Color32(0, 0, 0, 0);
            var body = new Color32(50, 92, 184, 255);
            var trim = new Color32(240, 240, 240, 255);
            var head = new Color32(245, 216, 170, 255);
            var hair = new Color32(28, 30, 38, 255);
            for (var y = 0; y < 16; y++)
            {
                for (var x = 0; x < 16; x++)
                {
                    tex.SetPixel(x, y, clear);
                }
            }
            for (var y = 2; y <= 8; y++)
            {
                for (var x = 4; x <= 11; x++) tex.SetPixel(x, y, body);
            }
            for (var x = 4; x <= 11; x++)
            {
                tex.SetPixel(x, 2, trim);
                tex.SetPixel(x, 8, trim);
            }
            for (var y = 9; y <= 12; y++)
            {
                for (var x = 5; x <= 10; x++) tex.SetPixel(x, y, head);
            }
            for (var y = 12; y <= 14; y++)
            {
                for (var x = 4; x <= 11; x++) tex.SetPixel(x, y, hair);
            }
            tex.Apply();
            return Sprite.Create(tex, new Rect(0, 0, 16, 16), new Vector2(0.5f, 0.2f), 16f);
        }

        private static Sprite BuildEnemySprite()
        {
            var tex = new Texture2D(16, 16, TextureFormat.RGBA32, false);
            tex.filterMode = FilterMode.Point;
            var clear = new Color32(0, 0, 0, 0);
            var green = new Color32(68, 186, 82, 255);
            var dark = new Color32(26, 82, 34, 255);
            for (var y = 0; y < 16; y++)
            {
                for (var x = 0; x < 16; x++) tex.SetPixel(x, y, clear);
            }
            for (var y = 4; y <= 10; y++)
            {
                for (var x = 3; x <= 12; x++) tex.SetPixel(x, y, green);
            }
            for (var x = 5; x <= 10; x++) tex.SetPixel(x, 11, dark);
            tex.SetPixel(6, 8, dark);
            tex.SetPixel(9, 8, dark);
            tex.Apply();
            return Sprite.Create(tex, new Rect(0, 0, 16, 16), new Vector2(0.5f, 0.2f), 16f);
        }

        private Transform rootPlayerTransform()
        {
            var root = transform.Find(RootName);
            return root == null ? null : root.Find(PlayerName);
        }

        private Texture2D LoadTexture(string fileName)
        {
            var path = Path.Combine(Application.streamingAssetsPath, "Sao", fileName);
            if (!File.Exists(path))
            {
                Debug.LogError("[SaoMapBootstrap] Art not found: " + path);
                return null;
            }

            var bytes = File.ReadAllBytes(path);
            var tex = new Texture2D(2, 2, TextureFormat.RGBA32, false);
            if (!tex.LoadImage(bytes))
            {
                Debug.LogError("[SaoMapBootstrap] Failed to decode image: " + path);
                return null;
            }
            return tex;
        }

        private static Vector2 TileRectCenter(int x0, int y0, int x1, int y1)
        {
            var width = (x1 - x0 + 1);
            var height = (y1 - y0 + 1);
            return new Vector2(x0 + width * 0.5f, y0 + height * 0.5f);
        }

        private static Vector2 TileRectSize(int x0, int y0, int x1, int y1)
        {
            var width = (x1 - x0 + 1);
            var height = (y1 - y0 + 1);
            return new Vector2(width, height);
        }

        private static Transform GetOrCreateChild(Transform parent, string name)
        {
            var child = parent.Find(name);
            if (child != null) return child;
            var go = new GameObject(name);
            go.transform.SetParent(parent, false);
            return go.transform;
        }

        private static void ClearChildren(Transform parent)
        {
            for (var i = parent.childCount - 1; i >= 0; i--)
            {
                var child = parent.GetChild(i);
                if (Application.isPlaying)
                {
                    Destroy(child.gameObject);
                }
                else
                {
                    DestroyImmediate(child.gameObject);
                }
            }
        }
    }

    public class SaoMarkerGizmo : MonoBehaviour
    {
        public Vector2 size = Vector2.one * 0.8f;
        public Color color = Color.white;

        private void OnDrawGizmos()
        {
            Gizmos.color = color;
            Gizmos.DrawWireCube(transform.position, new Vector3(size.x, size.y, 0f));
        }
    }
}
