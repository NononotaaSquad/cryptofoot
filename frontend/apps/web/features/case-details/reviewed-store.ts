import { useSyncExternalStore } from "react";

// Reviewed state of a Case, kept only in this browser's `localStorage`, one
// key per Case id; no backend call (Q4). The only module that touches the key.

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

function readReviewed(caseId: string): boolean {
  try {
    return window.localStorage.getItem(keyOf(caseId)) !== null;
  } catch {
    // Storage blocked by the browser: the Case reads as not Reviewed.
    return false;
  }
}

function writeReviewed(caseId: string, reviewed: boolean): void {
  try {
    if (reviewed) {
      window.localStorage.setItem(keyOf(caseId), new Date().toISOString());
    } else {
      window.localStorage.removeItem(keyOf(caseId));
    }
  } catch {
    // Storage blocked or full: the mark is not kept; nothing else to do.
  }
  for (const listener of listeners) listener();
}

/**
 * Whether the Analyst marked the Case as Reviewed, and a setter. The server
 * render and the first client render read `false`, then the stored value, so
 * the HTML never mismatches on hydration.
 */
export function useCaseReviewed(
  caseId: string
): [reviewed: boolean, setReviewed: (reviewed: boolean) => void] {
  const reviewed = useSyncExternalStore(
    subscribe,
    () => readReviewed(caseId),
    () => false
  );
  return [
    reviewed,
    (next) => {
      writeReviewed(caseId, next);
    },
  ];
}
