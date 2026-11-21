import type { PersonIdentifiersResponse } from "@/lib/api/generated/model";

// The kinds of identifiers of an investigated person and their Polish names,
// shared by New case (form fields) and Case details (header) (Q33).

// The six narrowing kinds in the order of the New case form. This order also
// picks the label of a Case without a full name (Q34).
export const narrowingIdentifierKinds = [
  "nickname",
  "login",
  "email",
  "messengerId",
  "profileUrl",
  "walletAddress",
] as const;

export type NarrowingIdentifierKind = (typeof narrowingIdentifierKinds)[number];

export type IdentifierKind = NarrowingIdentifierKind | "fullName";

export const identifierKindLabels: Record<IdentifierKind, string> = {
  nickname: "Nick",
  login: "Login",
  email: "Adres e-mail",
  messengerId: "Identyfikator z komunikatora",
  profileUrl: "Adres profilu",
  walletAddress: "Adres portfela",
  fullName: "Imię i nazwisko",
};

export interface PresentIdentifier {
  kind: NarrowingIdentifierKind;
  value: string;
}

// The backend sends `null` or leaves out an absent identifier; a blank value
// counts as absent too.
function present(value: string | null | undefined): string | null {
  const trimmed = value?.trim() ?? "";
  return trimmed === "" ? null : trimmed;
}

/** The narrowing identifiers of a Case that have a value, in form order. */
export function presentNarrowingIdentifiers(
  identifiers: PersonIdentifiersResponse
): PresentIdentifier[] {
  return narrowingIdentifierKinds.flatMap((kind) => {
    const value = present(identifiers[kind]);
    return value === null ? [] : [{ kind, value }];
  });
}

/**
 * The label of a Case: the full name or, without one, the first narrowing
 * identifier in form order (Q34). `null` only for a Case with no identifier
 * at all, which the minimum data rule does not allow.
 */
export function caseLabel(
  identifiers: PersonIdentifiersResponse
): string | null {
  return (
    present(identifiers.fullName) ??
    presentNarrowingIdentifiers(identifiers)[0]?.value ??
    null
  );
}
