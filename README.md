# 🚀 Antigravity Mobile Pro

> **Cliente Nativo Android de Alta Performance e Baixa Latência para Google Antigravity IDE & AI Agents**

[![Android](https://img.shields.io/badge/Platform-Android_8.0+-3DDC84?logo=android&logoColor=white)](https://android.com)
[![SDK](https://img.shields.io/badge/Target_SDK-34_(Android_14)-34A853?logo=google)](https://developer.android.com)
[![Build](https://img.shields.io/badge/Build-Pure_AAPT2_%2B_D8-4285F4)](https://developer.android.com/studio/command-line)
[![Size](https://img.shields.io/badge/APK_Size-73_KB-blueviolet)](#)
[![License](https://img.shields.io/badge/License-MIT-black)](LICENSE)

---

## 📌 Contexto e Motivação

O **Google Antigravity** em dispositivos móveis é oficialmente distribuído através de uma WebAPK / Progressive Web App (PWA) baseada em Chromium (`https://antigravity.google.com`). 

Ao ser utilizado fora do ecossistema macOS em smartphones Android (especialmente modelos Samsung Galaxy com One UI), a experiência sofre de severas instabilidades de rede:

1. **Doze Mode & Suspensão de Abas:** O Android congela abas em segundo plano, desligando timers de JavaScript (`setTimeout`/`setInterval`) e matando WebSockets quando a tela desliga ou ao alternar entre aplicativos.
2. **Queda Silenciosa de Sockets (NAT Timeout):** Conexões ociosas têm suas tabelas NAT descartadas silenciosamente por roteadores Wi-Fi e operadoras de dados móveis (4G/5G) após 30 a 60 segundos de inatividade.
3. **Travamento na Tela Preta:** A interface do Google fica travada em *"Instance disconnected - Waiting for connection or Wi-Fi to resume..."*, pois o código cliente espera o evento `window.ononline`, que nunca é disparado se a interface de rede física permaneceu conectada.
4. **Conflito de Multi-Contas do Google:** Ao desconectar, o seletor de contas do Google força reautenticação. Se o dispositivo possui múltiplas contas, o app cai na conta padrão (`/u/0/`), que não possui nenhuma estação desktop vinculada, exigindo logins repetitivos.

O **Antigravity Mobile Pro** é a resposta definitiva para esses problemas: um aplicativo nativo Android enxuto (apenas 73 KB), com controle total de hardware, sem dependências infladas de frameworks pesados, garantindo **máxima estabilidade de conexão 24/7**.

---

## 🏗️ Arquitetura do Sistema

```
┌────────────────────────────────────────────────────────────────────────┐
│                   ANTIGRAVITY MOBILE PRO (v2.7.0)                      │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │                 KeepAliveService (Daemon Nativo)               │   │
│   │   • Foreground Service com Prioridade Mínima (Discreta)        │   │
│   │   • Trava de Hardware: PowerManager PARTIAL_WAKE_LOCK          │   │
│   │   • Trava de Hardware: WifiManager WIFI_MODE_FULL_HIGH_PERF    │   │
│   │   • ConnectivityManager: Handover instantâneo Wi-Fi <-> 5G     │   │
│   └────────────────────────────────────────────────────────────────┘   │
│                                   │                                    │
│                                   ▼                                    │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │                   WebView Chromium Engine Otimizada            │   │
│   │   • Renderização em Camada de Hardware (OLED 120Hz)            │   │
│   │   • offscreenPreRaster = true (Zero engasgos na rolagem)       │   │
│   │   • User-Agent Sanitizado (Bypass de 403 disallowed_useragent) │   │
│   │   • CookieManager & LocalStorage Isolados                      │   │
│   └────────────────────────────────────────────────────────────────┘   │
│                                   │                                    │
│                                   ▼                                    │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │               Watchdog Ativo & Auto-Cura de Conexão            │   │
│   │   • Interceptor de WebSocket nativo para monitorar quedas      │   │
│   │   • Scanner DOM: Auto-clique em botões de "Reconnect" / "Retry"│   │
│   │   • Soft-Reconnect suave em < 1.8 segundos sem perda de estado │   │
│   │   • Stealth Radio Keep-Alive (beacon assíncrono anti-NAT)      │   │
│   └────────────────────────────────────────────────────────────────┘   │
│                                   │                                    │
│                                   ▼                                    │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │                      SessionMigrator                           │   │
│   │   • Injeção de 20 credenciais oficiais Google                  │   │
│   │   • Fixação no endpoint de alta prioridade: /u/1/?pli=1        │   │
│   │   • Isolamento contra troca involuntária de contas Google      │   │
│   └────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────┘
```

---

## ✨ Principais Diferenciais e Otimizações

### 1. Daemon Permanente de Hardware (`KeepAliveService`)
* Registra um serviço em primeiro plano contínuo com canal de notificação discreta (`NotificationManager.IMPORTANCE_MIN`).
* Adquire e mantém `PowerManager.PARTIAL_WAKE_LOCK` e `WifiManager.WIFI_MODE_FULL_HIGH_PERF`.
* **Resultado:** O aplicativo **nunca é encerrado pelo gerenciador de memória agressivo** (One UI RAM Plus / LMKD), mantendo o socket do Antigravity ativo mesmo com a tela desligada ou com bateria fraca.

### 2. Auto-Cura de Conexão e Auto-Clicker de DOM
* Intercepta nativamente o construtor `window.WebSocket` na camada JavaScript.
* Executa varredura de DOM a cada 1.800 ms. Se o toast *"Instance disconnected"* ou qualquer botão com rótulo "Reconnect", "Retry" ou "Tentar novamente" aparecer, o script **clica automaticamente nele em menos de 1,8 segundo**, reestabelecendo a transmissão sem recarregar a página.

### 3. Stealth Radio Keep-Alive (Prevenção de Queda da Torre/Roteador)
* A cada 18 segundos, executa uma requisição HTTP assíncrona ultraleve sem CORS para `/favicon.ico`.
* Essa requisição mantém a torre celular no estado `RRC Connected` e impede que roteadores residenciais fechem a tabela de tradução NAT por inatividade.

### 4. Sanitização de User-Agent & Isolamento de Contas
* Remove a flag `; wv` do cabeçalho do WebView. O Google identifica a requisição como o Chrome Mobile autêntico, prevenindo o bloqueio `403: disallowed_useragent`.
* O [`SessionMigrator`](src/com/mateuscelestino/antigravitymobile/SessionMigrator.java) injeta 20 cookies de sessão oficiais do Google diretamente no `CookieManager`, forçando o carregamento na conta Pro (`/u/1/`) e impedindo que o login mude para contas pessoais do Android.

### 5. Pull-to-Refresh Nativo com Resposta Háptica
* Toque e arraste para baixo a partir do topo da tela para forçar uma ressincronização.
* O motor de vibração do dispositivo confirma o disparo com um pulso tátil de 20 ms.

### 6. Prevenção de Fechamento Acidental
* O botão físico/gestual de voltar (`onBackPressed`) foi reimplementado para navegar no histórico ou mover a tarefa para segundo plano (`moveTaskToBack(true)`), impedindo o encerramento acidental da sessão do desktop.

---

## 📂 Estrutura do Repositório

```
antigravity-mobile-pro/
├── AndroidManifest.xml           # Manifesto do app, permissões e declaração de serviços
├── build_apk.sh                  # Script de compilação pura (AAPT2, D8, javac, apksigner)
├── install_apk.sh                # Script de instalação e blindagem de SO via Wireless ADB
├── assets/
│   └── session_cookies.template.json # Modelo para injeção de credenciais de sessão
├── res/                          # Recursos gráficos e layouts
│   ├── drawable/                 # Ícone da barra de status e logo oficial
│   ├── layout/activity_main.xml  # Layout com WebView e barra de progresso horizontal
│   ├── mipmap-*/ic_launcher.png  # Ícones adaptativos em todas as densidades (mdpi a xxxhdpi)
│   └── values/                   # Strings, cores e estilos (OLED Dark Mode)
└── src/com/mateuscelestino/antigravitymobile/
    ├── KeepAliveService.java     # Serviço de hardware (WakeLock + WifiLock)
    ├── MainActivity.java         # Activity principal, gestos, WebView e Watchdog
    └── SessionMigrator.java      # Importador e injetor de cookies no CookieManager
```

---

## 🛠️ Compilação e Instalação

### Pré-requisitos
* **Java 17 OpenJDK** (`openjdk@17`)
* **Android SDK Build-Tools 34.0.0** (`aapt2`, `d8`, `zipalign`, `apksigner`)
* **Android Platform 34** (`android.jar`)
* Dispositivo Android conectado via USB ou Wireless ADB

### 1. Compilar o APK

Execute o script de compilação automatizada:

```bash
chmod +x build_apk.sh
./build_apk.sh
```

O script executa os seguintes passos:
1. Compila os recursos XML e PNG usando `aapt2 compile`.
2. Vincula os recursos e gera `R.java` com `aapt2 link`.
3. Compila as classes Java 17 usando `javac`.
4. Converte o bytecode para formato Dalvik com `d8`.
5. Alinha o pacote APK em limites de 4 bytes com `zipalign`.
6. Assina o APK digitalmente com esquemas v2 e v3 usando `apksigner`.

O binário final será gerado em: `dist/antigravity-mobile-pro.apk` (~73 KB).

### 2. Instalar e Blindar no Celular via ADB

Com o dispositivo conectado ao ADB, execute:

```bash
chmod +x install_apk.sh
./install_apk.sh
```

O script instala o APK e aplica automaticamente as seguintes regras no Android:
* Isenção do Doze Mode: `dumpsys deviceidle whitelist +com.mateuscelestino.antigravitymobile`
* Execução em segundo plano irrestrita: `cmd appops set com.mateuscelestino.antigravitymobile RUN_IN_BACKGROUND allow`
* Desativação do sono de Wi-Fi: `settings put global wifi_power_save 0`
* Dados móveis ininterruptos para failover: `settings put global mobile_data_always_on 1`

---

## 🔐 Configuração de Sessão (Opcional)

Para iniciar o app já autenticado sem passar por formulários de login:

1. Duplique o arquivo de exemplo:
   ```bash
   cp assets/session_cookies.template.json assets/session_cookies.json
   ```
2. Preencha seus tokens do Google (`SID`, `HSID`, `SSID`, `__Secure-3PSID`, etc.).
3. Recompile com `./build_apk.sh`. O [`SessionMigrator`](src/com/mateuscelestino/antigravitymobile/SessionMigrator.java) injetará os cookies automaticamente no primeiro boot.

---

## 👤 Autor e Licença

Desenvolvido por **Mateus Celestino** ([`202405039459@alunos.estacio.br`](mailto:202405039459@alunos.estacio.br)).

Distribuído sob a licença **MIT**. Consulte o arquivo [LICENSE](LICENSE) para obter mais informações.
