# Graph Report - antigravity-mobile-pro  (2026-10-26)

## Corpus Check
- 8 files · ~16,846 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 115 nodes · 191 edges · 12 communities (8 shown, 4 thin omitted)
- Extraction: 100% EXTRACTED · 0% INFERRED · 0% AMBIGUOUS
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `97d699ac`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- 🚀 Antigravity Mobile Pro
- 2. Soluções Implementadas no Antigravity Mobile Pro
- KeepAliveService
- MainActivity
- android.content.Context
- .onCreate
- NetworkStateListener
- .onShowFileChooser
- Override
- build_apk.sh
- install_apk.sh
- android.webkit.WebView

## God Nodes (most connected - your core abstractions)
1. `MainActivity` - 30 edges
2. `KeepAliveService` - 16 edges
3. `🚀 Antigravity Mobile Pro` - 8 edges
4. `AntigravityWebViewClient` - 7 edges
5. `✨ Principais Diferenciais e Otimizações` - 7 edges
6. `NetworkStateListener` - 5 edges
7. `AntigravityWebChromeClient` - 5 edges
8. `AntigravityNativeBridge` - 5 edges
9. `2. Soluções Implementadas no Antigravity Mobile Pro` - 5 edges
10. `🏛️ Documentação de Engenharia e Arquitetura: Antigravity Mobile Pro` - 4 edges

## Surprising Connections (you probably didn't know these)
- `MainActivity` --implements--> `NetworkStateListener`  [EXTRACTED]
  src/com/mateuscelestino/antigravitymobile/MainActivity.java → src/com/mateuscelestino/antigravitymobile/KeepAliveService.java

## Import Cycles
- None detected.

## Communities (12 total, 4 thin omitted)

### Community 0 - "🚀 Antigravity Mobile Pro"
Cohesion: 0.11
Nodes (17): 1. Compilar o APK, 1. Daemon Permanente de Hardware (`KeepAliveService`), 2. Auto-Cura de Conexão e Auto-Clicker de DOM, 2. Instalar e Blindar no Celular via ADB, 3. Stealth Radio Keep-Alive (Prevenção de Queda da Torre/Roteador), 4. Sanitização de User-Agent & Isolamento de Contas, 5. Pull-to-Refresh Nativo com Resposta Háptica, 6. Prevenção de Fechamento Acidental (+9 more)

### Community 1 - "2. Soluções Implementadas no Antigravity Mobile Pro"
Cohesion: 0.17
Nodes (11): 1. Análise da Causa Raiz: Por que a WebAPK Original Falha?, 2. Soluções Implementadas no Antigravity Mobile Pro, 3. Especificação do Pipeline de Compilação, A. Foreground Service e Locks de Hardware, A. Suspensão de Abas e Congelamento de Timers, B. Interceptação e Watchdog de WebSocket na Camada Web, B. O Problema da Detecção de Queda (`navigator.onLine`), C. Descarte de Tabelas NAT de Operadoras Celulares (RRC Idle State) (+3 more)

### Community 2 - "KeepAliveService"
Cohesion: 0.16
Nodes (11): android.app.Notification, android.app.Service, android.content.Intent, android.net.ConnectivityManager, android.os.IBinder, NetworkCallback, Intent, Override (+3 more)

### Community 3 - "MainActivity"
Cohesion: 0.46
Nodes (7): android.app.Activity, android.net.Uri, android.os.Handler, android.os.Vibrator, android.webkit.ValueCallback, android.widget.ProgressBar, MainActivity

### Community 5 - ".onCreate"
Cohesion: 0.15
Nodes (5): android.annotation.SuppressLint, android.os.Bundle, android.webkit.JavascriptInterface, Handler, AntigravityNativeBridge

### Community 7 - ".onShowFileChooser"
Cohesion: 0.40
Nodes (3): android.webkit.WebChromeClient, FileChooserParams, AntigravityWebChromeClient

### Community 9 - "build_apk.sh"
Cohesion: 0.50
Nodes (3): JAVA_HOME, PATH, build_apk.sh script

### Community 13 - "android.webkit.WebView"
Cohesion: 0.25
Nodes (6): android.graphics.Bitmap, android.webkit.WebResourceError, android.webkit.WebResourceRequest, android.webkit.WebView, android.webkit.WebViewClient, AntigravityWebViewClient

## Knowledge Gaps
- **26 isolated node(s):** `build_apk.sh script`, `JAVA_HOME`, `PATH`, `install_apk.sh script`, `A. Suspensão de Abas e Congelamento de Timers` (+21 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 36 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **4 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MainActivity` connect `MainActivity` to `KeepAliveService`, `.onCreate`, `NetworkStateListener`, `.onShowFileChooser`, `Override`, `android.webkit.WebView`?**
  _High betweenness centrality (0.205) - this node is a cross-community bridge._
- **Why does `KeepAliveService` connect `KeepAliveService` to `NetworkStateListener`?**
  _High betweenness centrality (0.126) - this node is a cross-community bridge._
- **Why does `NetworkStateListener` connect `NetworkStateListener` to `KeepAliveService`, `MainActivity`?**
  _High betweenness centrality (0.080) - this node is a cross-community bridge._
- **What connects `build_apk.sh script`, `JAVA_HOME`, `PATH` to the rest of the system?**
  _26 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `🚀 Antigravity Mobile Pro` be split into smaller, more focused modules?**
  _Cohesion score 0.1111111111111111 - nodes in this community are weakly interconnected._