#!/usr/bin/env python3
"""JUnit XML の結果を Markdown に要約する。常に exit 0。

使い方: summarize_tests.py <結果XMLのglob> <出力ファイル> <ラベル>
"""
import glob
import sys
import xml.etree.ElementTree as ET


def main():
    pattern = sys.argv[1] if len(sys.argv) > 1 else ""
    out = sys.argv[2] if len(sys.argv) > 2 else "summary.md"
    label = sys.argv[3] if len(sys.argv) > 3 else ""
    lines = []
    try:
        files = sorted(glob.glob(pattern, recursive=True))
        total = failed = errors = skipped = 0
        problems = []
        parsed = 0
        for f in files:
            try:
                root = ET.parse(f).getroot()
            except Exception as e:
                lines.append("XML解析失敗: `%s` (%s)" % (f, e))
                continue
            parsed += 1
            for tc in root.iter("testcase"):
                total += 1
                name = "%s.%s" % (tc.get("classname", ""), tc.get("name", ""))
                for kind in ("failure", "error"):
                    el = tc.find(kind)
                    if el is not None:
                        if kind == "failure":
                            failed += 1
                        else:
                            errors += 1
                        msg = (el.get("message") or el.text or "").strip()
                        msg = " ".join(msg.split())[:200]
                        problems.append((kind, name, msg))
                        break
                else:
                    if tc.find("skipped") is not None:
                        skipped += 1
        if parsed == 0:
            lines.append("テスト結果XMLが見つかりませんでした (%s)" % pattern)
        else:
            passed = total - failed - errors - skipped
            lines.append("### %s" % label)
            lines.append("")
            lines.append("| 合計 | 成功 | 失敗 | エラー | スキップ |")
            lines.append("|---|---|---|---|---|")
            lines.append("| %d | %d | %d | %d | %d |" % (total, passed, failed, errors, skipped))
            if problems:
                lines.append("")
                lines.append("#### 失敗・エラーしたテスト")
                for kind, name, msg in problems:
                    lines.append("- [%s] `%s`: %s" % (kind, name, msg))
    except Exception as e:
        lines.append("要約の生成中に例外: %s" % e)
    try:
        with open(out, "w", encoding="utf-8") as fh:
            fh.write("\n".join(lines) + "\n")
    except Exception as e:
        print("write failed: %s" % e)


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print("error: %s" % e)
    sys.exit(0)
