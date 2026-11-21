import pluginReactHooks from "eslint-plugin-react-hooks";
import globals from "globals";

import { createBaseConfig } from "./base.js";

/**
 * ESLint configuration for internal React libraries (packages/ui).
 *
 * @param {string} tsconfigRootDir pass `import.meta.dirname`
 * @param {import("eslint").Linter.Config[]} [extra] package-specific overrides
 */
export const createReactInternalConfig = (tsconfigRootDir, extra = []) =>
  createBaseConfig(tsconfigRootDir, [
    {
      languageOptions: {
        globals: {
          ...globals.serviceworker,
          ...globals.browser,
        },
      },
    },
    pluginReactHooks.configs.flat.recommended,
    ...extra,
  ]);
