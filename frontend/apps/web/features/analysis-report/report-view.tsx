"use client";

import { useQueryClient } from "@tanstack/react-query";
import { Button } from "@workspace/ui/components/button";
import { Skeleton } from "@workspace/ui/components/skeleton";
import type { JSX } from "react";
import { useEffect, useRef } from "react";

import { nextPollInterval } from "@/lib/analysis-run/poll-interval";
import { toUserMessage } from "@/lib/api/client";
import {
  useGetAnalysis,
  useGetReport,
  useListMentions,
} from "@/lib/api/generated/analysis-controller/analysis-controller";
import {
  getGetCaseQueryKey,
  getListAnalysesQueryKey,
} from "@/lib/api/generated/case-controller/case-controller";
import type {
  AnalysisResponseStatus,
  PersonIdentifiersResponse,
} from "@/lib/api/generated/model";

import { AttributionList } from "./attribution-list";
import { ExcludedAddresses } from "./excluded-addresses";
import { RejectedAddresses } from "./rejected-addresses";
import { ReportSummary } from "./report-summary";
import { toReportView } from "./report-view-model";

interface ReportViewProps {
  caseId: string;
  analysisId: string;
  caseIdentifiers: PersonIdentifiersResponse;
}

/**
 * The report of one Analysis. The report and its Mentions answer 409 while
 * the Analysis runs, so the Analysis is polled first (S3 rule, in the
 * background, Q16) and the report is fetched only once it has ended.
 */
export function ReportView({
  caseId,
  analysisId,
  caseIdentifiers,
}: ReportViewProps): JSX.Element {
  const analysis = useGetAnalysis(analysisId, {
    query: {
      // No polling after an error: the error state with "Spróbuj ponownie"
      // takes over.
      refetchInterval: (query) =>
        query.state.status === "error"
          ? false
          : nextPollInterval(query.state.data?.data.status),
    },
  });
  const status = analysis.data?.data.status;
  const ended = status !== undefined && nextPollInterval(status) === false;

  const report = useGetReport(analysisId, { query: { enabled: ended } });
  const mentions = useListMentions(analysisId, { query: { enabled: ended } });

  useRefreshCaseWhenAnalysisEnds(caseId, analysisId, status);

  if (analysis.isError || report.isError || mentions.isError) {
    const failed = [analysis, report, mentions].filter(
      (query) => query.isError
    );
    return (
      <ReportError
        error={failed[0]?.error}
        onRetry={() => {
          for (const query of failed) void query.refetch();
        }}
      />
    );
  }

  if (analysis.isPending) return <ReportLoading />;

  if (!ended) {
    // Running: the summary from the Analysis alone; no sections yet.
    const runningView = toReportView(
      { analysis: analysis.data.data },
      [],
      caseIdentifiers
    );
    return <ReportSummary view={runningView} />;
  }

  if (report.isPending || mentions.isPending) return <ReportLoading />;

  const view = toReportView(
    report.data.data,
    mentions.data.data,
    caseIdentifiers
  );

  return (
    <div className="flex flex-col gap-10">
      <ReportSummary view={view} />
      {/* A failed Analysis shows only the summary with its grey block. */}
      {view.state === "failed" ? null : (
        <>
          <AttributionList
            heading={view.attributionsHeading}
            attributions={view.attributions}
          />
          <ExcludedAddresses
            heading={view.excludedHeading}
            excludedAddresses={view.excludedAddresses}
          />
          <RejectedAddresses
            heading={view.rejectedHeading}
            rejectedAddresses={view.rejectedAddresses}
          />
        </>
      )}
    </div>
  );
}

// Invalidates the Case and its Analysis list once this Analysis is seen going
// from RUNNING to an end status (not when a finished one is opened): the
// select and the header must not keep showing "W toku".
function useRefreshCaseWhenAnalysisEnds(
  caseId: string,
  analysisId: string,
  status: AnalysisResponseStatus | undefined
) {
  const queryClient = useQueryClient();
  const last = useRef<{
    analysisId: string;
    status: AnalysisResponseStatus;
  } | null>(null);

  useEffect(() => {
    if (status === undefined) return;
    const previous = last.current;
    last.current = { analysisId, status };
    if (
      previous?.analysisId === analysisId &&
      previous.status === "RUNNING" &&
      status !== "RUNNING"
    ) {
      void queryClient.invalidateQueries({
        queryKey: getListAnalysesQueryKey(caseId),
      });
      void queryClient.invalidateQueries({
        queryKey: getGetCaseQueryKey(caseId),
      });
    }
  }, [queryClient, caseId, analysisId, status]);
}

function ReportLoading() {
  return (
    <div aria-busy="true" className="flex flex-col gap-4">
      <span className="sr-only" role="status">
        Wczytywanie raportu
      </span>
      <Skeleton className="h-8 w-96 max-w-full" />
      <Skeleton className="h-5 w-64" />
      <Skeleton className="h-24 w-full" />
    </div>
  );
}

function ReportError({
  error,
  onRetry,
}: {
  error: unknown;
  onRetry: () => void;
}) {
  return (
    <div
      role="alert"
      className="flex flex-col items-start gap-3 rounded-lg border border-border p-6"
    >
      <h2 className="text-xl font-bold">Coś poszło nie tak</h2>
      <p>{toUserMessage(error)}</p>
      <Button size="xl" onClick={onRetry}>
        Spróbuj ponownie
      </Button>
    </div>
  );
}
