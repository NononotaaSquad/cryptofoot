# Next.js web app (apps/web) as a standalone Node server.
# Build from the repository root:
#   docker build -f frontend/Dockerfile --build-arg NEXT_PUBLIC_API_URL=https://api.example.com frontend

FROM node:22-slim AS deps
WORKDIR /app
COPY package.json package-lock.json ./
COPY apps/web/package.json apps/web/
COPY packages/ui/package.json packages/ui/
COPY packages/eslint-config/package.json packages/eslint-config/
COPY packages/typescript-config/package.json packages/typescript-config/
RUN npm install -g "$(node -p "require('./package.json').packageManager")" && npm ci

# Continue from deps: npm ci also creates workspace-level node_modules
# (apps/web/node_modules), which the type check in `next build` needs.
# .dockerignore keeps `COPY . .` from overwriting any node_modules.
FROM deps AS build
# Inlined into the browser bundle by `next build`; there is no runtime override.
ARG NEXT_PUBLIC_API_URL
ENV NEXT_PUBLIC_API_URL=${NEXT_PUBLIC_API_URL}
ENV NEXT_TELEMETRY_DISABLED=1
RUN test -n "$NEXT_PUBLIC_API_URL" || \
  (echo "ERROR: build with --build-arg NEXT_PUBLIC_API_URL=<backend URL> (see apps/web/.env.example)" >&2 && exit 1)
COPY . .
RUN npm run build -w web

FROM node:22-slim AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000
ENV HOSTNAME=0.0.0.0
COPY --from=build --chown=node:node /app/apps/web/.next/standalone ./
COPY --from=build --chown=node:node /app/apps/web/.next/static ./apps/web/.next/static
# apps/web has no public/ folder yet. When one is added, uncomment:
# COPY --from=build --chown=node:node /app/apps/web/public ./apps/web/public
USER node
EXPOSE 3000
CMD ["node", "apps/web/server.js"]
