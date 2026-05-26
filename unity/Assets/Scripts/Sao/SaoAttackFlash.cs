using UnityEngine;

namespace Sao
{
    public class SaoAttackFlash : MonoBehaviour
    {
        public float life = 0.12f;
        public float expand = 3.0f;

        private float _timer;
        private SpriteRenderer _sr;
        private Vector3 _baseScale;

        private void Awake()
        {
            _sr = GetComponent<SpriteRenderer>();
            _baseScale = transform.localScale;
        }

        private void OnEnable()
        {
            _timer = life;
        }

        private void Update()
        {
            _timer -= Time.deltaTime;
            if (_timer <= 0f)
            {
                gameObject.SetActive(false);
                return;
            }
            var t = Mathf.Clamp01(_timer / life);
            transform.localScale = _baseScale * (1f + (1f - t) * expand);
            if (_sr != null)
            {
                var c = _sr.color;
                c.a = t * 0.9f;
                _sr.color = c;
            }
        }
    }
}
