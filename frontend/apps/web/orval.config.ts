import { defineConfig } from "orval";

export default defineConfig({
  api: {
    input: "./lib/api/openapi.json",
    output: {
      client: "react-query",
      httpClient: "fetch",
      mode: "tags-split",
      target: "./lib/api/generated",
      schemas: "./lib/api/generated/model",
      clean: true,
      formatter: "prettier",
      override: {
        mutator: { path: "./lib/api/client.ts", name: "customFetch" },
        fetch: {
          includeHttpResponseReturnType: true,
          // client.ts throws on non-2xx, so only success types can be returned.
          forceSuccessResponse: true,
        },
      },
    },
  },
});
