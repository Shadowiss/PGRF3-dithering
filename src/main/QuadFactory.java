package main;

import lwjglutils.OGLBuffers;

public class QuadFactory {

    public static OGLBuffers getQuad() {
        //float[] vbData = {-1, -1, 1, -1, 1, 1, -1, 1};
        float[] vbData = {
                -1, -1,0,1,
                1, -1,1,1,
                1, 1,1,0,
                -1, 1,0,0};
        int[] ibData = {0, 1, 2, 0, 2, 3};

        OGLBuffers.Attrib[] attributes = {
                new OGLBuffers.Attrib("inPosition", 2),
                new OGLBuffers.Attrib("inTextureCoord", 2)
        };

        return new OGLBuffers(vbData,4, attributes, ibData);
    }
}
