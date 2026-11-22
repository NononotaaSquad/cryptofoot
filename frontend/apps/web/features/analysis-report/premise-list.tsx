import { Badge } from "@workspace/ui/components/badge";
import { buttonVariants } from "@workspace/ui/components/button";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@workspace/ui/components/collapsible";
import { cn } from "@workspace/ui/lib/utils";
import {
  CircleCheck,
  CircleQuestionMark,
  CircleX,
  Info,
  type LucideIcon,
  TriangleAlert,
} from "lucide-react";
import { useState } from "react";

import { MentionLink } from "./attribution-details";
import type { PremiseLine, PremiseView } from "./report-view-model";

// Icon + word per status, so the status is never carried by colour alone.
const statusIcons: Record<
  PremiseView["status"]["icon"],
  { icon: LucideIcon; className: string }
> = {
  counted: {
    icon: CircleCheck,
    className: "bg-success-tint text-success-text",
  },
  forReview: {
    icon: TriangleAlert,
    className: "bg-warning-tint text-warning-text",
  },
  dismissed: { icon: CircleX, className: "bg-muted text-foreground" },
  unknown: { icon: CircleQuestionMark, className: "bg-muted text-foreground" },
};

/** "Przesłanki (N)"; renders nothing for an older Analysis without them. */
export function PremiseList({
  heading,
  premises,
}: {
  heading: string;
  premises: PremiseView[];
}) {
  if (premises.length === 0) return null;
  return (
    <div className="flex flex-col gap-2">
      <h4 className="font-semibold">{heading}</h4>
      <ol className="flex flex-col gap-6">
        {premises.map((premise) => (
          <Premise key={premise.key} premise={premise} />
        ))}
      </ol>
    </div>
  );
}

function Premise({ premise }: { premise: PremiseView }) {
  const { icon: StatusIcon, className: statusClassName } =
    statusIcons[premise.status.icon];
  return (
    <li className="flex min-w-0 flex-col gap-2">
      <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
        <span className="font-medium">{premise.title}</span>
        <Badge className={statusClassName}>
          <StatusIcon data-icon="inline-start" aria-hidden="true" />
          {premise.status.label}
        </Badge>
        {premise.withdrawnNote !== null ? (
          <span className="inline-flex items-center gap-1 text-warning-text">
            <Info className="size-4 shrink-0" aria-hidden="true" />
            {premise.withdrawnNote}
          </span>
        ) : null}
        {premise.kind !== null ? <span>· {premise.kind}</span> : null}
        <span className="tabular-nums">· {premise.points}&nbsp;pkt</span>
        <span className="text-muted-foreground">
          · {premise.kindAssessedBy}
        </span>
      </div>

      {premise.reason !== null ? <p>Powód: {premise.reason}</p> : null}

      <div className="flex flex-col gap-1">
        <span>
          <span className="font-medium">{premise.sourceName}</span>
          {premise.author !== null ? (
            <span className="text-muted-foreground">
              {" "}
              · autor: {premise.author}
            </span>
          ) : null}
          {premise.collectedAt !== null ? (
            <span className="text-muted-foreground">
              {" "}
              · zebrano {premise.collectedAt}
            </span>
          ) : null}
        </span>
        <MentionLink mention={premise} />
      </div>

      <blockquote className="border-l-2 border-border pl-3 break-words whitespace-pre-wrap text-muted-foreground">
        {premise.window.before}
        {premise.window.address !== null ? (
          <mark className="rounded-sm bg-warning-tint px-0.5 font-medium break-all text-foreground">
            {premise.window.address}
          </mark>
        ) : null}
        {premise.window.after}
      </blockquote>

      <Lines lines={[...premise.facts, ...premise.answers]} />

      {premise.technicalDetails !== null ? (
        <TechnicalDetails
          title={premise.title}
          lines={premise.technicalDetails}
        />
      ) : null}
    </li>
  );
}

function Lines({ lines }: { lines: PremiseLine[] }) {
  if (lines.length === 0) return null;
  return (
    <dl className="grid grid-cols-1 gap-x-6 gap-y-1 sm:grid-cols-[max-content_1fr]">
      {lines.map((line) => (
        <div key={line.label} className="contents">
          <dt className="text-muted-foreground">{line.label}</dt>
          <dd className="min-w-0 break-words">
            {line.value}
            <span className="text-muted-foreground"> · {line.assessedBy}</span>
          </dd>
        </div>
      ))}
    </dl>
  );
}

// Collapsed on entry. The trigger is a native button (Enter/Space,
// aria-expanded) styled as a link with a focus ring, as in Punktacja.
function TechnicalDetails({
  title,
  lines,
}: {
  title: string;
  lines: { label: string; value: string }[];
}) {
  const [open, setOpen] = useState(false);
  return (
    <Collapsible
      open={open}
      onOpenChange={setOpen}
      className="flex flex-col items-start gap-2"
    >
      <CollapsibleTrigger
        className={buttonVariants({ variant: "link", className: "px-0" })}
      >
        {open ? "Ukryj" : "Pokaż"} szczegóły techniczne
        <span className="sr-only">: {title}</span>
      </CollapsibleTrigger>
      <CollapsibleContent>
        <dl
          className={cn(
            "grid grid-cols-1 gap-x-6 gap-y-1 sm:grid-cols-[max-content_1fr]",
            "text-muted-foreground"
          )}
        >
          {lines.map((line) => (
            <div key={line.label} className="contents">
              <dt>{line.label}</dt>
              <dd className="min-w-0 break-all tabular-nums">{line.value}</dd>
            </div>
          ))}
        </dl>
      </CollapsibleContent>
    </Collapsible>
  );
}
