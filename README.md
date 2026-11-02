# Narutoto - Shinobi Extras (Forge 1.20.1)

Addon para o **Narutoto 0.4.1+** (obrigatorio so para rodar; nao precisa do jar para compilar).
Adiciona **jutsus e personagens que o Narutoto nao tem** (conferido na lista de textos do jar).

## Compilar
1. Suba esta pasta num repositorio do GitHub.
2. Aba **Actions** -> workflow **Build mod** -> baixe o artefato `shinobiextras-addon` (o `.jar`).
3. Coloque em `mods/` junto com o Narutoto e as dependencias dele.
(Se o build reclamar do Gradle, troque `gradle-version: 8.8` por `8.1.1` em `.github/workflows/build.yml`.)

## Jutsus (9)
Aprenda na tela **Jutsus Extras** (botao no canto superior direito das telas `JutsuBoard`/`OtherBoard` do Narutoto,
tecla **K**, ou o comando `/jutsusextras`) pagando niveis de XP, ou com o **pergaminho** do jutsu.
**Z** usa o jutsu selecionado, **X** troca para o proximo aprendido. Cada jutsu tem recarga e custa chakra do Narutoto
(se o addon nao conseguir ler o chakra, nao cobra). No criativo nao ha recarga nem custo.

| Jutsu | XP | Efeito |
|---|---|---|
| Kirigakure no Jutsu | 15 | Nevoa de 15 s: cega e atrasa quem esta em volta; mobs perdem voce de vista; velocidade para voce |
| Nikudan Sensha | 12 | Investida de bola humana: resistencia e dano forte no caminho |
| Gatsuuga | 14 | Investida giratoria que acerta varias vezes |
| Shikotsumyaku | 20 | Espinhos de osso no ponto mirado: dano, lancamento para cima, lentidao |
| Kikaichu | 10 | Veneno + fraqueza no alvo; contra jogadores, suga chakra |
| Bakuha (Jibaku Nendo) | 25 | Marca um ponto e explode apos 2 s (**nao destroi blocos**) |
| Samehada | 18 | Golpe curto: fere, cura voce e rouba chakra de jogadores |
| Chidori Nagashi | 22 | Aura eletrica de 8 s: fere e afasta quem chegar perto |
| Shunshin no Jutsu | 8 | Teleporte curto para frente (ate 12 blocos) |

## Shinobis (5) - ovos de spawn e spawn raro pelo mundo
- **Zabuza** (hostil): Kirigakure + Kubikiri. Larga o pergaminho do *Kirigakure*.
- **Kisame** (hostil): onda de agua + Samehada (rouba vida). Larga o pergaminho do *Samehada*.
- **Deidara** (hostil): bombas de argila que explodem sem destruir blocos. Larga o pergaminho do *Bakuha*.
- **Kimimaro** (hostil): espinhos de osso. Larga o pergaminho do *Shikotsumyaku*.
- **Rock Lee** (pacifico): so reage se for atacado; com pouca vida abre os portoes (forca + velocidade).

Os pergaminhos tambem aparecem na aba criativa **Shinobi Extras**.

## Skins
As skins em `assets/shinobiextras/textures/entity/` (`zabuza`, `kisame`, `deidara`, `kimimaro`, `rock_lee`) sao
**provisorias, pintadas por codigo**. Troque por skins de 64x64 com bracos largos usando o mesmo nome de arquivo.

## Limites conhecidos
- O codigo **nao foi compilado nem testado no jogo**. Se o build ou o jogo falhar, mande o erro/`latest.log`.
- Os jutsus usam teclas proprias (nao entram nos slots de jutsu do Narutoto).
- Spawns configuraveis em `data/shinobiextras/forge/biome_modifier/` (apague um arquivo para tirar o personagem).
