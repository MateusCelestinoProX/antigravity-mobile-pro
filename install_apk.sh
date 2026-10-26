#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APK_PATH="$SCRIPT_DIR/dist/antigravity-mobile-pro.apk"

if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK não encontrado em $APK_PATH. Execute ./build_apk.sh primeiro."
    exit 1
fi

echo "=========================================================="
echo "📲 INSTALANDO ANTIGRAVITY MOBILE PRO NO DISPOSITIVO"
echo "=========================================================="

DEVICE=$(adb devices | grep -E 'device$' | head -n 1 | awk '{print $1}')

if [ -z "$DEVICE" ]; then
    echo "❌ Nenhum dispositivo conectado via ADB."
    exit 1
fi

echo "Dispositivo alvo: $DEVICE"
adb -s "$DEVICE" install -r "$APK_PATH"

echo "Aplicando isenções de bateria e permissões..."
adb -s "$DEVICE" shell dumpsys deviceidle whitelist +com.mateuscelestino.antigravitymobile
adb -s "$DEVICE" shell cmd appops set com.mateuscelestino.antigravitymobile RUN_IN_BACKGROUND allow
adb -s "$DEVICE" shell cmd appops set com.mateuscelestino.antigravitymobile RUN_ANY_IN_BACKGROUND allow
adb -s "$DEVICE" shell cmd appops set com.mateuscelestino.antigravitymobile POST_NOTIFICATION allow
adb -s "$DEVICE" shell settings put global wifi_power_save 0
adb -s "$DEVICE" shell settings put global mobile_data_always_on 1

echo "Iniciando aplicativo..."
adb -s "$DEVICE" shell am start -n com.mateuscelestino.antigravitymobile/.MainActivity

echo "✅ Instalação e blindagem de conexão concluídas com sucesso!"
