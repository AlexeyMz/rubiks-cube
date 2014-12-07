package ru.alexeymz.rubikscube.view;

import static android.opengl.GLES20.*;
import static ru.alexeymz.rubikscube.graphics.MathUtils.*;

import android.graphics.Color;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Locale;

import ru.alexeymz.rubikscube.core.Axis;
import ru.alexeymz.rubikscube.core.CubeCoords;
import ru.alexeymz.rubikscube.core.CubeSide;
import ru.alexeymz.rubikscube.core.DataCube;
import ru.alexeymz.rubikscube.core.Rotation;
import ru.alexeymz.rubikscube.elements.SmallCube;

public class CubeView {
    private static final String vertexShaderCode =
        "uniform vec3[7] vsColorMap;" +
        "uniform mat4 mMVP;" +
        "uniform mat4 mWorld;" +
        "uniform highp float iPartIndex;" +
        "uniform float[6] fsSides;" +
        "attribute vec4 vPosition;" +
        "attribute lowp float iSideNum;" +
        "varying vec3 vColor;" +
        "void main() {" +
        "  gl_Position = mMVP * mWorld * vPosition;" +
        "  vColor = vsColorMap[int(fsSides[int(iSideNum)])];" +
        "}";

    private static final String touchShaderCode =
        "uniform mat4 mMVP;" +
        "uniform mat4 mWorld;" +
        "uniform highp float iPartIndex;" +
        "attribute vec4 vPosition;" +
        "attribute lowp float iSideNum;" +
        "varying vec3 vColor;" +
        "void main() {" +
        "  gl_Position = mMVP * mWorld * vPosition;" +
        "  vColor = vec3(floor(iPartIndex / 256.0) / 255.0, " +
        "    mod(iPartIndex, 256.0) / 255.0, iSideNum / 255.0);" +
        "}";

    private static final String fragmentShaderCode =
        "precision mediump float;" +
        "varying vec3 vColor;" +
        "void main() {" +
        "  gl_FragColor = vec4(vColor, 1.0);" +
        "}";

    private int screenProgram;
    private int touchProgram;

    private int worldUniform;
    private int partIndexUniform;
    private int sidesUniform;

