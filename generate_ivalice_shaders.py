"""
Projeto Ivalice - Iris & OptiFine PBR Shaderpack Generator
Generates Ivalice-Realism-PBR.zip for the Iris and OptiFine compatibility profile.
Supports LabPBR 1.3 normal maps (_n.png) with POM, specular maps (_s.png) with metallic/smoothness,
warm Dalmascan tone mapping, and atmospheric mist.
"""
import io
from pathlib import Path
import zipfile

BASE_DIR = Path(__file__).resolve().parent
DEST_PATHS = [
    BASE_DIR / "shaderpacks/Ivalice-Realism-PBR.zip",
    BASE_DIR / "run/shaderpacks/Ivalice-Realism-PBR.zip",
    BASE_DIR / "instancia_pronta/shaderpacks/Ivalice-Realism-PBR.zip",
]

SHADERS_PROPERTIES = """# Ivalice Realism PBR Shaderpack
# Designed for Iris Shaders & OptiFine (Minecraft 26.3 NeoForge - Projeto Ivalice)
# LabPBR 1.3 Material Pipeline + Warm Ivalice Tonemapping + Parallax Occlusion Mapping

screen = IVALICE_PARALLAX POM_DEPTH
screen.columns = 2
sliders = POM_DEPTH
"""

SKY_VSH = """#version 120
varying vec4 skyTint;
varying vec3 skyDirection;
uniform mat4 gbufferModelViewInverse;

void main() {
    skyTint = gl_Color;
    skyDirection = mat3(gbufferModelViewInverse) * (gl_ModelViewMatrix * gl_Vertex).xyz;
    gl_Position = ftransform();
}
"""

SKY_FSH = """#version 120
varying vec4 skyTint;
varying vec3 skyDirection;
uniform vec3 fogColor;
uniform vec3 skyColor;
uniform int renderStage;

/* DRAWBUFFERS:0 */

void main() {
    if (renderStage == MC_RENDER_STAGE_SKY || renderStage == MC_RENDER_STAGE_VOID) {
        float elevation = normalize(skyDirection).y;
        float height = smoothstep(-0.06, 0.42, elevation);
        vec3 upperSky = max(skyColor, fogColor * 0.35);
        gl_FragData[0] = vec4(mix(fogColor, upperSky, height), 1.0);
    } else {
        gl_FragData[0] = skyTint;
    }
}
"""

SKY_TEXTURED_VSH = """#version 120
varying vec4 skyTint;
varying vec2 uv;
void main() {
    skyTint = gl_Color;
    uv = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    gl_Position = ftransform();
}
"""

SKY_TEXTURED_FSH = """#version 120
uniform sampler2D gtexture;
varying vec4 skyTint;
varying vec2 uv;
/* DRAWBUFFERS:0 */
void main() {
    gl_FragData[0] = texture2D(gtexture, uv) * skyTint;
}
"""

