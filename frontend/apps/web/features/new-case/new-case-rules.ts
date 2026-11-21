import {
  type MultiValueIdentifierKind,
  type NewCaseFormValues,
  multiValueIdentifierKinds,
} from "./identifier-fields";

// Five narrowing kinds hold lists; the cryptocurrency address and the full
// name hold one value or `null` when absent (backend `CreateCaseRequest`).
export type NewCaseIdentifiers = Record<MultiValueIdentifierKind, string[]> & {
  cryptocurrencyAddress: string | null;
  fullName: string | null;
};

export type MinimumDataDecision =
  | { result: "ok"; identifiers: NewCaseIdentifiers }
  | { result: "empty" }
  | { result: "fullNameOnly" };

// A multi-value field may hold several values separated by commas. Values are
// only trimmed, not normalised or deduplicated (open question V6).
export function splitValues(value: string): string[] {
  return value
    .split(",")
    .map((part) => part.trim())
    .filter((part) => part !== "");
}

// A single-value field is only trimmed, never split on commas; an empty or
// whitespace-only value counts as absent.
function singleValue(value: string): string | null {
  return value.trim() || null;
}

// Invariant E1: at least one narrowing identifier; a full name alone is not
// enough. Returns the parsed identifiers with `ok`, so the caller does not
// parse the form again.
export function decideMinimumData(
  values: NewCaseFormValues
): MinimumDataDecision {
  const multiValue: Record<MultiValueIdentifierKind, string[]> = {
    nickname: splitValues(values.nickname),
    login: splitValues(values.login),
    email: splitValues(values.email),
    messengerId: splitValues(values.messengerId),
    profileUrl: splitValues(values.profileUrl),
  };
  const cryptocurrencyAddress = singleValue(values.cryptocurrencyAddress);
  const fullName = singleValue(values.fullName);

  const hasNarrowingIdentifier =
    cryptocurrencyAddress !== null ||
    multiValueIdentifierKinds.some((kind) => multiValue[kind].length > 0);

  if (hasNarrowingIdentifier) {
    return {
      result: "ok",
      identifiers: { ...multiValue, cryptocurrencyAddress, fullName },
    };
  }

  return fullName === null ? { result: "empty" } : { result: "fullNameOnly" };
}
