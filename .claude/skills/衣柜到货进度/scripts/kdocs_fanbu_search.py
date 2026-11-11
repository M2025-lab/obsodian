#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
衣柜 / 定制柜「返补」信息检索脚本。

数据来源（均由 KdocsDownloadService 定时从金山文档导出）：
  1. 反补到货时间（新）（第二个文档，「8.28 新流程」数据）
       - sheet「反补到货时间（新）」，表头在第 1 行
       - 关联键：项目单号
       - 返补到工地时间列：预计到工地时间
  2. 返补台账（第二个文档「橱木设计师返补下单台账」）
       - sheet「衣柜返补」「橱柜返补」，表头在第 2 行
       - 关联键：项目ID
       - 返补到工地时间列：预计送至工地时间
  3. 衣柜到货进度（第一个文档）
       - sheet「衣柜进度扫码查询底表*」
       - 关联键：项目订单id
       - 返补到工地时间列：衣柜返补到工地时间 / 橱柜返补到工地时间

「返补到工地时间」优先级：新流程 > 返补台账 > 第一个文档。
1 只提供时间、不提供返补明细；2 提供完整返补明细（反补产品/反补原因/反补单号等）。

用法（executePythonScript 会通过 SCRIPT_PARAMS 环境变量传入 JSON）：
  {"action":"query","query":"826041314000002288"}
  {"action":"query","query":"张三"}
  {"action":"list","query":"李春龙"}
  {"action":"info"}

本地调试：
  SCRIPT_PARAMS='{"action":"query","query":"..."}' python3 kdocs_fanbu_search.py
