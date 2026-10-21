---
version: 1
slug: "frontend-src-app-app-tsx"
primary_target: "frontend/src/app/App.tsx"
related_targets: ["frontend/src"]
---

Scope: whole NANDESK PWA (public storefront, collaborator flow, moderation/admin). Mode: Operate (home search is Operate too: the visitor came to find a box). Code-first (no image generation).

Audience/job: tourists and locals find which market + box sells a product, in PT/EN/ES, on a phone at the market with bad signal; collaborators run a 3-step task (accept, check-in, send) under sun; moderators review diffs.

## Direction contract

THESIS: The feira's own price board. Every list of what a stall sells is set like the hand-chalked "pedra de preços": product, dotted leader, price, unit, in fixed columns with tabular figures, and the board carries the date it was last checked. Refuses the category default of photo cards in a grid.

OWN-WORLD: Brand-pinned tokens (14.6). White ground; shell band in Açaí 900 with the search field set into it; Tucupi 400 only on dark (focus ring on the band, "atualizado" marks on dark, primary button on the band). Açaí 700 reserved for things you can press (raise from the warm consumer app). Archivo 112% for headings, market names, box codes and prices; Atkinson Hyperlegible Next for everything else. Hairline Cinza 200 rules between rows instead of boxes; radius 10 on controls, 14 on sheets, pill only on chips; no shadows except sheets.

STORY: Open → type "castanha" → see each market as a board section, boxes as rows with price/unit/availability and "verificado há N dias" → tap a box → its full board. Works offline from the saved snapshot, saying when it was saved. Collaborators see tasks as a board ranked by deadline.

FIRST VIEWPORT: Açaí 900 band: logo left, language switch right, a large search input (≥56px) with the prompt "O que você procura?" and quick terms (jambu, banho de cheiro, castanha-do-pará). Below on white: "Feiras" two rows (Ver-o-Peso, Feira da 25) with box counts and today's hours; then "Categorias" as a two-column ruled list. Bottom nav for collaborators only.

FORM: Candidate 6 of my grounded list (lousa de preços da feira). Raises: deadline as a proportional bar sized to remaining time (from the labanotation score's exact-duration discipline); changed rows stay marked "atualizado" until seen (from the gate board); action colour only on actionables (from the warm consumer app). seed: df901e5de88c0f73

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance

Signature interaction: the price row. Leader dots fill the gap between product and price at any width (CSS flex leader), prices in tabular Archivo; when fresh data replaces cached data the changed cell gets a short wipe and a Tucupi underline that stays until the row scrolls into view.
Unresolved: real photos (none yet; typographic until collaborators upload); Feira da 25 coordinates approximate.
