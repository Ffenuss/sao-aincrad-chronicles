using UnityEngine;

namespace Sao
{
    [RequireComponent(typeof(Rigidbody2D))]
    public class SaoPlayerController : MonoBehaviour
    {
        public float moveSpeed = 4.4f;

        private Rigidbody2D _rb;
        private Vector2 _input;
        private Vector2 _lastFacing = Vector2.down;
        private SaoPlayerStats _stats;

        public Vector2 CurrentMove => _input;
        public Vector2 LastFacing => _lastFacing;

        private void Awake()
        {
            _rb = GetComponent<Rigidbody2D>();
            _stats = GetComponent<SaoPlayerStats>();
            _rb.gravityScale = 0f;
            _rb.freezeRotation = true;
            _rb.interpolation = RigidbodyInterpolation2D.Interpolate;
            _rb.collisionDetectionMode = CollisionDetectionMode2D.Continuous;
        }

        private void Update()
        {
            var key = new Vector2(Input.GetAxisRaw("Horizontal"), Input.GetAxisRaw("Vertical"));
            var touch = SaoVirtualJoystick.MoveVector;
            var mixed = key.sqrMagnitude >= touch.sqrMagnitude ? key : touch;
            _input = mixed.normalized;
            if (_input.sqrMagnitude > 0.0001f)
            {
                _lastFacing = _input;
            }
        }

        private void FixedUpdate()
        {
            if (_stats != null && _stats.IsDead)
            {
                _rb.velocity = Vector2.zero;
                return;
            }
            _rb.velocity = _input * moveSpeed;
        }
    }
}
