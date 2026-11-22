"use client";

import { Button } from "@workspace/ui/components/button";
import { Skeleton } from "@workspace/ui/components/skeleton";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { ComparisonSection } from "@/features/analysis-comparison/comparison-section";
import { previousAnalysisWithResults } from "@/features/analysis-comparison/comparison-view-model";
import { ReportView } from "@/features/analysis-report/report-view";
import { toUserMessage } from "@/lib/api/client";
import {
  useGetCase,
  useGetSchedule,
  useListAnalyses,
} from "@/lib/api/generated/case-controller/case-controller";
import type { AnalysisResponse } from "@/lib/api/generated/model";
import { env } from "@/lib/env";

import { AnalysisSelect } from "./analysis-select";
import { CaseHeader } from "./case-header";
import { toAnalysisOrder } from "./reviewed-mark";
import { ScheduleButton } from "./schedule-button";
import { schedulePollInterval } from "./schedule-poll-interval";
import { ScheduleStateRow } from "./schedule-state-row";
import { toScheduleView } from "./schedule-view-model";
import { StartAnalysisButton } from "./start-analysis-button";

interface CaseDetailsViewProps {
  caseId: string;
  // The `?analysis=` param; without it the newest Analysis is shown.
  analysisId: string | undefined;
}

// Newest first. The backend sorts the same way today, but the contract does
// not promise an order.
function newestFirst(
  analyses: readonly AnalysisResponse[]
): AnalysisResponse[] {
  const time = (analysis: AnalysisResponse) =>
    analysis.startedAt === undefined ? 0 : Date.parse(analysis.startedAt);
  return [...analyses].sort((a, b) => time(b) - time(a));
}