GBUFFERS_TERRAIN_VSH = """#version 130

/*
 * Ivalice Realism PBR - Terrain Vertex Shader
 * Computes view-space TBN, light vector and the atlas tile bounds (mc_midTexCoord)
 * so Parallax Occlusion Mapping never samples neighbouring atlas sprites.
 */

varying vec4 color;
varying vec2 texcoord;
varying vec2 lmcoord;
varying vec3 normal;
varying vec3 viewVector;
varying vec3 lightVector;
varying mat3 tbnMatrix;
varying vec4 tileBounds;   // xy = tile origin in atlas, zw = tile size in atlas
varying vec2 localCoord;   // 0..1 inside the sprite

attribute vec4 at_tangent;
attribute vec2 mc_midTexCoord;

uniform vec3 sunPosition;

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    color = gl_Color;

    vec2 midCoord = (gl_TextureMatrix[0] * vec4(mc_midTexCoord, 0.0, 1.0)).xy;
    vec2 halfSize = texcoord - midCoord;
    tileBounds.zw = max(abs(halfSize) * 2.0, vec2(1.0e-6));
    tileBounds.xy = min(texcoord, midCoord - halfSize);
    localCoord = sign(halfSize) * 0.5 + 0.5;

    vec4 viewPos = gl_ModelViewMatrix * gl_Vertex;
    viewVector = -viewPos.xyz;

    vec3 n = normalize(gl_NormalMatrix * gl_Normal);
    if (length(n) < 0.1) n = vec3(0.0, 1.0, 0.0);
    vec3 t;
    if (length(at_tangent.xyz) > 0.01) {
        t = normalize(gl_NormalMatrix * at_tangent.xyz);
    } else {
        t = normalize(abs(n.y) < 0.99 ? cross(n, vec3(0.0, 1.0, 0.0)) : cross(n, vec3(1.0, 0.0, 0.0)));
    }
    t = normalize(t - dot(t, n) * n);
    vec3 b = cross(t, n) * (at_tangent.w < 0.0 ? -1.0 : 1.0);
    tbnMatrix = mat3(t, b, n);
    normal = n;

    lightVector = normalize(sunPosition);
    if (length(sunPosition) < 0.1) {
        lightVector = normalize(vec3(0.3, 0.8, 0.5));
    }

    gl_Position = ftransform();
}
"""

