using UnityEngine;

namespace Sao
{
    [RequireComponent(typeof(SpriteRenderer))]
    [RequireComponent(typeof(BoxCollider2D))]
    public class SaoEnemy : MonoBehaviour
    {
        public string enemyId;
        public int maxHp = 35;
        public int hp = 35;
        public float moveSpeed = 2.1f;
        public float aggroRange = 4.5f;
        public float attackRange = 0.9f;
        public float attackCooldown = 0.85f;
        public int contactDamage = 9;
        public float hurtKnockback = 1.6f;

        private Rigidbody2D _rb;
        private SaoPlayerStats _player;
        private float _attackTimer;
        private SpriteRenderer _renderer;
        private float _hitFlashTimer;
        private Vector2 _impulseVelocity;

        public void Init(string id, int health)
        {
            enemyId = id;
            maxHp = Mathf.Max(1, health);
            hp = maxHp;
        }

        private void Awake()
        {
            _rb = GetComponent<Rigidbody2D>();
            _renderer = GetComponent<SpriteRenderer>();
            if (_rb == null) _rb = gameObject.AddComponent<Rigidbody2D>();
            _rb.gravityScale = 0f;
            _rb.freezeRotation = true;
            _rb.interpolation = RigidbodyInterpolation2D.Interpolate;
            _rb.collisionDetectionMode = CollisionDetectionMode2D.Continuous;
        }

        private void Start()
        {
            _player = FindObjectOfType<SaoPlayerStats>();
        }

        private void FixedUpdate()
        {
            if (_hitFlashTimer > 0f)
            {
                _hitFlashTimer -= Time.fixedDeltaTime;
                if (_hitFlashTimer <= 0f && _renderer != null) _renderer.color = Color.white;
            }
            _impulseVelocity = Vector2.Lerp(_impulseVelocity, Vector2.zero, 8f * Time.fixedDeltaTime);

            if (_player == null || _player.IsDead)
            {
                _rb.velocity = Vector2.zero;
                return;
            }

            if (_attackTimer > 0f) _attackTimer -= Time.fixedDeltaTime;

            var toPlayer = (Vector2)(_player.transform.position - transform.position);
            var dist = toPlayer.magnitude;
            if (dist > aggroRange)
            {
                _rb.velocity = Vector2.zero;
                return;
            }

            if (dist > attackRange)
            {
                var dir = toPlayer.normalized;
                _rb.velocity = dir * moveSpeed + _impulseVelocity;
            }
            else
            {
                _rb.velocity = _impulseVelocity;
                if (_attackTimer <= 0f)
                {
                    _player.TakeDamage(contactDamage, transform.position);
                    SaoFloatingCombatText.Spawn(_player.transform.position + new Vector3(0f, 0.75f, 0f), "-" + contactDamage, new Color(1f, 0.35f, 0.35f, 1f));
                    _attackTimer = attackCooldown;
                }
            }
        }

        public bool TakeDamage(int damage)
        {
            var dealt = Mathf.Max(1, damage);
            hp = Mathf.Max(0, hp - dealt);
            if (_renderer != null)
            {
                _renderer.color = new Color(1f, 0.55f, 0.55f, 1f);
                _hitFlashTimer = 0.08f;
            }
            SaoFloatingCombatText.Spawn(transform.position + new Vector3(0f, 0.78f, 0f), dealt.ToString(), new Color(1f, 0.92f, 0.72f, 1f));
            if (_player != null)
            {
                var away = ((Vector2)transform.position - (Vector2)_player.transform.position).normalized;
                _impulseVelocity += away * hurtKnockback;
            }
            if (hp <= 0)
            {
                SaoFloatingCombatText.Spawn(transform.position + new Vector3(0f, 0.98f, 0f), "KO", new Color(1f, 0.58f, 0.22f, 1f));
                Destroy(gameObject);
                return true;
            }
            return false;
        }
    }
}
