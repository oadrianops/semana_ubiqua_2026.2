import js from '@eslint/js';
import i18next from 'eslint-plugin-i18next';
import jsxA11y from 'eslint-plugin-jsx-a11y';
import reactHooks from 'eslint-plugin-react-hooks';
import globals from 'globals';
import tseslint from 'typescript-eslint';

// Lint de acessibilidade (jsx-a11y) e de textos fixos (i18next/no-literal-string) — especificação 15.3.
export default tseslint.config(
  {
    ignores: [
      'dist',
      'dev-dist',
      'coverage',
      'playwright-report',
      'test-results',
      'src/shared/api/schema.d.ts',
    ],
  },
  {
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    files: ['**/*.{ts,tsx}'],
    languageOptions: { ecmaVersion: 2022, globals: { ...globals.browser, ...globals.serviceworker } },
    plugins: { 'react-hooks': reactHooks, 'jsx-a11y': jsxA11y, i18next },
    rules: {
      ...reactHooks.configs.recommended.rules,
      ...jsxA11y.flatConfigs.recommended.rules,
      'react-hooks/set-state-in-effect': 'off',
      // Tabelas que rolam para o lado no celular: role="region" com tabIndex={0} (axe scrollable-region-focusable).
      'jsx-a11y/no-noninteractive-tabindex': ['error', { roles: ['tabpanel', 'region'], tags: [] }],
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],
      'no-restricted-syntax': [
        'error',
        { selector: "JSXAttribute[name.name='dangerouslySetInnerHTML']", message: 'Proibido (seção 12.3).' },
      ],
      'no-restricted-globals': [
        'error',
        { name: 'localStorage', message: 'Token nunca em localStorage (8.6); use src/shared/lib/storage.' },
      ],
    },
  },
  {
    files: ['src/**/*.tsx'],
    ignores: ['src/**/*.test.tsx', 'src/test/**'],
    rules: {
      'i18next/no-literal-string': [
        'error',
        {
          mode: 'jsx-text-only',
          'jsx-attributes': { include: ['alt', 'aria-label', 'title', 'placeholder', 'label'] },
          words: {
            exclude: [
              'NANDESK',
              'R\\$',
              '·',
              '—',
              '/',
              '\\+',
              '%',
              '\\d+',
              '[A-Z]{2}',
              'PT',
              'EN',
              'ES',
              '…',
              ':',
              '\\(',
              '\\)',
              '×',
              '•',
              ',',
              '“',
              '”',
              '–',
              '"',
            ],
          },
        },
      ],
    },
  },
  {
    files: ['e2e/**/*.ts', '*.config.ts', 'src/test/**', '**/*.test.{ts,tsx}'],
    languageOptions: { globals: { ...globals.node } },
    rules: { 'no-restricted-globals': 'off' },
  },
);
