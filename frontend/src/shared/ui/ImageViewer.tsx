import { ChevronLeft, ChevronRight, Minus, Plus, Scan, X } from 'lucide-react';
import { Dialog } from 'radix-ui';
import { useCallback, useEffect, useRef, useState, type PointerEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { cn } from '../lib/cn';

export interface ViewerPhoto {
  /** Foto em tamanho cheio. */
  url: string;
  /** Miniatura já baixada, mostrada enquanto a foto cheia carrega. */
  thumbUrl?: string;
  alt: string;
}

interface View {
  s: number;
  x: number;
  y: number;
}

const MIN = 1;
const MAX = 5;
const STEP = 1.6;
const DOUBLE_TAP = 2.5;
const FIT: View = { s: 1, x: 0, y: 0 };

/**
 * Foto ampliada em tela cheia (RF-03): pinça, toque duplo, roda do mouse e botões aproximam; arrastar move;
 * deslizar ou as setas trocam de foto; Esc fecha (Radix Dialog: foco preso, título anunciado). Carregado sob
 * demanda para não pesar no JS inicial (9.14).
 */
export default function ImageViewer({
  photos,
  index,
  onIndexChange,
  onClose,
}: {
  photos: ViewerPhoto[];
  index: number;
  onIndexChange: (index: number) => void;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const total = photos.length;
  const photo = photos[Math.min(Math.max(index, 0), total - 1)];
  const [view, setView] = useState<View>(FIT);
  const [animate, setAnimate] = useState(true);
  const [dragging, setDragging] = useState(false);
  const [loaded, setLoaded] = useState<string | null>(null);
  const [announce, setAnnounce] = useState('');
  const stage = useRef<HTMLDivElement | null>(null);
  // O Radix monta o conteúdo num portal depois do primeiro render: o ouvinte da roda espera o elemento existir
  const [stageEl, setStageEl] = useState<HTMLDivElement | null>(null);
  const img = useRef<HTMLImageElement>(null);
  const viewRef = useRef(view);
  /** Estado da vista também num ref: os gestos leem o valor atual sem esperar o próximo render. */
  const apply = useCallback((v: View) => {
    viewRef.current = v;
    setView(v);
  }, []);
  const pointers = useRef(new Map<number, { x: number; y: number }>());
  const gesture = useRef<{
    start: { x: number; y: number; time: number } | null;
    pinch: { dist: number; mid: { x: number; y: number }; base: View } | null;
    moved: boolean;
    lastTap: { x: number; y: number; time: number } | null;
  }>({ start: null, pinch: null, moved: false, lastTap: null });

  // Foto nova: volta ao enquadramento inteiro e baixa a versão cheia
  const url = photo?.url;
  useEffect(() => {
    setAnimate(false);
    apply(FIT);
    if (!url) return;
    const pre = new Image();
    pre.onload = () => setLoaded(url);
    pre.src = url;
    return () => {
      pre.onload = null;
    };
  }, [url, apply]);

  /** Não deixa a foto sair do quadro: com zoom, a borda da imagem para na borda da tela. */
  const clamp = useCallback((v: View): View => {
    const s = Math.min(MAX, Math.max(MIN, v.s));
    const el = img.current;
    const box = stage.current;
    if (!el || !box || s <= MIN + 0.001) return { s: MIN, x: 0, y: 0 };
    const maxX = Math.max(0, (el.offsetWidth * s - box.clientWidth) / 2);
    const maxY = Math.max(0, (el.offsetHeight * s - box.clientHeight) / 2);
    return { s, x: Math.min(maxX, Math.max(-maxX, v.x)), y: Math.min(maxY, Math.max(-maxY, v.y)) };
  }, []);

  /** Aproxima mantendo parado o ponto (px, py), medido a partir do centro do quadro. */
  const zoomAt = useCallback(
    (next: number, px = 0, py = 0, base: View = viewRef.current) => {
      const s = Math.min(MAX, Math.max(MIN, next));
      const k = s / base.s;
      apply(clamp({ s, x: px - (px - base.x) * k, y: py - (py - base.y) * k }));
    },
    [clamp, apply],
  );

  const pair = () => {
    const [a, b] = [...pointers.current.values()];
    return a && b ? ([a, b] as const) : null;
  };

  const relative = (clientX: number, clientY: number) => {
    const r = stage.current!.getBoundingClientRect();
    return { x: clientX - (r.left + r.width / 2), y: clientY - (r.top + r.height / 2) };
  };

  const byButton = (next: number) => {
    setAnimate(true);
    const s = Math.min(MAX, Math.max(MIN, next));
    zoomAt(s);
    setAnnounce(t('viewer.zoomLevel', { percent: Math.round(s * 100) }));
  };

  const go = (delta: number) => {
    const next = index + delta;
    if (next >= 0 && next < total) onIndexChange(next);
  };

  // Roda do mouse/trackpad: precisa de ouvinte não passivo para impedir a rolagem da página
  useEffect(() => {
    const el = stageEl;
    if (!el) return;
    const onWheel = (e: WheelEvent) => {
      e.preventDefault();
      setAnimate(false);
      const p = relative(e.clientX, e.clientY);
      zoomAt(viewRef.current.s * Math.exp(-e.deltaY * 0.0015), p.x, p.y);
    };
    el.addEventListener('wheel', onWheel, { passive: false });
    return () => el.removeEventListener('wheel', onWheel);
  }, [stageEl, zoomAt]);

  const onPointerDown = (e: PointerEvent<HTMLDivElement>) => {
    e.currentTarget.setPointerCapture(e.pointerId);
    pointers.current.set(e.pointerId, { x: e.clientX, y: e.clientY });
    setAnimate(false);
    const g = gesture.current;
    const two = pair();
    if (pointers.current.size === 2 && two) {
      const [a, b] = two;
      g.pinch = {
        dist: Math.hypot(a.x - b.x, a.y - b.y),
        mid: relative((a.x + b.x) / 2, (a.y + b.y) / 2),
        base: viewRef.current,
      };
      g.start = null;
    } else if (pointers.current.size === 1) {
      g.start = { x: e.clientX, y: e.clientY, time: e.timeStamp };
      g.moved = false;
      setDragging(viewRef.current.s > MIN);
    }
  };

  const onPointerMove = (e: PointerEvent<HTMLDivElement>) => {
    const prev = pointers.current.get(e.pointerId);
    if (!prev) return;
    pointers.current.set(e.pointerId, { x: e.clientX, y: e.clientY });
    const g = gesture.current;
    const two = pair();
    if (g.pinch && two) {
      const [a, b] = two;
      const mid = relative((a.x + b.x) / 2, (a.y + b.y) / 2);
      const s = g.pinch.base.s * (Math.hypot(a.x - b.x, a.y - b.y) / g.pinch.dist);
      const k = Math.min(MAX, Math.max(MIN, s)) / g.pinch.base.s;
      apply(
        clamp({
          s,
          x: mid.x - (g.pinch.mid.x - g.pinch.base.x) * k,
          y: mid.y - (g.pinch.mid.y - g.pinch.base.y) * k,
        }),
      );
      return;
    }
    if (g.start && Math.hypot(e.clientX - g.start.x, e.clientY - g.start.y) > 8) g.moved = true;
    if (viewRef.current.s > MIN) {
      const v = viewRef.current;
      apply(clamp({ ...v, x: v.x + e.clientX - prev.x, y: v.y + e.clientY - prev.y }));
    }
  };

  const onPointerUp = (e: PointerEvent<HTMLDivElement>) => {
    const g = gesture.current;
    pointers.current.delete(e.pointerId);
    if (pointers.current.size < 2) g.pinch = null;
    if (pointers.current.size === 1) {
      // Saiu um dedo da pinça: o que ficou continua arrastando sem pular
      const [rest] = [...pointers.current.values()];
      if (rest) g.start = { x: rest.x, y: rest.y, time: e.timeStamp };
      g.moved = true;
      return;
    }
    setDragging(false);
    const start = g.start;
    g.start = null;
    if (!start) return;
    const dx = e.clientX - start.x;
    const dy = e.clientY - start.y;
    // Deslizar troca de foto quando ela está inteira na tela
    if (viewRef.current.s <= MIN && Math.abs(dx) > 60 && Math.abs(dx) > Math.abs(dy) * 1.5) {
      go(dx < 0 ? 1 : -1);
      return;
    }
    // Toque duplo (no mouse, o duplo clique cuida disso)
    if (e.pointerType !== 'mouse' && !g.moved && e.timeStamp - start.time < 300) {
      const last = g.lastTap;
      if (last && e.timeStamp - last.time < 320 && Math.hypot(e.clientX - last.x, e.clientY - last.y) < 30) {
        g.lastTap = null;
        toggleAt(e.clientX, e.clientY);
      } else {
        g.lastTap = { x: e.clientX, y: e.clientY, time: e.timeStamp };
      }
    }
  };

  const toggleAt = (clientX: number, clientY: number) => {
    setAnimate(true);
    const p = relative(clientX, clientY);
    zoomAt(viewRef.current.s > MIN ? MIN : DOUBLE_TAP, p.x, p.y);
  };

  const onKeyDown = (e: globalThis.KeyboardEvent) => {
    const v = viewRef.current;
    if (e.key === '+' || e.key === '=') byButton(v.s * STEP);
    else if (e.key === '-') byButton(v.s / STEP);
    else if (e.key === '0') byButton(MIN);
    else if (e.key === 'ArrowLeft' || e.key === 'ArrowRight') {
      const dir = e.key === 'ArrowLeft' ? -1 : 1;
      if (v.s > MIN) {
        setAnimate(true);
        apply(clamp({ ...v, x: v.x - dir * 80 }));
      } else go(dir);
    } else if ((e.key === 'ArrowUp' || e.key === 'ArrowDown') && v.s > MIN) {
      setAnimate(true);
      apply(clamp({ ...v, y: v.y + (e.key === 'ArrowUp' ? 80 : -80) }));
    } else return;
    e.preventDefault();
  };
  // Teclado na janela inteira: um botão que fica desabilitado (ex.: "Ver a foto inteira") tira o foco do diálogo
  const keyHandler = useRef(onKeyDown);
  useEffect(() => {
    keyHandler.current = onKeyDown;
  });
  useEffect(() => {
    const listener = (e: globalThis.KeyboardEvent) => keyHandler.current(e);
    window.addEventListener('keydown', listener);
    return () => window.removeEventListener('keydown', listener);
  }, []);

  if (!photo) return null;
  const zoomed = view.s > MIN + 0.001;
  const tool =
    'inline-flex size-11 shrink-0 items-center justify-center rounded-[var(--radius-control)] text-on-band hover:bg-band-hover disabled:opacity-40 disabled:hover:bg-transparent';

  return (
    <Dialog.Root open onOpenChange={(open) => !open && onClose()}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-50 bg-band" />
        <Dialog.Content
          className="on-band fixed inset-0 z-50 flex flex-col bg-band pt-[var(--safe-top)] pb-[var(--safe-bottom)] text-on-band outline-none"
        >
          <div className="safe-x flex min-h-16 items-center gap-3">
            <Dialog.Title className="min-w-0 flex-1 truncate font-semibold">
              {total > 1 ? (
                <span className="tabular">{t('viewer.counter', { n: index + 1, total })}</span>
              ) : (
                t('viewer.title')
              )}
              <span className="sr-only"> · {photo.alt}</span>
            </Dialog.Title>
            <Dialog.Close className={tool} aria-label={t('action.close')}>
              <X size={22} aria-hidden="true" />
            </Dialog.Close>
          </div>

          <div
            ref={(el) => {
              stage.current = el;
              setStageEl(el);
            }}
            className={cn(
              'relative flex min-h-0 flex-1 touch-none items-center justify-center overflow-hidden select-none',
              zoomed ? (dragging ? 'cursor-grabbing' : 'cursor-grab') : 'cursor-zoom-in',
            )}
            onPointerDown={onPointerDown}
            onPointerMove={onPointerMove}
            onPointerUp={onPointerUp}
            onPointerCancel={onPointerUp}
            onDoubleClick={(e) => toggleAt(e.clientX, e.clientY)}
          >
            <img
              ref={img}
              key={photo.url}
              src={loaded === photo.url ? photo.url : (photo.thumbUrl ?? photo.url)}
              alt={photo.alt}
              draggable={false}
              className="max-h-full max-w-full object-contain"
              style={{
                transform: `translate3d(${view.x}px, ${view.y}px, 0) scale(${view.s})`,
                transition: animate ? 'transform var(--duration-base) var(--ease-out)' : 'none',
              }}
            />
          </div>

          <Dialog.Description className="safe-x py-1 text-center text-sm text-on-band-muted max-sm:sr-only">
            {t('viewer.hint')}
          </Dialog.Description>
          <div className="safe-x flex min-h-16 items-center justify-center gap-1 sm:gap-2">
            {total > 1 && (
              <button
                type="button"
                className={tool}
                onClick={() => go(-1)}
                disabled={index === 0}
                aria-label={t('viewer.prev')}
              >
                <ChevronLeft size={24} aria-hidden="true" />
              </button>
            )}
            <button
              type="button"
              className={tool}
              onClick={() => byButton(view.s / STEP)}
              disabled={!zoomed}
              aria-label={t('viewer.zoomOut')}
            >
              <Minus size={22} aria-hidden="true" />
            </button>
            <button
              type="button"
              className={tool}
              onClick={() => byButton(MIN)}
              disabled={!zoomed}
              aria-label={t('viewer.reset')}
            >
              <Scan size={22} aria-hidden="true" />
            </button>
            <button
              type="button"
              className={tool}
              onClick={() => byButton(view.s * STEP)}
              disabled={view.s >= MAX}
              aria-label={t('viewer.zoomIn')}
            >
              <Plus size={22} aria-hidden="true" />
            </button>
            {total > 1 && (
              <button
                type="button"
                className={tool}
                onClick={() => go(1)}
                disabled={index === total - 1}
                aria-label={t('viewer.next')}
              >
                <ChevronRight size={24} aria-hidden="true" />
              </button>
            )}
          </div>
          <p className="sr-only" aria-live="polite">
            {announce}
          </p>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
