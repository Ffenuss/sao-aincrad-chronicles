using System.IO;
using UnityEditor;
using UnityEditor.Build.Reporting;
using UnityEditor.Build;

public static class SaoAndroidBuild
{
    public static void BuildDebugApk()
    {
        const string scenePath = "Assets/Scenes/TownOfBeginnings.unity";
        var outDir = "Builds/Android";
        Directory.CreateDirectory(outDir);
        var outPath = Path.Combine(outDir, "SaoUnity.apk");

        CleanupBeeState();

        EditorUserBuildSettings.SwitchActiveBuildTarget(BuildTargetGroup.Android, BuildTarget.Android);
        PlayerSettings.SetScriptingBackend(NamedBuildTarget.Android, ScriptingImplementation.IL2CPP);
        PlayerSettings.Android.targetArchitectures = AndroidArchitecture.ARM64 | AndroidArchitecture.X86_64;
        PlayerSettings.applicationIdentifier = "com.sao.aincrad.unity";
        PlayerSettings.productName = "SAO Aincrad Unity";

        var options = new BuildPlayerOptions
        {
            scenes = new[] { scenePath },
            locationPathName = outPath,
            target = BuildTarget.Android,
            options = BuildOptions.Development,
        };

        BuildReport report = BuildPipeline.BuildPlayer(options);
        if (report.summary.result != BuildResult.Succeeded)
        {
            throw new System.Exception("[SaoAndroidBuild] Build failed: " + report.summary.result);
        }
        UnityEngine.Debug.Log("[SaoAndroidBuild] APK built: " + outPath);
    }

    private static void CleanupBeeState()
    {
        var beeDir = Path.Combine(Directory.GetCurrentDirectory(), "Library", "Bee");
        if (!Directory.Exists(beeDir)) return;

        foreach (var file in Directory.GetFiles(beeDir, "TundraBuildState.state*"))
        {
            TryDelete(file);
        }
        TryDelete(Path.Combine(beeDir, "tundra.digestcache"));
        TryDelete(Path.Combine(beeDir, "tundra.digestcache.tmp"));
    }

    private static void TryDelete(string path)
    {
        if (!File.Exists(path)) return;
        try
        {
            File.Delete(path);
        }
        catch
        {
            // If cleanup fails, build can still continue; Unity may recover.
        }
    }
}
