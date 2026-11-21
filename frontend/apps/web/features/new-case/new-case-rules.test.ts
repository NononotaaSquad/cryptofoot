import { describe, expect, it } from "vitest";

import {
  type NewCaseFormValues,
  emptyNewCaseFormValues,
  narrowingIdentifierKinds,
} from "./identifier-fields";
import { decideMinimumData } from "./new-case-rules";

function form(values: Partial<NewCaseFormValues>): NewCaseFormValues {
  return { ...emptyNewCaseFormValues, ...values };
}

describe("decideMinimumData", () => {
  it("returns empty for an empty form", () => {
    expect(decideMinimumData(form({}))).toEqual({ result: "empty" });
  });

  it("returns empty when the fields hold only spaces", () => {
    expect(
      decideMinimumData(form({ nickname: "  ", email: "   ", login: " " }))
    ).toEqual({ result: "empty" });
  });

  it("returns fullNameOnly when only the full name is given", () => {
    expect(decideMinimumData(form({ fullName: "Jan Kowalski" }))).toEqual({
      result: "fullNameOnly",
    });
  });

  it("returns empty when the full name holds only spaces", () => {
    expect(decideMinimumData(form({ fullName: "   " }))).toEqual({
      result: "empty",
    });
  });

  it.each(narrowingIdentifierKinds)(
    "returns ok for %s alone with the trimmed value",
    (kind) => {
      const decision = decideMinimumData(form({ [kind]: " value " }));

      expect(decision.result).toBe("ok");
      if (decision.result === "ok") {
        expect(decision.identifiers[kind]).toBe("value");
        expect(decision.identifiers.fullName).toBeNull();
      }
    }
  );

  it("keeps a value with a comma as one trimmed value", () => {
    const decision = decideMinimumData(
      form({ nickname: " krypto_janek, janek2 " })
    );

    expect(decision.result).toBe("ok");
    if (decision.result === "ok") {
      expect(decision.identifiers.nickname).toBe("krypto_janek, janek2");
    }
  });

  it("returns ok with the parsed identifiers for a narrowing identifier plus the full name", () => {
    expect(
      decideMinimumData(
        form({
          email: " jan@example.com ",
          walletAddress: "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkf",
          fullName: "  Jan Kowalski ",
        })
      )
    ).toEqual({
      result: "ok",
      identifiers: {
        nickname: null,
        login: null,
        email: "jan@example.com",
        messengerId: null,
        profileUrl: null,
        walletAddress: "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkf",
        fullName: "Jan Kowalski",
      },
    });
  });
});
