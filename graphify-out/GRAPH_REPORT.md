# Graph Report - antigravity-mobile-pro  (2026-10-25)

## Corpus Check
- 3 files · ~10,030 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 79 nodes · 155 edges · 14 communities (7 shown, 6 thin omitted)
- Extraction: 100% EXTRACTED · 0% INFERRED · 0% AMBIGUOUS
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `445e41ad`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- AntigravityWebViewClient
- AntigravityNativeBridge
- KeepAliveService
- android.content.Intent
- .onCreate
- MainActivity
- NetworkStateListener
- android.webkit.WebView
- Override
- build_apk.sh
- .buildNotification
- KeepAliveService.java
- .onReceivedError

## God Nodes (most connected - your core abstractions)
1. `MainActivity` - 30 edges
2. `KeepAliveService` - 16 edges
3. `AntigravityWebViewClient` - 7 edges
4. `NetworkStateListener` - 5 edges
5. `AntigravityWebChromeClient` - 5 edges
6. `AntigravityNativeBridge` - 5 edges
7. `build_apk.sh script` - 1 edges
8. `JAVA_HOME` - 1 edges
9. `PATH` - 1 edges

## Surprising Connections (you probably didn't know these)
- `MainActivity` --implements--> `NetworkStateListener`  [EXTRACTED]
  src/com/mateuscelestino/antigravitymobile/MainActivity.java → src/com/mateuscelestino/antigravitymobile/KeepAliveService.java

## Import Cycles
- None detected.

## Communities (14 total, 6 thin omitted)

### Community 0 - "AntigravityWebViewClient"
Cohesion: 0.33
Nodes (3): android.graphics.Bitmap, android.webkit.WebViewClient, AntigravityWebViewClient

### Community 2 - "KeepAliveService"
Cohesion: 0.39
Nodes (4): NetworkCallback, KeepAliveService, WakeLock, WifiLock

### Community 3 - "android.content.Intent"
Cohesion: 0.33
Nodes (3): android.content.Intent, android.os.IBinder, Override

### Community 4 - ".onCreate"
Cohesion: 0.29
Nodes (3): android.annotation.SuppressLint, android.os.Bundle, Handler

### Community 5 - "MainActivity"
Cohesion: 0.46
Nodes (7): android.app.Activity, android.net.Uri, android.os.Handler, android.os.Vibrator, android.webkit.ValueCallback, android.widget.ProgressBar, MainActivity

### Community 7 - "android.webkit.WebView"
Cohesion: 0.40
Nodes (4): android.webkit.WebChromeClient, android.webkit.WebView, FileChooserParams, AntigravityWebChromeClient

### Community 9 - "build_apk.sh"
Cohesion: 0.50
Nodes (3): JAVA_HOME, PATH, build_apk.sh script

## Knowledge Gaps
- **3 isolated node(s):** `build_apk.sh script`, `JAVA_HOME`, `PATH`
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 10 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **6 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MainActivity` connect `MainActivity` to `AntigravityWebViewClient`, `AntigravityNativeBridge`, `android.content.Intent`, `.onCreate`, `NetworkStateListener`, `android.webkit.WebView`, `Override`, `.performSmartReconnect`?**
  _High betweenness centrality (0.416) - this node is a cross-community bridge._
- **Why does `KeepAliveService` connect `KeepAliveService` to `android.content.Intent`, `.buildNotification`, `KeepAliveService.java`, `NetworkStateListener`?**
  _High betweenness centrality (0.257) - this node is a cross-community bridge._
- **Why does `NetworkStateListener` connect `NetworkStateListener` to `KeepAliveService`, `MainActivity`?**
  _High betweenness centrality (0.171) - this node is a cross-community bridge._
- **What connects `build_apk.sh script`, `JAVA_HOME`, `PATH` to the rest of the system?**
  _3 weakly-connected nodes found - possible documentation gaps or missing edges._