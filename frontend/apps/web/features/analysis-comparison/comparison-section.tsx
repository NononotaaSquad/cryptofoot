"use client";

import { Button } from "@workspace/ui/components/button";
import { Skeleton } from "@workspace/ui/components/skeleton";
import { cn } from "@workspace/ui/lib/utils";
import {
  ArrowUpDown,
  Ban,
  CirclePlus,
  EyeOff,
  Info,
  TriangleAlert,
  type LucideIcon,
} from "lucide-react";
import Link from "next/link";

import { toUserMessage } from "@/lib/api/client";
import { useCompareAnalyses } from "@/lib/api/generated/case-controller/case-controller";

import {
  type ChangeKind,
  type ChangeView,
  comparisonLabels,
  toComparisonView,
} from "./comparison-view-model";

interface ComparisonSectionProps {
  caseId: string;
  earlierAnalysisId: string;
  laterAnalysisId: string;
}

// Icon + word per kind, so the kind is never carried by the icon alone.
const changeIcons: Record<ChangeKind, LucideIcon> = {
  NEW: CirclePlus,
  CONFIDENCE_LEVEL_CHANGED: ArrowUpDown,
  EXCLUSION_CHANGED: Ban,
  NOT_OBSERVED: EyeOff,
  UNKNOWN: Info,
};

/**
 * What changed between the displayed Analysis and the previous one with
 * results (E6.3). Both Analyses have ended and never change, so the answer is
 * kept for the session. A failed read shows its own error here and never
 * hides the report below.
 */
export function ComparisonSection({
  caseId,
  earlierAnalysisId,
  laterAnalysisId,
}: ComparisonSectionProps) {
  const comparison = useCompareAnalyses(
    caseId,
    { earlierAnalysisId, laterAnalysisId },
    { query: { staleTime: Number.POSITIVE_INFINITY } }
  );

  if (comparison.isPending) {
    return (
      <div aria-busy="true" className="flex flex-col gap-3">
        <span className="sr-only" role="status">
          Wczytywanie zmian od poprzedniej analizy
        </span>
        <Skeleton className="h-7 w-80 max-w-full" />
        <Skeleton className="h-20 w-full" />
      </div>
    );
  }

  if (comparison.isError) {
    return (
      <section
        role="alert"
        className="flex flex-col items-start gap-3 rounded-lg border border-border p-6"
      >
        <h2 className="text-xl font-bold">Coś poszło nie tak</h2>
        <p>{toUserMessage(comparison.error)}</p>
        <Button
          size="xl"
          onClick={() => {
            void comparison.refetch();
          }}
        >
          Spróbuj ponownie
        </Button>
      </section>
    );
  }

  const view = toComparisonView(comparison.data.data);

  return (
    <section
      aria-labelledby="comparison-heading"
      className="flex flex-col gap-4"
    >
      <div className="flex flex-col gap-1">
        <h2 id="comparison-heading" className="text-xl font-bold">
          {view.heading}
        </h2>
        <p className="flex flex-wrap items-baseline gap-x-3 gap-y-1 text-muted-foreground">
          {view.comparedWith}
          {view.earlierAnalysisId === null ? null : (
            <Link
              href={`/cases/${caseId}?analysis=${view.earlierAnalysisId}`}
              scroll={false}
              className="rounded-sm font-semibold text-primary underline-offset-4 outline-none hover:underline focus-visible:ring-3 focus-visible:ring-ring/50"
            >
              {comparisonLabels.showEarlier}
            </Link>
          )}
        </p>
      </div>

      {view.limitations.length > 0 ? (
        <div className="flex flex-col gap-2 rounded-lg border border-warning bg-warning-tint p-4 text-warning-text">
          <h3 className="flex items-center gap-2 font-semibold">
            <TriangleAlert className="size-5" aria-hidden="true" />
            {comparisonLabels.limitationsHeading}
          </h3>
          <ul className="flex list-disc flex-col gap-1 pl-5">
            {view.limitations.map((sentence, index) => (
              // The same code may come twice; the index keeps keys unique.
              <li key={`${String(index)}-${sentence}`}>{sentence}</li>
            ))}
          </ul>
        </div>
      ) : null}

      {view.emptyMessage !== null ? (
        <p className="rounded-lg bg-muted p-6">{view.emptyMessage}</p>
      ) : (
        <ul className="flex flex-col divide-y divide-border rounded-lg border border-border">
          {view.changes.map((change) => (
            <ChangeRow key={change.key} change={change} />
          ))}
        </ul>
      )}
    </section>
  );
}

function ChangeRow({ change }: { change: ChangeView }) {
  const Icon = changeIcons[change.kind];
  return (
    <li className="flex flex-wrap items-start gap-x-8 gap-y-2 px-4 py-4">
      <div className="flex min-w-0 flex-1 basis-72 flex-col gap-1">
        <span className="font-mono break-all">{change.address}</span>
        <span className="text-sm text-muted-foreground">{change.network}</span>
      </div>
      <div className="flex min-w-0 flex-1 basis-72 flex-col gap-1">
        <span
          className={cn(
            "flex items-center gap-2 font-semibold",
            change.kind === "NEW" && "text-primary"
          )}
        >
          <Icon className="size-4 shrink-0" aria-hidden="true" />
          {change.label}
        </span>
        <span>{change.detail}</span>
        {change.absenceNote !== null ? (
          <span
            className={cn(
              "flex items-start gap-1.5 text-sm",
              change.absenceNote.verifiable
                ? "text-muted-foreground"
                : "text-warning-text"
            )}
          >
            {change.absenceNote.verifiable ? null : (
              <TriangleAlert
                className="mt-0.5 size-4 shrink-0"
                aria-hidden="true"
              />
            )}
            {change.absenceNote.text}
          </span>
        ) : null}
      </div>
    </li>
  );
}
