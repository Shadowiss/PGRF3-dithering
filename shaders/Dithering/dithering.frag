#version 150
uniform sampler2D texture;
uniform sampler2D bayerMatrixTexture;
uniform int mode;
uniform int bayerMatrixMode;
uniform float mouseX;
uniform int th;
uniform float winWidth;
in vec2 textureCoord;
out vec4 outColor;

//https://en.wikipedia.org/wiki/Ordered_dithering
int thresholdMap4[4] = int[]
(0, 2,
3, 1);

int thresholdMap9[9] = int[]
(0, 7, 3,
6, 5, 2,
4, 1, 8);

int thresholdMap16[16] = int[]
(0, 8, 2, 10,
12, 4, 14, 6,
3, 11, 1, 9,
15, 7, 13, 5);

int thresholdMap64[64] = int[]
(0, 48, 12, 60, 3, 51, 15, 63,
32, 16, 44, 28, 35, 19, 47, 31,
8, 56, 4, 52, 11, 59, 7, 55,
40, 24, 36, 20, 43, 27, 39, 23,
2, 50, 24, 62, 1, 49, 13, 61,
34, 18, 46, 30, 33, 17, 45, 29,
10, 58, 6, 54, 9, 57, 5, 53,
42, 26, 38, 22, 41, 25, 37, 21);

float getDitherValue(int bayerMatrixMode){
    vec2 value;
    switch (bayerMatrixMode){
        //https://en.wikipedia.org/wiki/Ordered_dithering
        //http://alex-charlton.com/posts/Dithering_on_the_GPU/
        case 0:
        value = vec2(int(mod(gl_FragCoord.x, 2)), int(mod(gl_FragCoord.y, 2)));
        return thresholdMap4[int(value.x) + int(value.y)*2] * (1 / 4.0);
        case 1:
        value = vec2(int(mod(gl_FragCoord.x, 3)), int(mod(gl_FragCoord.y, 3)));
        return thresholdMap9[int(value.x) + int(value.y)*3] * (1/9.0);
        case 2:
        value = vec2(int(mod(gl_FragCoord.x, 4)), int(mod(gl_FragCoord.y, 4)));
        return thresholdMap16[int(value.x) + int(value.y)*4] *(1/16.0);
        default :
        value = vec2(int(mod(gl_FragCoord.x, 8)), int(mod(gl_FragCoord.y, 8)));
        return thresholdMap64[int(value.x) + int(value.y)*int(8)] * (1/64.0);
    }
}
float tresholdDithering(float color, float treshold) {
    if (color < treshold){
        return 0.0;
    }
    return 1.0;
}
float orderedDithering(float color, int bayerMatrixMode){
    //http://alex-charlton.com/posts/Dithering_on_the_GPU/
    float d = getDitherValue(bayerMatrixMode);
    float closestColor = (color < 0.5) ? 0 : 1;
    float secondClosestColor = 1 - closestColor;
    float distance = abs(closestColor - color);
    return (distance < d) ? closestColor : secondClosestColor;
}
vec3 orderedDitheringTexture(){
    vec2 value = vec2(int(mod(gl_FragCoord.x, 16)), int(mod(gl_FragCoord.y, 16)));
    return texture2D(bayerMatrixTexture, (value / 16)).rgb;
}
float randomValue(vec2 p){
    //https://stackoverflow.com/questions/5149544/can-i-generate-a-random-number-inside-a-pixel-shader
    // e^pi (Gelfond's constant)
    // 2^sqrt(2) (Gelfond–Schneider constant)
    vec2 K1 = vec2(23.14069263277926, 2.665144142690225);
    return fract(cos(dot(p, K1)) * 12345.6789);
}
float randomDithering(float color) {
    if (color < randomValue(vec2(gl_FragCoord.xy)))
    return 0.0;
    return 1.0;
}
float reduceGrayColor(float color, int th){
    float threshold = 1.0/th;
    float center, gainedColor, tempDist;
    float closestColor;
    float distance;

    if (color<threshold)
    return 0;
    if (color> (1.0-threshold))
    {
        return 1;
    }
    //find closest color
    int count=0;
    for (float i = threshold;i<(1.0-threshold);i=i+threshold)
    {
        center = (i+threshold)/ 2;
        gainedColor=i+center;
        tempDist=abs(gainedColor-color);
        if (count==0)
        {
            distance=tempDist;
            closestColor=gainedColor;
        } else
        {
            if (tempDist<distance)
            {
                closestColor=gainedColor;
                distance=tempDist;
            }
        }
        count++;
    }
    return closestColor;
}

