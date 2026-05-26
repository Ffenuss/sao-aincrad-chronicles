using System;

namespace Sao
{
    [Serializable]
    public class FloorSceneSpec
    {
        public int floorNumber;
        public string floorName;
        public int mapTiles;
        public int tileSize;
        public string artSource;
        public RectSpec spawn;
        public ExitSpec exit;
        public EnemySpawnSpec[] enemySpawns;
        public NpcSpawnSpec[] npcs;
        public RoadSpec[] roads;
        public RectTilesSpec fountain;
        public RectTilesSpec[] blockers;
    }

    [Serializable]
    public class RectSpec
    {
        public float x;
        public float y;
        public float width;
        public float height;
    }

    [Serializable]
    public class ExitSpec : RectSpec
    {
        public string name;
        public int toFloor;
    }

    [Serializable]
    public class EnemySpawnSpec : RectSpec
    {
        public string id;
        public string type;
        public float respawn;
    }

    [Serializable]
    public class NpcSpawnSpec : RectSpec
    {
        public string id;
        public string name;
        public string role;
        public string dialog;
    }

    [Serializable]
    public class RoadSpec
    {
        public int x0;
        public int y0;
        public int x1;
        public int y1;
        public string kind;
    }

    [Serializable]
    public class RectTilesSpec
    {
        public string name;
        public int x0;
        public int y0;
        public int x1;
        public int y1;
    }
}
