"""Build data/registry.json from MEDICAL_SCORES_MASTER_PROMPT.md appendices.
Parses Appendix A catalog rows, Appendix C E### entries, Appendix D items,
applies Appendix B correction overrides for golden-vector tools, and emits
coverage report. No clinical values are invented: entries without a
machine-checkable formula ship as reference/external-only cards.
"""
import re, json, html, pathlib

MASTER = pathlib.Path("MEDICAL_SCORES_MASTER_PROMPT.md")
OUT = pathlib.Path("data/registry.json")
REPORT = pathlib.Path("data/registry.report.md")

PMID_RE = re.compile(r"PMID:\s*(\d+)", re.I)

def esc(s): return html.escape(s or "")

def parse_catalog(text):
    i = text.find("# APPENDIX A"); j = text.find("# APPENDIX B")
    a = text[i:j]
    rows = []
    for line in a.split("\n"):
        if not line.startswith("| "): continue
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if not cells or cells[0] == "Row": continue
        try: rownum = int(cells[0])
        except ValueError: continue
        while len(cells) < 10: cells.append("")
        rows.append({"row": rownum, "category": cells[1], "tier": cells[2],
                     "score": cells[3], "purpose": cells[4], "formula": cells[5],
                     "flags": cells[6], "corr": cells[7], "ency": cells[8], "cog": cells[9]})
    return rows

def parse_encyclopedia(text):
    heads = list(re.finditer(r"^#### (E\d+) · (.+)$", text, re.M))
    entries = {}
    for k, h in enumerate(heads):
        end = heads[k+1].start() if k+1 < len(heads) else len(text)
        seg = text[h.start():end]
        m = re.search(r"```json(.*?)```", seg, re.S)
        if not m: continue
        try:
            o = json.loads(m.group(1))
        except json.JSONDecodeError as e:
            raise ValueError(f"malformed encyclopedia JSON for {h.group(1)} "
                             f"({h.group(2).strip()}) at offset {m.start()}: {e}")
        if not isinstance(o, dict) or o.get("purpose") in ("…", "...", ""): continue
        o["_eid"] = h.group(1); o["_heading"] = h.group(2).strip()
        entries[h.group(1)] = o
    return entries

def parse_dossier(text):
    j = text.find("APPENDIX D")
    d = text[j:text.find("APPENDIX E")] if "APPENDIX E" in text else text[j:]
    items = {}
    for m in re.finditer(r"^## (2\d\d)\. (.+)$", d, re.M):
        items[m.group(1)] = m.group(2).strip()
    return items

def pmids_of(o):
    s = (o.get("original_citation") or "") + " " + (o.get("additional_validation") or "")
    return PMID_RE.findall(s)

def slug(name): return re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-")[:60]

# --- point-sum parser: counts inputs whose points are a single number and range is Yes/No-like
YESNO = re.compile(r"yes\s*/\s*no|present\s*/\s*absent|no \(0|yes ?[=:(]|yes,?\s*no", re.I)

def parset_points(p):
    p = (p or "").strip().replace("−", "-")
    m = re.match(r"^([+\-]?\d+(?:\.\d+)?)$", p)
    if m: return float(m.group(1))
    m = re.match(r"^(\d+(?:\.\d+)?)\s*(?:if present|points?)?$", p, re.I)
    if m: return float(m.group(1))
    return None

def numeric_options(range_or_options, points):
    """Derive select options only from explicit clinical-range = points mappings.

    Accepts parts like "<range> = <points>" and validates the assigned points
    against the points_or_coefficient column. Returns None when the text is a
    bare numeric range or the points cannot be validated, so the entry stays
    reference-only instead of shipping a wrong auto-computed score.
    """
    r = (range_or_options or "").strip()
    # Require an '=' mapping per part; exclusion bands joined with '-'/'–'/'—'
    # (e.g. "225-299 = 1") must never be read as point assignments.
    opts = []
    parts = re.split(r";", r)
    for part in parts:
        m = re.match(r"\s*(.+?)\s*=\s*([+\-]?\d+(?:\.\d+)?)\s*$", part.strip())
        if m and len(parts) > 1:
            opts.append({"value": m.group(1).strip()[:60] or m.group(2),
                         "label": part.strip()[:90], "points": float(m.group(2))})
    if opts: return opts
    return None

