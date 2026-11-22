import { Clock4Icon, TriangleAlertIcon } from "lucide-react";

import { demoLabel } from "./schedule-labels";
import type { ScheduleStateRow as ScheduleStateRowView } from "./schedule-view-model";

interface ScheduleStateRowProps {
  row: ScheduleStateRowView;
}

// Case scope: between the Case header and the select of Analyses. Nothing for
// a disabled Schedule. A skip or a failure replaces the plain row (Q6, Q15).
export function ScheduleStateRow({ row }: ScheduleStateRowProps) {
  if (row.kind === "none") return null;
  const warning = row.kind === "warning";
  const Icon = warning ? TriangleAlertIcon : Clock4Icon;

  // Design: a 4px left edge, grey for the plain row; a skip or a failure is
  // a warning (amber), not an error.
  return (
    <div
      className={
        warning
          ? "flex w-full items-start gap-2.5 border-l-4 border-warning-text bg-warning-tint px-4 py-2.5 text-warning-text"
          : "flex w-full items-start gap-2.5 border-l-4 border-border-strong px-4 py-2.5"
      }
    >
      <Icon aria-hidden="true" className="mt-0.5 size-5 shrink-0" />
      <p className={warning ? "leading-normal font-bold" : "leading-normal"}>
        {row.text}
        {row.demo ? (
          <span className="ml-3 rounded-sm bg-primary px-2 py-0.5 text-sm font-semibold text-primary-foreground">
            {demoLabel}
          </span>
        ) : null}
      </p>
    </div>
  );
}
