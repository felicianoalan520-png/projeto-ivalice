"""
Projeto Ivalice - Iris & OptiFine PBR Shaderpack Generator
Generates Ivalice-Realism-PBR.zip shaderpack with GLSL #version 120 compatibility profile.
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
]

SHADERS_PROPERTIES = """# Ivalice Realism PBR Shaderpack
# Designed for Iris Shaders & OptiFine (Minecraft 26.3 NeoForge - Projeto Ivalice)
# LabPBR 1.3 Material Pipeline + Warm Ivalice Tonemapping + Parallax Occlusion Mapping

screen.main=MAIN
screen.MAIN.comment=Ivalice Realism PBR Engine Settings
screen.MAIN.columns=2

option.NORMAL_MAP=true
option.SPECULAR_MAP=true
option.POM=true
option.POM_DEPTH=0.095
option.WARM_IVALICE_TONE=true
option.MIST_DENSITY=0.0035

profile.LOW=NORMAL_MAP,SPECULAR_MAP,POM:false
profile.MEDIUM=NORMAL_MAP,SPECULAR_MAP,POM:true
profile.HIGH=NORMAL_MAP,SPECULAR_MAP,POM:true
profile.default=MEDIUM
"""

GBUFFERS_TERRAIN_VSH = """#version 120

/*
 * Ivalice Realism PBR - Terrain Vertex Shader
 * Computes View-space TBN matrix, light vectors, and lightmap coordinates.
 */

varying vec4 color;
varying vec2 texcoord;
varying vec2 lmcoord;
varying vec3 normal;
varying vec3 viewVector;
varying vec3 lightVector;
varying mat3 tbnMatrix;

attribute vec4 at_tangent;

uniform vec3 sunPosition;

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    color = gl_Color;

    vec4 viewPos = gl_ModelViewMatrix * gl_Vertex;
    viewVector = -viewPos.xyz;

    vec3 n = normalize(gl_NormalMatrix * gl_Normal);
    vec3 t;
    if (length(at_tangent.xyz) > 0.01) {
        t = normalize(gl_NormalMatrix * at_tangent.xyz);
    } else {
        t = normalize(abs(n.y) < 0.99 ? cross(n, vec3(0.0, 1.0, 0.0)) : cross(n, vec3(1.0, 0.0, 0.0)));
    }
    vec3 b = cross(t, n) * (at_tangent.w != 0.0 ? at_tangent.w : 1.0);
    tbnMatrix = mat3(t, b, n);
    normal = n;

    lightVector = normalize(sunPosition);
    if (length(lightVector) < 0.1) {
        lightVector = normalize(vec3(0.3, 0.8, 0.5));
    }

    gl_Position = ftransform();
}
"""

GBUFFERS_TERRAIN_FSH = """#version 120

/*
 * Ivalice Realism PBR - Terrain Fragment Shader
 * Full LabPBR 1.3 specification:
 * - Normal map (_n.png): R=nx, G=ny, B=AO, A=height
 * - Specular map (_s.png): R=smoothness, G=F0/metalness, B=porosity, A=emissive
 * - Parallax Occlusion Mapping (POM)
 */

uniform sampler2D texture;
uniform sampler2D normals;
uniform sampler2D specular;
uniform sampler2D lightmap;

varying vec4 color;
varying vec2 texcoord;
varying vec2 lmcoord;
varying vec3 normal;
varying vec3 viewVector;
varying vec3 lightVector;
varying mat3 tbnMatrix;

/* DRAWBUFFERS:0 */

