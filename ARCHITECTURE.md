# 🏛️ Documentação de Engenharia e Arquitetura: Antigravity Mobile Pro

Este documento detalha as decisões técnicas, restrições do kernel Linux/Android, protocolos de rede e soluções de engenharia implementadas no **Antigravity Mobile Pro**.

---

## 1. Análise da Causa Raiz: Por que a WebAPK Original Falha?

O aplicativo oficial móvel do Antigravity é uma **Trusted Web Activity (TWA) / WebAPK** orquestrada pelo Google Chrome. Em sistemas operacionais com gerenciamento de energia agressivo (como a Samsung One UI no Android 14), essa arquitetura apresenta quatro pontos de falha fatais:

### A. Suspensão de Abas e Congelamento de Timers
O Chromium mobile implementa economia agressiva de CPU:
* Quando a tela desliga ou o app perde o foco, o renderer do Chromium limita a execução de timers JavaScript (`setTimeout`, `setInterval`) para no máximo **1 execução a cada 60 segundos**, congelando em seguida.
* O heartbeat interno do WebSocket do Antigravity (que opera com timeouts de 15 a 30 segundos) deixa de responder às mensagens de ping/pong do servidor de relay do Google (`daily-cloudcode-pa.googleapis.com`).
* O servidor remoto encerra o socket por timeout (`close code 1006` / `1001`).

### B. O Problema da Detecção de Queda (`navigator.onLine`)
O frontend web do Google Antigravity utiliza o evento padrão da Web API:
```javascript
window.addEventListener('online', () => reconnect());
```
Quando o socket é encerrado por inatividade ou timeout de NAT, a interface de rede física (Wi-Fi ou 4G) **continua conectada**. Logo:
* O evento `offline` nunca é disparado pelo sistema operacional.
* O evento `online` consequentemente **nunca é emitido**.
* O app entra em estado de deadlock: a interface exibe *"Instance disconnected - Waiting for connection or Wi-Fi to resume..."* e o usuário é forçado a matar o processo no gerenciador de tarefas.

### C. Descarte de Tabelas NAT de Operadoras Celulares (RRC Idle State)
Em redes móveis LTE e 5G, as operadoras aplicam regras de economia de espectro de rádio:
* Se um dispositivo passa 15 a 30 segundos sem transmitir pacotes, a estação rádio-base (ERB) comuta o modem do aparelho do estado `RRC_CONNECTED` para `RRC_IDLE`.
* Roteadores NAT e firewalls de borda descartam a entrada de tradução de portas para conexões TCP sem tráfego bidirecional.
* Quando o servidor do Google tenta enviar um evento de IA em background, os pacotes são descartados e a conexão TCP morre em silêncio.

---

## 2. Soluções Implementadas no Antigravity Mobile Pro

### A. Foreground Service e Locks de Hardware

O [`KeepAliveService.java`](src/com/mateuscelestino/antigravitymobile/KeepAliveService.java) opera como um serviço de sistema contínuo vinculado à `MainActivity`:

```java
// Aquisição de trava de alta performance para o driver Wi-Fi
wifiLock = wifiManager.createWifiLock(
    WifiManager.WIFI_MODE_FULL_HIGH_PERF,
    "AntigravityMobile::PersistentWifiLock"
);
wifiLock.setReferenceCounted(false);
wifiLock.acquire();

// Prevenção de suspensão de CPU pelo kernel
wakeLock = powerManager.newWakeLock(
    PowerManager.PARTIAL_WAKE_LOCK,
    "AntigravityMobile::PersistentWakeLock"
);
wakeLock.setReferenceCounted(false);
wakeLock.acquire();
```

* **`WIFI_MODE_FULL_HIGH_PERF`**: Desativa o modo 802.11 Power Save Poll (PSP) na placa de rede do smartphone, garantindo que o modem não adormeça entre intervalos DTIM de beacons do roteador.
* **`PARTIAL_WAKE_LOCK`**: Garante que o clock do processador continue ativo para responder imediatamente a eventos de rede recebidos.

### B. Interceptação e Watchdog de WebSocket na Camada Web

A [`MainActivity.java`](src/com/mateuscelestino/antigravitymobile/MainActivity.java) injeta um sentinela no contexto de execução do WebView via interface `@JavascriptInterface`:

```javascript
// Interceptação transparente do construtor WebSocket
if (window.WebSocket) {
  const OriginalWS = window.WebSocket;
  window.WebSocket = function(...args) {
    const ws = new OriginalWS(...args);
    ws.addEventListener('close', function(e) {
      if (window.AntigravityNative) {
        window.AntigravityNative.onSocketClosed(e.code);
      }
    });
    return ws;
  };
  window.WebSocket.prototype = OriginalWS.prototype;
}
```

