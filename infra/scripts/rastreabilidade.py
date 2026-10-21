#!/usr/bin/env python3
"""Matriz de rastreabilidade (seção 17.3): de cada requisito (RF, RN, RNF, CA) para o código, os testes e os
commits que o citam. Gera docs/rastreabilidade.md a partir da especificação e do repositório.

Uso: python infra/scripts/rastreabilidade.py   (na raiz do repositório)
"""
from __future__ import annotations

import re
import subprocess
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SPEC = ROOT / "docs" / "ESPECIFICACAO.md"
OUT = ROOT / "docs" / "rastreabilidade.md"
ID_RE = re.compile(r"\b(RF|RN|RNF|CA)-(\d{1,2})\b")
RANGE_RE = re.compile(r"\b(RF|RN|RNF|CA)-(\d{1,2}) a (?:(?:RF|RN|RNF|CA)-)?(\d{1,2})\b")
SOURCES = {
    "backend": ROOT / "backend" / "src" / "main",
    "backend-test": ROOT / "backend" / "src" / "test",
    "frontend": ROOT / "frontend" / "src",
}
EXTS = {".java", ".ts", ".tsx", ".sql", ".yml", ".properties", ".json"}


def key(prefix: str, num: str) -> str:
    # Mesmo formato da especificação: RF-01, RN-01, RNF-01 e CA-1.
    return f"{prefix}-{int(num)}" if prefix == "CA" else f"{prefix}-{int(num):02d}"


def ids_in(text: str) -> set[str]:
    found = {key(p, n) for p, n in ID_RE.findall(text)}
    for prefix, start, end in RANGE_RE.findall(text):
        found.update(key(prefix, str(i)) for i in range(int(start), int(end) + 1))
    return found


def requirements() -> dict[str, str]:
    """Linhas de tabela da especificação que começam com o ID: | RF-01 | Título | ..."""
    reqs: dict[str, str] = {}
    row = re.compile(r"^\|\s*\*{0,2}((?:RF|RN|RNF|CA)-\d{1,2})\*{0,2}\s*\|\s*([^|]+)\|")
    for line in SPEC.read_text(encoding="utf-8").splitlines():
        m = row.match(line)
        if m:
            rid = key(*m.group(1).split("-"))
            reqs.setdefault(rid, m.group(2).strip())
    return reqs


def scan_files() -> dict[str, dict[str, set[str]]]:
    hits: dict[str, dict[str, set[str]]] = defaultdict(lambda: defaultdict(set))
    for area, base in SOURCES.items():
        if not base.exists():
            continue
        for path in base.rglob("*"):
            if path.suffix not in EXTS or "node_modules" in path.parts:
                continue
            rel = path.relative_to(ROOT).as_posix()
            is_test = area == "backend-test" or ".test." in path.name
            for rid in ids_in(path.read_text(encoding="utf-8", errors="ignore")):
                hits[rid]["testes" if is_test else "código"].add(rel)
    return hits


def scan_commits() -> dict[str, list[str]]:
    try:
        log = subprocess.run(
            ["git", "log", "--format=%h %s%n%b%x00"], cwd=ROOT, capture_output=True, text=True, check=True
        ).stdout
    except (OSError, subprocess.CalledProcessError):
        return {}
    commits: dict[str, list[str]] = defaultdict(list)
    for entry in log.split("\x00"):
        entry = entry.strip()
        if not entry:
            continue
        sha = entry.split()[0]
        for rid in ids_in(entry):
            commits[rid].append(sha)
    return commits


def short(paths: set[str], limit: int = 3) -> str:
    items = sorted(paths)
    names = [f"`{Path(p).name}`" for p in items[:limit]]
    extra = len(items) - limit
    return ", ".join(names) + (f" (+{extra})" if extra > 0 else "") if items else "—"


def main() -> None:
    reqs = requirements()
    files = scan_files()
    commits = scan_commits()
    order = {"RF": 0, "RN": 1, "RNF": 2, "CA": 3}
    lines = [
        "# Matriz de rastreabilidade",
        "",
        "Gerada por `infra/scripts/rastreabilidade.py` a partir de `docs/ESPECIFICACAO.md`, do código, dos testes",
        "e das mensagens de commit (seção 17.3). Não edite à mão: rode o script de novo.",
        "",
        "| ID | Requisito | Código | Testes | Commits |",
        "|---|---|---|---|---|",
    ]
    missing_tests = []
    for rid in sorted(reqs, key=lambda r: (order[r.split("-")[0]], int(r.split("-")[1]))):
        code = files.get(rid, {}).get("código", set())
        tests = files.get(rid, {}).get("testes", set())
        shas = commits.get(rid, [])
        if not tests:
            missing_tests.append(rid)
        title = reqs[rid].replace("|", "/")
        lines.append(f"| {rid} | {title} | {short(code)} | {short(tests)} | {', '.join(shas[:3]) or '—'} |")
    lines += [
        "",
        f"**Requisitos sem teste automatizado que os cite ({len(missing_tests)}):** "
        + (", ".join(missing_tests) if missing_tests else "nenhum")
        + ". Alguns são verificados por E2E, inspeção ou validação com usuários (seções 15 e 17.6).",
        "",
    ]
    OUT.write_text("\n".join(lines), encoding="utf-8")
    print(f"{OUT.relative_to(ROOT)}: {len(reqs)} requisitos, {len(missing_tests)} sem teste citado")


if __name__ == "__main__":
    main()
