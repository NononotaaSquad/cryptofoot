import { z } from "zod";

// Only `next dev` and Vitest get a default. `next build` runs with
// NODE_ENV=production, so a build without the variable fails instead of
// silently baking localhost into the bundle (e.g. a Docker image).
// NODE_ENV is set by Next.js/Vitest themselves, not by the developer, so it
// does not belong in turbo.json's env inputs.
// eslint-disable-next-line turbo/no-undeclared-env-vars
const isProduction = process.env.NODE_ENV === "production";
const developmentDefault = isProduction ? undefined : "http://localhost:8080";

const envSchema = z.object({
  NEXT_PUBLIC_API_URL: z
    .url({ protocol: /^https?$/ })
    .transform((url) => url.replace(/\/+$/, "")),
});

// Next.js inlines only literal `process.env.NEXT_PUBLIC_*` references.
const parsed = envSchema.safeParse({
  NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL ?? developmentDefault,
});

if (!parsed.success) {
  throw new Error(
    `Invalid or missing NEXT_PUBLIC_API_URL (got: ${String(process.env.NEXT_PUBLIC_API_URL)}). ` +
      "Set it to the backend base URL, e.g. http://localhost:8080 - see apps/web/.env.example. " +
      "It is inlined at build time, so pass it to `next build` / `docker build --build-arg`."
  );
}

export const env = parsed.data;
