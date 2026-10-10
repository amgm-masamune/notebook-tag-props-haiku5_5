#!/usr/bin/env bash
# CI が撮ったスクリーンショット（PNG）を ci-screenshots ブランチへ push し、
# コミットコメントに載せる Markdown（画像リンク）を出力する。
#
# 使い方: publish_screenshots.sh <PNG を探すディレクトリ> <owner/repo> <sha7> <api-level> <出力Markdown>
#
# 配置: ci-screenshots:{sha7}/api{level}/{name}.png
# 環境変数:
#   GITHUB_TOKEN  push 用トークン（必須。認証は http.extraheader で渡し、URL には含めない）
#   REMOTE_URL    push 先の URL（省略時 https://github.com/<owner/repo>.git。ローカル検証用）
#
# PNG が無い場合は何もせず exit 0。push に失敗しても exit 1 で返す（呼び出し側で continue-on-error する）。
set -uo pipefail

SRC_DIR="${1:?PNG のディレクトリを指定}"
REPO="${2:?owner/repo を指定}"
SHA7="${3:?sha7 を指定}"
LEVEL="${4:?api-level を指定}"
OUT_MD="${5:?出力 Markdown のパスを指定}"

BRANCH="ci-screenshots"
DEST="${SHA7}/api${LEVEL}"
REMOTE="${REMOTE_URL:-https://github.com/${REPO}.git}"

# 後で作業ディレクトリを移動するため、入力・出力のパスは絶対パスに直しておく
OUT_MD="$(realpath -m "$OUT_MD")"
SRC_ABS="$(cd "$SRC_DIR" 2>/dev/null && pwd)"
: > "$OUT_MD"

mapfile -t PNGS < <(if [ -n "$SRC_ABS" ]; then find "$SRC_ABS" -type f -name '*.png' | sort; fi)
if [ "${#PNGS[@]}" -eq 0 ]; then
  echo "スクリーンショットが見つからないためスキップ: ${SRC_DIR}"
  exit 0
fi
echo "スクリーンショット ${#PNGS[@]} 枚を ${BRANCH}:${DEST} へ配置します"

# トークンは環境変数経由の extraheader で渡す（コマンドラインや URL に残さない）
if [ -n "${GITHUB_TOKEN:-}" ]; then
  AUTH_B64="$(printf 'x-access-token:%s' "$GITHUB_TOKEN" | base64 -w0)"
  export GIT_CONFIG_COUNT=1
  export GIT_CONFIG_KEY_0="http.https://github.com/.extraheader"
  export GIT_CONFIG_VALUE_0="AUTHORIZATION: basic ${AUTH_B64}"
fi
export GIT_TERMINAL_PROMPT=0

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
cd "$WORK" || exit 1

# 初回: ブランチが無ければ orphan で新規作成（既存ブランチは浅いクローンで取得）
if git clone -q --depth 1 --branch "$BRANCH" "$REMOTE" shots 2>/dev/null; then
  :
else
  rm -rf shots
  mkdir shots && cd shots || exit 1
  git init -q
  git checkout -q --orphan "$BRANCH"
  git remote add origin "$REMOTE"
fi
cd "$WORK/shots" || exit 1

publish_once() {
  # 常に最新のリモート状態の上に積む（pull --rebase 相当）。リモートに無ければ現状（orphan）のまま。
  if git fetch -q --depth 1 origin "$BRANCH" 2>/dev/null; then
    git checkout -q -f -B "$BRANCH" FETCH_HEAD || return 1
  fi
  rm -rf "$DEST"
  mkdir -p "$DEST"
  for p in "${PNGS[@]}"; do
    cp "$p" "$DEST/$(basename "$p")"
  done
  git add -A "$DEST" || return 1
  if git diff --cached --quiet; then
    echo "変更なし（既に配置済み）"
    return 0
  fi
  git -c user.name="github-actions[bot]" \
      -c user.email="41898282+github-actions[bot]@users.noreply.github.com" \
      commit -q -m "screenshots ${SHA7} api${LEVEL}" || return 1
  # 強制 push はしない。競合したら呼び出し側でリトライする
  git push -q origin "HEAD:refs/heads/${BRANCH}"
}

PUSHED=0
for attempt in 1 2 3 4 5; do
  if publish_once; then
    PUSHED=1
    break
  fi
  echo "push に失敗（試行 ${attempt}/5）。リモートを取り直して再試行します"
  sleep $((attempt + RANDOM % 3))
done

if [ "$PUSHED" -ne 1 ]; then
  echo "ci-screenshots への push に 5 回失敗しました"
  exit 1
fi

# コミットコメント用の画像リンク一覧
for p in "${PNGS[@]}"; do
  name="$(basename "$p" .png)"
  echo "![${name}](https://raw.githubusercontent.com/${REPO}/${BRANCH}/${DEST}/${name}.png)" >> "$OUT_MD"
  echo "" >> "$OUT_MD"
done
echo "画像リンクを ${OUT_MD} に出力しました"
exit 0
