# Programação para Dispositivos Móveis — apps dos encontros

Código de apoio dos encontros síncronos do componente. São dois aplicativos
Android completos, em Kotlin, feitos para serem lidos e rodados.

Nada aqui é resolução de atividade avaliativa. São apps de demonstração: o
domínio é registro de treinos, e eles existem para mostrar cada recurso das
unidades funcionando de ponta a ponta.

## O que tem aqui

| Pasta | Assunto | Encontro |
| --- | --- | --- |
| `01-treino-app/` | Componentes de tela: EditText, RadioButton, CheckBox, Chronometer, DatePicker, ListView. Ciclo de vida da Activity. | 1º |
| `02-treino-sqlite/` | Banco SQLite, SharedPreferences, segunda tela com Intent, menu de opções, notificação. | 2º |

Os dois apps usam `applicationId` diferente, então podem ficar instalados ao
mesmo tempo no mesmo aparelho ou emulador. Isso é proposital: dá para comparar
o comportamento lado a lado.

## Como abrir

No Android Studio: **File > Open**, e escolher a pasta `01-treino-app` ou
`02-treino-sqlite` — não a pasta raiz do repositório. Cada uma é um projeto
Gradle independente.

Pelo terminal:

```sh
cd 02-treino-sqlite
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Ambiente em que foi validado

| Item | Versão |
| --- | --- |
| Android Studio | 2026.1.2 |
| Android Gradle Plugin | 9.3.1 |
| Gradle | 9.6.1 |
| JDK | 21 |
| compileSdk | 37 |
| targetSdk | 36 |
| minSdk | 24 |
| Emulador | Pixel 9, Android 16 (API 36) |

Versões um pouco diferentes devem funcionar. Se o Android Studio oferecer
atualizar o Gradle ou o AGP ao abrir o projeto, pode aceitar.

## Por onde começar a ler

No `01-treino-app`, o arquivo é um só:

- `app/src/main/java/br/univates/ead/treino/MainActivity.kt`
- `app/src/main/res/layout/activity_main.xml`

No `02-treino-sqlite`, na ordem em que fazem sentido:

1. `TreinoDbHelper.kt` — tudo que fala com o banco
2. `MainActivity.kt` — a tela principal, que usa o helper
3. `Notificacoes.kt` — canal, permissão e envio
4. `DetalheActivity.kt` — a segunda tela e a leitura dos extras da Intent

Todo o código está comentado explicando o porquê de cada decisão, não só o quê.

## Uma observação sobre o material da disciplina

O material das unidades está em Java e foi escrito para versões anteriores do
Android. O código continua servindo para entender os conceitos, que não mudaram.
O que mudou são detalhes de API — permissão de notificação, canal de notificação,
forma de registrar o menu — e cada um deles está comentado no código destes dois
projetos, no ponto exato em que aparece.
