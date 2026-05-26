using UnityEngine;

namespace Sao
{
    public class SaoPlayerStats : MonoBehaviour
    {
        public int maxHp = 100;
        public int hp = 100;
        public float invulnerableAfterHit = 0.25f;
        public float respawnDelay = 1.2f;
        public float hurtKnockback = 1.2f;

        private float _invulnTimer;
        private float _respawnTimer = -1f;
        private Vector3 _spawnPoint;
        private SaoPlayerController _controller;
        private SaoPlayerCombat _combat;
        private SpriteRenderer _renderer;
        private float _hitFlashTimer;
        private Rigidbody2D _rb;

        public bool IsDead => hp <= 0;
        public float Hp01 => maxHp <= 0 ? 0f : Mathf.Clamp01(hp / (float)maxHp);

        private void Awake()
        {
            _controller = GetComponent<SaoPlayerController>();
            _combat = GetComponent<SaoPlayerCombat>();
            _renderer = GetComponent<SpriteRenderer>();
            _rb = GetComponent<Rigidbody2D>();
            _spawnPoint = transform.position;
            hp = maxHp;
        }

        private void Update()
        {
            if (_invulnTimer > 0f) _invulnTimer -= Time.deltaTime;
            if (_hitFlashTimer > 0f)
            {
                _hitFlashTimer -= Time.deltaTime;
                if (_hitFlashTimer <= 0f && _renderer != null && !IsDead)
                {
                    _renderer.color = Color.white;
                }
            }

            if (_respawnTimer >= 0f)
            {
                _respawnTimer -= Time.deltaTime;
                if (_respawnTimer <= 0f)
                {
                    RespawnNow();
                }
            }
        }

        public void SetSpawnPoint(Vector3 spawn)
        {
            _spawnPoint = spawn;
        }

        public void TakeDamage(int amount, Vector2? sourceWorld = null)
        {
            if (IsDead || _invulnTimer > 0f) return;
            var dealt = Mathf.Max(1, amount);
            hp = Mathf.Max(0, hp - dealt);
            _invulnTimer = invulnerableAfterHit;
            SaoFloatingCombatText.Spawn(transform.position + new Vector3(0f, 0.95f, 0f), "-" + dealt, new Color(1f, 0.35f, 0.35f, 1f));
            if (_renderer != null && hp > 0)
            {
                _renderer.color = new Color(1f, 0.58f, 0.58f, 1f);
                _hitFlashTimer = 0.08f;
            }
            if (_rb != null)
            {
                var src = sourceWorld ?? ((Vector2)transform.position - Vector2.down);
                var dir = ((Vector2)transform.position - src).normalized;
                _rb.velocity += dir * hurtKnockback;
            }
            if (IsDead)
            {
                if (_controller != null) _controller.enabled = false;
                if (_combat != null) _combat.enabled = false;
                if (_renderer != null) _renderer.color = new Color(1f, 1f, 1f, 0.45f);
                SaoFloatingCombatText.Spawn(transform.position + new Vector3(0f, 1.15f, 0f), "DOWN", new Color(1f, 0.78f, 0.25f, 1f));
                _respawnTimer = respawnDelay;
            }
        }

        private void RespawnNow()
        {
            transform.position = _spawnPoint;
            hp = maxHp;
            _invulnTimer = 0.75f;
            _respawnTimer = -1f;
            if (_controller != null) _controller.enabled = true;
            if (_combat != null) _combat.enabled = true;
            if (_renderer != null) _renderer.color = Color.white;
            SaoFloatingCombatText.Spawn(transform.position + new Vector3(0f, 1.05f, 0f), "RESPAWN", new Color(0.35f, 0.95f, 1f, 1f));
        }
    }
}
