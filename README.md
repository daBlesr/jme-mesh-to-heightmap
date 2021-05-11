# jme-mesh-to-heightmap
jMonkeyEngine Mesh to Heightmap Util


# usage

```
float scale = 0.052f;
int sizeMap = 256;
int patchSize = 65;

Geometry terrain = ((Geometry) ModelLoader.loadModel(
    assetManager,
    new File("terrain/map2.j3o"))
);

terrain.scale(scale);

MeshBasedHeightMap meshBasedHeightMap = new MeshBasedHeightMap(terrain.getMesh(), sizeMap);
meshBasedHeightMap.load();
meshBasedHeightMap.setHeightScale(scale);

terrainNode = new Node("terrain");

RigidBodyControl mapRigidBody = new RigidBodyControl(
    new HeightfieldCollisionShape(
        new TerrainQuad("my terrain", patchSize, sizeMap + 1, meshBasedHeightMap.getScaledHeightMap()),
        Vector3f.UNIT_XYZ
    ),
    0
);

terrainNode.addControl(mapRigidBody);
getPhysicsSpace().add(mapRigidBody);

terrainNode.attachChild(terrain);


```
