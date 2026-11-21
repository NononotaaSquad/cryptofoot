import { describe, expect, it } from "vitest";

import type { CreateCaseRequest } from "@/lib/api/generated/model";
import { narrowingIdentifierKinds } from "@/lib/identifiers/identifier-kinds";

import { type NewCaseRunInput, toNewCaseRunInput } from "./run-analysis";

const identifiers: CreateCaseRequest = { nickname: "zenek_fx" };
const previous: NewCaseRunInput = {
  kind: "newCase",
  identifiers,
  caseId: "case-1",
};

describe("toNewCaseRunInput", () => {
  it("reuses the created Case when the submitted identifiers are unchanged", () => {
    const resubmitted = { nickname: "zenek_fx", email: null };
    expect(toNewCaseRunInput(resubmitted, previous)).toEqual({
      kind: "newCase",
      identifiers: resubmitted,
      caseId: "case-1",
    });
  });

  it.each([...narrowingIdentifierKinds, "fullName"])(
    "does not reuse the Case when %s changed",
    (kind) => {
      const changed = { ...identifiers, [kind]: "different" };
      expect(toNewCaseRunInput(changed, previous)).toEqual({
        kind: "newCase",
        identifiers: changed,
      });
    }
  );

  it("creates a Case when the previous attempt did not create one", () => {
    const first = toNewCaseRunInput(identifiers);
    expect(first).toEqual({ kind: "newCase", identifiers });
    expect(toNewCaseRunInput(identifiers, first)).toEqual(first);
  });
});
