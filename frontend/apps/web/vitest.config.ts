import { defineConfig } from "vitest/config";

export default defineConfig({
  resolve: {
    // Reuse the `@/*` and `@workspace/ui/*` aliases from tsconfig.json.
    tsconfigPaths: true,
  },
  test: {
    environment: "node",
    include: ["{features,lib}/**/*.test.ts"],
    exclude: ["**/node_modules/**", "**/.next/**"],
  },
});
