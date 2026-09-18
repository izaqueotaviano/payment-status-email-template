# iztv (IPTV Player para Android TV / Fire TV)

App Android nativo de player de IPTV, feito para **Android TV** e **Fire TV Stick**, para instalar via
sideload (não requer Google Play / loja). Controle 100% pelo D-pad do controle remoto.

## Stack

- Kotlin
- Jetpack Compose for TV (`androidx.tv:tv-foundation` / `androidx.tv:tv-material`) para a interface
- Media3 ExoPlayer para reprodução (HLS, MPEG-TS, MP4, DASH)
- Room para persistência local (fontes, canais, favoritos)
- DataStore Preferences para configurações (fonte ativa, canal padrão)
- Retrofit/OkHttp + kotlinx.serialization para a API Xtream Codes e download de playlists M3U
- Storage Access Framework para importar um arquivo M3U local
- `minSdk 21`, `compileSdk`/`targetSdk 34`

## Funcionalidades

- **Fontes de canais**, múltiplas e salvas, alternáveis a qualquer momento:
  - Lista M3U/M3U8 por URL
  - Xtream Codes (host, usuário, senha)
  - Arquivo M3U local do dispositivo (via SAF)
- **Lista de canais**: agrupada por categoria, com busca por nome, favoritos, e edição por canal
  (segurar OK sobre o card abre um menu para: assistir, favoritar, esconder/mostrar, renomear, mudar de
  categoria, mover para cima/baixo na ordem, e definir/remover como canal padrão).
- **Player em tela cheia**: esquerda/direita do D-pad trocam de canal (anterior/próximo, seguindo a ordem
  visível da lista), voltar sai para a lista de canais, resolução adaptativa sem limite artificial (Media3
  decide conforme o stream e o aparelho).
- **Canal padrão**: se configurado, o app pula a lista e abre direto nesse canal; pode ser trocado ou
  removido a qualquer momento pela tela de canais (menu de edição) ou pela tela de Configurações.
- **Configurações**: gerenciar fontes salvas, ver/remover o canal padrão, limpar cache, sobre.

Fora de escopo por enquanto (mas a arquitetura não bloqueia adicionar depois): EPG e catálogo de VOD.

## Arquitetura (resumo)

```
domain/         modelos (Source, Channel) e interfaces de repositório/use case — não dependem de Android
data/local/     Room (entidades, DAOs, AppDatabase) + *RepositoryImpl
data/remote/    parser M3U, cliente Xtream Codes (Retrofit), PlaylistImporterImpl
data/datastore/ SettingsRepositoryImpl (DataStore Preferences)
player/         PlayerManager (wrapper do ExoPlayer)
ui/             telas Compose for TV + ViewModels (sources, channels, player, settings)
di/             AppContainer (DI manual) + AppViewModelFactory
navigation/     Routes + AppNavHost (NavHost do Compose Navigation)
```

Sem framework de DI (Hilt/Koin): `AppContainer` monta os singletons na mão e `AppViewModelFactory`
constrói cada ViewModel a partir dele — simples de seguir e sem processador de anotações extra além do
Room (KSP).

A troca de canal na lista faz um "merge" ao invés de substituir tudo: favoritos, ocultos, nome/categoria
customizados e a ordem são preservados entre atualizações, casando os canais pela URL (M3U) ou pelo
stream id (Xtream).

## Compilando

Este projeto foi gerado e revisado num ambiente sem acesso ao Android SDK nem ao Maven do Google
(`dl.google.com`/`maven.google.com` bloqueados na sandbox), então **não foi possível compilar/gerar o APK
aqui**. O código foi escrito e revisado com bastante cuidado (ver abaixo), mas rode e compile num ambiente
com Android SDK antes de usar em produção.

Pré-requisitos: JDK 17+ e Android SDK (`compileSdk 34`, `build-tools` correspondente) — o mais simples é
abrir a pasta `iptv-tv-player/` no Android Studio recente e deixar ele instalar o que faltar, ou exportar
`ANDROID_HOME`/`ANDROID_SDK_ROOT` manualmente.

```bash
cd iptv-tv-player
./gradlew assembleRelease
# APK gerado em: app/build/outputs/apk/release/app-release.apk
```

### Assinatura do APK

O projeto já vem com um keystore de debug (`debug.keystore`, senha `android`, alias `androiddebugkey` —
não é segredo, é o keystore padrão de debug do Android) configurado como assinatura de **release** em
`app/build.gradle.kts`, justamente para que `./gradlew assembleRelease` já gere um APK assinado pronto
para sideload sem nenhum passo extra.

Para usar seu próprio keystore de release, gere um e aponte para ele em `gradle.properties` (ou
`~/.gradle/gradle.properties`, para não versionar):

```bash
keytool -genkeypair -v -keystore minha-release.keystore -alias minha-chave \
  -keyalg RSA -keysize 2048 -validity 10000
```

```properties
RELEASE_STORE_FILE=minha-release.keystore
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=minha-chave
RELEASE_KEY_PASSWORD=...
```

### Instalando (sideload)

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

Ou copie o APK para um pendrive/rede e instale pelo gerenciador de arquivos do Fire TV/Android TV
(ative "Fontes desconhecidas" nas configurações do aparelho antes).

## Sobre as versões das bibliotecas

Todas as versões estão fixadas em `gradle/libs.versions.toml`. As de bibliotecas que não passam pelo
Google Maven (Kotlin, Retrofit, OkHttp, Coil, KSP) foram conferidas ao vivo contra o repositório Maven
Central no momento em que este projeto foi gerado. As do Google Maven (AndroidX, Compose, Media3, Room,
DataStore, Navigation, `androidx.tv`) **não puderam ser conferidas ao vivo** (rede bloqueada nesta
sandbox) — foram fixadas em versões estáveis conhecidas e coerentes entre si. Antes de compilar, vale a
pena:

- Abrir o projeto no Android Studio e rodar o "Upgrade Assistant" / aceitar sugestões do editor de
  versão do catálogo, especialmente para `androidx.tv:tv-foundation` / `androidx.tv:tv-material`
  (ainda em alpha — a API pode ter mudado desde então: <https://developer.android.com/jetpack/androidx/releases/tv>).
- Rodar `./gradlew build --refresh-dependencies` para pegar qualquer patch novo dentro da mesma versão
  menor.

## Testando no dia a dia

- **Emulador Android TV**: crie um AVD com uma imagem "Android TV" (Google TV ou Android TV) no Android
  Studio; o teclado do host simula o D-pad (setas + Enter).
- **Fire TV Stick real**: ative "Opções do desenvolvedor" → "ADB debugging" e "Apps de fontes
  desconhecidas", depois `adb connect <ip-do-fire-tv>:5555` e `adb install`.

## Limitações conhecidas / próximos passos

- EPG e catálogo de VOD ficaram de fora por decisão de escopo, mas nada na arquitetura os bloqueia (dá
  para adicionar uma tabela de programação e uma nova fonte/tela sem tocar no que já existe).
- O parser M3U cobre o formato padrão (`#EXTINF` com `tvg-logo`/`group-title`); listas com extensões
  muito fora do padrão podem precisar de ajustes pontuais.
- A API Xtream Codes tem variações entre provedores (alguns preferem `.m3u8` a `.ts` para o link do
  stream ao vivo); se um provedor específico não tocar, ajuste `buildXtreamStreamUrl` em
  `data/remote/xtream/XtreamApi.kt`.
