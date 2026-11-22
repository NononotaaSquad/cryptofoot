import { cn } from "@workspace/ui/lib/utils";
import { ChevronRight } from "lucide-react";
import type { Route } from "next";
import Link from "next/link";

import { AnalysisStatusBadge } from "@/features/analysis-report/status-badge";

import type { CaseRowView } from "./case-row-view-model";

// Column grid shared by the header and the rows (from `md` up). Below it the
// cells stack and name themselves, so a narrow screen loses nothing.
export const rowGrid =
  "md:grid md:grid-cols-[minmax(0,1fr)_10.5rem_14rem_10.5rem_1.5rem] md:items-center md:gap-x-4";

// The cell name: visible on a narrow screen, read aloud on a wide one (where
// the column header says it).
function CellName({ children }: { children: string }) {
  return (
    <span className="text-sm text-muted-foreground md:sr-only">{children}</span>
  );
}

// A value that may be shortened for the eye: the full text stays readable by
// assistive technology and as a tooltip.
function Shortened({ text, full }: { text: string; full: string }) {
  if (text === full) return <span className="break-all">{full}</span>;
  return (
    <>
      <span aria-hidden="true" title={full}>
        {text}
      </span>
      <span className="sr-only">{full}</span>
    </>
  );
}

export function CaseRow({ row }: { row: CaseRowView }) {
  const { confidence } = row;
  const highlight = confidence.kind === "level" && confidence.highlight;

  return (
    <li className="border-b border-border">
      <Link
        // The view model builds a /cases/{id}[?analysis={id}] address from
        // runtime ids, which typed routes cannot check.
        href={row.href as Route}
        className={cn(
          "group flex flex-col gap-3 border-l-4 px-4 py-4 outline-none focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:ring-inset md:py-5",
          rowGrid,
          // Blue only for a highest Confidence Level of 80 or more.
          highlight
            ? "border-primary bg-primary-tint"
            : "border-transparent hover:bg-muted/60"
        )}
      >
        <div className="flex min-w-0 flex-col gap-1">
          <div className="flex flex-wrap items-baseline gap-x-3">
            <span className="text-lg leading-snug font-semibold break-words group-hover:underline">
              <Shortened text={row.title} full={row.titleFull} />
            </span>
            {row.caseNumber === null ? null : (
              <span className="text-muted-foreground">
                <span className="sr-only">Numer sprawy: </span>
                {row.caseNumber}
              </span>
            )}
          </div>
          {row.identifiers.length > 0 ? (
            <p className="flex flex-wrap gap-x-2 leading-normal text-muted-foreground">
              {row.identifiers.map((identifier, index) => (
                <span key={identifier.label} className="min-w-0">
                  {index > 0 ? (
                    <span aria-hidden="true" className="mr-2">
                      ·
                    </span>
                  ) : null}
                  {identifier.label}:{" "}
                  <span className="text-foreground">
                    <Shortened text={identifier.text} full={identifier.full} />
                  </span>
                </span>
              ))}
              {row.hiddenIdentifierCount > 0 ? (
                <span>
                  <span aria-hidden="true" className="mr-2">
                    ·
                  </span>
                  +{row.hiddenIdentifierCount} więcej
                </span>
              ) : null}
            </p>
          ) : null}
        </div>

        <div className="flex flex-col items-start gap-1">
          <CellName>Status</CellName>
          <AnalysisStatusBadge
            status={row.status.key}
            label={row.status.label}
          />
        </div>

        <div className="flex flex-col gap-1">
          <CellName>Najwyższy stopień pewności</CellName>
          {confidence.kind === "level" ? (
            <p className="flex flex-wrap items-baseline gap-x-2">
              <span className="text-xl font-bold">{confidence.text}</span>
              {confidence.count === null ? null : (
                <span className="text-muted-foreground">
                  {confidence.count}
                </span>
              )}
            </p>
          ) : (
            <p className="text-muted-foreground">
              {confidence.text}
              {confidence.kind === "none" ? (
                <span className="sr-only"> (brak danych do pokazania)</span>
              ) : null}
            </p>
          )}
        </div>

        <div className="flex flex-col gap-1">
          <CellName>Ostatnia analiza</CellName>
          <p>{row.date}</p>
        </div>

        <ChevronRight
          aria-hidden="true"
          className="hidden size-5 text-primary md:block"
        />
      </Link>
    </li>
  );
}

export function CaseListHeader() {
  return (
    <div
      aria-hidden="true"
      className={cn(
        "hidden border-b border-l-4 border-border-strong border-l-transparent px-4 pb-2 text-sm font-semibold text-muted-foreground",
        rowGrid
      )}
    >
      <span>Sprawa</span>
      <span>Status</span>
      <span>Najwyższy stopień pewności</span>
      <span>Ostatnia analiza</span>
      <span />
    </div>
  );
}
