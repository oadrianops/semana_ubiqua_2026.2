/// <reference types="vitest/config" />
import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import { defineConfig, type Plugin } from 'vite';
import { VitePWA } from 'vite-plugin-pwa';

/** Pré-carrega só a fonte do texto (9.14); a de títulos chega pelo CSS com font-display: swap. */
function preloadTextFont(): Plugin {
  return {
    name: 'nandesk:preload-text-font',
    apply: 'build',
    transformIndexHtml: {
      order: 'post',
      handler(html, ctx) {
        const font = Object.keys(ctx.bundle ?? {}).find((name) =>
          /atkinson-hyperlegible-next-latin-wght-normal-[\w-]+\.woff2$/.test(name),
        );
        if (!font) return html;
        return [
          {
            tag: 'link',
            attrs: { rel: 'preload', as: 'font', type: 'font/woff2', href: `/${font}`, crossorigin: '' },
            injectTo: 'head',
          },
        ];
      },
    },
  };
}

// PWA com injectManifest por causa do push (ADR-0011, seção 9.13); manifesto conforme RF-35.
export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    preloadTextFont(),
    VitePWA({
      strategies: 'injectManifest',
      srcDir: 'src',
      filename: 'sw.ts',
      registerType: 'prompt',
      injectRegister: false,
      injectManifest: {
        globPatterns: ['**/*.{js,css,html,woff2,svg,png,ico,webmanifest}'],
        globIgnores: ['**/config.js', 'screenshots/**', 'brand/png/nandesk-logo*'],
        maximumFileSizeToCacheInBytes: 600 * 1024,
      },
      devOptions: { enabled: false, type: 'module' },
      manifest: {
        id: '/',
        name: 'NANDESK: feiras de Belém',
        short_name: 'NANDESK',
        description:
          'Em qual feira e em qual box encontrar castanha-do-pará, jambu, banho de cheiro e outros produtos do Ver-o-Peso e da Feira da 25.',
        start_url: '/',
        scope: '/',
        display: 'standalone',
        orientation: 'portrait',
        theme_color: '#4A2152',
        background_color: '#FFFFFF',
        lang: 'pt-BR',
        dir: 'ltr',
        categories: ['shopping', 'travel', 'food'],
        icons: [
          { src: '/brand/png/pwa-192x192.png', sizes: '192x192', type: 'image/png', purpose: 'any' },
          { src: '/brand/png/pwa-512x512.png', sizes: '512x512', type: 'image/png', purpose: 'any' },
          {
            src: '/brand/png/maskable-icon-512x512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'maskable',
          },
          {
            src: '/brand/png/monochrome-icon-512x512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'monochrome',
          },
        ],
        screenshots: [
          {
            src: '/screenshots/home-narrow.png',
            sizes: '780x1688',
            type: 'image/png',
            form_factor: 'narrow',
            label: 'Busca de produtos com feira e box',
          },
          {
            src: '/screenshots/home-wide.png',
            sizes: '1440x900',
            type: 'image/png',
            form_factor: 'wide',
            label: 'Feiras de Belém e categorias de produtos',
          },
        ],
        shortcuts: [
          {
            name: 'Buscar',
            short_name: 'Buscar',
            url: '/search',
            icons: [{ src: '/brand/png/pwa-192x192.png', sizes: '192x192' }],
          },
          {
            name: 'Minhas tarefas',
            short_name: 'Tarefas',
            url: '/collaborate',
            icons: [{ src: '/brand/png/pwa-192x192.png', sizes: '192x192' }],
          },
        ],
      },
    }),
  ],
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080' },
  },
  build: {
    target: 'es2022',
    cssCodeSplit: true,
    sourcemap: false,
    reportCompressedSize: false,
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
    css: false,
    coverage: {
      provider: 'v8',
      reporter: ['text-summary', 'lcov'],
      include: [
        'src/shared/lib/**',
        'src/shared/offline/**',
        'src/shared/api/**',
        'src/features/**/*.{ts,tsx}',
        'src/shared/hooks/**',
      ],
      exclude: [
        '**/*.test.*',
        'src/test/**',
        '**/*.d.ts',
        'src/features/**/pages/**',
        'src/features/admin/**',
      ],
      thresholds: { lines: 70, statements: 70 },
    },
  },
});
