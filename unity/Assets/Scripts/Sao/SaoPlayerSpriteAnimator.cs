using System.IO;
using UnityEngine;

namespace Sao
{
    [RequireComponent(typeof(SpriteRenderer))]
    [RequireComponent(typeof(SaoPlayerController))]
    public class SaoPlayerSpriteAnimator : MonoBehaviour
    {
        public string idleFileName = "player_idle.png";
        public string runFileName = "player_run.png";
        public int frameSize = 32;
        public float runFps = 10f;

        private SpriteRenderer _renderer;
        private SaoPlayerController _controller;
        private Sprite[][] _idleByDir;
        private Sprite[][] _runByDir;
        private float _animTimer;

        // Row order for sheets: 8 directions top-to-bottom.
        // You can tweak this mapping later if any row is visually swapped.
        private static readonly Vector2[] Directions =
        {
            Vector2.down,
            new Vector2(-1f, -1f).normalized,
            Vector2.left,
            new Vector2(-1f, 1f).normalized,
            Vector2.up,
            new Vector2(1f, 1f).normalized,
            Vector2.right,
            new Vector2(1f, -1f).normalized,
        };

        private void Awake()
        {
            _renderer = GetComponent<SpriteRenderer>();
            _controller = GetComponent<SaoPlayerController>();
            _idleByDir = SliceSheet(idleFileName);
            _runByDir = SliceSheet(runFileName);
        }

        private void Update()
        {
            if (_idleByDir == null || _runByDir == null) return;

            var move = _controller.CurrentMove;
            var moving = move.sqrMagnitude > 0.0001f;
            var dirIndex = DirectionIndex(moving ? move : _controller.LastFacing);
            var frames = moving ? _runByDir[dirIndex] : _idleByDir[dirIndex];
            if (frames == null || frames.Length == 0) return;

            if (moving)
            {
                _animTimer += Time.deltaTime * runFps;
                var frame = Mathf.FloorToInt(_animTimer) % frames.Length;
                _renderer.sprite = frames[frame];
            }
            else
            {
                _animTimer = 0f;
                _renderer.sprite = frames[0];
            }
        }

        private Sprite[][] SliceSheet(string fileName)
        {
            var path = Path.Combine(Application.streamingAssetsPath, "Sao", fileName);
            if (!File.Exists(path))
            {
                Debug.LogError("[SaoPlayerSpriteAnimator] Missing file: " + path);
                return null;
            }

            var tex = new Texture2D(2, 2, TextureFormat.RGBA32, false);
            tex.LoadImage(File.ReadAllBytes(path));
            tex.filterMode = FilterMode.Point;

            var cols = tex.width / frameSize;
            var rows = tex.height / frameSize;
            var outDirs = new Sprite[Mathf.Min(8, rows)][];
            for (var row = 0; row < outDirs.Length; row++)
            {
                outDirs[row] = new Sprite[cols];
                for (var col = 0; col < cols; col++)
                {
                    var x = col * frameSize;
                    var y = tex.height - ((row + 1) * frameSize);
                    outDirs[row][col] = Sprite.Create(
                        tex,
                        new Rect(x, y, frameSize, frameSize),
                        new Vector2(0.5f, 0.2f),
                        frameSize
                    );
                }
            }
            return outDirs;
        }

        private static int DirectionIndex(Vector2 move)
        {
            if (move.sqrMagnitude < 0.0001f) return 0;
            var best = 0;
            var bestDot = -999f;
            var n = move.normalized;
            for (var i = 0; i < Directions.Length; i++)
            {
                var d = Vector2.Dot(n, Directions[i]);
                if (d > bestDot)
                {
                    bestDot = d;
                    best = i;
                }
            }
            return best;
        }
    }
}
