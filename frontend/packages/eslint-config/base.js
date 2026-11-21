import js from "@eslint/js";
import eslintConfigPrettier from "eslint-config-prettier/flat";
import turboPlugin from "eslint-plugin-turbo";
import { defineConfig, globalIgnores } from "eslint/config";
import tseslint from "typescript-eslint";

/**
 * Shared strict, type-aware ESLint configuration.
 *
 * Each workspace passes its own directory (`import.meta.dirname`) so that the
 * typescript-eslint project service resolves the right tsconfig.json.
 *
 * @param {string} tsconfigRootDir
 * @param {import("eslint").Linter.Config[]} [extra] configs inserted before Prettier
 */
export const createBaseConfig = (tsconfigRootDir, extra = []) =>
  defineConfig([
    globalIgnores([
      "**/node_modules/**",
      "**/dist/**",
      "**/.turbo/**",
      "**/coverage/**",
    ]),
    js.configs.recommended,
    tseslint.configs.strictTypeChecked,
    tseslint.configs.stylisticTypeChecked,
    {
      languageOptions: {
        parserOptions: {
          projectService: true,
          tsconfigRootDir,
        },
      },
      plugins: {
        turbo: turboPlugin,
      },
      rules: {
        "turbo/no-undeclared-env-vars": "error",
        "@typescript-eslint/consistent-type-imports": "error",
        "no-console": ["error", { allow: ["warn", "error"] }],
      },
    },
    ...extra,
    {
      files: ["**/*.{js,mjs,cjs}"],
      extends: [tseslint.configs.disableTypeChecked],
    },
    eslintConfigPrettier,
  ]);
