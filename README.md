# Projeto Ivalice — Final Fantasy XII: The Zodiac Age Mod (Minecraft Java 26.3 / NeoForge)

Recriação modular e progressiva do universo de **Final Fantasy XII: The Zodiac Age** no **Minecraft Java 26.3 (NeoForge)** com **Java 25**, **TerraBlender** e **GeckoLib**.

---

## Status Atual do Desenvolvimento

### Etapa 1 — Estrutura do Mod e Geração de Biomas (CONCLUÍDA E VALIDADA)
- **Ambiente Técnico**:
  - Minecraft Java `26.3`
  - NeoForge `26.3.0.101` (`ModDevGradle 2.0.148`)
  - Java `25` (provisionado automaticamente via Foojay Toolchain)
  - TerraBlender `26.3.0.0.9`
  - GeckoLib `5.5.7`
  - Compatibilidade total com pipeline nativo de renderização e Iris Shaders.
- **15 Biomas Canônicos de Ivalice Gerados no Overworld (`IvaliceBiomes` & `IvaliceOverworldRegion`)**:
  1. `ivalice:dalmasca_estersand` — *Dalmasca Estersand* (Desertos rochosos, ravinas áridas, Areia e Arenito de Dalmasca, Arbustos de Estersand)
  2. `ivalice:dalmasca_westersand` — *Dalmasca Westersand* (Grandes dunas varridas pelo vento, tempestades de poeira ambiente)
  3. `ivalice:giza_plains_dry` — *Giza Plains (The Dry)* (Planícies douradas ensolaradas, depósitos de *Giza Sunstone Ore*, *Wild Gysahl Greens*)
  4. `ivalice:giza_plains_rains` — *Giza Plains (The Rains)* (Planícies alagadas sob chuva contínua, poças naturais de lama e água)
  5. `ivalice:ozmone_plain` — *Ozmone Plain* (Campos amplos e savana verde-esmeralda com *Wild Gysahl Greens*)
  6. `ivalice:golmore_jungle` — *Golmore Jungle* (Selva densa e sombreada, *Golmore Mossy Stone*, partículas de esporos místicos)
  7. `ivalice:phon_coast` — *Phon Coast* (Costa tropical de águas turquesa, praias de areia branca e falésias de calcita)
  8. `ivalice:tchita_uplands` — *Tchita Uplands* (Planaltos elevados e campos temperados)
  9. `ivalice:paramina_rift` — *Paramina Rift* (Desfiladeiros congelados, nevascas perpétuas, *Paramina Permafrost* e picos de gelo)
  10. `ivalice:salikawood` — *The Salikawood* (Floresta antiga exuberante de folhagem dourada/verde)
  11. `ivalice:feywood` — *The Feywood* (Floresta branca mística coberta de névoa *Mist*, *Feywood Mist-Soaked Soil* e *Feywood White Lily*)
  12. `ivalice:mosphoran_highwaste` — *Mosphoran Highwaste* (Penhascos montanhosos de *Mosphoran Slate*, cinzas vulcânicas/vapor e fontes)
  13. `ivalice:nabreus_deadlands` — *Nabreus Deadlands* (Pântanos sombrios sobre *Nabreus Mire*, neblina fúnebre e águas escuras)
  14. `ivalice:ogir_yensa_sandsea` — *Ogir-Yensa Sandsea* (Imenso mar de *Yensa Fine Sand* alaranjada na fronteira ocidental)
  15. `ivalice:cerobi_steppe` — *Cerobi Steppe* (Terraços rochosos ventilados e colinas elevadas)
- **Sistema Sazonal de Giza Plains (`GizaSeasonManager`)**:
  - Alternância determinística e configurável entre *The Dry* e *The Rains* baseada no relógio do Overworld, sem corrupção ou regeneração destrutiva de chunks.
- **Blocos Naturais e Flora (`IvaliceBlocks`)**:
  - `dalmasca_sand`, `dalmasca_sandstone`, `yensa_fine_sand`, `sunstone_ore`, `golmore_darkwood_log`, `golmore_moss_stone`, `feywood_mist_soil`, `paramina_permafrost`, `mosphoran_slate`, `nabreus_mire`, `estersand_scrub`, `gysahl_greens_wild`, `feywood_white_lily`.

### Etapas 2 a 5 — Recursos disponíveis

- **Etapa 2** — Itens, equipamentos, consumíveis, espólios, magias e técnicas.
- **Etapa 3** — Criaturas com modelos e animações GeckoLib e combate por turnos.
- **Etapa 4** — Bestiário, perfil do Clã Centurio, caçadas e recompensas.
- **Etapa 5** — Belias e Mateus como alvos, Bazaar, licenças e Gambits. Estruturas e masmorras personalizadas seguem pendentes.

---

## Geração e fauna do Overworld

Com a geração de Ivalice ativa, os novos trechos do Overworld normal usam as regiões de Ivalice. Terreno e biomas gravados em mundos existentes são preservados, e esses trechos passam a receber a fauna de Ivalice. Animais domesticados, criaturas com nome e NPCs como aldeões são preservados. Nether e End mantêm sua fauna.

Ichthon vive na água. As Marks e os Espers podem ser chamados pelo sistema de caçadas.

## Como Executar e Testar

No diretório `c:\Users\Pichau\Downloads\Projeto_Game\ivalice`:

```powershell
# Compilar e gerar o arquivo .jar em build/libs/ivalice-1.0.0-stage1.jar
.\gradlew.bat build

# Iniciar o Cliente do Minecraft 26.3 com o mod carregado
.\gradlew.bat runClient

# Iniciar o Servidor Dedicado do Minecraft 26.3
.\gradlew.bat runServer
```

Para instalar o `.jar` fora do ambiente de desenvolvimento, coloque-o na pasta `mods` do cliente e do servidor junto com TerraBlender 26.3.0.0.9 e GeckoLib 5.5.7.

Comandos de teste no jogo

- `/ivalice kit` entrega o kit de aventureiro.
- `/ivalice tp feywood` e `/ivalice tp ivalice:feywood` procuram a região num raio de 6.400 blocos na dimensão atual.
- `/ivalice status` mostra Gil, LP, rank do clã, caçadas, licenças e Gambits.
- `/ivalice bestiary` mostra os registros e a contagem de abates.
- `/ivalice hunt spawn rogue_tomato` inicia a caçada ao Rogue Tomato. Também há alvos como `thextera`, `flowering_cactoid`, `white_mousse`, `belias` e `mateus`.
