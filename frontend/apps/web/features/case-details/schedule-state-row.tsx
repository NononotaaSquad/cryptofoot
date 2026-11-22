import { CalendarClockIcon, TriangleAlertIcon } from "lucide-react";

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
  const Icon = warning ? TriangleAlertIcon : CalendarClockIcon;

  return (
    <div
      className={
        warning
          ? "flex w-full items-start gap-3 rounded-lg border-l-4 border-destructive bg-destructive/10 px-5 py-4"
          : "flex w-full items-start gap-3 rounded-lg border-l-4 border-primary bg-muted px-5 py-4"
      }
    >
      <Icon
        aria-hidden="true"
        className={
          warning
            ? "mt-0.5 size-5 shrink-0 text-destructive"
            : "mt-0.5 size-5 shrink-0 text-primary"
        }
      />
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
