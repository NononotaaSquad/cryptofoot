import path from "node:path";

import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  // Trace from the monorepo root so @workspace/* packages and the hoisted
  // node_modules end up in .next/standalone.
  outputFileTracingRoot: path.join(import.meta.dirname, "../.."),
  typedRoutes: true,
  transpilePackages: ["@workspace/ui"],
};

export default nextConfig;
