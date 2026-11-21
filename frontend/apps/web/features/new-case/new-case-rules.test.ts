import { describe, expect, it } from "vitest";

import {
  type NewCaseFormValues,
  emptyNewCaseFormValues,
  multiValueIdentifierKinds,
} from "./identifier-fields";
import { decideMinimumData, splitValues } from "./new-case-rules";

function form(values: Partial<NewCaseFormValues>): NewCaseFormValues {
  return { ...emptyNewCaseFormValues, ...values };
}

describe("splitValues", () => {
  it("splits several values in one field and trims them", () => {
    expect(splitValues(" krypto_janek ,janek2,  jk ")).toEqual([
      "krypto_janek",
      "janek2",
      "jk",
    ]);
  });

  it("drops empty parts between commas", () => {
    expect(splitValues(",a,, ,b,")).toEqual(["a", "b"]);
  });

  it("returns no values for an empty field", () => {
    expect(splitValues("")).toEqual([]);
  });
});

describe("decideMinimumData", () => {
  it("returns empty for an empty form", () => {
    expect(decideMinimumData(form({}))).toEqual({ result: "empty" });
  });

  it("returns empty when the fields hold only spaces and commas", () => {
    expect(
      decideMinimumData(form({ nickname: " , ,", email: "   ", login: ",," }))
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

  it.each(multiValueIdentifierKinds)("returns ok for %s alone", (kind) => {
    const decision = decideMinimumData(form({ [kind]: " value " }));

    expect(decision.result).toBe("ok");
    if (decision.result === "ok") {
      expect(decision.identifiers[kind]).toEqual(["value"]);
      expect(decision.identifiers.walletAddress).toBeNull();
      expect(decision.identifiers.fullName).toBeNull();
    }
  });

  it("returns ok for walletAddress alone with the trimmed address", () => {
    const decision = decideMinimumData(
      form({ walletAddress: "  bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkf " })
    );

    expect(decision.result).toBe("ok");
    if (decision.result === "ok") {
      expect(decision.identifiers.walletAddress).toBe(
        "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkf"
      );
      expect(decision.identifiers.fullName).toBeNull();
    }
  });

  it("returns empty when the wallet address holds only spaces", () => {
    expect(decideMinimumData(form({ walletAddress: "   " }))).toEqual({
      result: "empty",
    });
  });

  it("keeps a wallet address with a comma as one trimmed value", () => {
    const decision = decideMinimumData(form({ walletAddress: " abc, def " }));

    expect(decision.result).toBe("ok");
    if (decision.result === "ok") {
      expect(decision.identifiers.walletAddress).toBe("abc, def");
    }
  });

  it("returns ok with the parsed identifiers for a narrowing identifier plus the full name", () => {
    expect(
      decideMinimumData(
        form({
          nickname: "krypto_janek, janek2",
          email: "jan@example.com",
          fullName: "  Jan Kowalski ",
        })
      )
    ).toEqual({
      result: "ok",
      identifiers: {
        nickname: ["krypto_janek", "janek2"],
        login: [],
        email: ["jan@example.com"],
        messengerId: [],
        profileUrl: [],
        walletAddress: null,
        fullName: "Jan Kowalski",
      },
    });
  });
});
