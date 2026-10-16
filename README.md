# 🎯 Cannon Game

**Aluno:** João Eduardo Souza  
**Disciplina:** Programação III  
**Referência:** Android 6 for Programmers: An App-Driven Approach, 3rd Edition — Capítulo 6

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
| Canhão apontável | Toque em qualquer ponto da tela para apontar e disparar |
| Projétil | Bola disparada do cano do canhão em direção ao toque |
| 7 alvos coloridos | Alvos empilhados verticalmente; acertar um dá +3 s |
| Bloqueador móvel | Barra cinza que sobe e desce; rebate o projétil e aplica −2 s |
| Limite de tempo | 10 segundos iniciais; vence quem destruir todos os alvos a tempo |
| Sons | `cannon_fire`, `target_hit` e `blocker_hit` via `SoundPool` |
| HUD | Tempo restante e quantidade de disparos exibidos na tela |
| **[Extra]** Barra de tempo | Barra colorida (verde→vermelho) no rodapé mostra o tempo visualmente |
| **[Extra]** Recorde pessoal | Menor número de disparos salvo com `SharedPreferences` |

---

## Como Executar

### Pré-requisitos
- Android Studio **Ladybug** (2024.2) ou superior
- SDK Android 34 instalado
- Dispositivo físico ou emulador com API 24+

### Passos

```bash
# 1. Clone o repositório
git clone https://github.com/<seu-usuario>/CannonGame.git

# 2. Abra no Android Studio
#    File → Open → selecione a pasta CannonGame

# 3. Aguarde o sync do Gradle

# 4. Execute no dispositivo/emulador (orientação landscape)
#    Run → Run 'app'
```

> **Dica:** Use um dispositivo físico para a melhor experiência sonora e de toque.

---

## Estrutura do Projeto

```
CannonGame/
├── app/src/main/
│   ├── java/com/joaoeduardo/cannongame/
│   │   ├── MainActivity.java   # Activity: imersão, ciclo de vida
│   │   └── CannonView.java     # SurfaceView: lógica, desenho, sons
│   ├── res/
│   │   ├── raw/                # Arquivos de áudio (.wav)
│   │   └── values/             # strings, colors, themes
│   └── AndroidManifest.xml
├── build.gradle
├── settings.gradle
└── README.md
```

---

## Regras do Jogo

- **Toque** na tela para apontar e disparar o canhão.
- **Acertar um alvo** → +3 segundos de tempo.
- **Rebater no bloqueador** ou **errar** → −2 segundos.
- **Vitória:** todos os 7 alvos destruídos antes do tempo acabar.
- **Derrota:** o tempo chega a zero.

---

## Recursos Extras Implementados

1. **Barra de tempo visual** — barra horizontal no rodapé que muda de verde para vermelho conforme o tempo diminui, tornando a urgência imediatamente perceptível.
2. **Recorde pessoal (Best Shots)** — o menor número de disparos para vencer é salvo localmente via `SharedPreferences` e exibido no HUD e no diálogo de vitória.