void main() {
    // GrayScaleColor -> texture converted to grayscale
    //https://stackoverflow.com/questions/49863351/how-to-convert-image-to-greyscale-opengl-c
    float GrayScaleColor = dot(texture2D(texture, textureCoord).rgb, vec3(1/3.));
    //textureColor -> colored texture image
    vec3 textureColor = texture2D(texture, textureCoord).rgb;
    switch (mode){
        case 0:
        if (gl_FragCoord.x<winWidth || gl_FragCoord.x<mouseX){
            outColor = vec4(textureColor, 1.0);
        }
        else{
            outColor = vec4(GrayScaleColor, GrayScaleColor, GrayScaleColor, 1.0);
        }
        break;
        case 1:
        if (gl_FragCoord.x<winWidth || gl_FragCoord.x<mouseX){
            outColor = vec4(textureColor, 1.0);
        }
        else{
            outColor = vec4(vec3(tresholdDithering(GrayScaleColor, 0.5)), 1.0);
        }
        break;
        case 2:
        if (gl_FragCoord.x<winWidth || gl_FragCoord.x<mouseX){
            outColor = vec4(GrayScaleColor, GrayScaleColor, GrayScaleColor, 1.0);
        }
        else {
            outColor = vec4(vec3(orderedDithering(GrayScaleColor, bayerMatrixMode)), 1.0);
        }
        break;
        case 3:
        if (gl_FragCoord.x<winWidth || gl_FragCoord.x<mouseX){
            outColor = vec4(GrayScaleColor, GrayScaleColor, GrayScaleColor, 1.0);
        }
        else {
            outColor = vec4(vec3(randomDithering(GrayScaleColor)), 1.0);
        }
        break;
        case 4:
        if (gl_FragCoord.x<winWidth || gl_FragCoord.x<mouseX){
            outColor = vec4(textureColor, 1.0);
        }
        else {
            outColor = vec4(vec3(
            orderedDithering(textureColor.r, bayerMatrixMode),
            orderedDithering(textureColor.g, bayerMatrixMode),
            orderedDithering(textureColor.b, bayerMatrixMode)
            ), 1);
        }
        break;
        case 5:
        if (gl_FragCoord.x<winWidth || gl_FragCoord.x<mouseX){
            outColor = vec4(textureColor, 1.0);
        }
        else {
            outColor = vec4(vec3(
            randomDithering(textureColor.r),
            randomDithering(textureColor.g),
            randomDithering(textureColor.b)
            ), 1.0);
        }
        break;
        case 6:
        if (gl_FragCoord.x<winWidth || gl_FragCoord.x<mouseX){
            outColor = vec4(textureColor, 1.0);
        }
        else {
            outColor = vec4(floor(textureColor + (textureColor*vec3(orderedDitheringTexture()))), 1);
        }
        break;
        case 7:
        if (gl_FragCoord.x<winWidth || gl_FragCoord.x<mouseX){
            outColor = vec4(GrayScaleColor, GrayScaleColor, GrayScaleColor, 1.0);
        }
        else {
            outColor=vec4(vec3(reduceGrayColor(GrayScaleColor, th)), 1.0);
        }
        break;
    }
}

