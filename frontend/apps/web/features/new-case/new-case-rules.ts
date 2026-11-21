import {
  type NarrowingIdentifierKind,
  type NewCaseFormValues,
  narrowingIdentifierKinds,
} from "./identifier-fields";

export type NewCaseIdentifiers = Record<NarrowingIdentifierKind, string[]> & {
  fullName: string | null;
};

export type MinimumDataDecision =
  | { result: "ok"; identifiers: NewCaseIdentifiers }
  | { result: "empty" }
  | { result: "fullNameOnly" };

// One field may hold several values separated by commas. Values are only
// trimmed, not normalised or deduplicated (open question V6).
export function splitValues(value: string): string[] {
  return value
    .split(",")
    .map((part) => part.trim())
    .filter((part) => part !== "");
}

// Invariant E1: at least one narrowing identifier; a full name alone is not
// enough. Returns the parsed identifiers with `ok`, so the caller does not
// parse the form again.
export function decideMinimumData(
  values: NewCaseFormValues
): MinimumDataDecision {
  const narrowing: Record<NarrowingIdentifierKind, string[]> = {
    nickname: splitValues(values.nickname),
    login: splitValues(values.login),
    email: splitValues(values.email),
    messengerId: splitValues(values.messengerId),
    profileAddress: splitValues(values.profileAddress),
    cryptocurrencyAddress: splitValues(values.cryptocurrencyAddress),
  };
  const fullName = values.fullName.trim() || null;

  const hasNarrowingIdentifier = narrowingIdentifierKinds.some(
    (kind) => narrowing[kind].length > 0
  );

  if (hasNarrowingIdentifier) {
    return { result: "ok", identifiers: { ...narrowing, fullName } };
  }

  return fullName === null ? { result: "empty" } : { result: "fullNameOnly" };
}
