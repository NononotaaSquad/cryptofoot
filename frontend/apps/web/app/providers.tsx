"use client";

import {
  QueryClient,
  QueryClientProvider,
  environmentManager,
} from "@tanstack/react-query";
import { Toaster } from "@workspace/ui/components/sonner";
import { ThemeProvider } from "next-themes";
import type { ReactNode } from "react";

import { isApiError } from "@/lib/api/client";

const MAX_RETRIES = 3;

function makeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 60 * 1000,
        retry: (failureCount, error) => {
          // A 4xx will not succeed on retry; network errors and 5xx might.
          if (
            isApiError(error) &&
            error.httpStatus >= 400 &&
            error.httpStatus < 500
          ) {
            return false;
          }

          return failureCount < MAX_RETRIES;
        },
      },
    },
  });
}

let browserQueryClient: QueryClient | undefined;

// One client per server request (no data shared between users), one for the
// whole browser session (so a suspending render does not lose the cache).
function getQueryClient() {
  if (environmentManager.isServer()) return makeQueryClient();
  browserQueryClient ??= makeQueryClient();
  return browserQueryClient;
}

export function Providers({ children }: { children: ReactNode }) {
  const queryClient = getQueryClient();

  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider
        attribute="class"
        defaultTheme="system"
        enableSystem
        disableTransitionOnChange
      >
        {children}
        <Toaster />
      </ThemeProvider>
    </QueryClientProvider>
  );
}
