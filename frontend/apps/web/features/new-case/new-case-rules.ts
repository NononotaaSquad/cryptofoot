import {
  type NewCaseFieldName,
  type NewCaseFormValues,
  narrowingIdentifierKinds,
} from "./identifier-fields";

// Every identifier holds one value or `null` when absent (backend
// `CreateCaseRequest`).
export type NewCaseIdentifiers = Record<NewCaseFieldName, string | null>;

export type MinimumDataDecision =
  | { result: "ok"; identifiers: NewCaseIdentifiers }
  | { result: "empty" }
  | { result: "fullNameOnly" };

// A field is only trimmed, never split on commas; an empty or whitespace-only
// value counts as absent. Values are not normalised (open question V6).
function singleValue(value: string): string | null {
  return value.trim() || null;
}

// Invariant E1: at least one narrowing identifier; a full name alone is not
// enough. Returns the parsed identifiers with `ok`, so the caller does not
// parse the form again.
export function decideMinimumData(
  values: NewCaseFormValues
): MinimumDataDecision {
  const identifiers: NewCaseIdentifiers = {
    nickname: singleValue(values.nickname),
    login: singleValue(values.login),
    email: singleValue(values.email),
    messengerId: singleValue(values.messengerId),
    profileUrl: singleValue(values.profileUrl),
    walletAddress: singleValue(values.walletAddress),
    fullName: singleValue(values.fullName),
  };

  const hasNarrowingIdentifier = narrowingIdentifierKinds.some(
    (kind) => identifiers[kind] !== null
  );

  if (hasNarrowingIdentifier) {
    return { result: "ok", identifiers };
  }

  return identifiers.fullName === null
    ? { result: "empty" }
    : { result: "fullNameOnly" };
}
