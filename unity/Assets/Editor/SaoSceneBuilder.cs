using Sao;
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine;

public static class SaoSceneBuilder
{
    [MenuItem("SAO/Build Town Of Beginnings Scene")]
    public static void BuildTownScene()
    {
        var scene = EditorSceneManager.NewScene(NewSceneSetup.EmptyScene, NewSceneMode.Single);

        var cameraGo = new GameObject("Main Camera");
        var camera = cameraGo.AddComponent<Camera>();
        camera.orthographic = true;
        camera.tag = "MainCamera";

        var lightGo = new GameObject("Global Light");
        lightGo.AddComponent<Light>().type = LightType.Directional;

        var root = new GameObject("SaoWorld");
        var bootstrap = root.AddComponent<SaoMapBootstrap>();
        bootstrap.rebuildOnStart = true;
        bootstrap.clearBeforeBuild = true;
        bootstrap.Build();

        const string scenePath = "Assets/Scenes/TownOfBeginnings.unity";
        EditorSceneManager.SaveScene(scene, scenePath);
        AssetDatabase.SaveAssets();
        AssetDatabase.Refresh();
        Debug.Log("[SaoSceneBuilder] Scene built: " + scenePath);
    }
}
