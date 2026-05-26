using UnityEngine;

namespace Sao
{
    [RequireComponent(typeof(SaoPlayerController))]
    public class SaoPlayerCombat : MonoBehaviour
    {
        public int damage = 14;
        public float cooldown = 0.28f;
        public float range = 1.05f;
        public float radius = 0.55f;
        public float skill1Multiplier = 1.22f;
        public float skill2Multiplier = 1.48f;
        public float skill3Multiplier = 1.92f;

        private SaoPlayerController _controller;
        private float _cooldownTimer;
        private GameObject _flash;
        public float Cooldown01 => cooldown <= 0f ? 0f : Mathf.Clamp01(_cooldownTimer / cooldown);

        private void Awake()
        {
            _controller = GetComponent<SaoPlayerController>();
            BuildFlash();
        }

        private void Update()
        {
            if (_cooldownTimer > 0f) _cooldownTimer -= Time.deltaTime;
            if (!Input.GetKeyDown(KeyCode.Space)) return;
            TriggerAttack();
        }

        public void TriggerAttack()
        {
            TriggerAttackWithMultiplier(1f);
        }

        public void TriggerSkill1()
        {
            TriggerAttackWithMultiplier(skill1Multiplier);
        }

        public void TriggerSkill2()
        {
            TriggerAttackWithMultiplier(skill2Multiplier);
        }

        public void TriggerSkill3()
        {
            TriggerAttackWithMultiplier(skill3Multiplier);
        }

        private void TriggerAttackWithMultiplier(float multiplier)
        {
            if (_cooldownTimer > 0f) return;
            _cooldownTimer = cooldown;

            var dir = _controller.LastFacing.sqrMagnitude > 0.001f ? _controller.LastFacing.normalized : Vector2.down;
            var center = (Vector2)transform.position + dir * range;
            ShowFlash(center);
            var hits = Physics2D.OverlapCircleAll(center, radius);
            foreach (var hit in hits)
            {
                var enemy = hit.GetComponent<SaoEnemy>();
                if (enemy != null)
                {
                    enemy.TakeDamage(Mathf.RoundToInt(damage * Mathf.Max(0.5f, multiplier)));
                }
            }
        }

        private void OnDrawGizmosSelected()
        {
            if (_controller == null) return;
            var dir = _controller.LastFacing.sqrMagnitude > 0.001f ? _controller.LastFacing.normalized : Vector2.down;
            var center = (Vector2)transform.position + dir * range;
            Gizmos.color = new Color(1f, 0.35f, 0.35f, 0.8f);
            Gizmos.DrawWireSphere(center, radius);
        }

        private void BuildFlash()
        {
            _flash = new GameObject("AttackFlash");
            _flash.transform.SetParent(transform, false);
            _flash.transform.localPosition = Vector3.zero;
            var sr = _flash.AddComponent<SpriteRenderer>();
            sr.sprite = BuildFlashSprite();
            sr.color = new Color(0.95f, 0.98f, 1f, 0f);
            sr.sortingOrder = 140;
            _flash.AddComponent<SaoAttackFlash>();
            _flash.SetActive(false);
        }

        private void ShowFlash(Vector2 worldPos)
        {
            if (_flash == null) return;
            _flash.transform.position = new Vector3(worldPos.x, worldPos.y, 0f);
            _flash.transform.localScale = Vector3.one * 0.18f;
            _flash.SetActive(false);
            _flash.SetActive(true);
        }

        private static Sprite BuildFlashSprite()
        {
            var tex = new Texture2D(16, 16, TextureFormat.RGBA32, false);
            tex.filterMode = FilterMode.Point;
            var clear = new Color32(0, 0, 0, 0);
            var white = new Color32(250, 253, 255, 255);
            for (var y = 0; y < 16; y++)
            {
                for (var x = 0; x < 16; x++) tex.SetPixel(x, y, clear);
            }
            for (var i = 2; i < 14; i++)
            {
                tex.SetPixel(i, 8, white);
                tex.SetPixel(8, i, white);
            }
            for (var i = 4; i < 12; i++)
            {
                tex.SetPixel(i, i, white);
                tex.SetPixel(15 - i, i, white);
            }
            tex.Apply();
            return Sprite.Create(tex, new Rect(0, 0, 16, 16), new Vector2(0.5f, 0.5f), 16f);
        }
    }
}
