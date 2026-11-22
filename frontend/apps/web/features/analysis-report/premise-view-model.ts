import type {
  PremiseAnswersResponse,
  PremiseFactsResponse,
  PremiseResponse,
  YesNoProbabilitiesResponse,
} from "@/lib/api/generated/model";

import {
  assessedByLabel,
  formatDateTime,
  formatPercent,
  identifierTypeLabel,
  premiseAnswerLabels,
  premiseAnswerWords,
  premiseAuthorIsPersonLabel,
  premiseFactLabels,
  premiseKindLabel,
  premiseLabels,
  premiseReasonLabel,
  premiseStatus,
  type PremiseStatusIcon,
} from "./report-labels";

/** One line of the facts or answers list: "label: value" and who assessed it. */
export interface PremiseLine {
  label: string;
  value: string;
  /** "Oceniono: reguła" or "Oceniono: model". */
  assessedBy: string;
}

export interface PremiseView {
  key: string;
  /** Place in the block, from 1; Punktacja lines name the Premise by it. */
  number: number;
  /** "Przesłanka 1" */
  title: string;
  /** `icon` picks the icon; `label` is the word (not colour alone). */
  status: { icon: PremiseStatusIcon; label: string };
  /** "Adres wycofany przez autora", next to the status, not instead of it. */
  withdrawnNote: string | null;
  /** Why the Premise was dismissed or needs review; `null` otherwise. */
  reason: string | null;
  /** "Dyskusja"; `null` when the Premise has no kind (a dismissed one). */
  kind: string | null;
  /** Who assessed the kind: "Oceniono: reguła" or "Oceniono: model". */
  kindAssessedBy: string;
  /** "80", "0" */
  points: string;
  sourceName: string;
  /** `null` when the link is not http(s): it is then shown as plain text. */
  url: string | null;
  urlText: string;
  /** `null` without an author: no "Autor" line. */
  author: string | null;
  /** "21.11.2026, 14:32", or `null` without a collection time. */
  collectedAt: string | null;
  /** The text window; `address` is `null` when its position is unknown,
   *  and the whole window is then in `before`. */
  window: { before: string; address: string | null; after: string };
  facts: PremiseLine[];
  /** The model's three answers; empty when the model did not answer. */
  answers: PremiseLine[];
  /** Raw numbers of the answers; `null` without a model answer. */
  technicalDetails: { label: string; value: string }[] | null;
}

const statusOrder: Record<PremiseStatusIcon, number> = {
  counted: 0,
  forReview: 1,
  dismissed: 2,
  unknown: 3,
};

/** `null` for a link that is not http(s): it must never become an `<a>`. */
export function httpUrl(url: string): string | null {
  return /^https?:\/\//i.test(url) ? url : null;
}

/**
 * The Premises of one Attribution in the order of the block: "Zaliczona",
 * "Do weryfikacji", "Odrzucona", and the API order inside a group
 * (`sort` is stable).
 */
export function premiseViews(premises: PremiseResponse[]): PremiseView[] {
  return premises
    .map((premise, index) => ({
      premise,
      index,
      status: premiseStatus(premise.status ?? ""),
    }))
    .sort((a, b) => statusOrder[a.status.icon] - statusOrder[b.status.icon])
    .map(({ premise, index, status }, position) =>
      premiseView(premise, status, index, position + 1)
    );
}

function premiseView(
  premise: PremiseResponse,
  status: PremiseView["status"],
  apiIndex: number,
  number: number
): PremiseView {
  const url = premise.url ?? "";
  const answers = premise.answers ?? null;
  // The API says who assessed kind, declaration and warning (D16); a missing
  // value counts as the rules, never as the model.
  const assessedBy = assessedByLabel(premise.assessedBy ?? "RULE");

  return {
    key: premise.id ?? String(apiIndex),
    number,
    title: premiseLabels.title(number),
    status,
    withdrawnNote: premise.withdrawn === true ? premiseLabels.withdrawn : null,
    reason:
      premise.dismissalReason == null
        ? null
        : premiseReasonLabel(premise.dismissalReason),
    kind: premise.kind == null ? null : premiseKindLabel(premise.kind),
    kindAssessedBy: assessedBy,
    points: String(premise.points ?? 0),
    sourceName: premise.sourceName ?? "",
    url: httpUrl(url),
    urlText: url,
    author: premise.author?.trim() ? premise.author : null,
    collectedAt:
      premise.collectedAt === undefined
        ? null
        : formatDateTime(premise.collectedAt),
    window: splitWindow(
      premise.text ?? "",
      premise.addressStart,
      premise.addressLength
    ),
    facts: factLines(premise.facts ?? {}),
    answers:
      answers === null
        ? []
        : answerLines(answers, premise.undetermined ?? [], assessedBy),
    technicalDetails: answers === null ? null : technicalDetails(answers),
  };
}

