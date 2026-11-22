import { buttonVariants } from "@workspace/ui/components/button";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@workspace/ui/components/collapsible";
import {
  ChevronDown,
  ChevronUp,
  CircleCheck,
  Info,
  TriangleAlert,
} from "lucide-react";
import { type ReactNode, useState } from "react";

import type { AddressActivityFigures } from "./address-activity-figures";
import { PremiseList } from "./premise-list";
import type { AttributionView, MentionView } from "./report-view-model";

export function AttributionDetails({
  attribution,
}: {
  attribution: AttributionView;
}) {
  return (
    // Design: 36px between blocks, 18px bold block headings, 14px under them.
    <div className="flex flex-col gap-9">
      <Scoring attribution={attribution} />

      <Block heading="Uzasadnienie">
        <p>{attribution.justification}</p>
      </Block>

      <PremiseList
        heading={attribution.premisesHeading}
        premises={attribution.premises}
      />

      <Block heading={attribution.mentionsHeading}>
        <ul className="flex flex-col gap-5">
          {attribution.mentions.map((mention) => (
            <li
              key={mention.key}
              className="flex flex-col gap-1.5 not-first:border-t not-first:border-border not-first:pt-5"
            >
              <span>
                <span className="font-semibold">{mention.sourceName}</span>
                {mention.collectedAt !== null ? (
                  <span className="text-muted-foreground">
                    {" "}
                    · zebrano {mention.collectedAt}
                  </span>
                ) : null}
              </span>
              <MentionLink mention={mention} />
              {mention.context !== null ? (
                <blockquote className="border-l-3 border-border-strong py-1 pl-3.5 text-muted-foreground italic">
                  „{mention.context}”
                </blockquote>
              ) : null}
            </li>
          ))}
        </ul>
      </Block>

      <Block heading="Weryfikacja adresu">
        <ul className="flex flex-col gap-2">
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
      className="flex flex-col gap-3.5"
    >
      <h4 className="flex items-center gap-3 text-lg leading-snug font-bold">
        Punktacja
        <CollapsibleTrigger
          className={buttonVariants({
            variant: "link",
            className: "h-auto px-0 text-base font-semibold",
          })}
        >
          {open ? "Zwiń" : "Rozwiń"}
          <span className="sr-only"> punktację</span>
          {open ? (
            <ChevronUp aria-hidden="true" className="size-6" />
          ) : (
            <ChevronDown aria-hidden="true" className="size-6" />
          )}
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
    <div className="flex flex-col gap-3.5">
      {/* Design: the three figures side by side, label over a bold value. */}
      <dl className="flex flex-wrap gap-x-8 gap-y-3">
        <Figure label="Łączny wolumen" value={figures.volume} />
        <Figure label="Liczba transakcji" value={figures.transactionCount} />
        <Figure label="Okres" value={figures.period ?? "—"} />
      </dl>
      <p className="text-sm break-all text-muted-foreground">
        Źródło danych: {figures.source}
      </p>
      {figures.events.length > 0 ? (
        <ul className="flex flex-col border-t border-border">
          {figures.events.map((event) => (
            <li
              key={event.key}
              className="grid grid-cols-[6rem_minmax(0,1fr)] gap-x-4 border-b border-border py-3 last:border-b-0"
            >
              <span className="text-muted-foreground">{event.occurredAt}</span>
              <span className="flex min-w-0 flex-col gap-1">
                <span>
                  <span className="font-semibold">{event.typeLabel}</span>
                  {event.amount !== null ? ` ${event.amount}` : null}
                </span>
                {event.counterparty !== null ? (
                  <span className="break-all text-muted-foreground">
                    Kontrahent: {event.counterparty}
                  </span>
                ) : null}
                <span className="break-all text-muted-foreground">
                  Transakcja –{" "}
                  {event.transactionUrl === null ? (
                    event.transaction
                  ) : (
                    <a
                      href={event.transactionUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-primary underline underline-offset-3"
                    >
                      {event.transaction}
                    </a>
                  )}
                </span>
                <span className="flex items-center gap-1.5 text-warning-text">
                  <TriangleAlert
                    className="size-4 shrink-0"
                    aria-hidden="true"
                  />
                  {event.cashOutNote}
                </span>
              </span>
            </li>
          ))}
        </ul>
      ) : null}
      {figures.coverageLimitations.map((sentence) => (
        <p key={sentence} className="flex items-start gap-2 text-warning-text">
          <TriangleAlert
            className="mt-0.5 size-4 shrink-0"
            aria-hidden="true"
          />
          {sentence}
        </p>
      ))}
    </div>
  );
}

function Figure({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-0.5">
      <dt className="text-sm text-muted-foreground">{label}</dt>
      <dd className="text-lg font-bold">{value}</dd>
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
    <div className="flex flex-col gap-3.5">
      <h4 className="text-lg leading-snug font-bold">{heading}</h4>
      {children}
    </div>
  );
}

/** Source link; a non-http(s) address is shown as text, never as a link. */
export function MentionLink({
  mention,
}: {
  mention: Pick<MentionView, "url" | "urlText">;
}) {
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