"""

import sys
import os
import re
import json
import time
import select
import warnings
from typing import Any, Dict, List, Optional

# PythonScriptTool 用 redirectErrorStream(true) 把 stderr 并入 stdout，
# 任何 warning 都会污染 JSON 输出，必须静音。台账 N 列（电话）存在越界日期值会触发 UserWarning。
warnings.filterwarnings("ignore")

try:
    import openpyxl
    HAS_OPENPYXL = True
except ImportError:
    HAS_OPENPYXL = False

# 返补台账干净缓存（第二个文档）与索引
FANBU_CLEAN_PATH = os.environ.get("KDOCS_FANBU_CLEAN_PATH", "/pvc/tmp/返补台账.clean.xlsx")
FANBU_INDEX_PATH = os.environ.get("KDOCS_FANBU_INDEX_PATH", "/pvc/tmp/返补台账.index.json")

# 反补到货时间（新）索引：与返补台账同属第二个文档的干净缓存，只是另建一份索引
XIN_INDEX_PATH = os.environ.get("KDOCS_FANBU_XIN_INDEX_PATH", "/pvc/tmp/返补台账.新流程.index.json")

# 衣柜到货进度干净缓存（第一个文档，用于返补时间兜底）
YIGUI_CLEAN_PATH = os.environ.get("KDOCS_YIGUI_CLEAN_PATH", "/pvc/tmp/衣柜到货.clean.xlsx")
YIGUI_INDEX_PATH = os.environ.get("KDOCS_YIGUI_INDEX_PATH", "/pvc/tmp/衣柜到货.index.json")

FANBU_SHEETS = ["衣柜返补", "橱柜返补"]
# 第二个文档中「返补到工地时间」优先级最高的 sheet（8.28 后新流程数据）
XIN_SHEET = "反补到货时间（新）"
# 第一个文档中承载返补时间的 sheet 名（带日期后缀，用前缀匹配）
YIGUI_SCAN_SHEET_PREFIX = "衣柜进度扫码查询底表"

# 新流程 sheet 的品类（定制柜-套外/套内/橱柜）归一化到与台账一致的「衣柜/橱柜」，
# 用于该 sheet 没有时间时挑第一个文档的哪一列兜底。
XIN_CATEGORY_MAP = {"橱柜": "橱柜", "定制柜-套内": "衣柜", "定制柜-套外": "衣柜"}

DEFAULT_MAX_RESULTS = 10
MAX_RESULTS_CAP = 30

# 返补台账里「无返补信息」之类的哨兵值，统一按空处理
SENTINEL_RE = re.compile(r"无|暂无|/N/?A|#N/A", re.IGNORECASE)


def _clean(v: Any) -> str:
    """单元格值归一化：None → ''，日期去掉 00:00:00 尾巴。"""
    if v is None:
        return ""
    s = str(v).strip()
    if s.endswith(" 00:00:00"):
        s = s[:10]
    if s.endswith(".0") and s[:-2].isdigit():
        s = s[:-2]
    return s


def _real(v: Any) -> bool:
    """是否为真实取值（排除空与「无xx信息」哨兵）。"""
    s = _clean(v)
    return bool(s) and not SENTINEL_RE.search(s)


def _is_stale(index_path: str, source_path: str) -> bool:
    """索引不存在，或比数据源旧 → 需要重建。"""
    if not os.path.isfile(index_path):
        return True
    if not os.path.isfile(source_path):
        return False
    return os.path.getmtime(source_path) > os.path.getmtime(index_path) + 1


def _write_index(index: Dict, index_path: str) -> None:
    """原子写索引：先写临时文件再 rename，避免查询读到半截文件。"""
    tmp = index_path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(index, f, ensure_ascii=False)
    os.replace(tmp, index_path)


def _read_index(index_path: str) -> Optional[Dict]:
    try:
        with open(index_path, encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return None


def _find_sheet(wb, name: str):
    """按名称精确匹配，失败则前缀匹配（第一个文档 sheet 名带日期后缀）。"""
    if name in wb.sheetnames:
        return wb[name]
    for n in wb.sheetnames:
        if n.startswith(name):
            return wb[n]
    return None


def build_fanbu_index(src_path: str) -> Dict[str, List[Dict]]:
    """构建返补台账索引：{项目ID: [返补记录, ...]}。"""
    wb = openpyxl.load_workbook(src_path, read_only=True, data_only=True)
    index: Dict[str, List[Dict]] = {}
    try:
        for sheet_name in FANBU_SHEETS:
            ws = _find_sheet(wb, sheet_name)
            if ws is None:
                continue
            rows = ws.iter_rows(values_only=True)
            next(rows, None)                      # 第 1 行是「设计师填写 B-N 列」分组标题
            header = next(rows, None)             # 第 2 行才是真实表头
            if not header:
                continue
            headers = [_clean(h) for h in header]
            if "项目ID" not in headers:
                continue
            i_proj = headers.index("项目ID")
            category = "衣柜" if "衣柜" in sheet_name else "橱柜"
            for row in rows:
                key = _clean(row[i_proj]) if i_proj < len(row) else ""
                if not key:
                    continue
                rec: Dict[str, Any] = {"_品类": category}
                for i, h in enumerate(headers):
                    if not h or i >= len(row):
                        continue
                    val = _clean(row[i])
                    if val:
                        rec[h] = val
                index.setdefault(key, []).append(rec)
    finally:
        wb.close()
    return index


def build_xin_index(src_path: str) -> Dict[str, Dict]:
    """
    构建「反补到货时间（新）」索引：{项目单号: {"时间": 最晚有效时间, "品类": 衣柜|橱柜}}。
    同一项目多行时取「预计到工地时间」最晚的一条，与台账的处理一致。
    表头在第 1 行。没有有效时间的项目也入索引（时间留空），
    这样它的品类仍能供「第一个文档兜底」挑列使用。
    """
    wb = openpyxl.load_workbook(src_path, read_only=True, data_only=True)
    index: Dict[str, Dict] = {}
    try:
        ws = _find_sheet(wb, XIN_SHEET)
        if ws is None:
            return index
        rows = ws.iter_rows(values_only=True)
        header = next(rows, None)
        if not header:
            return index
        headers = [_clean(h) for h in header]
        if "项目单号" not in headers or "预计到工地时间" not in headers:
            return index
        i_proj = headers.index("项目单号")
        i_time = headers.index("预计到工地时间")
        i_cat = headers.index("品类") if "品类" in headers else None
        for row in rows:
            key = _clean(row[i_proj]) if i_proj < len(row) else ""
            if not key:
                continue
            time_val = _clean(row[i_time]) if i_time < len(row) else ""
            if not _real(time_val):
                time_val = ""
            category = XIN_CATEGORY_MAP.get(
                _clean(row[i_cat]) if i_cat is not None and i_cat < len(row) else "", "")
            cur = index.get(key)
            if cur is None:
                index[key] = {"时间": time_val, "品类": category}
                continue
            if time_val and time_val > cur["时间"]:
                cur["时间"] = time_val
            if category and not cur["品类"]:
                cur["品类"] = category
    finally:
        wb.close()
    return index


def build_yigui_index(src_path: str) -> Dict[str, Dict]:
    """构建第一个文档的返补时间索引：{项目订单id: {衣柜, 橱柜, 客户}}。"""
    wb = openpyxl.load_workbook(src_path, read_only=True, data_only=True)
    index: Dict[str, Dict] = {}
    try:
        ws = _find_sheet(wb, YIGUI_SCAN_SHEET_PREFIX)
        if ws is None:
            return index
        rows = ws.iter_rows(values_only=True)
        header = next(rows, None)
        if not header:
            return index
        headers = [_clean(h) for h in header]
        if "项目订单id" not in headers:
            return index
        i_proj = headers.index("项目订单id")
        i_yg = headers.index("衣柜返补到工地时间") if "衣柜返补到工地时间" in headers else None
        i_cg = headers.index("橱柜返补到工地时间") if "橱柜返补到工地时间" in headers else None
        i_name = headers.index("客户姓名") if "客户姓名" in headers else None
        for row in rows:
            key = _clean(row[i_proj]) if i_proj < len(row) else ""
            if not key:
                continue
            index[key] = {
                "衣柜": _clean(row[i_yg]) if i_yg is not None and i_yg < len(row) else "",
                "橱柜": _clean(row[i_cg]) if i_cg is not None and i_cg < len(row) else "",
                "客户": _clean(row[i_name]) if i_name is not None and i_name < len(row) else "",
            }
    finally:
        wb.close()
    return index


def _ensure_index(kind: str) -> Dict:
    """
    取索引，必要时重建。返回 {} 表示数据源缺失。
    kind: 'fanbu'（返补台账） | 'xin'（反补到货时间新） | 'yigui'（第一个文档）
    """
    if kind == "fanbu":
        src, idx_path, builder = FANBU_CLEAN_PATH, FANBU_INDEX_PATH, build_fanbu_index
    elif kind == "xin":
        src, idx_path, builder = FANBU_CLEAN_PATH, XIN_INDEX_PATH, build_xin_index
    else:
        src, idx_path, builder = YIGUI_CLEAN_PATH, YIGUI_INDEX_PATH, build_yigui_index

    if not os.path.isfile(src):
        return {}

    if not _is_stale(idx_path, src):
        cached = _read_index(idx_path)
        if cached is not None:
            return cached

    if not HAS_OPENPYXL:
        return {}
    try:
        index = builder(src)
    except Exception as e:
        sys.stderr.write(f"[kdocs_fanbu_search] 构建 {kind} 索引失败: {e}\n")
        return {}
    try:
        _write_index(index, idx_path)
    except Exception as e:
        sys.stderr.write(f"[kdocs_fanbu_search] 写索引失败: {e}\n")
    return index


def _split_keywords(query: str) -> List[str]:
    """按中文逗号/英文逗号/空白拆分关键词，与 kdocs_search.py 保持一致。"""
    return [kw.strip() for kw in re.split(r"[，,\s]+", query) if kw.strip()]


def _match_key(rec: Dict, key: str) -> bool:
    """一条返补记录是否命中查询词（项目ID / 反补单号 / 产品订单号 / 客户姓名 / 三方主单号）。"""
    for field in ("项目ID", "反补单号", "产品订单号", "客户姓名", "三方主单号", "收货人姓名"):
        if key in _clean(rec.get(field)):
            return True
    return False


def _resolve_project(fanbu_index: Dict, xin_index: Dict, yigui_index: Dict,
                     query: str) -> List[str]:
    """把查询词解析成项目ID列表：按分隔符拆分，任一关键词命中即算，
    每个关键词内部先精确命中项目ID，再按其他字段模糊匹配。"""
    resolved: List[str] = []
    for q in _split_keywords(query):
        if q in fanbu_index or q in xin_index or q in yigui_index:
            resolved.append(q)
            continue
        # 新流程表没有明细字段，只能按项目单号本身做子串匹配
        hits = [pid for pid, recs in fanbu_index.items()
                if any(_match_key(r, q) for r in recs)]
        hits += [pid for pid in xin_index if q and q in pid]
        if not hits:
            # 最后用第一个文档的客户姓名兜底
            hits = [pid for pid, v in yigui_index.items() if q and q in _clean(v.get("客户"))]
        resolved.extend(hits)
    return list(dict.fromkeys(resolved))


def _sort_key(rec: Dict):
    """按提报时间排序，缺失排最后。"""
    return rec.get("提报时间") or ""


def _fallback_only(yigui_index: Dict, query: str, max_results: int) -> Dict[str, Any]:
    """
    返补台账不可用时的降级查询：只用第一个文档的返补时间列。
    此时没有返补单号 / 反补产品 / 反补原因等明细，必须在返回里明确说明。
    """
    pids: List[str] = []
    for q in _split_keywords(query):
        if q in yigui_index:
            pids.append(q)
        else:
            pids.extend(pid for pid, v in yigui_index.items()
                        if q and q in _clean(v.get("客户")))
    pids = list(dict.fromkeys(pids))

    projects = []
    for pid in pids[:max_results]:
        y = yigui_index.get(pid, {}) or {}
        yg, cg = _clean(y.get("衣柜")), _clean(y.get("橱柜"))
        yg_ok, cg_ok = _real(yg), _real(cg)
        if yg_ok and not cg_ok:
            value, source = yg, "衣柜到货进度（衣柜列兜底）"
        elif cg_ok and not yg_ok:
            value, source = cg, "衣柜到货进度（橱柜列兜底）"
        elif yg_ok and cg_ok:
            value, source = "", "无法确定（衣柜/橱柜返补时间并存，请人工核对）"
        else:
            value, source = "", "无"
        projects.append({
            "项目ID": pid,
            "客户姓名": _clean(y.get("客户")),
            "返补记录数": 0,
            "返补到工地时间": value,
            "返补到工地时间来源": source,
            "返补记录": [],
        })

    return {
        "success": True,
        "degraded": True,
        "query": query,
        "total": len(pids),
        "projects": projects,
        "message": (f"返补台账缓存不可用（{FANBU_CLEAN_PATH}），以下仅为第一个文档的返补时间兜底值，"
                    "缺少返补单号/反补产品/反补原因等明细，请提示用户数据可能不完整"),
    }


def _latest_record(recs: List[Dict]) -> Dict:
    """
    取「最新一条」返补记录 = 预计送至工地时间最晚的一条（缺失则退化为最后一条）。
    同一项目常有多条返补，最新一条才代表当前预期到工地时间。
    """
    if not recs:
        return {}
    dated = [r for r in recs if _clean(r.get("预计送至工地时间"))]
    if not dated:
        return recs[-1]
    return max(dated, key=lambda r: _clean(r.get("预计送至工地时间")))


def query_fanbu(query: str, max_results: int) -> Dict[str, Any]:
    fanbu_index = _ensure_index("fanbu")
    xin_index = _ensure_index("xin")
    yigui_index = _ensure_index("yigui")

    if not fanbu_index and not xin_index:
        # 第二个文档完全缺失时降级：只用第一个文档的返补时间列兜底，不直接失败
        if yigui_index:
            return _fallback_only(yigui_index, query, max_results)
        return {
            "success": False,
            "message": ("返补数据未就绪：未找到返补台账缓存。请确认金山文档定时下载任务已执行，"
                        f"或手动触发 /admin/kdocs/download（期望文件 {FANBU_CLEAN_PATH}）"),
        }

    projects = _resolve_project(fanbu_index, xin_index, yigui_index, query)
    if not projects:
        return {
            "success": True,
            "query": query,
            "total": 0,
            "projects": [],
            "message": f"未找到与「{query}」相关的返补记录，请核实项目单号或客户姓名",
        }

    out = []
    for pid in projects[:max_results]:
        recs = sorted(fanbu_index.get(pid, []), key=_sort_key)
        xin = xin_index.get(pid, {}) or {}
        yigui = yigui_index.get(pid, {}) or {}
        # 最新一条 = 预计送至工地时间最晚的一条，作为主结论
        latest = _latest_record(recs)

        items = []
        for i, r in enumerate(recs):
            item = {
                "序号": i + 1,
                "品类": r.get("_品类", ""),
                "提报时间": r.get("提报时间", ""),
                "设计师": r.get("木作设计师姓名") or r.get("设计师", ""),
                "客户姓名": r.get("客户姓名", ""),
                "反补产品": r.get("反补产品", ""),
                "反补原因": r.get("反补原因", ""),
                "责任人": r.get("责任人", ""),
                # 单号列两表不同名：衣柜返补=反补单号，橱柜返补=三方主单号，统一归一化输出
                "反补单号": r.get("反补单号") or r.get("三方主单号", ""),
                "预计出货时间": r.get("预计出货时间", ""),
                "预计送至工地时间": r.get("预计送至工地时间", ""),
                "项目是否已竣工": r.get("项目是否已竣工", ""),
                "是否需要调度扣款": r.get("是否需要调度扣款", ""),
                # 是否为最新一条（主结论），回答时以它为准
                "是否最新": r is latest,
            }
            items.append({k: v for k, v in item.items() if v != "" or k == "是否最新"})

        # 返补到工地时间优先级：新流程 > 返补台账 > 第一个文档
        xin_time = _clean(xin.get("时间"))
        time_from_fanbu = _clean(latest.get("预计送至工地时间"))
        if _real(xin_time):
            time_value, time_source = xin_time, "反补到货时间（新）"
        elif _real(time_from_fanbu):
            time_value, time_source = time_from_fanbu, "返补台账"
        else:
            # 回退到第一个文档。品类未知时不猜列：
            # 只有单列有值才采纳，两列都有值时无法判断属于衣柜还是橱柜，宁可留空。
            yg, cg = _clean(yigui.get("衣柜")), _clean(yigui.get("橱柜"))
            yg_ok, cg_ok = _real(yg), _real(cg)
            # 台账无记录时用新流程 sheet 的品类兜底（已归一化为 衣柜/橱柜）
            category = latest.get("_品类", "") or xin.get("品类", "")
            if category == "橱柜" and cg_ok:
                time_value, time_source = cg, "衣柜到货进度（橱柜列兜底）"
            elif category == "衣柜" and yg_ok:
                time_value, time_source = yg, "衣柜到货进度（衣柜列兜底）"
            elif yg_ok and not cg_ok:
                time_value, time_source = yg, "衣柜到货进度（衣柜列兜底）"
            elif cg_ok and not yg_ok:
                time_value, time_source = cg, "衣柜到货进度（橱柜列兜底）"
            elif yg_ok and cg_ok:
                time_value, time_source = "", "无法确定（衣柜/橱柜返补时间并存，请人工核对）"
            else:
                time_value, time_source = "", "无"

        out.append({
            "项目ID": pid,
            "客户姓名": (recs[0].get("客户姓名") if recs else "") or yigui.get("客户", ""),
            "返补记录数": len(recs),
            "返补到工地时间": time_value,
            "返补到工地时间来源": time_source,
            "返补记录": items,
        })

    result: Dict[str, Any] = {
        "success": True,
        "query": query,
        "total": len(projects),
        "projects": out,
        "message": f"命中 {len(projects)} 个项目，已返回 {len(out)} 个",
    }
    if not fanbu_index:
        # 第二个文档的返补台账 sheet 读不到：有返补时间但缺全部明细，必须让调用方知道
        result["degraded"] = True
        result["message"] = ("返补台账明细不可用，以下仅为「反补到货时间（新）」/第一个文档的时间值，"
                             "缺少返补单号、反补产品、反补原因等明细，请提示用户数据可能不完整")
    return result


def data_info() -> Dict[str, Any]:
    """数据就绪情况：文件是否存在、最后更新时间、索引规模。"""

    def stat(path: str) -> Dict[str, Any]:
        if not os.path.isfile(path):
            return {"path": path, "exists": False}
        return {
            "path": path,
            "exists": True,
            "size_mb": round(os.path.getsize(path) / 1024 / 1024, 1),
            "mtime": time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(os.path.getmtime(path))),
        }

    fanbu = _read_index(FANBU_INDEX_PATH) or {}
    xin = _read_index(XIN_INDEX_PATH) or {}
    yigui = _read_index(YIGUI_INDEX_PATH) or {}
    return {
        "success": True,
        "返补台账": stat(FANBU_CLEAN_PATH),
        "衣柜到货进度": stat(YIGUI_CLEAN_PATH),
        "返补索引项目数": len(fanbu),
        "新流程索引项目数": len(xin),
        "衣柜到货索引项目数": len(yigui),
        "新流程索引文件": stat(XIN_INDEX_PATH),
        "openpyxl": HAS_OPENPYXL,
    }


def execute(params: Dict[str, Any]) -> Dict[str, Any]:
    action = _clean(params.get("action")) or "query"

    if action == "info":
        return data_info()

    query = _clean(params.get("query"))
    if not query:
        return {"success": False, "message": "query 不能为空（请提供项目单号 / 客户姓名 / 反补单号）"}

    try:
        max_results = int(params.get("max_results", DEFAULT_MAX_RESULTS))
    except (TypeError, ValueError):
        max_results = DEFAULT_MAX_RESULTS
    max_results = max(1, min(max_results, MAX_RESULTS_CAP))

    if action in ("query", "list", "search"):
        return query_fanbu(query, max_results)

    return {"success": False, "message": f"未知 action: {action}，支持: query / info"}


def _parse_input() -> Dict[str, Any]:
    raw = os.environ.get("SCRIPT_PARAMS")
    if raw:
        raw = raw.strip()
    if not raw and len(sys.argv) > 1:
        raw = " ".join(sys.argv[1:])
    if not raw and not sys.stdin.isatty():
        try:
            ready, _, _ = select.select([sys.stdin], [], [], 0.1)
            if ready:
                raw = sys.stdin.read().strip()
        except Exception:
            pass
    if not raw:
        return {"action": "info"}

    try:
        data = json.loads(raw)
    except (json.JSONDecodeError, ValueError):
        return {"action": "query", "query": raw}

    if isinstance(data, dict) and "jsonParams" in data:
        inner = data["jsonParams"]
        if isinstance(inner, str):
            try:
                data = json.loads(inner)
            except (json.JSONDecodeError, ValueError):
                return {"action": "query", "query": inner}
        else:
            data = inner

    if not isinstance(data, dict):
        return {"action": "query", "query": str(data)}
    return data


if __name__ == "__main__":
    try:
        result = execute(_parse_input())
    except Exception as e:  # fail-open：始终输出合法 JSON
        result = {"success": False, "message": f"返补查询异常: {e}"}
    print(json.dumps(result, ensure_ascii=False, indent=2))
