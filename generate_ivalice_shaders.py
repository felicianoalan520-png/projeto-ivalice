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
attribute vec4 mc_midTexCoord;

uniform vec3 sunPosition;

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    color = gl_Color;

    vec2 midCoord = (gl_TextureMatrix[0] * vec4(mc_midTexCoord.xy, 0.0, 1.0)).xy;
    vec2 halfSize = texcoord - midCoord;
    tileBounds.zw = max(abs(halfSize) * 2.0, vec2(1.0e-6));
    tileBounds.xy = min(texcoord, midCoord - halfSize);
    localCoord = sign(halfSize) * 0.5 + 0.5;

    vec4 viewPos = gl_ModelViewMatrix * gl_Vertex;
    viewVector = -viewPos.xyz;

    vec3 n = normalize(gl_NormalMatrix * gl_Normal);
    vec3 t;
    if (length(at_tangent.xyz) > 0.01) {
        t = normalize(gl_NormalMatrix * at_tangent.xyz);
    } else {
        t = normalize(abs(n.y) < 0.99 ? cross(n, vec3(0.0, 1.0, 0.0)) : cross(n, vec3(1.0, 0.0, 0.0)));
    }
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

GBUFFERS_TERRAIN_FSH = """#version 120

/*
 * Ivalice Realism PBR - Terrain Fragment Shader
 * LabPBR 1.3: _n.png (R/G normal, B AO, A height) + _s.png (R smooth, G F0, A emissive)
 * Parallax Occlusion Mapping is computed in sprite-local space and wrapped inside the
 * atlas tile, so it can never leak into other sprites (fixes the "grid / see-through" bug).
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
varying vec4 tileBounds;
varying vec2 localCoord;

#define POM_DEPTH 0.18
#define POM_DISTANCE 24.0

/* DRAWBUFFERS:0 */

vec2 toAtlas(vec2 local) {
    return tileBounds.xy + fract(local) * tileBounds.zw;
}

float heightAt(vec2 local) {
    return texture2D(normals, toAtlas(local)).a;
}

vec2 parallaxLocal(vec2 local, vec3 viewTS, out float outHeight) {
    const int steps = 12;
    float layer = 1.0 / float(steps);
    vec2 delta = -viewTS.xy / max(viewTS.z, 0.25) * POM_DEPTH * layer;

    float depth = 0.0;
    float h = 1.0 - heightAt(local);
    vec2 prevLocal = local;
    float prevH = h;
    for (int i = 0; i < steps; i++) {
        if (depth >= h) break;
        prevLocal = local;
        prevH = h;
        local += delta;
        depth += layer;
        h = 1.0 - heightAt(local);
    }
    float after = h - depth;
    float before = prevH - (depth - layer);
    float w = clamp(after / (after - before + 1.0e-4), 0.0, 1.0);
    outHeight = 1.0 - (depth - w * layer);
    return mix(local, prevLocal, w);
}

float pomShadow(vec2 local, vec3 lightTS, float h0) {
    if (lightTS.z <= 0.0) return 0.55;
    const int steps = 6;
    vec2 delta = lightTS.xy / max(lightTS.z, 0.3) * POM_DEPTH / float(steps);
    float shadow = 1.0;
    for (int i = 1; i <= steps; i++) {
        float rayH = h0 + float(i) / float(steps) * (1.0 - h0);
        if (heightAt(local + delta * float(i)) > rayH + 0.04) {
            shadow = 0.6;
            break;
        }
    }
    return shadow;
}

void main() {
    vec3 V = normalize(viewVector);
    vec3 viewTS = normalize(V * tbnMatrix);

    vec2 local = localCoord;
    float pHeight = 1.0;
    bool usePom = false;

    vec4 nRaw = texture2D(normals, texcoord);
    float dist = length(viewVector);
    if (dist < POM_DISTANCE && nRaw.a > 0.02 && nRaw.a < 0.98) {
        local = parallaxLocal(localCoord, viewTS, pHeight);
        usePom = true;
    }

    // Only remap the atlas lookup when POM actually moved the coordinate;
    // otherwise keep the hardware-filtered original texcoord (no seams, no mip issues).
    vec2 uv = usePom ? toAtlas(local) : texcoord;

    vec4 albedo = texture2D(texture, uv) * color;
    if (albedo.a < 0.1) discard;

    vec4 nSample = usePom ? texture2D(normals, uv) : nRaw;
    vec3 N = normal;
    // Vanilla blocks without a _n.png get Iris' flat default; only apply real maps.
    if (nSample.a > 0.0 && abs(nSample.r - 0.5) + abs(nSample.g - 0.5) > 0.004) {
        vec3 nTS = vec3(nSample.rg * 2.0 - 1.0, 0.0);
        nTS.xy *= 1.4;
        nTS.z = sqrt(max(0.0, 1.0 - dot(nTS.xy, nTS.xy)));
        N = normalize(tbnMatrix * normalize(nTS));
    }
    float ao = clamp(nSample.b, 0.35, 1.0);

    vec3 L = normalize(lightVector);
    float shadow = usePom ? pomShadow(local, normalize(L * tbnMatrix), pHeight) : 1.0;

    vec4 sSample = texture2D(specular, uv);
    float smoothness = sSample.r;
    float f0 = sSample.g;
    float emissive = sSample.a < 0.999 ? sSample.a : 0.0;

    vec4 lm = texture2D(lightmap, lmcoord);
    float NdotL = max(0.0, dot(N, L));
    vec3 H = normalize(L + V);
    float roughness = max(0.05, 1.0 - smoothness);
    float spec = pow(max(0.0, dot(N, H)), min(2.0 / (roughness * roughness), 256.0)) * smoothness * 0.6;

    vec3 specColor = vec3(1.0);
    if (f0 > 0.88) {
        specColor = albedo.rgb;
        albedo.rgb *= 0.35 + 0.65 * (1.0 - smoothness);
    }

    // Keep vanilla brightness as baseline (lightmap), add directional relief on top.
    float sky = lm.b > 0.0 ? 1.0 : 1.0;
    float relief = mix(0.82, 1.12, NdotL) * shadow;
    vec3 finalColor = albedo.rgb * lm.rgb * relief * mix(0.85, 1.0, ao);
    finalColor += spec * specColor * lm.rgb * shadow;
    if (emissive > 0.25) finalColor += albedo.rgb * emissive * 1.6;

    gl_FragData[0] = vec4(finalColor * sky, albedo.a);
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