Quando um fechamento anormal (códigos `1006`, `1001` ou `1005`) é detectado, a ponte nativa dispara o método:
```java
@JavascriptInterface
public void onSocketClosed(int code) {
    handler.postDelayed(() -> performSmartReconnect(false), 800);
}
```

Adicionalmente, um scanner de mutação de DOM busca botões de retry antes de qualquer recarregamento pesado:
```javascript
const buttons = document.querySelectorAll('button, [role="button"]');
for (let i = 0; i < buttons.length; i++) {
  const label = (buttons[i].innerText || buttons[i].getAttribute('aria-label') || '').toLowerCase();
  if (label.includes('reconnect') || label.includes('retry') || label.includes('reconectar')) {
    buttons[i].click();
    return;
  }
}
```

### C. Manutenção do Rádio Ativo (Stealth Keep-Alive)

Para prevenir o descarte de rotas NAT pelas operadoras de telefonia e roteadores Wi-Fi sem inflar o tráfego do WebSocket da aplicação, implementamos um beacon assíncrono via JavaScript:

```java
private void startStealthRadioKeepAlive() {
    handler.postDelayed(new Runnable() {
        @Override
        public void run() {
            if (webView != null && isNetworkConnected()) {
                String keepAliveJs = "fetch('https://antigravity.google.com/favicon.ico', { mode: 'no-cors', cache: 'no-store' }).catch(function(){});";
                webView.evaluateJavascript(keepAliveJs, null);
            }
            handler.postDelayed(this, 18000); // 18 segundos
        }
    }, 10000);
}
```

* O endpoint `/favicon.ico` reside na mesma infraestrutura Google Cloud Edge (Anycast IP).
* Ao transmitir um pacote TCP com flag SYN/ACK a cada 18 segundos, o hardware de rádio permanece em `RRC_CONNECTED`, eliminando a latência de 500 ms de despertar do rádio e renovando a tabela NAT do firewall.

### D. Bypass de 403 Disallowed User-Agent e Isolamento de Sessão

O Google Identity bloqueia tentativas de autenticação OAuth em WebViews que incluam a flag `; wv`:

```java
String defaultUA = settings.getUserAgentString();
String sanitizedUA = defaultUA.replace("; wv", "");
settings.setUserAgentString(sanitizedUA);
```

Com essa higienização simples:
1. O WebView se apresenta com o cabeçalho do Google Chrome Mobile legítimo.
2. O Google autoriza o fluxo de autenticação OAuth sem exigir o uso do Custom Tabs externo.
3. O [`SessionMigrator.java`](src/com/mateuscelestino/antigravitymobile/SessionMigrator.java) injeta os 20 tokens de autenticação direto no `CookieManager`, garantindo que o app inicie já autenticado na conta Pro sob `/u/1/`.

---

## 3. Especificação do Pipeline de Compilação

Diferente de projetos típicos que dependem de dezenas de megabytes do Gradle wrapper e centenas de dependências de bibliotecas AndroidX, este projeto adota uma arquitetura de compilação pura e direta:

```bash
# 1. Compilação de Recursos
$BUILD_TOOLS/aapt2 compile --dir res -o build/compiled_res.zip

# 2. Ligação do APK e geração de R.java
$BUILD_TOOLS/aapt2 link -I "$ANDROID_JAR" \
  --manifest AndroidManifest.xml \
  -o build/app-res.apk \
  -A assets \
  build/compiled_res.zip \
  --java build/gen

# 3. Compilação Java 17
javac -cp "$ANDROID_JAR:build/gen" \
  -d build/classes \
  build/gen/com/mateuscelestino/antigravitymobile/R.java \
  src/com/mateuscelestino/antigravitymobile/*.java

# 4. Geração de Bytecode Dalvik (classes.dex)
$BUILD_TOOLS/d8 --output build/dex --lib "$ANDROID_JAR" $(find build/classes -name "*.class")

# 5. Empacotamento, Alinhamento de Memória e Assinatura Digital
$BUILD_TOOLS/zipalign -p -f -v 4 build/app-with-dex.apk dist/antigravity-mobile-pro-aligned.apk
$BUILD_TOOLS/apksigner sign --ks antigravity.keystore --out dist/antigravity-mobile-pro.apk dist/antigravity-mobile-pro-aligned.apk
```

* **Tempo de build:** ~3 segundos.
* **Tamanho final:** ~73 KB (contra 50+ MB de apps gerados por frameworks híbridos).
* **Consumo de memória RAM:** Menos de 40 MB em repouso.
