package jmeanimator.terrain;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.util.BufferUtils;

import java.nio.FloatBuffer;
import java.util.*;

/**
 * MeshBasedHeightMap generates a heightmap based on a Mesh.
 * Particularly useful to generate a collision shape for a (much too detailed) terrain model from blender.
 * @author daBlesr
 */
public class MeshBasedHeightMap extends AbstractHeightMap {

    private Mesh mesh;
    private HashMap<Vector2f, Float> averagePerPos;
    private int lookAroundMatrixSize = 4;

    /**
     * @param size size of one edge of the heightmap.
     */
    public MeshBasedHeightMap(Mesh mesh, int size) {
        this.mesh = mesh;
        this.size = size;
        this.heightData = new float[(size * size)];
    }

    /**
     * If the mesh contains a lot of large triangles, the heightmap will look around to other
     * elements to average out the height. This is the size of the matrix that is used to look around.
     * @param lookAroundMatrixSize
     */
    public void setLookAroundMatrixSize(int lookAroundMatrixSize) {
        if (lookAroundMatrixSize % 2 != 0) {
            throw new IllegalArgumentException("lookAroundMatrixSize must be an even number");
        }
        this.lookAroundMatrixSize = lookAroundMatrixSize;
    }

    @Override
    public boolean load() {
        loadMapFromMesh();
        return true;
    }

    private void loadMapFromMesh() {

        FloatBuffer positionBuffer = mesh.getFloatBuffer(VertexBuffer.Type.Position);

        // list of positions of each vertex of the mesh.
        List<Vector3f> positionList = Arrays.asList(BufferUtils.getVector3Array(positionBuffer));

        Float minX = positionList.stream().map(Vector3f::getX).min(Float::compare).get();
        Float maxX = positionList.stream().map(Vector3f::getX).max(Float::compare).get();
        Float minZ = positionList.stream().map(Vector3f::getZ).min(Float::compare).get();
        Float maxZ = positionList.stream().map(Vector3f::getZ).max(Float::compare).get();

        Map<Vector2f, List<Float>> heightsPerPosition = new HashMap<>();

        positionList
            .forEach(p -> heightsPerPosition.compute(
                // normalize coordinate of vertex to the heightmap size
                new Vector2f(
                    (float) Math.floor((p.x - minX) / (maxX - minX) * size),
                    (float) Math.floor((p.z - minZ) / (maxZ - minZ) * size)
                ),
                (v, c) -> {
                    if (c == null) {
                        ArrayList<Float> l = new ArrayList<>();
                        l.add(p.y);
                        return l;
                    }
                    c.add(p.y);
                    return c;
                }
            ));

        averagePerPos = new HashMap<>();

        // compute the average height per heightmap coordinate.
        heightsPerPosition.forEach((key, value) -> averagePerPos.put(
            key,
            (float) value
                .stream()
                .mapToDouble(Float::doubleValue)
                .average()
                .orElse(Double.NaN)
        ));

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {

                Vector2f coord = new Vector2f(i, j);

                if (averagePerPos.containsKey(coord)) {
                    heightData[i + j * size] = averagePerPos.get(coord);
                } else {
                    heightData[i + j * size] = getInterpolatedHeight(coord);
                }
            }
        }
    }

    // look around to other vertices if the height of the current heightmap coordinate has no value.
    private float getInterpolatedHeight(Vector2f pos) {
        List<Float> toAverage = new ArrayList<>();
        for (int i = -lookAroundMatrixSize / 2; i <= lookAroundMatrixSize / 2; i++) {
            for (int j = - lookAroundMatrixSize / 2; j <= lookAroundMatrixSize / 2; j++) {

                Vector2f coord = new Vector2f(pos.x + i, pos.y + j);

                if (averagePerPos.containsKey(coord)) {
                    toAverage.add(averagePerPos.get(coord));
                }
            }
        }

        return (float) toAverage
            .stream()
            .mapToDouble(Float::doubleValue)
            .average()
            .orElse(0);
    }
}