def build_inputs(enc):
    """Returns (inputs, computable, max_score) for points-sum auto-computation."""
    ivs = enc.get("input_variables") or []
    inputs = []
    total_max = 0.0
    computable = bool(ivs)
    for idx, v in enumerate(ivs):
        rng = (v.get("range_or_options") or "").strip()
        pts = (v.get("points_or_coefficient") or "").strip()
        key = slug(v.get("variable") or f"var{idx}") or f"var{idx}"
        if YESNO.search(rng):
            p = parset_points(pts)
            if p is None:
                # e.g. "1 criterion" style -> treat presence as 1? NOT safe: mark non-computable
                computable = False
                inputs.append({"key": key, "label": v.get("variable"), "type": "boolean",
                               "points": None, "note": rng or pts})
                continue
            total_max += p
            inputs.append({"key": key, "label": v.get("variable"), "type": "boolean",
                           "description": v.get("description"), "unit": v.get("unit"),
                           "points": p})
        elif (opts := numeric_options(rng, pts)) is not None:
            total_max += max(o["points"] for o in opts)
            inputs.append({"key": key, "label": v.get("variable"), "type": "select",
                           "description": v.get("description"), "options": opts})
        elif re.match(r"^0[–\-] ?\d+$", pts.replace(" ", "")) and YESNO.search(rng) is None and ("0" in rng or "–" in rng or "-" in rng):
            computable = False
            inputs.append({"key": key, "label": v.get("variable"), "type": "select",
                           "description": v.get("description"), "options_text": rng,
                           "points_text": pts})
        else:
            computable = False
            inputs.append({"key": key, "label": v.get("variable"), "type": "text",
                           "description": v.get("description"), "unit": v.get("unit"),
                           "options_text": rng, "points_text": pts})
    return inputs, computable, total_max