// Start and length are UTF-16 code units, the same as Kotlin `String`
// indices, so `slice` needs no conversion. A position outside the window
// gives the whole text without a highlight.
function splitWindow(
  text: string,
  start: number | undefined,
  length: number | undefined
): PremiseView["window"] {
  if (
    start === undefined ||
    length === undefined ||
    !Number.isInteger(start) ||
    !Number.isInteger(length) ||
    start < 0 ||
    length <= 0 ||
    start + length > text.length
  ) {
    return { before: text, address: null, after: "" };
  }
  return {
    before: text.slice(0, start),
    address: text.slice(start, start + length),
    after: text.slice(start + length),
  };
}

// The code facts of D3: always assessed by the rules. An absent fact gives
// no line.
function factLines(facts: PremiseFactsResponse): PremiseLine[] {
  const lines: { label: string; value: string | null }[] = [
    {
      label: premiseFactLabels.authorIsPerson,
      value:
        facts.authorIsPerson === undefined
          ? null
          : premiseAuthorIsPersonLabel(facts.authorIsPerson),
    },
    {
      label: premiseFactLabels.underProfile,
      value: yesNoWord(facts.underProfile),
    },
    {
      label: premiseFactLabels.identifierTypes,
      value:
        facts.identifierTypes === undefined ||
        facts.identifierTypes.length === 0
          ? null
          : facts.identifierTypes.map(identifierTypeLabel).join(", "),
    },
    {
      label: premiseFactLabels.identifierDistance,
      value:
        facts.identifierDistance == null
          ? null
          : String(facts.identifierDistance),
    },
    {
      label: premiseFactLabels.conflictingEmail,
      value: yesNoWord(facts.conflictingEmail),
    },
    {
      label: premiseFactLabels.addressCount,
      value:
        facts.addressCount === undefined ? null : String(facts.addressCount),
    },
    {
      label: premiseFactLabels.host,
      value: facts.host?.trim() ? facts.host : null,
    },
  ];
  const assessedBy = assessedByLabel("RULE");
  return lines.flatMap(({ label, value }) =>
    value === null ? [] : [{ label, value, assessedBy }]
  );
}

function yesNoWord(value: boolean | undefined): string | null {
  if (value === undefined) return null;
  return value ? premiseAnswerWords.yes : premiseAnswerWords.no;
}

const kindOptions = [
  { code: "OWN_DECLARATION", key: "ownDeclaration" },
  { code: "SALE_OR_SETTLEMENT", key: "saleOrSettlement" },
  { code: "DISCUSSION", key: "discussion" },
  { code: "MASS_COMMENT", key: "massComment" },
] as const;

function answerLines(
  answers: PremiseAnswersResponse,
  undetermined: string[],
  assessedBy: string
): PremiseLine[] {
  const lines: PremiseLine[] = [];
  if (answers.kind !== undefined) {
    const kind = answers.kind;
    // On a tie the option asked first wins, as in the backend.
    const top = kindOptions
      .map((option) => ({ code: option.code, probability: kind[option.key] }))
      .reduce((best, option) =>
        (option.probability ?? -1) > (best.probability ?? -1) ? option : best
      );
    lines.push({
      label: premiseAnswerLabels.kind,
      value: answerText(
        undetermined.includes("KIND")
          ? premiseAnswerWords.undetermined
          : premiseKindLabel(top.code),
        top.probability
      ),
      assessedBy,
    });
  }
  for (const question of ["declaration", "warning"] as const) {
    const answer = answers[question];
    if (answer === undefined) continue;
    lines.push({
      label: premiseAnswerLabels[question],
      value: answerText(
        undetermined.includes(question.toUpperCase())
          ? premiseAnswerWords.undetermined
          : yesNoAnswerWord(answer),
        answer.yes
      ),
      assessedBy,
    });
  }
  return lines;
}

// The frontend keeps no thresholds (D9): "niepewne" comes only from the
// backend's `undetermined` flag; otherwise the word is the more probable
// option, never the rounded percentage.
function yesNoAnswerWord(answer: YesNoProbabilitiesResponse): string {
  return (answer.yes ?? 0) >= (answer.no ?? 0)
    ? premiseAnswerWords.yes
    : premiseAnswerWords.no;
}

function answerText(word: string, probability: number | undefined): string {
  return probability === undefined
    ? word
    : `${word} (${formatPercent(probability)})`;
}

// Raw probability of every option. The request id of the decision model is
// never shown.
function technicalDetails(
  answers: PremiseAnswersResponse
): { label: string; value: string }[] {
  const lines: { label: string; value: number | undefined }[] = [
    ...kindOptions.map((option) => ({
      label: `${premiseAnswerLabels.kind}, ${premiseKindLabel(option.code)}`,
      value: answers.kind?.[option.key],
    })),
    ...(["declaration", "warning"] as const).flatMap((question) => [
      {
        label: `${premiseAnswerLabels[question]}, ${premiseAnswerWords.yes}`,
        value: answers[question]?.yes,
      },
      {
        label: `${premiseAnswerLabels[question]}, ${premiseAnswerWords.no}`,
        value: answers[question]?.no,
      },
    ]),
  ];
  return lines.flatMap(({ label, value }) =>
    value === undefined
      ? []
      : [
          {
            label,
            // Two decimals: also cuts binary noise (0.020000000000000018).
            value: String(Math.round(value * 100) / 100),
          },
        ]
  );
}
