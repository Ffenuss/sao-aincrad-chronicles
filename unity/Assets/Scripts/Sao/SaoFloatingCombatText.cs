using UnityEngine;

namespace Sao
{
    public class SaoFloatingCombatText : MonoBehaviour
    {
        public string textValue = "0";
        public Color color = Color.white;
        public float life = 0.55f;
        public float riseSpeed = 0.95f;
        public float drift = 0.25f;

        private float _timer;
        private Vector3 _start;
        private float _driftDir;

        public static void Spawn(Vector3 worldPos, string text, Color tint)
        {
            var go = new GameObject("CombatText");
            var comp = go.AddComponent<SaoFloatingCombatText>();
            comp.transform.position = worldPos;
            comp.textValue = text;
            comp.color = tint;
        }

        private void Awake()
        {
            _timer = life;
            _start = transform.position;
            _driftDir = Random.value < 0.5f ? -1f : 1f;
        }

        private void Update()
        {
            _timer -= Time.deltaTime;
            if (_timer <= 0f)
            {
                Destroy(gameObject);
                return;
            }

            var t = 1f - (_timer / life);
            transform.position = _start + new Vector3(_driftDir * drift * t, riseSpeed * t, 0f);
        }

        private void OnGUI()
        {
            var cam = Camera.main;
            if (cam == null) return;
            var screen = cam.WorldToScreenPoint(transform.position);
            if (screen.z < 0f) return;

            var alpha = Mathf.Clamp01(_timer / life);
            var prev = GUI.color;
            GUI.color = new Color(color.r, color.g, color.b, alpha);
            var rect = new Rect(screen.x - 24f, Screen.height - screen.y - 12f, 64f, 22f);
            GUI.Label(rect, textValue);
            GUI.color = prev;
        }
    }
}