GBUFFERS_TERRAIN_FSH = """#version 130

/*
 * Ivalice Realism PBR - Terrain Fragment Shader
 * LabPBR 1.3: _n.png (R/G normal, B AO, A height) + _s.png (R smooth, G F0, A emissive)
 * The parallax ray is confined to the original quad's atlas footprint. Coverage is
 * always read from the original atlas UV, including cutouts and vegetation.
 */

uniform sampler2D gtexture;
uniform sampler2D normals;
uniform sampler2D specular;
uniform sampler2D lightmap;
uniform float alphaTestRef;
uniform vec3 shadowLightPosition;
uniform vec3 sunPosition;
uniform float rainStrength;

varying vec4 color;
varying vec2 texcoord;
varying vec2 lmcoord;
varying vec3 normal;
varying vec3 viewVector;
varying vec3 lightVector;
varying mat3 tbnMatrix;
varying vec4 tileBounds;
varying vec2 localCoord;

#define IVALICE_PARALLAX 0 // [0 1]
#define POM_DEPTH 0.06 // [0.03 0.06 0.09]

/* DRAWBUFFERS:0 */

#if IVALICE_PARALLAX == 1
vec2 toAtlas(vec2 local, vec2 inset) {
    return tileBounds.xy + clamp(local, inset, 1.0 - inset) * tileBounds.zw;
}

float heightAt(vec2 local, vec2 inset, vec2 uvDx, vec2 uvDy) {
    return textureGrad(normals, toAtlas(local, inset), uvDx, uvDy).a;
}

vec2 parallaxLocal(vec2 local, vec3 viewTS, float fade, vec2 inset, vec2 uvDx, vec2 uvDy) {
    const int steps = 12;
    float layer = 1.0 / float(steps);
    vec2 delta = -viewTS.xy / max(viewTS.z, 0.35) * POM_DEPTH * fade * layer;

    float depth = 0.0;
    float surface = 1.0 - heightAt(local, inset, uvDx, uvDy);
    vec2 previous = local;
    float previousGap = 0.0;
    for (int i = 0; i < steps; i++) {
        if (depth >= surface) break;
        previous = local;
        previousGap = surface - depth;
        local += delta;
        depth += layer;
        surface = 1.0 - heightAt(local, inset, uvDx, uvDy);
    }
    float overshoot = max(0.0, depth - surface);
    float blend = overshoot / max(overshoot + previousGap, 1.0e-4);
    return mix(local, previous, blend);
}
#endif

void main() {
    vec4 baseColor = texture2D(gtexture, texcoord);
    float coverage = baseColor.a * color.a;
    if (coverage < max(0.1, alphaTestRef)) discard;

    vec4 nSample = texture2D(normals, texcoord);
    vec4 sSample = texture2D(specular, texcoord);
    vec3 V = normalize(viewVector);
    vec3 L = length(shadowLightPosition) > 0.1 ? normalize(shadowLightPosition) : normalize(lightVector);
    float sunFacing = dot(L, normalize(lightVector));
    float litSky = smoothstep(0.55, 0.95, lmcoord.y);
    float directLight = litSky * mix(0.18, 1.0, smoothstep(-0.1, 0.5, sunFacing)) * (1.0 - rainStrength * 0.65);

    #if IVALICE_PARALLAX == 1
    vec2 uvDx = dFdx(texcoord);
    vec2 uvDy = dFdy(texcoord);
    vec2 atlasPixels = vec2(textureSize(gtexture, 0));
    vec2 quadPixels = tileBounds.zw * atlasPixels;
    vec4 drift = (abs(dFdx(tileBounds)) + abs(dFdy(tileBounds))) * atlasPixels.xyxy;
    vec2 edge = min(localCoord, 1.0 - localCoord);
    vec3 viewTS = normalize(V * tbnMatrix);
    float fade = (1.0 - smoothstep(12.0, 20.0, length(viewVector))) *
                 smoothstep(0.04, 0.16, min(edge.x, edge.y)) *
                 smoothstep(0.35, 0.60, viewTS.z);
    if (fade > 0.0 && coverage > 0.99 && nSample.a < 0.995 &&
            min(quadPixels.x, quadPixels.y) >= 8.0 &&
            max(max(drift.x, drift.y), max(drift.z, drift.w)) < 0.10) {
        vec2 mipFootprint = max(abs(uvDx), abs(uvDy)) * atlasPixels;
        vec2 inset = clamp((0.5 + mipFootprint) / quadPixels, vec2(0.02), vec2(0.4));
        vec2 local = parallaxLocal(localCoord, viewTS, fade, inset, uvDx, uvDy);
        vec2 uv = toAtlas(local, inset);
        vec4 displaced = textureGrad(gtexture, uv, uvDx, uvDy);
        if (displaced.a > 0.99) {
            baseColor.rgb = displaced.rgb;
            nSample = textureGrad(normals, uv, uvDx, uvDy);
            sSample = textureGrad(specular, uv, uvDx, uvDy);
        }
    }
    #endif

    vec4 albedo = vec4(baseColor.rgb * color.rgb, coverage);
    vec3 N = normalize(normal);
    float distanceFade = 1.0 - smoothstep(14.0, 54.0, length(viewVector));
    vec2 normalXY = nSample.rg * 2.0 - 1.0;
    vec3 nTS = vec3(normalXY, sqrt(max(0.0, 1.0 - dot(normalXY, normalXY))));
    N = normalize(mix(N, normalize(tbnMatrix * nTS), 0.48 * distanceFade));
    float ao = clamp(nSample.b, 0.45, 1.0);
    float smoothness = clamp(sSample.r, 0.0, 0.9);
    float f0 = sSample.g < 0.9 ? min(sSample.g, 0.16) : 0.28;
    float emissive = sSample.a < 0.999 ? sSample.a : 0.0;

    vec4 lm = texture2D(lightmap, lmcoord);
    float NdotL = max(0.0, dot(N, L));
    vec3 H = normalize(L + V);
    float roughness = max(0.28, 1.0 - smoothness);
    float spec = pow(max(0.0, dot(N, H)), min(2.0 / (roughness * roughness), 40.0));
    spec *= NdotL * smoothness * f0 * directLight * distanceFade;

    // Preserve the vanilla lightmap and apply material contrast at close range.
    float relief = mix(1.0, mix(0.89, 1.06, NdotL), directLight);
    vec3 finalColor = albedo.rgb * lm.rgb * relief * mix(1.0, ao, 0.25 * distanceFade);
    finalColor += spec * mix(vec3(1.0), albedo.rgb, step(0.9, sSample.g)) * lm.rgb;
    finalColor += albedo.rgb * emissive * 0.42;

    gl_FragData[0] = vec4(finalColor, albedo.a);
}
"""

