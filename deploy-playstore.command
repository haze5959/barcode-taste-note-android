#!/bin/bash
set -e

# 스크립트 위치(= 프로젝트 루트)로 이동. (iOS deploy-appstore.command 와 동일 패턴)
cd "$(dirname "$0")"

echo "=========================================="
echo "    BarNote Play Store Deployment Script  "
echo "=========================================="

GRADLE_FILE="app/build.gradle.kts"
LOCAL_PROPS="local.properties"
PACKAGE_NAME="com.oq.barnote"

# ── Gradle 실행기 결정 (gradlew 우선, 없으면 시스템 gradle) ─────────────
if [ -x "./gradlew" ]; then
    GRADLE="./gradlew"
elif command -v gradle >/dev/null 2>&1; then
    GRADLE="gradle"
else
    echo "❌ gradlew / gradle 둘 다 없습니다."
    echo "   → 'gradle wrapper --gradle-version 8.9' 로 래퍼를 생성하거나 Gradle 을 설치하세요."
    exit 1
fi
echo "ℹ️  Gradle 실행기: $GRADLE"

# local.properties 의 key=value 읽기 헬퍼
prop() { grep -E "^$1=" "$LOCAL_PROPS" 2>/dev/null | head -1 | cut -d'=' -f2- | tr -d '\r'; }

# ── 1) 배포 사전 점검: 필요한 키/파일 ───────────────────────────────────
echo ""
echo "🔍 배포 사전 점검..."
MISSING=0
note() { echo "   ⚠️  $1"; MISSING=1; }

[ -f "$GRADLE_FILE" ] || { echo "❌ $GRADLE_FILE 없음. 프로젝트 루트에서 실행하세요."; exit 1; }
[ -f "app/google-services.json" ] || note "app/google-services.json 없음 (Firebase 콘솔에서 다운로드)."

# 릴리스 업로드 키스토어 (local.properties)
STORE_FILE="$(prop release.storeFile)"
if [ -z "$STORE_FILE" ]; then
    note "릴리스 키스토어 미설정 (local.properties: release.storeFile / storePassword / keyAlias / keyPassword)."
elif [ ! -f "$STORE_FILE" ]; then
    note "릴리스 키스토어 파일을 찾을 수 없음: $STORE_FILE"
fi

# Play 서비스 계정 JSON
SA_FILE="$(prop play.serviceAccountFile)"; SA_FILE="${SA_FILE:-play-service-account.json}"
[ -f "$SA_FILE" ] || note "Play 서비스 계정 JSON 없음: $SA_FILE"

if [ "$MISSING" -ne 0 ]; then
    cat <<EOF

──────────────────────────────────────────────────────────────
❌ 아직 자동 업로드 준비가 안 됐습니다. 아래 키만 등록하면 동작합니다.
──────────────────────────────────────────────────────────────
[A] 업로드 키스토어 생성 (최초 1회, 안전한 곳에 백업):
    keytool -genkeypair -v -keystore upload-keystore.jks \\
        -alias upload -keyalg RSA -keysize 2048 -validity 10000

[B] local.properties 에 추가 (gitignore 처리됨):
    release.storeFile=upload-keystore.jks
    release.storePassword=<키스토어 비밀번호>
    release.keyAlias=upload
    release.keyPassword=<키 비밀번호>
    play.serviceAccountFile=play-service-account.json
    play.track=internal        # internal/alpha/beta/production (생략 시 internal)

[C] Play Console / Google Cloud:
    1. Play Console 에서 앱 생성 (패키지명: $PACKAGE_NAME)
    2. 설정 → API 액세스 → 서비스 계정 연결 + '출시 관리자' 권한 부여
    3. 해당 서비스 계정의 JSON 키 발급 → 프로젝트 루트에 $SA_FILE 로 저장
    4. 새 앱은 첫 AAB 1개를 Play Console 에서 수동 업로드해야 트랙이 열립니다
       (이후부터 이 스크립트로 자동 업로드).
──────────────────────────────────────────────────────────────
EOF
    exit 1
fi
echo "✅ 사전 점검 통과."

# ── 2) 버전 증가 (iOS deploy-appstore.command 와 동일한 모드) ───────────
#   인자: --major / --minor / --patch(기본) / --none
OLD_CODE="$(grep -Eo 'versionCode = [0-9]+' "$GRADLE_FILE" | grep -Eo '[0-9]+' | head -1)"
OLD_NAME="$(grep -Eo 'versionName = "[0-9.]+"' "$GRADLE_FILE" | cut -d '"' -f2 | head -1)"
if [ -z "$OLD_CODE" ] || [ -z "$OLD_NAME" ]; then
    echo "❌ $GRADLE_FILE 에서 versionCode/versionName 을 찾지 못했습니다."
    exit 1
fi
echo ""
echo "현재 버전: $OLD_NAME (code $OLD_CODE)"

IFS='.' read -r major minor patch <<< "$OLD_NAME"
major=${major:-1}; minor=${minor:-0}; patch=${patch:-0}

case "$1" in
    --major) major=$((major+1)); minor=0; patch=0; echo "모드: Major Version Update";;
    --minor) minor=$((minor+1)); patch=0; echo "모드: Minor Version Update";;
    --none)  echo "모드: 버전 유지 (재시도용)";;
    *)       patch=$((patch+1)); echo "모드: Patch Version Update (기본)";;
esac

if [ "$1" != "--none" ]; then
    NEW_NAME="${major}.${minor}.${patch}"
    NEW_CODE=$((OLD_CODE+1))
    # macOS BSD sed
    sed -i '' "s/versionName = \"[0-9.]*\"/versionName = \"$NEW_NAME\"/" "$GRADLE_FILE"
    sed -i '' "s/versionCode = [0-9]*/versionCode = $NEW_CODE/" "$GRADLE_FILE"
    echo "✅ 새 버전: $NEW_NAME (code $NEW_CODE) — $GRADLE_FILE 갱신됨."
else
    NEW_NAME="$OLD_NAME"; NEW_CODE="$OLD_CODE"
    echo "ℹ️  Play 는 동일 versionCode 재업로드를 거부합니다. --none 은 빌드/업로드 재시도 전용."
fi

# ── 3) 릴리스 AAB 빌드 + Play 업로드 (Gradle Play Publisher) ────────────
TRACK="$(prop play.track)"; TRACK="${TRACK:-internal}"
echo ""
echo "🏗️  릴리스 AAB 빌드 + Play Console 업로드 중...  (track: $TRACK)"
echo "    ($GRADLE clean publishReleaseBundle)"

# publishReleaseBundle = bundleRelease(서명된 AAB) 빌드 후 Play 트랙에 업로드 (GPP).
"$GRADLE" clean publishReleaseBundle

echo ""
echo "=========================================="
echo "🎉 Play Console 업로드 완료!"
echo "버전 $NEW_NAME (code $NEW_CODE) 가 '$TRACK' 트랙에서 처리/검토 중입니다."
echo "=========================================="