vec2 getParallaxCoord(vec2 uv, vec3 viewDirTS, out float outHeight) {
    float depthScale = 0.095;
    const int numSteps = 12;
    float stepSize = 1.0 / float(numSteps);
    vec2 deltaUV = -viewDirTS.xy * depthScale * stepSize / max(viewDirTS.z, 0.12);

    vec2 currUV = uv;
    vec2 prevUV = uv;
    float currLayerDepth = 0.0;
    float prevLayerDepth = 0.0;
    float currHeight = 1.0 - texture2D(normals, currUV).a;
    float prevHeight = currHeight;

    for (int i = 0; i < numSteps; i++) {
        if (currLayerDepth >= currHeight) {
            float afterDepth = currHeight - currLayerDepth;
            float beforeDepth = prevHeight - prevLayerDepth;
            float weight = afterDepth / (afterDepth - beforeDepth + 0.0001);
            vec2 finalUV = mix(currUV, prevUV, weight);
            outHeight = 1.0 - mix(currLayerDepth, prevLayerDepth, weight);
            return finalUV;
        }
        prevUV = currUV;
        prevHeight = currHeight;
        prevLayerDepth = currLayerDepth;

        currUV += deltaUV;
        currHeight = 1.0 - texture2D(normals, currUV).a;
        currLayerDepth += stepSize;
    }
    outHeight = 1.0 - currLayerDepth;
    return currUV;
}

float computePomShadow(vec2 pcoord, vec3 lightDirTS, float pHeight) {
    if (lightDirTS.z <= 0.0) return 0.25;
    float depthScale = 0.095;
    const int shadowSteps = 6;
    float stepSize = 1.0 / float(shadowSteps);
    vec2 deltaUV = lightDirTS.xy * depthScale * stepSize / max(lightDirTS.z, 0.15);

    float currentLayerDepth = 1.0 - pHeight;
    vec2 currUV = pcoord;
    float shadow = 1.0;

    for (int i = 0; i < shadowSteps; i++) {
        currUV += deltaUV;
        currentLayerDepth -= stepSize;
        if (currentLayerDepth <= 0.0) break;
        float hSample = 1.0 - texture2D(normals, currUV).a;
        if (hSample < currentLayerDepth - 0.03) {
            shadow -= (1.0 - float(i) / float(shadowSteps)) * 0.45;
            break;
        }
    }
    return clamp(shadow, 0.25, 1.0);
}