COMPOSITE_VSH = """#version 120

/*
 * Ivalice Realism PBR - Composite Vertex Shader
 */

varying vec2 texcoord;

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    gl_Position = ftransform();
}
"""

COMPOSITE_FSH = """#version 120

/*
 * Ivalice Realism PBR - Composite Fragment Shader
 * Warm Dalmascan Golden Tone Mapping & Subtle Atmospheric Mist
 */

uniform sampler2D colortex0;
uniform sampler2D depthtex0;
uniform mat4 gbufferProjectionInverse;
uniform vec3 fogColor;
uniform float fogStart;
uniform float fogEnd;
uniform float far;
uniform int fogShape;
uniform int isEyeInWater;

varying vec2 texcoord;

/* DRAWBUFFERS:0 */

void main() {
    vec4 scene = texture2D(colortex0, texcoord);
    float depth = texture2D(depthtex0, texcoord).r;
    vec3 color = scene.rgb;

    if (depth < 0.999999) {
        vec4 projected = gbufferProjectionInverse * vec4(texcoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
        vec3 view = projected.xyz / max(0.00001, abs(projected.w));
        float distance = fogShape == 1 ? max(length(view.xz), abs(view.y)) : length(view);
        float endDistance = fogEnd > 1.0 ? fogEnd : far;
        float startDistance = isEyeInWater == 0 ? min(fogStart, endDistance * 0.68) : fogStart;
        float fog = smoothstep(max(0.0, startDistance), max(startDistance + 1.0, endDistance), distance);
        color = mix(color, fogColor, fog);
    }

    // A restrained grade keeps night and dusk from crushing to black.
    color = clamp(color * vec3(1.035, 1.005, 0.98), 0.0, 1.0);
    gl_FragData[0] = vec4(color, scene.a);
}
"""

FINAL_VSH = """#version 120

/*
 * Ivalice Realism PBR - Final Vertex Shader
 */

varying vec2 texcoord;

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    gl_Position = ftransform();
}
"""

FINAL_FSH = """#version 120

/*
 * Ivalice Realism PBR - Final Fragment Shader
 * Subtle filmic vignette and output to display
 */

uniform sampler2D colortex0;

varying vec2 texcoord;

void main() {
    vec4 color = texture2D(colortex0, texcoord);

    vec2 coord = (texcoord - 0.5) * 2.0;
    float vignette = 1.0 - dot(coord, coord) * 0.045;
    color.rgb *= clamp(vignette, 0.0, 1.0);

    gl_FragColor = color;
}
"""

README_TEXT = """========================================================================
IVALICE REALISM PBR SHADERPACK (v1.4)
Projeto Ivalice - Final Fantasy XII Minecraft 26.3 NeoForge
========================================================================

FEATURES:
1. Full LabPBR 1.3 Material Standard Support:
   - Normal Maps (_n.png):
     * R: Tangent Normal X
     * G: Tangent Normal Y
     * B: Cavity / Ambient Occlusion
     * A: Heightmap for Parallax Occlusion Mapping (POM)
   - Specular Maps (_s.png):
     * R: Perceptual Smoothness (Roughness = 1.0 - Smoothness)
     * G: F0 Reflectance / Metallic (> 225 indicates metallic conductive surface)
     * B: Porosity / Subsurface Scattering
     * A: Emissive veins (Amber lantern, Sunstone ore, Feywood runes, Scorched magma)

2. Atmosphere & Aesthetics:
   - Warm Dalmascan Solar Tone Mapping: Inspired by FFXII's golden desert sunlight.
   - Micro-relief depth and crisp surface specular reflections on marble, steel, gold, and rime ice.
   - Atmospheric regional haze/mist.

3. Compatibility:
   - Iris Shaders on NeoForge 26.3.
   - Parallax is off by default. Enable it in Shader Pack Settings.

INSTALLATION:
Place 'Ivalice-Realism-PBR.zip' into your '.minecraft/shaderpacks' directory
and activate it via Iris Video Settings -> Shader Packs.
"""

