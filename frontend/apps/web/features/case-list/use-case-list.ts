"use client";

import { useListCases } from "@/lib/api/generated/case-controller/case-controller";

// The visible screen reads the list again every 10 s. TanStack Query pauses
// the interval while the tab is hidden and the window focus refetches a list
// older than `staleTime`. Polling only reads: it starts no Analysis and marks
// nothing as reviewed.
const pollIntervalMs = 10_000;
const staleTimeMs = 5_000;

/**
 * The one query of the Case list. The list view and the header counter both
 * call this, so they share one cache entry, one request and one timer.
 */
export function useCaseList() {
  return useListCases({
    query: {
      refetchInterval: pollIntervalMs,
      refetchOnWindowFocus: true,
      staleTime: staleTimeMs,
    },
  });
}
