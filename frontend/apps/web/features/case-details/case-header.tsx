"use client";

import { Button } from "@workspace/ui/components/button";
import { CheckIcon } from "lucide-react";
import { useRef, useState, type ReactNode } from "react";
import { toast } from "sonner";

import type { PersonIdentifiersResponse } from "@/lib/api/generated/model";
import {
  caseLabel,
  identifierKindLabels,
  presentNarrowingIdentifiers,
} from "@/lib/identifiers/identifier-kinds";

import { isCovered, type AnalysisOrder } from "./reviewed-mark";
import {
  markReviewed,
  unmarkReviewed,
  useReviewedMark,
} from "./reviewed-store";
import { StartAnalysisButton } from "./start-analysis-button";

const collapsedIdentifierCount = 2;
const identifierListId = "case-details-identifiers";

interface CaseHeaderProps {
  caseId: string;
  identifiers: PersonIdentifiersResponse;
  // False for a Case without any Analysis: the empty state carries the only
  // start button, and there is nothing to review yet.
  showActions: boolean;
  // The terminal Analysis shown below, the only one the Reviewed button marks.
  // Null while it runs or when none is shown: no Reviewed control then.
  reviewTarget?: AnalysisOrder | null;
  // The clock button of the Schedule, right of "Rozpocznij nową analizę".
  scheduleButton?: ReactNode;
}

// Case scope on top of Case details: label, identifiers, Reviewed control and
// "Rozpocznij nową analizę". No Case Number (Q3, issue #56).
export function CaseHeader({
  caseId,
  identifiers,
  showActions,
  reviewTarget = null,
  scheduleButton,
}: CaseHeaderProps) {
  const [expanded, setExpanded] = useState(false);
  const present = presentNarrowingIdentifiers(identifiers);
  const hiddenCount = present.length - collapsedIdentifierCount;
  const shown =
    expanded || hiddenCount <= 0
      ? present
      : present.slice(0, collapsedIdentifierCount);

  return (
    <div className="flex flex-wrap items-start justify-between gap-6">
      <div className="flex min-w-0 flex-col gap-2.5">
        <h1 className="text-[2.5rem] leading-tight font-bold break-words">
          {caseLabel(identifiers) ?? "Sprawa"}
        </h1>
        <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
          <ul
            id={identifierListId}
            className="flex flex-wrap gap-x-3 gap-y-1 leading-normal"
          >
            {shown.map((identifier, index) => (
              <li key={identifier.kind} className="break-all">
                {index > 0 ? (
                  <span
                    aria-hidden="true"
                    className="mr-3 text-muted-foreground"
                  >
                    ·
                  </span>
                ) : null}
                <span className="text-muted-foreground">
                  {identifierKindLabels[identifier.kind]} –
                </span>{" "}
                {identifier.value}
              </li>
            ))}
          </ul>
          {hiddenCount > 0 ? (
            <Button
              variant="link"
              className="h-auto px-0 text-base font-semibold"
              aria-expanded={expanded}
              aria-controls={identifierListId}
              onClick={() => {
                setExpanded((value) => !value);
              }}
            >
              {expanded ? "Zwiń" : `+${String(hiddenCount)} więcej`}
            </Button>
          ) : null}
        </div>
      </div>

      {showActions ? (
        <div className="flex flex-wrap items-center gap-6">
          {reviewTarget === null ? null : (
            <ReviewedControl caseId={caseId} target={reviewTarget} />
          )}
          <div className="flex items-center gap-3">
            <StartAnalysisButton caseId={caseId} />
            {scheduleButton}
          </div>
        </div>
      ) : null}
    </div>
  );
}

const storageFailure =
  "Nie udało się zapisać oznaczenia w przeglądarce. Sprawdź, czy przeglądarka pozwala zapisywać dane.";

function ReviewedControl({
  caseId,
  target,
}: {
  caseId: string;
  target: AnalysisOrder;
}) {
  const mark = useReviewedMark(caseId);
  const reviewed = isCovered(mark, target);
  // The pressed button is replaced by the other one; focus follows it, so a
  // keyboard user does not land on the page body.
  const focusNext = useRef(false);
  const focusIfToggled = (element: HTMLButtonElement | null) => {
    if (element !== null && focusNext.current) {
      focusNext.current = false;
      element.focus();
    }
  };
  const toggle = (next: boolean) => {
    focusNext.current = true;
    // The target is the Analysis shown when the button was drawn, so a newer
    // one that ends meanwhile stays new. A failed save shows no success.
    const kept = next ? markReviewed(caseId, target) : unmarkReviewed(caseId);
    if (!kept) {
      focusNext.current = false;
      toast.error(storageFailure);
    }
  };

  if (!reviewed) {
    return (
      <Button
        variant="outline"
        size="xl"
        ref={focusIfToggled}
        onClick={() => {
          toggle(true);
        }}
      >
        Oznacz jako przejrzaną
      </Button>
    );
  }

  return (
    <div className="flex flex-wrap items-center gap-4">
      <p className="flex items-center gap-2 font-semibold">
        <CheckIcon aria-hidden="true" className="size-5 text-primary" />
        Sprawa przejrzana
      </p>
      <Button
        variant="link"
        className="h-auto px-0 text-base font-semibold"
        ref={focusIfToggled}
        onClick={() => {
          toggle(false);
        }}
      >
        Cofnij oznaczenie
      </Button>
    </div>
  );
}
