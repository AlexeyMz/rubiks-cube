package ru.alexeymz.rubikscube.view;

import static ru.alexeymz.rubikscube.graphics.MathUtils.*;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Collections;

import ru.alexeymz.rubikscube.core.Axis;
import ru.alexeymz.rubikscube.core.CubeCoords;
import ru.alexeymz.rubikscube.core.CubeSide;
import ru.alexeymz.rubikscube.core.DataCube;
import ru.alexeymz.rubikscube.core.Rotation;
import ru.alexeymz.rubikscube.elements.SmallCube;

public class CubeView {
    private final String vertexShaderCode =
        "uniform mat4 mMVP" +
        "uniform mat4 mWorld;" +
        "attribute vec4 vPosition;" +
        "attribute vec4 vNormal;" +
        "void main() {" +
        "  gl_Position = mMVP * mWorld * vPosition;" +
        "}";

    private final String fragmentShaderCode =
        "precision mediump float;" +
        "void main() {" +
        "  gl_FragColor = vec4(0, 1, 0, 1);" +
        "}";

    private int effectProgram;
    private int worldUniform;

    private float[] vertices = {
        -.5f, +.5f, +.5f,  //   1----2
        -.5f, +.5f, -.5f,  //  /:   /|
        +.5f, +.5f, -.5f,  // 0----3 |
        +.5f, +.5f, +.5f,  // | 5..|.6
        -.5f, -.5f, +.5f,  // |.   |/
        -.5f, -.5f, -.5f,  // 4----7
        +.5f, -.5f, -.5f,
        +.5f, -.5f, +.5f,
    };
    private float[] normals = {
        0,  1,  0,  0,  1,  0,
        0, -1,  0,  0, -1,  0,
       -1,  0,  0, -1,  0,  0,
        1,  0,  0,  1,  0,  0,
        0,  0,  1,  0,  0,  1,
        0,  0, -1,  0,  0, -1,
    };
    private short[] indices = {
        0, 2, 1,  //    0 5
        0, 3, 2,  //    |/
        4, 6, 7,  // 2--o--3
        4, 5, 6,  //   /|
        4, 1, 5,  //  4 1
        4, 0, 1,
        7, 2, 3,
        7, 6, 2,
        4, 3, 0,
        4, 7, 3,
        5, 2, 6,
        5, 1, 2,
    };


    private FloatBuffer vertexBuffer;
    private FloatBuffer normalBuffer;
    private ShortBuffer indexBuffer;
    private int[] indexBufferObject = new int[1];

    private DataCube<CubePart> viewCube;

    private int animatedLayer;
    private Axis animatedAxis;

    private float rotationAngle;
    private int stepsLeft = 0;

    public CubeView(DataCube<SmallCube> model, int[] colorMap, boolean removeBlackParts) {
        if (colorMap == null)
            throw new IllegalArgumentException("colorMap");

        initializeView(model, colorMap, removeBlackParts);
        initializeBuffers();
    }

    private void initializeView(DataCube<SmallCube> model, int[] colorMap, boolean removeBlackParts) {
        viewCube = new DataCube<CubePart>(model.size);
        float[] partTransform = new float[16];
        Matrix.setIdentityM(partTransform, 0);
        float scaleFactor = 0.9f * 2f / model.size;
        Matrix.scaleM(partTransform, 0, scaleFactor, scaleFactor, scaleFactor);
        EmptyCubePart emptyPart = new EmptyCubePart();

        float[] position = new float[3];

        int maxIndex = model.size - 1;
        for (int i = 0; i < model.size; i++) {
            for (int j = 0; j < model.size; j++) {
                for (int k = 0; k < model.size; k++) {
                    boolean isInnerCube = i != 0 && j != 0 && k != 0 &&
                            i != maxIndex && j != maxIndex && k != maxIndex;

                    if (model.get(i, j, k) == null || removeBlackParts && isInnerCube) {
                        viewCube.set(i, j, k, emptyPart);
                    } else {
                        CubePart part = new DefaultCubePart();
                        paintCubePart(part, model.get(i, j, k), colorMap);

                        copy(partTransform, part.world, 16);
                        new CubeCoords(i, j, k).getPositionInSpace(position, 0, model.size);

                        setPosition(part.world, position[0], position[1], position[2]);

                        viewCube.set(i, j, k, part);
                    }
                }
            }
        }
    }

    public boolean isAnimationInProgress() {
        return stepsLeft > 0;
    }

    private void paintCubePart(CubePart part, SmallCube model, int[] colorMap) {
        for (CubeSide side : CubeSide.values()) {
            part.set(side, colorMap[model.get(side)]);
        }
    }

    public void beginLayerRotation(
            Rotation rotation, int stepCount) {
        if (rotation.layer >= viewCube.size)
            throw new IllegalArgumentException("rotatedLayer must be in [0..size)");
        if (stepCount <= 0)
            throw new IllegalArgumentException("stepCount must be >= 0.");
        if (isAnimationInProgress())
            throw new IllegalStateException("Animation already in progress.");

        animatedAxis = rotation.axis;
        animatedLayer = rotation.layer;
        stepsLeft = stepCount;

        rotationAngle = (float)Math.PI / (2 * stepCount) * (rotation.clockwise ? +1 : -1);
    }