GBUFFERS_ENTITIES_VSH = """#version 120

/*
 * Ivalice Realism PBR - Entities / Hand / Armor Vertex Shader
 * Enhances 3D GeckoLib entities and held items with directional rim lighting and specular sheen.
 */

varying vec4 color;
varying vec2 texcoord;
varying vec2 lmcoord;
varying vec3 normal;
varying vec3 viewVector;
varying vec3 lightVector;

uniform vec3 sunPosition;

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    color = gl_Color;

    vec4 viewPos = gl_ModelViewMatrix * gl_Vertex;
    viewVector = -viewPos.xyz;
    normal = normalize(gl_NormalMatrix * gl_Normal);

    lightVector = normalize(sunPosition);
    if (length(lightVector) < 0.1) {
        lightVector = normalize(vec3(0.3, 0.8, 0.5));
    }

    gl_Position = ftransform();
}
"""

GBUFFERS_ENTITIES_FSH = """#version 120

/*
 * Ivalice Realism PBR - Entities / Hand / Armor Fragment Shader
 * Adds directional shading, rim light, and crisp specular highlights to 3D GeckoLib creatures & weapons.
 */

uniform sampler2D texture;
uniform sampler2D lightmap;
uniform vec4 entityColor;

varying vec4 color;
varying vec2 texcoord;
varying vec2 lmcoord;
varying vec3 normal;
varying vec3 viewVector;
varying vec3 lightVector;

/* DRAWBUFFERS:0 */

void main() {
    vec4 albedo = texture2D(texture, texcoord) * color;
    if (albedo.a < 0.1) {
        discard;
    }
    albedo.rgb = mix(albedo.rgb, entityColor.rgb, entityColor.a);

    vec3 N = normalize(normal);
    vec3 V = normalize(viewVector);
    vec3 L = normalize(lightVector);
    vec3 H = normalize(L + V);

    float NdotL = max(0.0, dot(N, L));
    float NdotV = max(0.0, dot(N, V));
    float rim = pow(1.0 - NdotV, 3.2) * 0.28;
    float spec = pow(max(0.0, dot(N, H)), 36.0) * 0.22;

    vec4 lm = texture2D(lightmap, lmcoord);
    vec3 lighting = lm.rgb * (0.58 + 0.48 * NdotL) + vec3(1.15, 1.04, 0.90) * (spec + rim);

    gl_FragData[0] = vec4(albedo.rgb * lighting, albedo.a);
}
"""

GBUFFERS_WATER_VSH = GBUFFERS_ENTITIES_VSH.replace(
    "#version 120",
    "#version 120\nattribute vec2 mc_Entity;\nvarying float waterBlock;",
    1,
).replace("void main() {", "void main() {\n    waterBlock = float(mc_Entity.x == 10000.0);", 1)

