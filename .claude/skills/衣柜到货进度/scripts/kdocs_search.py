#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
衣柜材料进度 Excel 检索脚本。
读取定时下载的"衣柜到货进度" Excel，支持关键词搜索前两个工作表。

用法:
  python3 kdocs_search.py '{"action":"list_sheets"}'
  python3 kdocs_search.py '{"action":"search","query":"8250420","max_results":10}'
"""

import sys
import json
import os
import re
import select
from typing import Dict, Any, List

try:
    import openpyxl
    HAS_OPENPYXL = True
except ImportError:
    HAS_OPENPYXL = False

# Excel 路径：优先从环境变量读取，否则用默认路径
EXCEL_PATH = os.environ.get("KDOCS_EXCEL_PATH", "/pvc/tmp/衣柜到货.xlsx")
# 只查前两个 sheet
SHEET_LIMIT = 3


def _match(text: str, keywords: List[str]) -> bool:
    """判断 text 是否包含任一关键词（不区分大小写）"""
    text_lower = text.lower()
    return any(kw.lower() in text_lower for kw in keywords)


def _load_sheets(excel_path: str, limit: int = 0):
    """加载 sheet 数据，limit=0 表示全部（仅可见 sheet）"""
    wb = openpyxl.load_workbook(excel_path, read_only=True, data_only=True)
    sheets = {}
    visible = [n for n in wb.sheetnames if wb[n].sheet_state == 'visible']
    names = visible[:limit] if limit > 0 else visible
    for sheet_name in names:
        ws = wb[sheet_name]
        rows = []
        headers = []
        for i, row in enumerate(ws.iter_rows(values_only=True)):
            if i == 0:
                headers = [str(c).strip() if c is not None else "" for c in row]
                continue
            rows.append([str(c).strip() if c is not None else "" for c in row])
        sheets[sheet_name] = {"headers": headers, "rows": rows}
    wb.close()
    return sheets


def search(keywords: List[str], sheets_data: Dict, sheet_filter: str = "",
           max_results: int = 20) -> List[Dict]:
    """搜索所有 sheet，返回匹配行"""
    results = []
    for sheet_name, data in sheets_data.items():
        if sheet_filter and sheet_filter not in sheet_name:
            continue
        headers = data["headers"]
        for row in data["rows"]:
            row_text = " ".join(row)
            if not _match(row_text, keywords):
                continue
            row_dict = {}
            for idx, header in enumerate(headers):
                row_dict[header] = row[idx] if idx < len(row) else ""
            results.append({"sheet": sheet_name, "data": row_dict})
            if len(results) >= max_results:
                return results
    return results


def list_sheets(excel_path: str) -> List[Dict]:
    """列出前两个可见 sheet 及其结构"""
    wb = openpyxl.load_workbook(excel_path, read_only=True, data_only=True)
    result = []
    visible = [n for n in wb.sheetnames if wb[n].sheet_state == 'visible']
    for name in visible[:SHEET_LIMIT]:
        ws = wb[name]
        headers = []
        for i, row in enumerate(ws.iter_rows(values_only=True)):
            if i == 0:
                headers = [str(c).strip() if c is not None else "" for c in row]
                break
        result.append({
            "name": name,
            "rows": ws.max_row - 1,
            "columns": len(headers),
            "headers": headers,
        })
    wb.close()
    return result


def execute(action: str, **kwargs) -> Dict[str, Any]:
    if not HAS_OPENPYXL:
        return {"success": False, "message": "缺少 openpyxl，请 pip install openpyxl"}

    excel_path = kwargs.get("excel_path") or EXCEL_PATH
    if not excel_path or not os.path.isfile(excel_path):
        return {"success": False,
                "message": f"Excel 文件不存在: {excel_path}，请确认定时下载任务正常执行"}

    action = (action or "").strip().lower()

    if action == "search":
        query = kwargs.get("query", "")
        if not query:
            return {"success": False, "message": "query 不能为空"}
        keywords = [kw.strip() for kw in re.split(r"[，,\s]+", query) if kw.strip()]
        sheet_filter = kwargs.get("sheet", "")
        max_results = int(kwargs.get("max_results", 20))

        try:
            sheets_data = _load_sheets(excel_path, SHEET_LIMIT)
        except Exception as e:
            return {"success": False, "message": f"读取 Excel 失败：{e}"}

        results = search(keywords, sheets_data, sheet_filter=sheet_filter,
                         max_results=max_results)
        return {
            "success": True,
            "query": query,
            "total": len(results),
            "results": results,
            "message": f"共找到 {len(results)} 条记录",
        }

    elif action == "list_sheets":
        sheets = list_sheets(excel_path)
        return {"success": True, "sheets": sheets,
                "message": f"共 {len(sheets)} 个 sheet"}

    else:
        return {"success": False,
                "message": f"未知 action: {action}，支持: search / list_sheets"}


def main(input_data: Dict[str, Any] = None) -> Dict[str, Any]:
    if input_data is None:
        input_data = {}
    action = input_data.get("action", "search")
    kwargs = {k: v for k, v in input_data.items() if k != "action"}
    return execute(action, **kwargs)


def _parse_input() -> Dict[str, Any]:
    raw = None
    env_params = os.environ.get("SCRIPT_PARAMS")
    if env_params:
        raw = env_params.strip()
    if raw is None and len(sys.argv) > 1:
        raw = " ".join(sys.argv[1:])
    if raw is None and not sys.stdin.isatty():
        ready, _, _ = select.select([sys.stdin], [], [], 0.1)
        if ready:
            raw = sys.stdin.read().strip()
    if raw is None:
        return {"action": "list_sheets"}

    try:
        data = json.loads(raw)
    except (json.JSONDecodeError, ValueError):
        return {"action": "search", "query": raw}

    if isinstance(data, dict) and "jsonParams" in data:
        inner = data["jsonParams"]
        if isinstance(inner, str):
            try:
                data = json.loads(inner)
            except (json.JSONDecodeError, ValueError):
                return {"action": "search", "query": inner}
        else:
            data = inner

    if not isinstance(data, dict):
        return {"action": "search", "query": str(data)}
    return data


if __name__ == "__main__":
    input_data = _parse_input()
    result = main(input_data)
    print(json.dumps(result, ensure_ascii=False, indent=2))
