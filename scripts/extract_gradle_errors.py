#!/usr/bin/env python3
"""Gradle ログからエラーらしき行を抜き出し、Markdown のコードブロックで出力する。常に exit 0。

使い方: extract_gradle_errors.py <ログファイル> [出力ファイル]
出力ファイルを省略すると標準出力へ書く。
"""
import os
import re
import sys

MAX_LINES = 80
MAX_CHARS = 300
STACK_AFTER_CRASH = 8
WHAT_WENT_WRONG_MAX_LINES = 30


def extract(lines):
    picked = []
    seen = set()

    def add(text):
        text = text.rstrip()
        if not text.strip():
            return
        text = text[:MAX_CHARS]
        if text in seen:
            return
        seen.add(text)
        picked.append(text)

    what_went_wrong = False
    block_left = 0
    crash_left = 0
    for line in lines:
        text = line.rstrip("\r\n")

        if what_went_wrong:
            # 次の「* Try:」などの見出し、または上限行数で区切る
            if text.startswith("* ") or block_left <= 0:
                what_went_wrong = False
            else:
                add(text)
                block_left -= 1

        if text.startswith("e: "):
            add(text)
        elif "error:" in text:
            add(text)
        elif re.search(r"> Task .* FAILED", text):
            add(text)
        elif text.startswith("* What went wrong:"):
            add(text)
            what_went_wrong = True
            block_left = WHAT_WENT_WRONG_MAX_LINES
        elif text.startswith("FAILURE:"):
            add(text)
        elif "Execution failed for task" in text:
            add(text)
        elif "INSTALL_FAILED" in text:
            add(text)

        if "AndroidRuntime" in text or "FATAL EXCEPTION" in text:
            add(text)
            crash_left = STACK_AFTER_CRASH
        elif crash_left > 0:
            add(text)
            crash_left -= 1

    return picked


def render(log_path):
    if not os.path.isfile(log_path):
        return "Gradle ログがありません"
    with open(log_path, encoding="utf-8", errors="replace") as fh:
        lines = fh.readlines()
    picked = extract(lines)
    if not picked:
        return "該当するエラー行は見つかりませんでした"

    shown = picked[:MAX_LINES]
    longest = max((len(m) for m in re.findall(r"`+", "\n".join(shown))), default=0)
    fence = "`" * max(3, longest + 1)
    out = [fence + "text"] + shown + [fence]
    if len(picked) > MAX_LINES:
        out.append("")
        out.append("（%d 行まで表示。残り %d 行は省略。全文はアーティファクトの Gradle ログを参照）"
                   % (MAX_LINES, len(picked) - MAX_LINES))
    return "\n".join(out)


def main():
    log_path = sys.argv[1] if len(sys.argv) > 1 else ""
    out = sys.argv[2] if len(sys.argv) > 2 else ""
    text = render(log_path) + "\n"
    if out:
        with open(out, "w", encoding="utf-8") as fh:
            fh.write(text)
    else:
        sys.stdout.write(text)


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print("error: %s" % e)
    sys.exit(0)
