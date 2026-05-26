using System.IO;
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine;

[InitializeOnLoad]
public static class AutoOpenTownScene
{
    private const string ScenePath = "Assets/Scenes/TownOfBeginnings.unity";
    private const string SessionKey = "sao.auto_open_town_scene_once";

    static AutoOpenTownScene()
    {
        // Open once per editor session to avoid interrupting normal work.
        if (SessionState.GetBool(SessionKey, false))
        {
            return;
        }

        EditorApplication.delayCall += TryOpenTownScene;
    }

    private static void TryOpenTownScene()
    {
        if (SessionState.GetBool(SessionKey, false))
        {
            return;
        }

        if (!File.Exists(ScenePath))
        {
            return;
        }

        var active = EditorSceneManager.GetActiveScene();
        var activePath = active.path ?? string.Empty;
        if (activePath == ScenePath)
        {
            SessionState.SetBool(SessionKey, true);
            return;
        }

        if (EditorSceneManager.SaveCurrentModifiedScenesIfUserWantsTo())
        {
            EditorSceneManager.OpenScene(ScenePath, OpenSceneMode.Single);
            SessionState.SetBool(SessionKey, true);
            Debug.Log("[AutoOpenTownScene] Opened TownOfBeginnings scene.");
        }
    }
}
