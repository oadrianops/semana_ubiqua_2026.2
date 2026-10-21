"""Gera a Archivo de títulos: instância com largura fixa de 112% (14.6) e pesos 600–800, subconjunto latino.

Origem: @fontsource-variable/archivo (SIL OFL 1.1). Uso: python scripts/build-display-font.py
Mantém o orçamento de fontes da seção 9.14 (≤ 120 KB no total).
"""
import sys
from pathlib import Path
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

root = Path(__file__).resolve().parent.parent
src = root / "node_modules/@fontsource-variable/archivo/files/archivo-latin-wdth-normal.woff2"
out = root / "src/assets/fonts/archivo-display-latin.woff2"

font = TTFont(src)
instantiateVariableFont(font, {"wdth": 112, "wght": (600, 800)}, inplace=True)
font.flavor = "woff2"
font.save(out)
print(f"{out.name}: {out.stat().st_size / 1024:.1f} KB")
sys.exit(0)