    private void rotateLayer(float angle) {
        for (int i = 0; i < viewCube.size; i++) {
            for (int j = 0; j < viewCube.size; j++) {
                switch (animatedAxis) {
                    case LEFT:
                        rotateAround(viewCube.get(animatedLayer, i, j).world,
                                0, 0, 0, angle, 0, 0);
                        break;
                    case TOP:
                        rotateAround(viewCube.get(i, animatedLayer, j).world,
                                0, 0, 0, 0, -angle, 0);
                        break;
                    case DEPTH:
                        rotateAround(viewCube.get(i, j, animatedLayer).world,
                                0, 0, 0, 0, 0, -angle);
                        break;
                }
            }
        }
    }

    public void updateAnimation() {
        if (!isAnimationInProgress()) { return; }

        rotateLayer(rotationAngle);

        stepsLeft--;
        if (stepsLeft == 0) {
            endRotation();
        }
    }

    public void endAnimation() {
        if (!isAnimationInProgress())
            throw new IllegalStateException("No animation in progress.");

        float rotationLeft = rotationAngle * stepsLeft;
        rotateLayer(rotationLeft);

        stepsLeft = 0;
        endRotation();
    }

    private void endRotation() {
        viewCube.rotateLayer(animatedAxis, animatedLayer, rotationAngle > 0);
    }

    private void initializeBuffers() {
        vertexBuffer = createBuffer(vertices.length * 4).asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        normalBuffer = createBuffer(normals.length * 4).asFloatBuffer();
        normalBuffer.put(normals);
        normalBuffer.position(0);

        indexBuffer = createBuffer(indices.length * 2).asShortBuffer();
        indexBuffer.put(indices);
        indexBuffer.position(0);

        GLES20.glGenBuffers(1, indexBufferObject, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBufferObject[0]);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * 2,
                indexBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    private static ByteBuffer createBuffer(int size) {
        return ByteBuffer.allocate(size).order(ByteOrder.nativeOrder());
    }

    private void initializePrograms() {
        effectProgram = createProgram(vertexShaderCode, fragmentShaderCode);
    }

    private int createProgram(String vertexShaderCode, String fragmentShaderCode) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        int program = GLES20.glCreateProgram();         // create empty OpenGL ES Program
        GLES20.glAttachShader(program, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(program, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(program);                  // creates OpenGL ES program executables
        return program;
    }

    private static int loadShader(int type, String shaderCode) {
        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    public void draw(float[] mvp) {
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(effectProgram);

        // get handle to vertex shader's vPosition member
        int positionAttribute = GLES20.glGetAttribLocation(effectProgram, "vPosition");
        int normalAttribute = GLES20.glGetAttribLocation(effectProgram, "vNormal");
        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(positionAttribute);
        GLES20.glEnableVertexAttribArray(normalAttribute);
        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(positionAttribute, 3 /* coords per vertex */,
                GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glVertexAttribPointer(normalAttribute, 3 /* coords per vertex */,
                GLES20.GL_FLOAT, false, 0, normalBuffer);

        // Bind index buffer
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBufferObject[0]);

        int mvpUniform = GLES20.glGetUniformLocation(effectProgram, "mMVP");
        GLES20.glUniformMatrix4fv(mvpUniform, 1, false, mvp, 0);
        worldUniform = GLES20.glGetUniformLocation(effectProgram, "mWorld");

        drawParts();

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(positionAttribute);
        GLES20.glDisableVertexAttribArray(normalAttribute);
        // Unbind index buffer
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    private void drawParts() {
        final int maxIndex = viewCube.size - 1;

        // draw Up and Down sides
        for (int i = 0; i < viewCube.size; i++) {
            for (int j = 0; j < viewCube.size; j++) {
                drawPart(i, 0, j);
                drawPart(i, maxIndex, j);
            }
        }

        // draw Left and Right sides without up and down rows
        for (int i = 1; i < maxIndex; i++) {
            for (int j = 0; j < viewCube.size; j++) {
                drawPart(0, i, j);
                drawPart(maxIndex, i, j);
            }
        }

        // draw Front and Back sides without side rows and columns
        for (int i = 1; i < maxIndex; i++) {
            for (int j = 1; j < maxIndex; j++) {
                drawPart(i, j, 0);
                drawPart(i, j, maxIndex);
            }
        }
    }

    private void drawPart(int left, int top, int depth) {
        CubePart part = viewCube.get(left, top, depth);
        // Set cube position and rotation
        GLES20.glUniformMatrix4fv(worldUniform, 1, false, part.world, 0);
        // Draw the triangles
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length / 3, GLES20.GL_UNSIGNED_SHORT, 0);
    }
}
