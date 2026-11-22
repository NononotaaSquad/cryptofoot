import { Badge } from "@workspace/ui/components/badge";
import { CircleX, LoaderCircle, TriangleAlert } from "lucide-react";

import type { ReportView } from "./report-view-model";
import { AnalysisStatusBadge } from "./status-badge";

export function ReportSummary({ view }: { view: ReportView }) {
  const { state, summary } = view;

  return (
    <section aria-labelledby="report-heading" className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center gap-3">
        <h2 id="report-heading" className="text-2xl font-bold">
          {summary.heading}
        </h2>
        <AnalysisStatusBadge
          status={summary.status.key}
          label={summary.status.label}
        />
      </div>

      {state === "running" && summary.notice !== null ? (
        // Polite live region: the Analyst hears when it changes.
        <p role="status" className="flex items-center gap-2">
          <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
          {summary.notice}
        </p>
      ) : null}

      <dl className="grid grid-cols-[max-content_1fr] gap-x-6 gap-y-2">
        <dt className="text-muted-foreground">Rodzaj analizy</dt>
        <dd>{summary.triggerLabel}</dd>
        <dt className="text-muted-foreground">Rozpoczęcie</dt>
        <dd>{summary.startedAt}</dd>
        <dt className="text-muted-foreground">Zakończenie</dt>
        <dd>{summary.finishedAt ?? "—"}</dd>
        <dt className="text-muted-foreground">Sieci</dt>
        <dd>
          {summary.networks.length > 0 ? summary.networks.join(", ") : "—"}
        </dd>
      </dl>

      <div className="flex flex-col gap-2">
        <h3 className="font-semibold">{summary.sourcesHeading}</h3>
        <ul className="flex flex-col gap-1">
          {summary.sources.map((source, index) => (
            // The name is not guaranteed unique by the contract.
            <li
              key={`${source.name}-${String(index)}`}
              className="flex flex-wrap items-center gap-x-1.5 gap-y-1"
            >
              <span className="font-medium">{source.name}</span>
              {source.checkedAtText !== null ? (
                <span className="text-muted-foreground">
                  {source.checkedAtText}
                </span>
              ) : null}
              {source.unavailableText !== null ? (
                <span className="inline-flex items-center gap-1 font-medium text-warning-text">
                  <TriangleAlert className="size-4" aria-hidden="true" />
                  {source.unavailableText}
                </span>
              ) : null}
              {source.testDataLabel !== null ? (
                <Badge variant="outline">{source.testDataLabel}</Badge>
              ) : null}
            </li>
          ))}
        </ul>
      </div>

      {state === "failed" ? (
        // In place of the Attributions; the Report Limitations say why.
        <div className="flex flex-col gap-3 rounded-lg bg-muted p-6">
          <p className="flex items-center gap-2 font-semibold">
            <CircleX className="size-5" aria-hidden="true" />
            {summary.notice}
          </p>
          <ReportLimitationList sentences={summary.reportLimitations} />
        </div>
      ) : state === "partial" || summary.reportLimitations.length > 0 ? (
        <div className="flex flex-col gap-2 rounded-lg border border-warning bg-warning-tint p-4 text-warning-text">
          {state === "partial" ? (
            <p className="flex items-center gap-2 font-semibold">
              <TriangleAlert className="size-5" aria-hidden="true" />
              {summary.notice}
            </p>
          ) : null}
          {summary.reportLimitations.length > 0 ? (
            <>
              <h3 className="font-semibold">Ograniczenia raportu</h3>
              <ReportLimitationList sentences={summary.reportLimitations} />
            </>
          ) : null}
        </div>
      ) : null}
    </section>
  );
}

function ReportLimitationList({ sentences }: { sentences: string[] }) {
  if (sentences.length === 0) return null;
  return (
    <ul className="flex list-disc flex-col gap-1 pl-5">
      {sentences.map((sentence, index) => (
        // The same code may come twice; the index keeps keys unique.
        <li key={`${String(index)}-${sentence}`}>{sentence}</li>
      ))}
    </ul>
  );
}