GBUFFERS_WATER_FSH = """#version 120

/*
 * Ivalice Realism PBR - Translucent / Water / Stained Glass Fragment Shader
 * Adds Fresnel reflections and solar specular glints on water and Archadian stained glass.
 */

uniform sampler2D texture;
uniform sampler2D lightmap;
uniform vec3 fogColor;
uniform float sunAngle;

varying vec4 color;
varying vec2 texcoord;
varying vec2 lmcoord;
varying vec3 normal;
varying vec3 viewVector;
varying vec3 lightVector;
varying float waterBlock;

/* DRAWBUFFERS:0 */

void main() {
    vec4 albedo = texture2D(texture, texcoord) * color;
    if (albedo.a < 0.05) {
        discard;
    }

    vec3 N = normalize(normal);
    vec3 V = normalize(viewVector);
    vec3 L = normalize(lightVector);
    vec3 H = normalize(L + V);

    float NdotV = max(0.0, dot(N, V));
    float fresnel = pow(1.0 - NdotV, 4.0) * 0.45;
    float sunSpec = pow(max(0.0, dot(N, H)), 48.0) * 0.15 * smoothstep(0.6, 0.97, lmcoord.y);

    vec4 lm = texture2D(lightmap, lmcoord);
    vec3 finalRgb = albedo.rgb * lm.rgb + vec3(1.25, 1.15, 0.98) * (sunSpec + fresnel * 0.35);
    float finalAlpha = clamp(albedo.a + fresnel * 0.36 + sunSpec * 0.2, 0.0, 1.0);
    if (waterBlock > 0.5) {
        float daylight = smoothstep(0.0, 0.08, sunAngle) * (1.0 - smoothstep(0.42, 0.50, sunAngle));
        vec3 waterTint = mix(color.rgb * vec3(0.53, 0.67, 0.79), albedo.rgb, 0.32);
        finalRgb = mix(waterTint * lm.rgb, fogColor, fresnel * 0.56);
        finalRgb += vec3(1.0, 0.94, 0.81) * sunSpec * daylight * 0.65;
        finalAlpha = clamp(max(0.42, albedo.a) + fresnel * 0.69, 0.0, 0.90);
    }

    gl_FragData[0] = vec4(finalRgb, finalAlpha);
}
"""

FILES = {
    "shaders/shaders.properties": SHADERS_PROPERTIES,
    "shaders/lang/en_us.lang": "option.IVALICE_PARALLAX=Parallax relief\noption.POM_DEPTH=Parallax depth\n",
    "shaders/lang/pt_br.lang": "option.IVALICE_PARALLAX=Relevo por paralaxe\noption.POM_DEPTH=Profundidade do relevo\n",
    "shaders/gbuffers_skybasic.vsh": SKY_VSH,
    "shaders/gbuffers_skybasic.fsh": SKY_FSH,
    "shaders/gbuffers_skytextured.vsh": SKY_TEXTURED_VSH,
    "shaders/gbuffers_skytextured.fsh": SKY_TEXTURED_FSH,
    "shaders/gbuffers_terrain.vsh": GBUFFERS_TERRAIN_VSH,
    "shaders/gbuffers_terrain.fsh": GBUFFERS_TERRAIN_FSH,
    "shaders/gbuffers_entities.vsh": GBUFFERS_ENTITIES_VSH,
    "shaders/gbuffers_entities.fsh": GBUFFERS_ENTITIES_FSH,
    "shaders/gbuffers_hand.vsh": GBUFFERS_ENTITIES_VSH,
    "shaders/gbuffers_hand.fsh": GBUFFERS_ENTITIES_FSH,
    "shaders/block.properties": "block.10000=minecraft:water minecraft:bubble_column\n",
    "shaders/gbuffers_water.vsh": GBUFFERS_WATER_VSH,
    "shaders/gbuffers_water.fsh": GBUFFERS_WATER_FSH,
    "shaders/composite.vsh": COMPOSITE_VSH,
    "shaders/composite.fsh": COMPOSITE_FSH,
    "shaders/final.vsh": FINAL_VSH,
    "shaders/final.fsh": FINAL_FSH,
    "README-IVALICE-SHADERS.txt": README_TEXT,
}


def build_shaderpack_zip():
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        for filename, content in FILES.items():
            entry = zipfile.ZipInfo(filename)
            zf.writestr(entry, content.encode("utf-8"), compress_type=zipfile.ZIP_DEFLATED)
    return buf.getvalue()


def main():
    zip_bytes = build_shaderpack_zip()
    for dest in DEST_PATHS:
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(zip_bytes)
        print(f"Created shaderpack: {dest} ({len(zip_bytes)} bytes)")


if __name__ == "__main__":
    main()
