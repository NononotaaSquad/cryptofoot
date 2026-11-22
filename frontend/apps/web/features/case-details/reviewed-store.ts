import { useMemo, useSyncExternalStore } from "react";

import {
  markAfterReview,
  parseMark,
  serializeMark,
  type AnalysisOrder,
  type ReviewedMark,
} from "./reviewed-mark";

// Reviewed mark of a Case, kept only in this browser's `localStorage`, one key
// per Case id; no backend call (Q4, user decision 2026-11-22). The only module
// that touches the key. Details, the list and the badge share it.

const keyPrefix = "olcys.case-reviewed.";

// Writes in this tab do not fire the `storage` event, so the store tells its
// own subscribers; the `storage` event covers the other tabs.
const listeners = new Set<() => void>();

function keyOf(caseId: string): string {
  return keyPrefix + caseId;
}

function subscribe(onChange: () => void): () => void {
  listeners.add(onChange);
  window.addEventListener("storage", onChange);
  return () => {
    listeners.delete(onChange);
    window.removeEventListener("storage", onChange);
  };
}

function notify(): void {
  for (const listener of listeners) listener();
}

// The raw string is the snapshot: a primitive, so it is stable between reads.
function readRaw(caseId: string): string | null {
  try {
    return window.localStorage.getItem(keyOf(caseId));
  } catch {
    // Storage blocked by the browser: the Case reads as not reviewed.
    return null;
  }
}

/** The valid mark of the Case, or null (none, legacy, malformed, blocked). */
export function readMark(caseId: string): ReviewedMark | null {
  return parseMark(readRaw(caseId));
}

/**
 * Marks `target` as reviewed. Reads the stored mark again at write time, so a
 * mark of another tab or a newer one is never moved back. Returns whether the
 * Analysis is covered after the call: false when the storage did not keep it.
 */
export function markReviewed(caseId: string, target: AnalysisOrder): boolean {
  const next = markAfterReview(
    readMark(caseId),
    target,
    new Date().toISOString()
  );
  if (next === null) return true;
  try {
    window.localStorage.setItem(keyOf(caseId), serializeMark(next));
  } catch {
    // Storage blocked or full: the mark is not kept.
    return false;
  }
  notify();
  return readMark(caseId)?.id === next.id;
}

/** Removes the mark of this Case only. Returns false when it stayed. */
export function unmarkReviewed(caseId: string): boolean {
  try {
    window.localStorage.removeItem(keyOf(caseId));
  } catch {
    return false;
  }
  notify();
  return readRaw(caseId) === null;
}

/**
 * The Case's valid Reviewed mark. The server render and the first client
 * render read `null`, then the stored value, so the HTML never mismatches on
 * hydration.
 */
export function useReviewedMark(caseId: string): ReviewedMark | null {
  const raw = useSyncExternalStore(
    subscribe,
    () => readRaw(caseId),
    () => null
  );
  return useMemo(() => parseMark(raw), [raw]);
}
