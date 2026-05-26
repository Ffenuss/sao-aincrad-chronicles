using System.Linq;
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine;

public static class SaoSceneSmokeCheck
{
    [MenuItem("SAO/Smoke Check Town Scene")]
    public static void CheckTownScene()
    {
        const string scenePath = "Assets/Scenes/TownOfBeginnings.unity";
        var scene = EditorSceneManager.OpenScene(scenePath, OpenSceneMode.Single);
        if (!scene.IsValid())
        {
            throw new System.Exception("[Smoke] Failed to open Town scene.");
        }

        var world = GameObject.Find("SaoWorld");
        var player = GameObject.Find("Player");
        var colliders = GameObject.Find("Colliders");
        var enemies = GameObject.Find("Enemies");

        if (world == null) throw new System.Exception("[Smoke] SaoWorld missing.");
        if (player == null) throw new System.Exception("[Smoke] Player missing.");
        if (colliders == null) throw new System.Exception("[Smoke] Colliders root missing.");
        if (enemies == null) throw new System.Exception("[Smoke] Enemies root missing.");

        var playerCol = player.GetComponent<BoxCollider2D>();
        if (playerCol == null) throw new System.Exception("[Smoke] Player collider missing.");

        var overlap = colliders.GetComponentsInChildren<BoxCollider2D>()
            .Any(c => c.bounds.Intersects(playerCol.bounds));
        if (overlap)
        {
            throw new System.Exception("[Smoke] Player spawn intersects collision.");
        }

        var enemyCount = enemies.GetComponentsInChildren<Transform>().Count(t => t != enemies.transform);
        if (enemyCount <= 0)
        {
            throw new System.Exception("[Smoke] No enemies spawned.");
        }

        Debug.Log("[Smoke] Town scene OK. Enemies: " + enemyCount);
    }
}
