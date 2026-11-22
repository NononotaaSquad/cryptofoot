import { Badge } from "@workspace/ui/components/badge";
import { Check, CircleX, LoaderCircle, TriangleAlert } from "lucide-react";

import type { ReportView } from "./report-view-model";
import { AnalysisStatusBadge } from "./status-badge";

export function ReportSummary({ view }: { view: ReportView }) {
  const { state, summary } = view;

  return (
    <section aria-labelledby="report-heading" className="flex flex-col gap-5">
      <div className="flex flex-col items-start gap-3">
        <h2
          id="report-heading"
          className="text-[1.75rem] leading-tight font-bold"
        >
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
        {summary.versionLines.map((line) => (
          <div key={line.label} className="contents">
            <dt className="text-muted-foreground">{line.label}</dt>
            <dd className="break-all">{line.value}</dd>
          </div>
        ))}
      </dl>

      {/* Design: Sources with a check per line | Networks, one per line. */}
      <div className="grid grid-cols-[minmax(0,3fr)_minmax(0,2fr)] gap-x-12 gap-y-6">
        <div className="flex flex-col gap-2.5">
          <h3 className="font-semibold">{summary.sourcesHeading}</h3>
          <ul className="flex flex-col gap-2.5">
            {summary.sources.map((source, index) => (
              // The name is not guaranteed unique by the contract.
              <li
                key={`${source.name}-${String(index)}`}
                className="flex flex-wrap items-center gap-x-2 gap-y-1"
              >
                {source.unavailableText === null ? (
                  <Check
                    className="size-4 shrink-0 text-success-text"
                    aria-hidden="true"
                  />
                ) : (
                  <TriangleAlert
                    className="size-4 shrink-0 text-warning-text"
                    aria-hidden="true"
                  />
                )}
                <span>{source.name}</span>
                {source.checkedAtText !== null ? (
                  <span className="text-muted-foreground">
                    {source.checkedAtText}
                  </span>
                ) : null}
                {source.unavailableText !== null ? (
                  <span className="font-semibold text-warning-text">
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
        <div className="flex flex-col gap-2.5">
          <h3 className="font-semibold">Sieci</h3>
          {summary.networks.length > 0 ? (
            <ul className="flex flex-col gap-2.5">
              {summary.networks.map((network) => (
                <li key={network}>{network}</li>
              ))}
            </ul>
          ) : (
            <p>—</p>
          )}
        </div>
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