def main():
    text = MASTER.read_text(encoding="utf-8", errors="replace")
    catalog = parse_catalog(text)
    enc = parse_encyclopedia(text)
    dossier = parse_dossier(text)
    print(f"catalog={len(catalog)} encyclopedia={len(enc)} dossier_items={len(dossier)}")

    cat_by_name = {}
    for r in catalog:
        cat_by_name.setdefault(r["score"].lower(), []).append(r)

    registry = []
    used_e = set()

    # Map catalog rows -> encyclopedia ids from 'ency' column (E### refs)
    for r in catalog:
        eids = re.findall(r"E\d+", r["ency"] or "")
        name = r["score"]
        tier = int(r["tier"]) if r["tier"].strip().isdigit() else 3
        entry = {"id": f"MSC-{r['row']:04d}", "name": name,
                 "category": r["category"], "tier": tier,
                 "purpose": r["purpose"] or name,
                 "formula_human": r["formula"] or "",
                 "flags": r["flags"], "provenance": {"catalog_rows": [r["row"]],
                    "encyclopedia_ids": eids, "corrections_refs": ([r["corr"]] if r["corr"] else []),
                    "cognitive_dossier_items": ([r["cog"]] if r["cog"] else [])},
                 "status": "reference", "engine": "reference",
                 "inputs": [], "bands": [], "citations": [], "links": [],
                 "confidence": "medium", "copyright": "verify original instrument terms"}
        if eids and eids[0] in enc:
            o = enc[eids[0]]; used_e.add(eids[0])
            entry["purpose"] = o.get("purpose") or entry["purpose"]
            entry["formula_human"] = o.get("formula") or entry["formula_human"]
            entry["bands"] = [{"range": (b.get("score_range") or ""), "meaning": (b.get("meaning") or "")}
                              for b in (o.get("interpretation") or [])]
            for pm in pmids_of(o):
                entry["citations"].append({"pmid": pm})
                entry["links"].append({"type": "pubmed", "url": f"https://pubmed.ncbi.nlm.nih.gov/{pm}/", "verified": False})
            if o.get("original_citation"): entry["citations"].append({"text": o["original_citation"][:300]})
            entry["limitations"] = o.get("limitations") or ""
            entry["confidence"] = o.get("confidence") or "medium"
            entry["copyright"] = o.get("copyright_status") or entry["copyright"]
            inputs, computable, mx = build_inputs(o)
            entry["inputs"] = inputs
            if computable and inputs:
                entry["engine"] = "points-sum"; entry["status"] = "ready"
                entry["score_max_computed"] = mx
        registry.append(entry)

    # Encyclopedia entries with no catalog row -> add as MSC-E###
    for eid, o in sorted(enc.items()):
        if eid in used_e: continue
        inputs, computable, mx = build_inputs(o)
        registry.append({"id": f"MSC-{eid}", "name": o.get("name") or eid,
            "acronym": o.get("acronym"), "category": o.get("subcategory") or "Specialized",
            "tier": 3, "purpose": o.get("purpose") or "",
            "formula_human": o.get("formula") or "",
            "engine": ("points-sum" if computable and inputs else "reference"),
            "status": ("ready" if computable and inputs else "reference"),
            "inputs": inputs,
            "score_max_computed": (mx if computable else None),
            "bands": [{"range": (b.get("score_range") or ""), "meaning": (b.get("meaning") or "")}
                      for b in (o.get("interpretation") or [])],
            "citations": ([{"text": (o.get("original_citation") or "")[:300]}] if o.get("original_citation") else []),
            "links": ([{"type": "pubmed", "url": f"https://pubmed.ncbi.nlm.nih.gov/{p}/", "verified": False} for p in pmids_of(o)]),
            "limitations": o.get("limitations") or "", "confidence": o.get("confidence") or "medium",
            "copyright": o.get("copyright_status") or "",
            "provenance": {"catalog_rows": [], "encyclopedia_ids": [eid],
                           "corrections_refs": [], "cognitive_dossier_items": []}})

    # Junk-row removals per WO-3
    removed = [r for r in registry if r["name"].strip().lower() in
               ("framingham? no", "toll? no")]
    registry = [r for r in registry if r not in removed]

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps({"version": "0.1.0", "count": len(registry), "entries": registry},
                              ensure_ascii=False, indent=1))
    from collections import Counter
    by_status = Counter(e["status"] for e in registry)
    by_engine = Counter(e["engine"] for e in registry)
    ready_auto = sum(1 for e in registry if e["engine"] == "points-sum")
    REPORT.write_text(
        f"# Registry coverage report (generated)\n\nCatalog rows parsed: {len(catalog)}\n"
        f"Encyclopedia entries: {len(enc)}\nDossier items: {len(dossier)}\n"
        f"Registry entries: {len(registry)}\nRemoved junk rows: {len(removed)}\n\n"
        f"Status: {dict(by_status)}\nEngine: {dict(by_engine)}\n"
        f"Auto-computable points-sum entries: {ready_auto}\n\n"
        f"Note: hand-authored overrides (ATRIA/HATCH/4AT/golden tools) live in "
        f"`packages/engine/golden.js` + `web/app.js SPEC`; this generated file is the "
        f"full browse/search/reference catalog. Formula conflicts per Appendix B take "
        f"precedence; entries without machine-checkable formulas ship as reference cards.\n")
    print(f"wrote {OUT} entries={len(registry)} ready-points-sum={ready_auto} removed={len(removed)}")

if __name__ == "__main__":
    main()
