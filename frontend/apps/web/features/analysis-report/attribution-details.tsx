import { buttonVariants } from "@workspace/ui/components/button";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@workspace/ui/components/collapsible";
import { CircleCheck, Info, TriangleAlert } from "lucide-react";
import { type ReactNode, useState } from "react";

import type { AddressActivityFigures } from "./address-activity-figures";
import type { AttributionView, MentionView } from "./report-view-model";

export function AttributionDetails({
  attribution,
}: {
  attribution: AttributionView;
}) {
  return (
    <div className="flex flex-col gap-6">
      <Scoring attribution={attribution} />

      {attribution.justification !== "" ? (
        <Block heading="Uzasadnienie">
          <p>{attribution.justification}</p>
        </Block>
      ) : null}

      <Block heading={attribution.mentionsHeading}>
        <ul className="flex flex-col gap-4">
          {attribution.mentions.map((mention) => (
            <li key={mention.key} className="flex flex-col gap-1">
              <span>
                <span className="font-medium">{mention.sourceName}</span>
                {mention.collectedAt !== null ? (
                  <span className="text-muted-foreground">
                    {" "}
                    · zebrano {mention.collectedAt}
                  </span>
                ) : null}
              </span>
              <MentionLink mention={mention} />
              {mention.context !== null ? (
                <blockquote className="border-l-2 border-border pl-3 text-muted-foreground italic">
                  „{mention.context}”
                </blockquote>
              ) : null}
            </li>
          ))}
        </ul>
      </Block>

      <Block heading="Weryfikacja adresu">
        <ul className="flex flex-col gap-1">
          {attribution.verification.map((line) => (
            <li
              key={line.text}
              className={
                line.ok
                  ? "flex items-center gap-2"
                  : "flex items-center gap-2 text-warning-text"
              }
            >
              {line.ok ? (
                <CircleCheck className="size-4 shrink-0" aria-hidden="true" />
              ) : (
                <TriangleAlert className="size-4 shrink-0" aria-hidden="true" />
              )}
              {line.text}
            </li>
          ))}
        </ul>
      </Block>

      <Block heading={attribution.activityHeading}>
        <Activity figures={attribution.activity} />
      </Block>
    </div>
  );
}

// Collapsible, not Accordion: one independent block. The trigger is a native
// button (Enter/Space, aria-expanded) styled as a link with a focus ring.
function Scoring({ attribution }: { attribution: AttributionView }) {
  const [open, setOpen] = useState(false);
  return (
    <Collapsible
      open={open}
      onOpenChange={setOpen}
      className="flex flex-col gap-2"
    >
      <h4 className="flex items-center gap-3 font-semibold">
        Punktacja
        <CollapsibleTrigger
          className={buttonVariants({ variant: "link", className: "px-0" })}
        >
          {open ? "Zwiń" : "Rozwiń"}
          <span className="sr-only"> punktację</span>
        </CollapsibleTrigger>
      </h4>
      <CollapsibleContent>
        <dl className="grid max-w-xl grid-cols-[1fr_max-content] gap-x-6 gap-y-1">
          {attribution.scoringLines.map((line, index) => (
            // The same rule may come twice; the index keeps keys unique.
            <div key={`${String(index)}-${line.label}`} className="contents">
              <dt>{line.label}</dt>
              <dd className="text-right tabular-nums">{line.points}</dd>
            </div>
          ))}
          <div className="contents font-bold">
            <dt>Stopień pewności</dt>
            <dd className="text-right tabular-nums">
              {attribution.confidenceTotal}
            </dd>
          </div>
        </dl>
      </CollapsibleContent>
    </Collapsible>
  );
}

function Activity({ figures }: { figures: AddressActivityFigures }) {
  if (!figures.available) {
    return (
      <p className="flex items-start gap-2 rounded-lg bg-muted p-4">
        <Info className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
        {figures.message}
      </p>
    );
  }
  return (
    <div className="flex flex-col gap-3">
      <dl className="grid grid-cols-[max-content_1fr] gap-x-6 gap-y-1">
        <dt className="text-muted-foreground">Łączny wolumen</dt>
        <dd>{figures.volume}</dd>
        <dt className="text-muted-foreground">Liczba transakcji</dt>
        <dd>{figures.transactionCount}</dd>
        <dt className="text-muted-foreground">Okres</dt>
        <dd>{figures.period ?? "—"}</dd>
      </dl>
      {figures.events.length > 0 ? (
        <ul className="flex flex-col gap-2">
          {figures.events.map((event) => (
            <li key={event.key} className="flex flex-col">
              <span>
                <span className="text-muted-foreground">
                  {event.occurredAt}
                </span>{" "}
                <span className="font-medium">{event.typeLabel}</span>
                {event.amount !== null ? ` ${event.amount}` : null}
              </span>
              {event.cashOutNote !== null ? (
                <span className="flex items-center gap-1.5 text-warning-text">
                  <TriangleAlert
                    className="size-4 shrink-0"
                    aria-hidden="true"
                  />
                  {event.cashOutNote}
                </span>
              ) : null}
            </li>
          ))}
        </ul>
      ) : null}
      {figures.coverageLimitation !== null ? (
        <p className="flex items-start gap-2 text-warning-text">
          <TriangleAlert
            className="mt-0.5 size-4 shrink-0"
            aria-hidden="true"
          />
          {figures.coverageLimitation}
        </p>
      ) : null}
    </div>
  );
}

function Block({
  heading,
  children,
}: {
  heading: string;
  children: ReactNode;
}) {
  return (
    <div className="flex flex-col gap-2">
      <h4 className="font-semibold">{heading}</h4>
      {children}
    </div>
  );
}

/** Source link; a non-http(s) address is shown as text, never as a link. */
export function MentionLink({ mention }: { mention: MentionView }) {
  return mention.url === null ? (
    <span className="break-all">{mention.urlText}</span>
  ) : (
    <a
      href={mention.url}
      target="_blank"
      rel="noopener noreferrer"
      className="break-all text-primary underline underline-offset-3"
    >
      {mention.urlText}
    </a>
  );
}
