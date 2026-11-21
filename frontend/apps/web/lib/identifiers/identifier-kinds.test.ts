import { describe, expect, it } from "vitest";

import type { PersonIdentifiersResponse } from "@/lib/api/generated/model";

import { caseLabel, presentNarrowingIdentifiers } from "./identifier-kinds";

function identifiers(
  values: Partial<PersonIdentifiersResponse>
): PersonIdentifiersResponse {
  return {
    nickname: null,
    login: null,
    email: null,
    messengerId: null,
    profileUrl: null,
    walletAddress: null,
    fullName: null,
    ...values,
  };
}

describe("caseLabel", () => {
  it("is the full name when the Case has one", () => {
    expect(
      caseLabel(identifiers({ fullName: "Jan Kowalski", nickname: "janek" }))
    ).toBe("Jan Kowalski");
  });

  it("is the first narrowing identifier in form order without a full name", () => {
    expect(
      caseLabel(
        identifiers({
          walletAddress: "bc1qxy",
          email: "jan@example.com",
          login: "jkowalski84",
        })
      )
    ).toBe("jkowalski84");
  });

  it("skips blank values", () => {
    expect(
      caseLabel(identifiers({ fullName: "  ", nickname: "", email: "a@b.pl" }))
    ).toBe("a@b.pl");
  });

  it("is null for a Case without identifiers", () => {
    expect(caseLabel({})).toBeNull();
  });
});

describe("presentNarrowingIdentifiers", () => {
  it("lists the narrowing identifiers with a value in form order, without the full name", () => {
    expect(
      presentNarrowingIdentifiers(
        identifiers({
          walletAddress: "bc1qxy",
          fullName: "Jan Kowalski",
          nickname: "krypto_janek",
          messengerId: "@janek_k",
        })
      )
    ).toEqual([
      { kind: "nickname", value: "krypto_janek" },
      { kind: "messengerId", value: "@janek_k" },
      { kind: "walletAddress", value: "bc1qxy" },
    ]);
  });
});
