using UnityEngine;

namespace Sao
{
    public class SaoVirtualJoystick : MonoBehaviour
    {
        public static Vector2 MoveVector { get; private set; }

        public float baseRadius = 90f;
        public float knobRadius = 36f;
        public float margin = 28f;

        private bool _dragging;
        private int _fingerId = -1;
        private Vector2 _baseCenter;
        private Vector2 _knobPos;

        private void Update()
        {
            if (Input.touchCount > 0)
            {
                HandleTouches();
                return;
            }

            HandleMouse();
        }

        private void HandleTouches()
        {
            if (!_dragging)
            {
                for (var i = 0; i < Input.touchCount; i++)
                {
                    var t = Input.GetTouch(i);
                    if (t.phase != TouchPhase.Began) continue;
                    if (t.position.x > Screen.width * 0.45f) continue;
                    _dragging = true;
                    _fingerId = t.fingerId;
                    _baseCenter = t.position;
                    _knobPos = _baseCenter;
                    MoveVector = Vector2.zero;
                    break;
                }
            }

            if (!_dragging) return;

            var found = false;
            for (var i = 0; i < Input.touchCount; i++)
            {
                var t = Input.GetTouch(i);
                if (t.fingerId != _fingerId) continue;
                found = true;
                if (t.phase == TouchPhase.Ended || t.phase == TouchPhase.Canceled)
                {
                    ResetStick();
                }
                else
                {
                    UpdateStick(t.position);
                }
                break;
            }

            if (!found) ResetStick();
        }

        private void HandleMouse()
        {
            if (Input.GetMouseButtonDown(0) && Input.mousePosition.x <= Screen.width * 0.45f)
            {
                _dragging = true;
                _baseCenter = Input.mousePosition;
                _knobPos = _baseCenter;
                MoveVector = Vector2.zero;
            }
            if (_dragging && Input.GetMouseButton(0))
            {
                UpdateStick(Input.mousePosition);
            }
            if (_dragging && Input.GetMouseButtonUp(0))
            {
                ResetStick();
            }
        }

        private void UpdateStick(Vector2 pointer)
        {
            var delta = pointer - _baseCenter;
            var clamped = Vector2.ClampMagnitude(delta, baseRadius);
            _knobPos = _baseCenter + clamped;
            MoveVector = clamped / baseRadius;
        }

        private void ResetStick()
        {
            _dragging = false;
            _fingerId = -1;
            MoveVector = Vector2.zero;
        }

        private void OnGUI()
        {
            if (!_dragging)
            {
                _baseCenter = new Vector2(margin + baseRadius, Screen.height - margin - baseRadius);
                _knobPos = _baseCenter;
            }
            DrawCircle(_baseCenter, baseRadius, new Color(0.08f, 0.22f, 0.48f, 0.45f));
            DrawCircle(_baseCenter, baseRadius * 0.78f, new Color(0.28f, 0.50f, 0.82f, 0.32f));
            DrawCircle(_knobPos, knobRadius, new Color(0.75f, 0.88f, 1f, 0.82f));
        }

        private static void DrawCircle(Vector2 center, float radius, Color color)
        {
            var prev = GUI.color;
            GUI.color = color;
            var r = new Rect(center.x - radius, Screen.height - center.y - radius, radius * 2f, radius * 2f);
            GUI.DrawTexture(r, Texture2D.whiteTexture);
            GUI.color = prev;
        }
    }
}
