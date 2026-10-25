# Graph Report - antigravity-mobile-pro  (2026-10-25)

## Corpus Check
- 3 files · ~4,105 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 75 nodes · 145 edges · 12 communities (7 shown, 5 thin omitted)
- Extraction: 100% EXTRACTED · 0% INFERRED · 0% AMBIGUOUS
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Override
- AntigravityNativeBridge
- KeepAliveService
- android.content.Intent
- MainActivity
- MainActivity.java
- NetworkStateListener
- .configureWebView
- .onShowFileChooser
- build_apk.sh
- .buildNotification
- KeepAliveService.java

## God Nodes (most connected - your core abstractions)
1. `MainActivity` - 26 edges
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

## Communities (12 total, 5 thin omitted)

### Community 0 - "Override"
Cohesion: 0.16
Nodes (8): android.graphics.Bitmap, android.webkit.WebResourceError, android.webkit.WebResourceRequest, android.webkit.WebView, android.webkit.WebViewClient, AntigravityWebViewClient, Intent, Override

### Community 2 - "KeepAliveService"
Cohesion: 0.39
Nodes (4): NetworkCallback, KeepAliveService, WakeLock, WifiLock

### Community 3 - "android.content.Intent"
Cohesion: 0.33
Nodes (3): android.content.Intent, android.os.IBinder, Override

### Community 5 - "MainActivity.java"
Cohesion: 0.40
Nodes (4): android.app.Activity, android.os.Bundle, android.os.Handler, android.widget.ProgressBar

### Community 7 - ".configureWebView"
Cohesion: 0.50
Nodes (3): android.annotation.SuppressLint, android.webkit.WebChromeClient, AntigravityWebChromeClient

### Community 8 - ".onShowFileChooser"
Cohesion: 0.50
Nodes (3): android.net.Uri, android.webkit.ValueCallback, FileChooserParams

### Community 9 - "build_apk.sh"
Cohesion: 0.50
Nodes (3): JAVA_HOME, PATH, build_apk.sh script

## Knowledge Gaps
- **3 isolated node(s):** `build_apk.sh script`, `JAVA_HOME`, `PATH`
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 10 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **5 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MainActivity` connect `MainActivity` to `Override`, `AntigravityNativeBridge`, `android.content.Intent`, `MainActivity.java`, `NetworkStateListener`, `.configureWebView`, `.onShowFileChooser`?**
  _High betweenness centrality (0.382) - this node is a cross-community bridge._
- **Why does `KeepAliveService` connect `KeepAliveService` to `android.content.Intent`, `.buildNotification`, `KeepAliveService.java`, `NetworkStateListener`?**
  _High betweenness centrality (0.268) - this node is a cross-community bridge._
- **Why does `NetworkStateListener` connect `NetworkStateListener` to `KeepAliveService`, `MainActivity`?**
  _High betweenness centrality (0.169) - this node is a cross-community bridge._
- **What connects `build_apk.sh script`, `JAVA_HOME`, `PATH` to the rest of the system?**
  _3 weakly-connected nodes found - possible documentation gaps or missing edges._