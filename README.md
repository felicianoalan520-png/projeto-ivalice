# ⚔️ Projeto Ivalice — Final Fantasy XII: The Zodiac Age (+ FFIV, FFV, FFVI & FFXV)

[![Minecraft](https://img.shields.io/badge/Minecraft_Java-26.3-22c55e?style=for-the-badge)](https://neoforged.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-26.3.0.101-ea580c?style=for-the-badge)](https://neoforged.net/)
[![Java](https://img.shields.io/badge/Java-25-38bdf8?style=for-the-badge)](https://adoptium.net/)
[![GeckoLib](https://img.shields.io/badge/GeckoLib_3D-5.5.7-a855f7?style=for-the-badge)](https://github.com/bernie-g/geckolib)
[![Shaders](https://img.shields.io/badge/Iris_LabPBR_1.3-32x32_HD_POM-eab308?style=for-the-badge)](#shaders-pbr-e-texturas-32x32-hd)

Adaptação jogável do universo de **Final Fantasy XII: The Zodiac Age** no **Minecraft Java 26.3 (NeoForge)** — expandida com criaturas e chefes interdimensionais de **Final Fantasy IV, V, VI e XV**, sistema de combate por turnos com animações dinâmicas e destruição/transformação elemental de cenário, progressão do **Nível 1 ao 99**, **12 Profissões do Zodíaco**, **Alquimia & Bazaar**, texturas **32x32 HD com mapas LabPBR (`_n.png` / `_s.png`)** e o shaderpack incluso **`Ivalice-Realism-PBR.zip`**.

---

## ✨ Destaques Principais

### 1. 🌍 15 Biomas Canônicos de Ivalice & Assentamentos Regionais
Gerados naturalmente no Overworld via **TerraBlender** (`IvaliceBiomes` & `IvaliceOverworldRegion`):
1. `ivalice:dalmasca_estersand` — *Dalmasca Estersand* (Desertos rochosos, ravinas áridas, Areia/Arenito de Dalmasca, Entreposto de Estersand)
2. `ivalice:dalmasca_westersand` — *Dalmasca Westersand* (Grandes dunas varridas pelo vento, tempestades de poeira)
3. `ivalice:giza_plains_dry` — *Giza Plains (The Dry)* (Planícies douradas ensolaradas, depósitos de *Sunstone Ore*, *Wild Gysahl Greens*, Aldeia de Giza)
4. `ivalice:giza_plains_rains` — *Giza Plains (The Rains)* (Planícies alagadas sob chuva contínua — alternância sazonal automática via `GizaSeasonManager`)
5. `ivalice:ozmone_plain` — *Ozmone Plain* (Savana verde-esmeralda com Chocobos selvagens e *Gysahl Greens*)
6. `ivalice:golmore_jungle` — *Golmore Jungle* (Selva densa e sombreada de *Golmore Darkwood*, *Mossy Stone* e esporos místicos)
7. `ivalice:phon_coast` — *Phon Coast* (Costa tropical de águas turquesa, praias claras e lírios costeiros)
8. `ivalice:tchita_uplands` — *Tchita Uplands* (Planaltos elevados varridos pelo vento)
9. `ivalice:paramina_rift` — *Paramina Rift* (Desfiladeiros congelados sobre *Paramina Permafrost* e *Glacier Rime Stone*)
10. `ivalice:salikawood` — *The Salikawood* (Floresta antiga de copas âmbar/douradas)
11. `ivalice:feywood` — *The Feywood* (Floresta branca mística imersa em *Mist*, *Feywood Runed Marble* e *White Lilies*)
12. `ivalice:mosphoran_highwaste` — *Mosphoran Highwaste* (Penhascos montanhosos de *Mosphoran Slate* e fontes termais)
13. `ivalice:nabreus_deadlands` — *Nabreus Deadlands* (Pântanos sombrios sobre *Nabreus Mire* e ruínas ancestrais)
14. `ivalice:ogir_yensa_sandsea` — *Ogir-Yensa Sandsea* (Mar de areia fina alaranjada e plataformas refinadoras)
15. `ivalice:cerobi_steppe` — *Cerobi Steppe* (Terraços rochosos ventilados e moinhos)

Inclui estruturas e blocos arquitetônicos para as grandes cidades (**Rabanastre**, **Archades**, **Bhujerba** e **Balfonheim Port**).

---

### 2. 🐉 Bestiário, Caçadas, Espers e Criaturas Interdimensionais
- **453 criaturas** no mod. **439 formas de The Zodiac Age** cobrem os 384 grupos do índice numerado do Clan Primer. Formas próximas podem compartilhar uma silhueta, com texturas e estatísticas próprias.
- A fauna se distribui pelos **15 biomas**. Monstros raros, chefes, Juízes e Espers podem ser enfrentados por contrato. Os inimigos de locais ainda sem geração própria são associados às regiões atuais de Ivalice.
- O saque, o roubo, a captura com **Poach** e os bônus de sequência de abates usam as tabelas disponíveis. As variantes de **FFIV, FFV, FFVI e FFXV** continuam no Nether e no Fim.
- Os modelos do GeckoLib têm animações de caminhada, ataque, conjuração e reação a dano. As texturas de criatura são de **256×256**, com materiais por família e detalhes pintados sobre as faces do modelo.

Referências do bestiário e dos contratos. [Índice do Clan Primer](https://jegged.com/Games/Final-Fantasy-XII/Clan-Primer/Bestiary/), [dados comunitários de IZJS](https://github.com/tzapp/tzapp.github.io/tree/4d17bc27651c50f2a0e0be44e0c6eb0e6a777791/src/database), [caçadas de The Zodiac Age](https://ffxii.us/hunts).

---

### 3. 📈 Progressão Linear do Nível 1 ao 99, 12 Profissões e Catálogo de The Zodiac Age
- **Nível 1 ao Nível 99**: Cada nível ganho aumenta automaticamente a Vida Máxima, Ataque Físico, Defesa e MP do personagem, além de destravar a 2ª profissão no Nível 5 e os graus superiores de magia.
- **1.246 registros de The Zodiac Age (`tza_catalog.tsv`)**: Todas as 16 categorias de armas, armaduras leves/pesadas/místicas, escudos, acessórios, consumíveis e espólios escalonados por *Rank* (`WOOD` → `STONE` → `IRON`/`CHAINMAIL` → `DIAMOND` → `NETHERITE`), com sprites de inventário. Dois itens compartilham o ID entre as listas de acessórios e itens-chave. Os ícones do catálogo usam imagens de 64×64, com ilustrações próprias para relíquias como Tournesol, Masamune e Zodiac Spear.
- **Comércio Regional vs. Alquimia & Bazaar**:
  - Aldeões mercadores vendem apenas itens básicos/regionais e pergaminhos de magia **até o Tier 2**.
  - Armas lendárias (`Tournesol`, `Zodiac Spear`, `Wyrmhero Blade`, `Masamune`, `Excalibur`), armaduras supremas (`Grand Armor`, `Maximilian`) e magias **Tier 3 (`-ga`)** e **Tier 4** exigem espólios de monstros/chefes e síntese em **Alquimia & Bazaar**.

---

### 4. 🔥 Magias Progressivas (Tier 1 a Tier 4) com Destruição e Transformação de Cenário
Para usar novas magias no combate por turnos (`Num 2`) ou em campo, o jogador deve aprender o respectivo pergaminho (Clique Direito) e cumprir o requisito de nível:
- **Tier 1 (`Common` • Nv. 1–8 • Vendidas em Aldeões)**: `Cure`, `Fire`, `Blizzard`, `Thunder`, `Aqua`, `Aero`, `Dark`.
- **Tier 2 (`Uncommon` • Nv. 14–28 • Vendidas em Aldeões)**: `Cura`, `Fira`, `Blizzara`, `Thundara`, `Aquara`, `Aerora`, `Darkra`.
- **Tier 3 `-ga` (`Rare` • Nv. 36–56 • Somente Fabricadas na Alquimia)**: `Hastega`, `Curaga`, `Firaga`, `Blizzaga`, `Thundaga`, `Waterga`, `Aeroga`, `Darkga`.
- **Tier 4 Cataclísmicas (`Epic` • Nv. 68–95 • Fabricadas com Relíquias do Nether & The End)**: `Holy`, `Flare`, `Ardor`, `Collapse`, `Scathe`.

**Impacto Físico no Mundo (`BattleVisual.applyWorldImpact`)**:
- `Firaga` / `Ardor` / `Flare`: Abrem crateras vulcânicas e calcinam o terreno em `scorched_magma_earth`, magma e rocha negra.
- `Blizzaga`: Congela águas, transforma o solo em `paramina_permafrost` e `glacier_rime_stone` e ergue pilares de cristal glacial.
- `Thundaga`: Invoca colunas de relâmpagos e vitrifica o solo em obsidiana.
- `Aeroga`: Arranca folhas/vegetação e lança inimigos para o alto.
- `Waterga`: Extingue chamas e inunda/enlameia o terreno em `nabreus_mire`.
- `Collapse` / `Scathe`: Abre crateras arcanas com `feywood_runed_marble` e obsidiana chorona.
- `Holy`: Consagra o solo em calcário de Bhujerba, pedras folheadas a ouro (`rabanastre_gold_inlay`) e lírios brancos de Feywood.

---

### 5. 🎨 Shaders PBR e Texturas 32x32 HD (`Ivalice-Realism-PBR.zip`)
- Todos os blocos arquitetônicos, terrenos e plantas de Ivalice possuem texturas **32x32 HD** com mapas **LabPBR 1.3**:
  - `_n.png`: Normal Map de alto relevo (`5.5x`), Ambient Occlusion nas fendas (`Blue`) e Heightmap POM (`Alpha 18..255`).
  - `_s.png`: Smoothness (`Red`), Metalness/F0 (`Green`), Porosity (`Blue`) e Emissive (`Alpha`) para veios de ouro, runas de Feywood, Sunstone e magma.
- O shaderpack **`shaderpacks/Ivalice-Realism-PBR.zip`** mantém a cobertura das texturas ao aplicar relevo, além de iluminação de borda, reflexos Fresnel e cores quentes. O relevo por paralaxe pode ser ativado nas opções do shaderpack e começa desligado.

Para Minecraft 26.3 / NeoForge 26.3.0.101, a instalação local usa [Iris 1.11.7](https://modrinth.com/mod/iris) e [Sodium 0.9.3-beta.1](https://modrinth.com/mod/sodium/version/SS023RVV). O Sodium beta.3 muda o formato de dados do terreno que essa versão do Iris lê, causando a grade e as transparências indevidas.

---

O menu de combate oferece alvo único e área para magias compatíveis, técnicas aprendidas, bolsa de consumíveis e até sete adversários. Escudos, acessórios e munições afetam a batalha. Depois de derrotar um Esper, sua relíquia permite invocá-lo como aliado temporário em campo.

## 🎮 Atalhos e Comandos

| Tecla | Função |
| :--- | :--- |
| **`Num 1`** | Abre o Menu Principal (*Crônicas de Ivalice* + *Guia de Iniciante*) |
| **`Num 2`** | Inicia o Combate por Turnos mirando em uma criatura (a tela oculta-se durante as animações!) |
| **`Num 3`** | Quadro de Caçadas do Clã Centurio (*Mark Hunts* de Rank I a Rank S++) |
| **`Num 4`** | Bestiário do Clã (*Clan Primer* — 453 criaturas de FFXII, FFIV, FFV, FFVI e FFXV) |
| **`Num 5`** | Personagem, 12 Profissões do Zodíaco, Licenças e Gambits |
| **`Num 6`** | Inventário e Equipamentos |

**Comandos úteis**:
- `/ivalice kit` — Entrega o kit inicial de aventureiro.
- `/ivalice tp <bioma>` — Teleporta para qualquer uma das 15 regiões de Ivalice.
- `/ivalice hunt spawn <id>` — Inicia a caçada ou o contrato regional (ex.: `rogue_tomato`, `belias`, `yiazmat`, `rubicante_nether`, `kefka_god_end`).
- `/ivalice gambit set <condição> <ação>` — Associa uma ação a um Gambit aprendido. Use `/ivalice gambit list` para consultar os IDs.
- `/ivalice catalog search <termo>` — Pesquisa o catálogo e mostra a forma de obtenção.
- `/ivalice catalog recipe <id>` — Consulta o preço ou os materiais.
- `/ivalice catalog order <id>` — Encomenda pelo Bazaar quando o item tem essa opção. A encomenda exige nível, Gil e espólios.

---

## 🛠️ Compilação e Execução

Requisitos de desenvolvimento: **JDK 25** (baixado automaticamente pelo Gradle Foojay Toolchain se necessário). O JAR gerado já inclui TerraBlender e GeckoLib.

```powershell
# Compilar e gerar o pacote .jar em build/libs/ivalice-1.0.0-stage1.jar
.\gradlew.bat build

# Executar o Cliente de Desenvolvimento do Minecraft 26.3
.\gradlew.bat runClient

# Executar o Servidor Dedicado
.\gradlew.bat runServer
```

Para regenerar a arte, instale Pillow no Python. As folhas-fonte ficam em `art/` e os PNGs de jogo são incluídos no JAR.