void main() {
    vec3 V = normalize(viewVector);
    vec3 viewDirTS = normalize(V * tbnMatrix);
    vec2 pcoord = texcoord;
    float pHeight = 1.0;

    vec4 nMapRaw = texture2D(normals, texcoord);
    if (nMapRaw.a > 0.01 && nMapRaw.a < 0.99) {
        pcoord = getParallaxCoord(texcoord, viewDirTS, pHeight);
    } else {
        pHeight = nMapRaw.a;
    }

    vec4 albedo = texture2D(texture, pcoord) * color;
    if (albedo.a < 0.1) {
        discard;
    }

    // Normal & AO from LabPBR _n.png
    vec4 nSample = texture2D(normals, pcoord);
    vec3 N = normal;
    if (length(nSample.rgb) > 0.05) {
        vec3 normalTS = nSample.xyz * 2.0 - 1.0;
        normalTS.xy *= 2.2;
        N = normalize(tbnMatrix * normalize(normalTS));
    }
    float ao = clamp(nSample.b, 0.2, 1.0);

    // Light vectors & Parallax Self-Shadowing
    vec3 L = normalize(lightVector);
    vec3 lightDirTS = normalize(L * tbnMatrix);
    float pomShadow = computePomShadow(pcoord, lightDirTS, pHeight);

    // Specular & Emission from LabPBR _s.png
    vec4 sSample = texture2D(specular, pcoord);
    float smoothness = sSample.r;
    float f0_metal = sSample.g;
    float emissiveRaw = sSample.a;

    vec3 H = normalize(L + V);
    float NdotL = max(0.0, dot(N, L));
    vec4 lm = texture2D(lightmap, lmcoord);
    vec3 ambient = lm.rgb * (0.60 + 0.40 * ao);

    // Crisp microfacet Blinn-Phong specular highlight
    float roughness = max(0.03, 1.0 - smoothness);
    float specPower = 2.0 / (roughness * roughness + 0.0005);
    float NdotH = max(0.0, dot(N, H));
    float specHighlight = pow(NdotH, min(specPower, 512.0)) * smoothness;

    vec3 specColor = vec3(1.0);
    if (f0_metal > 0.88) { // Metallic (> 225 LabPBR)
        specColor = mix(vec3(1.0), albedo.rgb, 0.90);
        albedo.rgb *= 0.12 + 0.88 * (1.0 - smoothness);
    }

    vec3 directLight = vec3(1.20, 1.08, 0.95) * (NdotL * pomShadow * 0.80 + 0.20);
    vec3 finalColor = albedo.rgb * (ambient * 0.65 + directLight * 0.70) + specHighlight * specColor * directLight * pomShadow;

    // Glowing veins / runes / magma emission boost
    if (emissiveRaw > 0.6) {
        vec3 glowColor = albedo.rgb * (2.8 * emissiveRaw);
        finalColor += glowColor;
    }

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

varying vec2 texcoord;

/* DRAWBUFFERS:0 */

float linearizeDepth(float depth) {
    float near = 0.1;
    float far = 256.0;
    return (2.0 * near) / (far + near - depth * (far - near));
}

void main() {
    vec4 scene = texture2D(colortex0, texcoord);
    float depth = texture2D(depthtex0, texcoord).r;
    float linDepth = linearizeDepth(depth);

    vec3 color = scene.rgb;

    // Warm Ivalice tone mapping (Dalmasca golden sunlight)
    vec3 warmColor = color * vec3(1.05, 1.01, 0.96);
    color = (warmColor * (2.51 * warmColor + 0.03)) / (warmColor * (2.43 * warmColor + 0.59) + 0.14);

    // Subtle atmospheric mist
    if (depth < 1.0) {
        float mistFactor = 1.0 - exp(-linDepth * 18.0 * 0.0035);
        mistFactor = clamp(mistFactor, 0.0, 0.45);
        vec3 mistColor = vec3(0.85, 0.80, 0.72);
        color = mix(color, mistColor, mistFactor);
    }

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
    float vignette = 1.0 - dot(coord, coord) * 0.12;
    color.rgb *= clamp(vignette, 0.0, 1.0);

    gl_FragColor = color;
}
"""

README_TEXT = """========================================================================
IVALICE REALISM PBR SHADERPACK (v1.3)
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
   - Iris Shaders & OptiFine on OpenGL 2.1+ / GLSL #version 120.
   - Fully optimized for NeoForge 26.3.

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

GBUFFERS_WATER_FSH = """#version 120

/*
 * Ivalice Realism PBR - Translucent / Water / Stained Glass Fragment Shader
 * Adds Fresnel reflections and solar specular glints on water and Archadian stained glass.
 */

uniform sampler2D texture;
uniform sampler2D lightmap;

varying vec4 color;
varying vec2 texcoord;
varying vec2 lmcoord;
varying vec3 normal;
varying vec3 viewVector;
varying vec3 lightVector;

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
    float sunSpec = pow(max(0.0, dot(N, H)), 96.0) * 0.65;

    vec4 lm = texture2D(lightmap, lmcoord);
    vec3 finalRgb = albedo.rgb * lm.rgb + vec3(1.25, 1.15, 0.98) * (sunSpec + fresnel * 0.35);
    float finalAlpha = clamp(albedo.a + fresnel * 0.25 + sunSpec * 0.35, 0.0, 1.0);

    gl_FragData[0] = vec4(finalRgb, finalAlpha);
}
"""

FILES = {
    "shaders/shaders.properties": SHADERS_PROPERTIES,
    "shaders/gbuffers_terrain.vsh": GBUFFERS_TERRAIN_VSH,
    "shaders/gbuffers_terrain.fsh": GBUFFERS_TERRAIN_FSH,
    "shaders/gbuffers_entities.vsh": GBUFFERS_ENTITIES_VSH,
    "shaders/gbuffers_entities.fsh": GBUFFERS_ENTITIES_FSH,
    "shaders/gbuffers_hand.vsh": GBUFFERS_ENTITIES_VSH,
    "shaders/gbuffers_hand.fsh": GBUFFERS_ENTITIES_FSH,
    "shaders/gbuffers_water.vsh": GBUFFERS_ENTITIES_VSH,
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
            zf.writestr(filename, content.encode("utf-8"))
    return buf.getvalue()


def main():
    zip_bytes = build_shaderpack_zip()
    for dest in DEST_PATHS:
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(zip_bytes)
        print(f"Created shaderpack: {dest} ({len(zip_bytes)} bytes)")


if __name__ == "__main__":
    main()
