# 🎯 Cannon Game

**Aluno:** João Eduardo Souza  
**Disciplina:** Programação III  
**Referência:** Android 6 for Programmers: An App-Driven Approach, 3rd Edition — Capítulo 6  
**Repositório:** [https://github.com/Jeudardo/CannonGame](https://github.com/Jeudardo/CannonGame)

---

## Objetivo do Projeto

Desenvolver o jogo **Cannon Game** em Android nativo (Java), aplicando os conceitos de:

- Animação quadro a quadro (*frame-by-frame*) com `Canvas` e `Paint`
- Controle de *game loop* via `Thread` dedicada
- Uso de `SurfaceView` e `SurfaceHolder`
- Reprodução de sons com `SoundPool`
- Modo imersivo e tela cheia
- Interatividade por eventos de toque (`MotionEvent`)

---

## Funcionalidades Implementadas

| Funcionalidade | Descrição |
|---|---|
| Canhão articulado | Toque na tela para mirar e disparar na direção desejada |
| Projétil com rastro | Bola de canhão disparada com velocidade ajustada e efeito visual |
| 7 alvos coloridos | Alvos individuais com cantos arredondados; cada acerto concede +3 s |
| Bloqueador dinâmico | Obstáculo móvel redimensionado para jogabilidade fluida; rebate e penaliza em −2 s |
| Limite de tempo | 25 segundos iniciais; vence quem eliminar todos os alvos a tempo |
| Áudio com SoundPool | Sons de disparo (`cannon_fire`), acerto (`target_hit`) e bloqueio (`blocker_hit`) |
| HUD completo | Exibe tempo restante, disparos feitos, alvos restantes e recorde pessoal |
| **[Extra]** Barra de tempo | Barra dinâmica colorida (verde→amarelo→vermelho) no rodapé da tela |
| **[Extra]** Recorde pessoal | Menor número de disparos salvo localmente via `SharedPreferences` |
| **[Extra]** Visual Dark Arcade | Paleta moderna de alto contraste, elementos arredondados e acabamento limpo |

---

## Como Executar

### Pré-requisitos
- Android Studio ou dispositivo físico Android
- SDK Android 24+ (Android 7.0 até Android 14/15)

### Passos

```bash
# 1. Clone o repositório
git clone https://github.com/Jeudardo/CannonGame.git

# 2. Abra no Android Studio
#    File → Open → selecione a pasta CannonGame

# 3. Aguarde a sincronização do Gradle

# 4. Execute no dispositivo físico ou emulador (orientação landscape)
#    Run → Run 'app' (Play verde ▶️)
```

---

## Estrutura do Projeto

```
CannonGame/
├── app/src/main/
│   ├── java/com/joaoeduardo/cannongame/
│   │   ├── MainActivity.java   # Activity: imersão, ciclo de vida
│   │   └── CannonView.java     # SurfaceView: game loop, física, desenho, sons
│   ├── res/
│   │   ├── raw/                # Áudios (.wav): cannon_fire, target_hit, blocker_hit
│   │   ├── drawable/           # Ícones e elementos visuais adaptativos
│   │   └── values/             # strings, colors, themes (Material Fullscreen)
│   └── AndroidManifest.xml
├── build.gradle
├── settings.gradle
└── README.md
```

---

## Regras do Jogo

- **Toque** em qualquer ponto da tela para apontar o canhão e disparar.
- **Acertar um alvo** → +3 segundos de tempo adicional.
- **Acertar o obstáculo móvel** ou **errar o tiro** → −2 segundos de penalidade.
- **Vitória:** todos os 7 alvos destruídos antes do tempo esgotar.
- **Derrota:** o cronômetro chega a zero.

---

## Recursos Extras Implementados

1. **Barra de tempo visual integrada**: Barra horizontal no rodapé que transiciona suavemente de verde para amarelo e vermelho conforme a contagem regressiva avança.
2. **Sistema de Recorde (High Score)**: O jogo salva no armazenamento local (`SharedPreferences`) a menor quantidade de tiros que o jogador precisou para vencer, incentivando a precisão.
3. **Interface e Estilização Dark Arcade**: Visual remodelado com fundo de alto contraste, alvos estilizados em formato de cartões arredondados, canhão com anel articulado e obstáculo com contorno em destaque.