    private static final float[] VERTICES = {
        -.5f, +.5f, +.5f,  //   1----2
        -.5f, +.5f, -.5f,  //  /:   /|
        +.5f, +.5f, -.5f,  // 0----3 |
        +.5f, +.5f, +.5f,  // | 5..|.6
        -.5f, -.5f, +.5f,  // |.   |/
        -.5f, -.5f, -.5f,  // 4----7
        +.5f, -.5f, -.5f,
        +.5f, -.5f, +.5f,
    };
    private static final short[] INDICES = {
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
    private static final float[] SIDE_VERTICES = new float[INDICES.length * 3];
    private static final float[] SIDE_NUMS = new float[INDICES.length];
    static {
        for (int i = 0; i < INDICES.length; i++) {
            int vertex = i * 3;
            int index = INDICES[i] * 3;
            SIDE_VERTICES[vertex] = VERTICES[index];
            SIDE_VERTICES[vertex + 1] = VERTICES[index + 1];
            SIDE_VERTICES[vertex + 2] = VERTICES[index + 2];
        }
        CubeSide[] sides = {
            CubeSide.UP, CubeSide.DOWN,
            CubeSide.LEFT, CubeSide.RIGHT,
            CubeSide.FRONT, CubeSide.BACK,
        };
        final int VERTICES_PER_SIDE = SIDE_NUMS.length / sides.length;
        for (int i = 0; i < sides.length; i++) {
            for (int j = 0; j < VERTICES_PER_SIDE; j++) {
                SIDE_NUMS[i * VERTICES_PER_SIDE + j] = sides[i].ordinal();
            }
        }
    }

    private static final int EMPTY_SPACE_ALPHA = 0;

    private int textureWidth, textureHeight;
    private int[] fbo = new int[1];
    private int[] texture = new int[1];
    private int[] depthRB = new int[1];
    private float[] clearColor = new float[4];

    private final float[] colorMap = new float[7 * 3];
    private final float[] cubeSides = new float[6];

    private FloatBuffer vertexBuffer;
    private FloatBuffer sideNumBuffer;
    private ByteBuffer readPixelsBuffer;

    private DataCube<CubePart> viewCube;

    private int animatedLayer;
    private Axis animatedAxis;

    private float rotationAngle;
    private int stepsLeft = 0;

    public CubeView(DataCube<SmallCube> model, int[] colorMap, boolean removeBlackParts,
                    int screenWidth, int screenHeight) {
        if (colorMap == null || colorMap.length < 7)
            throw new IllegalArgumentException("colorMap");

        this.textureWidth = screenWidth;
        this.textureHeight = screenHeight;
        initializeView(model, removeBlackParts);
        initializeBuffers();
        initFramebuffer(screenWidth, screenHeight);
        initializePrograms();
        createColorMap(colorMap);
    }

    private void initializeView(DataCube<SmallCube> model, boolean removeBlackParts) {
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
                        paintCubePart(part, model.get(i, j, k));

                        copy(partTransform, part.world, 16);
                        new CubeCoords(i, j, k).getPositionInSpace(position, 0, model.size);

                        setPosition(part.world, position[0], position[1], position[2]);

                        viewCube.set(i, j, k, part);
                    }
                }
            }
        }
    }

    private void paintCubePart(CubePart part, SmallCube model) {
        for (CubeSide side : CubeSide.values()) {
            part.set(side, model.get(side));
        }
    }

    private void createColorMap(int[] colorMap) {
        for (int i = 0; i < colorMap.length; i++) {
            int color = colorMap[i];
            int offset = i * 3;
            this.colorMap[offset] = Color.red(color) / 255f;
            this.colorMap[offset + 1] = Color.green(color) / 255f;
            this.colorMap[offset + 2] = Color.blue(color) / 255f;
        }
    }

    public boolean isAnimationInProgress() {
        return stepsLeft > 0;
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
        vertexBuffer = createDirectBuffer(SIDE_VERTICES.length * 4).asFloatBuffer();
        vertexBuffer.put(SIDE_VERTICES).position(0);

        sideNumBuffer = createDirectBuffer(SIDE_NUMS.length * 4).asFloatBuffer();
        sideNumBuffer.put(SIDE_NUMS).position(0);

        readPixelsBuffer = createDirectBuffer(4);
        readPixelsBuffer.position(0);
    }

    private void initFramebuffer(int width, int height) {
        glGenFramebuffers(1, fbo, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);

        // texture
        glGenTextures(1, texture, 0);
        glBindTexture(GL_TEXTURE_2D, texture[0]);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture[0], 0);

        // depth renderbuffer
        glGenRenderbuffers(1, depthRB, 0);
        glBindRenderbuffer(GL_RENDERBUFFER, depthRB[0]);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthRB[0]);

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException(String.format(
                "Error creating FBO: framebuffer is incomplete. Status: %s", status));
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private static ByteBuffer createDirectBuffer(int size) {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    private void initializePrograms() {
        screenProgram = createProgram(vertexShaderCode, fragmentShaderCode);
        touchProgram = createProgram(touchShaderCode, fragmentShaderCode);
    }

    private int createProgram(String vertexShaderCode, String fragmentShaderCode) {
        int vertexShader = loadShader(GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GL_FRAGMENT_SHADER, fragmentShaderCode);

        int program = glCreateProgram();         // create empty OpenGL ES Program
        glAttachShader(program, vertexShader);   // add the vertex shader to program
        glAttachShader(program, fragmentShader); // add the fragment shader to program
        glLinkProgram(program);                  // creates OpenGL ES program executables
        int[] status = new int[1];
        glGetProgramiv(program, GL_LINK_STATUS, status, 0);
        if (status[0] != GL_TRUE) {
            throw new IllegalStateException(String.format(Locale.US,
                "Shader link error:%n%s", glGetProgramInfoLog(program)));
        }
        return program;
    }

    private static int loadShader(int type, String shaderCode) {
        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = glCreateShader(type);

        // add the source code to the shader and compile it
        glShaderSource(shader, shaderCode);
        glCompileShader(shader);
        int[] status = new int[1];
        glGetShaderiv(shader, GL_COMPILE_STATUS, status, 0);
        if (status[0] != GL_TRUE) {
            throw new IllegalStateException(String.format(
                Locale.US, "Shader compile error:%n%s",
                glGetShaderInfoLog(shader)));
        }

        return shader;
    }

    public void draw(float[] mvp) {
        draw(mvp, true);

        glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);
        glViewport(0, 0, textureWidth, textureHeight);
        glGetFloatv(GL_COLOR_CLEAR_VALUE, clearColor, 0);
        glClearColor(1, 1, 1, EMPTY_SPACE_ALPHA);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        draw(mvp, false);

        glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private void draw(float[] mvp, boolean renderToScreen) {
        int program = renderToScreen ? screenProgram : touchProgram;
        glUseProgram(program);

        int positionAttribute = glGetAttribLocation(program, "vPosition");
        int sideNumAttribute = glGetAttribLocation(program, "iSideNum");

        glEnableVertexAttribArray(positionAttribute);
        glEnableVertexAttribArray(sideNumAttribute);

        glVertexAttribPointer(positionAttribute, 3 /* coords per vertex */,
                GL_FLOAT, false, 0, vertexBuffer);
        glVertexAttribPointer(sideNumAttribute, 1 /* index per vertex */,
                GL_FLOAT, false, 0, sideNumBuffer);

        int mvpUniform = glGetUniformLocation(program, "mMVP");
        glUniformMatrix4fv(mvpUniform, 1, false, mvp, 0);
        if (renderToScreen) {
            int colorMapUniform = glGetUniformLocation(program, "vsColorMap");
            glUniform3fv(colorMapUniform, 7, colorMap, 0);
        }
        worldUniform = glGetUniformLocation(program, "mWorld");
        sidesUniform = glGetUniformLocation(program, "fsSides");
        partIndexUniform = glGetUniformLocation(program, "iPartIndex");

        drawParts(renderToScreen);

        glDisableVertexAttribArray(positionAttribute);
        glDisableVertexAttribArray(sideNumAttribute);
    }

    private void drawParts(boolean toScreen) {
        final int maxIndex = viewCube.size - 1;

        // draw Up and Down sides
        for (int i = 0; i < viewCube.size; i++) {
            for (int j = 0; j < viewCube.size; j++) {
                drawPart(i, 0, j, toScreen);
                drawPart(i, maxIndex, j, toScreen);
            }
        }

        // draw Left and Right sides without up and down rows
        for (int i = 1; i < maxIndex; i++) {
            for (int j = 0; j < viewCube.size; j++) {
                drawPart(0, i, j, toScreen);
                drawPart(maxIndex, i, j, toScreen);
            }
        }

        // draw Front and Back sides without side rows and columns
        for (int i = 1; i < maxIndex; i++) {
            for (int j = 1; j < maxIndex; j++) {
                drawPart(i, j, 0, toScreen);
                drawPart(i, j, maxIndex, toScreen);
            }
        }
    }

    private void drawPart(int left, int top, int depth, boolean renderToScreen) {
        CubePart part = viewCube.get(left, top, depth);

        glUniformMatrix4fv(worldUniform, 1, false, part.world, 0);

        if (renderToScreen) {
            for (int i = 0; i < cubeSides.length; i++) {
                cubeSides[i] = part.get(CubeSide.fromOrdinal(i));
            }
            glUniform1fv(sidesUniform, cubeSides.length, cubeSides, 0);
        } else {
            glUniform1f(partIndexUniform, CubeCoords.toIndex(
                    viewCube.size, left, top, depth));
        }

        // Draw the triangles
        glDrawArrays(GL_TRIANGLES, 0, SIDE_VERTICES.length / 3);
    }

    /**
     * Determines part coordinates and side at specified screen point (x, y).
     * Works for cubes sizes less or equal to <code>floor(pow(2^16, 1/3)) == 40</code>.
     */
    public PartSideCoords locationAtPixel(int x, int y) {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);
        glReadPixels(x, textureHeight - y, 1, 1,
            GL_RGBA, GL_UNSIGNED_BYTE, readPixelsBuffer.position(0));
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        int r = (int)readPixelsBuffer.get() & 0xFF;
        int g = (int)readPixelsBuffer.get() & 0xFF;
        int b = (int)readPixelsBuffer.get() & 0xFF;
        int a = (int)readPixelsBuffer.get() & 0xFF;
        // part index is stored in (R, G) 8-bit components
        int partIndex = (r << 8) + g;
        int sideNumber = b;
        if (a == EMPTY_SPACE_ALPHA || sideNumber >= 6) { return null; }
        return new PartSideCoords(
            CubeCoords.fromIndex(viewCube.size, partIndex),
            CubeSide.fromOrdinal(sideNumber));
    }
}
