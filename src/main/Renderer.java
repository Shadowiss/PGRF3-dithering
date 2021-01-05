package main;

import lwjglutils.*;
import org.lwjgl.glfw.*;
import transforms.*;

import java.awt.*;
import java.io.IOException;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

public class Renderer extends AbstractRenderer {

    double mouseX, mouseY;
    int width = 800;
    int height = 800;

    OGLBuffers quad;
    OGLTexture2D texture, bayerMatrixTexture, tempTexture;
    int ditheringMethod = 0;
    int bayerMatrixMode = 0;
    int threshold = 256;
    boolean loadImage;

    int shaderProgramDither, locBayerMatrixMode, locMouseX, locThreshold, winWidth, locMode;

    Mat4 proj = new Mat4PerspRH(Math.PI / 4, 1, 0.1, 100.0);

    private final GLFWKeyCallback keyCallback = new GLFWKeyCallback() {
        @Override
        public void invoke(long window, int key, int scancode, int action, int mods) {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
            if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                switch (key) {
                    case GLFW_KEY_1 -> {
                        threshold = 0;
                        bayerMatrixMode = 0;
                        if (ditheringMethod == 7) {
                            ditheringMethod = 0;
                        } else {
                            ditheringMethod++;
                        }
                    }
                    case GLFW_KEY_2 -> {
                        if (ditheringMethod == 2 || ditheringMethod == 4) {
                            if (bayerMatrixMode == 3) {
                                bayerMatrixMode = 0;
                            } else {
                                bayerMatrixMode++;
                            }
                        }
                        if (ditheringMethod == 7) {
                            if (threshold >= 256) {
                                threshold = 2;
                            } else {
                                if (threshold == 0) {
                                    threshold = 2;
                                } else {
                                    threshold = threshold * 2;
                                }
                            }
                        }
                    }
                    case GLFW_KEY_N -> loadImage = !loadImage;
                }
            }
        }
    };

    private final GLFWWindowSizeCallback wsCallback = new GLFWWindowSizeCallback() {
        @Override
        public void invoke(long window, int w, int h) {
            if (w > 0 && h > 0 &&
                    (w != width || h != height)) {
                width = w;
                height = h;
                proj = new Mat4PerspRH(Math.PI / 4, height / (double) width, 0.1, 100.0);
                if (textRenderer != null)
                    textRenderer.resize(width, height);
            }
        }
    };

    private final GLFWCursorPosCallback cpCallback = new GLFWCursorPosCallback() {
        @Override
        public void invoke(long window, double x, double y) {
            mouseX = x;
            mouseY = y;
        }
    };

    @Override
    public GLFWKeyCallback getKeyCallback() {
        return keyCallback;
    }

    @Override
    public GLFWWindowSizeCallback getWsCallback() {
        return wsCallback;
    }

    @Override
    public GLFWCursorPosCallback getCursorCallback() {
        return cpCallback;
    }

    @Override
    public void init() {
        OGLUtils.printOGLparameters();
        OGLUtils.printLWJLparameters();
        OGLUtils.printJAVAparameters();
        glClearColor(0.8f, 0.8f, 0.8f, 1.0f);
        glUseProgram(this.shaderProgramDither);
        textRenderer = new OGLTextRenderer(width, height);
        quad = QuadFactory.getQuad();
        shaderProgramDither = ShaderUtils.loadProgram("/Dithering/dithering.vert",
                "/Dithering/dithering.frag",
                null, null, null, null);

        try {
            texture = new OGLTexture2D("textures/carnifex.jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            bayerMatrixTexture = new OGLTexture2D("textures/BayerMatrix.png");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void display() {
        String method = switch (ditheringMethod) {
            case 0 -> " 1.Color / grayscale ";
            case 1 -> " 2.Threshold halftone ";
            case 2 -> " 3.Ordered dithering ";
            case 3 -> " 4.Random dithering ";
            case 4 -> " 5.Ordered dithering color ";
            case 5 -> " 6.Random dithering color ";
            case 6 -> " 7.Ordered dithering color - BayerTexture[16] ";
            case 7 -> " 8.Grayscale ";
            default -> "default ";
        };
        String text1 = " [1] Change dithering methods ";
        String text2 = " [2] Change threshold map or divisor ";
        String text3 = " Current method: " + method;
        String text4 = " Current threshold map / divisor: ";
        String text5 = " [N] upload custom image ";
        if (ditheringMethod == 2 || ditheringMethod == 4) {
            String thresholdMap = switch (bayerMatrixMode) {
                case 0 -> " thresholdMap[4] ";
                case 1 -> " thresholdMap[9] ";
                case 2 -> " thresholdMap[16] ";
                case 3 -> " thresholdMap[64] ";
                default -> "default";
            };
            text4 = text4 + thresholdMap;

        }
        if (ditheringMethod == 7) {
            String divideThreshold = " Divisor: " + threshold;
            text4 = text4 + divideThreshold;
        }

        glEnable(GL_DEPTH_TEST);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

        locMode = glGetUniformLocation(shaderProgramDither, "mode");
        locBayerMatrixMode = glGetUniformLocation(shaderProgramDither, "bayerMatrixMode");
        locMouseX = glGetUniformLocation(shaderProgramDither, "mouseX");
        locThreshold = glGetUniformLocation(shaderProgramDither, "th");
        winWidth = glGetUniformLocation(shaderProgramDither, "winWidth");

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, width, height);
        glClearColor(0.5f, 0.1f, 0.1f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glUseProgram(shaderProgramDither);
        texture.bind(shaderProgramDither, "texture", 0);
        bayerMatrixTexture.bind(shaderProgramDither, "bayerMatrixTexture", 1);

        glUniform1i(locMode, ditheringMethod);
        glUniform1i(locBayerMatrixMode, bayerMatrixMode);
        glUniform1f(locMouseX, (float) (mouseX));
        glUniform1f(winWidth, (float) width / 2);
        glUniform1i(locThreshold, threshold);

        quad.draw(GL_TRIANGLES, shaderProgramDither);

        if (loadImage) {
            tempTexture = FileChooser.loadTexture();
            if (tempTexture != null) {
                texture = tempTexture;
            }
            loadImage = false;
        }
        textRenderer.setBackgroundColor(new Color(255, 255, 255));
        textRenderer.setColor(new Color(0, 0, 0));
        textRenderer.addStr2D(3, 20, text1);
        textRenderer.addStr2D(3, 40, text3);
        if (ditheringMethod == 2 || ditheringMethod == 4 || ditheringMethod == 7) {
            textRenderer.addStr2D(3, 60, text2);
        }
        if (ditheringMethod == 2 || ditheringMethod == 4 || ditheringMethod == 7) {
            textRenderer.addStr2D(3, 80, text4);
        }
        textRenderer.addStr2D(3, height - 3, text5);
        textRenderer.addStr2D(width - 150, height - 3, " (c) PGRF UHK Jirásek Jiří ");
    }

}