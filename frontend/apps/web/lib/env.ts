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
  // Lists the labelled demo Frequency in the Schedule popover. Only "true" is accepted.
  NEXT_PUBLIC_DEMO_SCHEDULE: z
    .literal("true")
    .optional()
    .transform((value) => value === "true"),
});

// Next.js inlines only literal `process.env.NEXT_PUBLIC_*` references.
const parsed = envSchema.safeParse({
  NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL ?? developmentDefault,
  NEXT_PUBLIC_DEMO_SCHEDULE:
    process.env.NEXT_PUBLIC_DEMO_SCHEDULE === ""
      ? undefined
      : process.env.NEXT_PUBLIC_DEMO_SCHEDULE,
});

if (!parsed.success) {
  const failed = parsed.error.issues.map((issue) => String(issue.path[0]));
  throw new Error(
    `Invalid environment variable(s): ${failed.join(", ")}. ` +
      `NEXT_PUBLIC_API_URL must be the backend base URL, e.g. http://localhost:8080 (got: ${String(process.env.NEXT_PUBLIC_API_URL)}); ` +
      `NEXT_PUBLIC_DEMO_SCHEDULE is optional and accepts only "true" (got: ${String(process.env.NEXT_PUBLIC_DEMO_SCHEDULE)}). ` +
      "See apps/web/.env.example. They are inlined at build time, so pass them to `next build` / `docker build --build-arg`."
  );
}

export const env = parsed.data;
