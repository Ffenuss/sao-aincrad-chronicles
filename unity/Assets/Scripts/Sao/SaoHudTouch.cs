using UnityEngine;

namespace Sao
{
    public class SaoHudTouch : MonoBehaviour
    {
        public SaoPlayerCombat playerCombat;
        public SaoPlayerStats playerStats;
        public float buttonSize = 98f;
        public float buttonGap = 16f;
        public float margin = 26f;

        private void Start()
        {
            if (playerCombat == null)
            {
                playerCombat = FindObjectOfType<SaoPlayerCombat>();
            }
            if (playerStats == null)
            {
                playerStats = FindObjectOfType<SaoPlayerStats>();
            }
        }

        private void OnGUI()
        {
            DrawPlayerHp();
            DrawCombatButtons();
        }

        private void DrawCombatButtons()
        {
            var right = Screen.width - margin - buttonSize;
            var bottom = Screen.height - margin - buttonSize;
            var atk = new Rect(right, bottom, buttonSize, buttonSize);
            var s1 = new Rect(right - (buttonSize + buttonGap), bottom + buttonSize * 0.35f, buttonSize * 0.82f, buttonSize * 0.82f);
            var s2 = new Rect(right - (buttonSize + buttonGap), bottom - buttonSize * 0.35f, buttonSize * 0.82f, buttonSize * 0.82f);
            var s3 = new Rect(right - (buttonSize * 2f + buttonGap * 2f), bottom, buttonSize * 0.82f, buttonSize * 0.82f);

            if (DrawRoundButton(atk, "ATK", new Color(0.70f, 0.20f, 0.24f, 0.95f)) && playerCombat != null) playerCombat.TriggerAttack();
            if (DrawRoundButton(s1, "S1", new Color(0.23f, 0.36f, 0.74f, 0.95f)) && playerCombat != null) playerCombat.TriggerSkill1();
            if (DrawRoundButton(s2, "S2", new Color(0.20f, 0.60f, 0.85f, 0.95f)) && playerCombat != null) playerCombat.TriggerSkill2();
            if (DrawRoundButton(s3, "S3", new Color(0.42f, 0.29f, 0.72f, 0.95f)) && playerCombat != null) playerCombat.TriggerSkill3();

            if (playerCombat != null && playerCombat.Cooldown01 > 0.01f)
            {
                var shade = atk;
                var h = shade.height * playerCombat.Cooldown01;
                shade.y += shade.height - h;
                shade.height = h;
                var prev = GUI.color;
                GUI.color = new Color(0f, 0f, 0f, 0.48f);
                GUI.DrawTexture(shade, Texture2D.whiteTexture);
                GUI.color = prev;
            }
        }

        private void DrawPlayerHp()
        {
            if (playerStats == null) return;
            var x = 22f;
            var y = 18f;
            var w = 320f;
            var h = 22f;
            var hp01 = Mathf.Clamp01(playerStats.Hp01);
            var back = new Rect(x, y, w, h);
            var fill = new Rect(x + 2f, y + 2f, (w - 4f) * hp01, h - 4f);
            var prev = GUI.color;
            GUI.color = new Color(0f, 0f, 0f, 0.62f);
            GUI.DrawTexture(back, Texture2D.whiteTexture);
            GUI.color = hp01 > 0.45f ? new Color(0.24f, 0.90f, 0.34f, 0.95f) : new Color(0.92f, 0.23f, 0.23f, 0.95f);
            GUI.DrawTexture(fill, Texture2D.whiteTexture);
            GUI.color = prev;
            GUI.Label(new Rect(x + 8f, y - 1f, 220f, 24f), $"HP {playerStats.hp}/{playerStats.maxHp}");
        }

        private static bool DrawRoundButton(Rect rect, string text, Color color)
        {
            var prev = GUI.color;
            GUI.color = new Color(0f, 0f, 0f, 0.22f);
            GUI.DrawTexture(new Rect(rect.x + 3f, rect.y + 4f, rect.width, rect.height), Texture2D.whiteTexture);
            GUI.color = color;
            GUI.DrawTexture(rect, Texture2D.whiteTexture);
            GUI.color = Color.white;
            var pressed = GUI.Button(rect, text);
            GUI.color = prev;
            return pressed;
        }
    }
}
