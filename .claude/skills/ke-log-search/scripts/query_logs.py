#!/usr/bin/env python3
"""Query KE internal Kibana (/internal/search/es) logs.

Read-only. Sends HTTPS POST with the user's login Cookie, prints the raw
JSON response to stdout. Does not persist responses or credentials.

Usage:
  query_logs.py --query '...' --start 'ISO8601' --end 'ISO8601' [--index IDX]
                [--size N] [--url URL] [--cookie-file F] [--dry-run]
  query_logs.py --id '<search id>' [--url URL] [--cookie-file F]
"""

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request

DEFAULT_INDEX = "index-10726-10334*"
TIME_FIELD = "timestamp"
MAX_SIZE = 500
DEFAULT_SIZE = 100

# ISO 8601 local time with explicit timezone (Z or +/-HH:MM)
TZ_RE = re.compile(
    r"^\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}(:\d{2}(\.\d+)?)?(Z|[+-]\d{2}:\d{2})$"
)


def die(msg, code=2):
    print(msg, file=sys.stderr)
    sys.exit(code)


def load_cookie(cookie_file):
    if cookie_file:
        path = os.path.expanduser(cookie_file)
        try:
            with open(path, "r", encoding="utf-8") as f:
                cookie = f.read().strip()
        except OSError as e:
            die("无法读取 cookie 文件: %s" % e)
        if not cookie:
            die("cookie 文件为空: %s" % path)
        if cookie.lower().startswith("cookie:"):
            die("cookie 文件不应包含 'Cookie:' 前缀，只存值")
        return cookie
    cookie = os.environ.get("KE_LOG_COOKIE", "").strip()
    if not cookie:
        die(
            "未提供凭据。请用 --cookie-file 指定文件，或设置 KE_LOG_COOKIE 环境变量。\n"
            "尚未配置时，可在本地终端执行（不回显输入）：\n"
            "  python3 -c 'import getpass, os; from pathlib import Path; "
            'p=Path.home()/".config/ke-log-search/cookie"; '
            "p.parent.mkdir(parents=True, exist_ok=True); "
            "fd=os.open(p, os.O_WRONLY|os.O_CREAT|os.O_TRUNC, 0o600); "
            "os.fchmod(fd, 0o600); f=os.fdopen(fd,\"w\"); "
            "f.write(getpass.getpass(\"Kibana Cookie: \")); f.close()'"
        )
    return cookie


def build_es_body(query, start, end, size):
    return {
        "size": size,
        "query": {
            "bool": {
                "must": [{"query_string": {"query": query, "analyze_wildcard": True}}],
                "filter": [
                    {
                        "range": {
                            TIME_FIELD: {
                                "gte": start,
                                "lte": end,
                                "format": "strict_date_optional_time",
                            }
                        }
                    }
                ],
            }
        },
        "sort": [{TIME_FIELD: {"order": "desc", "unmapped_type": "boolean"}}],
        "docvalue_fields": [{"field": TIME_FIELD, "format": "date_time"}],
        "stored_fields": "*",
        "script_fields": {},
        "_source": {"excludes": []},
    }


def build_payload(args):
    if args.id:
        return {"id": args.id, "serverStrategy": "es"}
    return {
        "params": {
            "index": args.index,
            "body": build_es_body(args.query, args.start, args.end, args.size),
            "ignore_unavailable": True,
            "allow_no_indices": True,
            "search_type": "query_then_fetch",
        },
        "serverStrategy": "es",
    }


def main():
    p = argparse.ArgumentParser(description="Query KE Kibana logs (read-only).")
    p.add_argument("--query", help="Elasticsearch query_string 查询条件")
    p.add_argument("--start", help="起始时间，ISO 8601 带时区，如 2026-10-21T14:00:00+08:00")
    p.add_argument("--end", help="结束时间，ISO 8601 带时区")
    p.add_argument("--index", default=DEFAULT_INDEX, help="索引模式（默认 %s）" % DEFAULT_INDEX)
    p.add_argument("--size", type=int, default=DEFAULT_SIZE, help="返回条数（默认 100，最多 %d）" % MAX_SIZE)
    p.add_argument("--id", help="按已有搜索 ID 获取结果（轮询用）")
    p.add_argument("--url", default=os.environ.get("KE_LOG_KIBANA_URL", ""),
                   help="Kibana 基地址，如 https://kibana.example.ke.com（也可用 KE_LOG_KIBANA_URL 环境变量）")
    p.add_argument("--cookie-file", help="存放 Cookie 值的 UTF-8 文件（不含 'Cookie:' 前缀）")
    p.add_argument("--dry-run", action="store_true", help="只打印请求体，不发送请求")
    args = p.parse_args()

    if args.id:
        pass  # follow-up request needs no query/time
    else:
        missing = [n for n, v in (("--query", args.query), ("--start", args.start), ("--end", args.end)) if not v]
        if missing:
            die("缺少参数: %s（或使用 --id 获取已有搜索结果）" % ", ".join(missing))
        for name, val in (("--start", args.start), ("--end", args.end)):
            if not TZ_RE.match(val):
                die("%s 必须是带时区的 ISO 8601（如 2026-10-21T14:00:00+08:00），收到: %s" % (name, val))
        if not (1 <= args.size <= MAX_SIZE):
            die("--size 必须在 1..%d 之间" % MAX_SIZE)

    url = args.url.rstrip("/")
    if not url:
        die("缺少 Kibana 地址：请用 --url 或设置 KE_LOG_KIBANA_URL 环境变量")
    endpoint = url + "/internal/search/es"

    payload = build_payload(args)

    if args.dry_run:
        print(json.dumps({"endpoint": endpoint, "payload": payload}, ensure_ascii=False, indent=2))
        return

    cookie = load_cookie(args.cookie_file)
    req = urllib.request.Request(
        endpoint,
        data=json.dumps(payload).encode("utf-8"),
        method="POST",
        headers={
            "Content-Type": "application/json",
            "kbn-xsrf": "true",
            "Accept": "application/json",
            "Cookie": cookie,
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            final_url = resp.geturl()
            body = resp.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        detail = ""
        try:
            detail = e.read().decode("utf-8", errors="replace")[:500]
        except Exception:
            pass
        if e.code in (401, 403):
            die("认证失败 (HTTP %d)：Cookie 可能已失效，请重新登录后更新凭据文件。%s" % (e.code, detail), 3)
        die("HTTP %d: %s" % (e.code, detail), 3)
    except urllib.error.URLError as e:
        die("连接失败（请检查内网/VPN 与 Kibana 地址）: %s" % e.reason, 3)

    if "/login" in final_url or final_url.rstrip("/") != endpoint:
        die("请求被重定向到 %s，可能需要重新登录刷新 Cookie" % final_url, 3)

    # stdout: raw JSON response, untouched
    print(body)


if __name__ == "__main__":
    main()