export function CaseDetailsView({ caseId, analysisId }: CaseDetailsViewProps) {
  const router = useRouter();
  const caseQuery = useGetCase(caseId);
  // The Schedule is read apart from the rest: a failed read never hides the
  // report. While it is enabled, the page checks the server in the background
  // (a failed poll keeps the last answer and the polling).
  const scheduleQuery = useGetSchedule(caseId, {
    query: {
      refetchInterval: (query) => schedulePollInterval(query.state.data?.data),
    },
  });
  const schedule = scheduleQuery.data?.data;
  const scheduleView = toScheduleView(schedule, {
    demoEnabled: env.NEXT_PUBLIC_DEMO_SCHEDULE,
  });
  const analysesQuery = useListAnalyses(caseId, {
    query: { refetchInterval: schedulePollInterval(schedule) },
  });
  // The `analysis` param for which the history was fetched again (see below).
  const [refetchedFor, setRefetchedFor] = useState<string | null>(null);

  const analyses =
    analysesQuery.data === undefined
      ? undefined
      : newestFirst(analysesQuery.data.data);
  const missing =
    analyses !== undefined &&
    analysisId !== undefined &&
    !analyses.some((analysis) => analysis.id === analysisId);
  const { refetch: refetchAnalyses } = analysesQuery;

  // Without an `analysis` param the newest Analysis is shown. Remember the one
  // first shown, so a Scheduled Analysis that arrives while the page is open
  // does not replace the report being read (Q7). The pin is dropped when the
  // address gets its own `analysis` param.
  const [pin, setPin] = useState<{
    param: string | undefined;
    id: string | null;
  }>({
    param: analysisId,
    id: null,
  });
  if (pin.param !== analysisId) {
    setPin({ param: analysisId, id: null });
  } else if (analysisId === undefined && pin.id === null) {
    const newest = analyses?.[0]?.id;
    if (newest !== undefined) setPin({ param: analysisId, id: newest });
  }

  // An Analysis started here opens this same page with its id while the
  // cached history (fresh for a minute) does not hold it yet. Before saying
  // the Analysis is not in the Case (Q36), fetch the history once more.
  useEffect(() => {
    if (!missing || refetchedFor === analysisId) return;
    void refetchAnalyses().then(() => {
      setRefetchedFor(analysisId);
    });
  }, [missing, refetchedFor, analysisId, refetchAnalyses]);

  if (caseQuery.isPending || analysesQuery.isPending) {
    return <CaseDetailsSkeleton />;
  }

  // An unknown caseId ends here too: the backend answers 404.
  if (caseQuery.isError || analysesQuery.isError) {
    return (
      <ErrorState
        error={caseQuery.error ?? analysesQuery.error}
        onRetry={() => {
          if (caseQuery.isError) void caseQuery.refetch();
          if (analysesQuery.isError) void analysesQuery.refetch();
        }}
      />
    );
  }

  const identifiers = caseQuery.data.data.identifiers ?? {};
  const history = analyses ?? [];

  if (history.length === 0) {
    return (
      <div className="flex flex-col gap-8">
        <CaseHeader
          caseId={caseId}
          identifiers={identifiers}
          showActions={false}
        />
        <div className="flex flex-col items-start gap-6 rounded-lg bg-muted px-8 py-10">
          <p className="text-lg leading-normal">
            Ta sprawa nie ma jeszcze analizy.
          </p>
          <StartAnalysisButton caseId={caseId} />
        </div>
      </div>
    );
  }

  if (missing && refetchedFor !== analysisId) {
    return <CaseDetailsSkeleton />;
  }

  const pinned =
    pin.id !== null && history.some((analysis) => analysis.id === pin.id)
      ? pin.id
      : null;
  const selectedId = missing
    ? null
    : (analysisId ?? pinned ?? history[0]?.id ?? null);
  // Manual and Scheduled Analyses alike: the displayed one is compared with
  // the previous one that has results (E6.3).
  const previousId =
    selectedId === null
      ? undefined
      : previousAnalysisWithResults(history, selectedId)?.id;

  // Only a terminal Analysis that is shown can be marked as reviewed.
  const displayed = history.find((analysis) => analysis.id === selectedId);
  const reviewTarget =
    displayed === undefined || displayed.status === "RUNNING"
      ? null
      : toAnalysisOrder(displayed);

  return (
    <div className="flex flex-col gap-8">
      <CaseHeader
        caseId={caseId}
        identifiers={identifiers}
        showActions
        reviewTarget={reviewTarget}
        scheduleButton={
          <ScheduleButton
            caseId={caseId}
            view={scheduleView}
            readError={schedule === undefined ? scheduleQuery.error : null}
            onRetryRead={() => {
              void scheduleQuery.refetch();
            }}
          />
        }
      />

      <ScheduleStateRow row={scheduleView.stateRow} />

      <AnalysisSelect
        analyses={history}
        selectedId={selectedId}
        onSelect={(id) => {
          // push, not replace: each choice is its own address, so the
          // browser Back button returns to the report seen before.
          router.push(`/cases/${caseId}?analysis=${id}`, { scroll: false });
        }}
      />

      {selectedId === null ? (
        <div className="flex flex-col items-start gap-4 rounded-lg bg-muted px-8 py-10">
          <p className="text-lg leading-normal">
            Nie znaleziono tej analizy w sprawie.
          </p>
          <Link
            href={`/cases/${caseId}`}
            className="rounded-sm text-base font-semibold text-primary underline-offset-4 outline-none hover:underline focus-visible:ring-3 focus-visible:ring-ring/50"
          >
            Pokaż najnowszą analizę
          </Link>
        </div>
      ) : (
        <ReportView
          caseId={caseId}
          analysisId={selectedId}
          caseIdentifiers={identifiers}
          comparison={
            previousId === undefined ? null : (
              <ComparisonSection
                caseId={caseId}
                earlierAnalysisId={previousId}
                laterAnalysisId={selectedId}
              />
            )
          }
        />
      )}
    </div>
  );
}

function CaseDetailsSkeleton() {
  return (
    <div aria-busy="true" className="flex flex-col gap-10">
      <span className="sr-only" role="status">
        Wczytywanie sprawy…
      </span>
      <div className="flex flex-col gap-3">
        <Skeleton className="h-12 w-80" />
        <Skeleton className="h-6 w-120 max-w-full" />
      </div>
      <Skeleton className="h-12 w-120 max-w-full" />
      <Skeleton className="h-64 w-full" />
    </div>
  );
}

interface ErrorStateProps {
  error: unknown;
  onRetry: () => void;
}

function ErrorState({ error, onRetry }: ErrorStateProps) {
  return (
    <div
      role="alert"
      className="flex flex-col items-start gap-4 rounded-lg bg-muted px-8 py-10"
    >
      <h1 className="text-2xl leading-tight font-bold">Coś poszło nie tak</h1>
      <p className="leading-normal">{toUserMessage(error)}</p>
      <Button size="xl" onClick={onRetry}>
        Spróbuj ponownie
      </Button>
    </div>
  );
}
