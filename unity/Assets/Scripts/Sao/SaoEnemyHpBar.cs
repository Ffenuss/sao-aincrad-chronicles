using UnityEngine;

namespace Sao
{
    [RequireComponent(typeof(SaoEnemy))]
    public class SaoEnemyHpBar : MonoBehaviour
    {
        public Vector2 offset = new Vector2(0f, 0.62f);
        public Vector2 size = new Vector2(0.72f, 0.09f);

        private SaoEnemy _enemy;
        private SpriteRenderer _back;
        private SpriteRenderer _fill;

        private void Awake()
        {
            _enemy = GetComponent<SaoEnemy>();
            Build();
        }

        private void LateUpdate()
        {
            if (_enemy == null || _fill == null) return;
            var hp01 = _enemy.maxHp <= 0 ? 0f : Mathf.Clamp01(_enemy.hp / (float)_enemy.maxHp);
            _fill.transform.localScale = new Vector3(Mathf.Max(0.02f, hp01), 1f, 1f);
            _fill.color = hp01 > 0.45f ? new Color(0.24f, 0.9f, 0.32f, 1f) : new Color(0.92f, 0.2f, 0.2f, 1f);
        }

        private void Build()
        {
            var root = new GameObject("EnemyHpBar");
            root.transform.SetParent(transform, false);
            root.transform.localPosition = new Vector3(offset.x, offset.y, 0f);

            var backGo = new GameObject("Back");
            backGo.transform.SetParent(root.transform, false);
            _back = backGo.AddComponent<SpriteRenderer>();
            _back.sprite = PixelRectSprite();
            _back.color = new Color(0f, 0f, 0f, 0.72f);
            _back.sortingOrder = 130;
            _back.transform.localScale = new Vector3(size.x, size.y, 1f);

            var fillGo = new GameObject("Fill");
            fillGo.transform.SetParent(root.transform, false);
            _fill = fillGo.AddComponent<SpriteRenderer>();
            _fill.sprite = PixelRectSprite();
            _fill.color = new Color(0.24f, 0.9f, 0.32f, 1f);
            _fill.sortingOrder = 131;
            _fill.transform.localPosition = new Vector3(-size.x * 0.5f + (size.x * 0.5f), 0f, 0f);
            _fill.transform.localScale = new Vector3(size.x, size.y * 0.75f, 1f);
        }

        private static Sprite PixelRectSprite()
        {
            var tex = new Texture2D(2, 2, TextureFormat.RGBA32, false);
            tex.filterMode = FilterMode.Point;
            tex.SetPixels(new[] { Color.white, Color.white, Color.white, Color.white });
            tex.Apply();
            return Sprite.Create(tex, new Rect(0, 0, 2, 2), new Vector2(0.5f, 0.5f), 2f);
        }
    }
}
