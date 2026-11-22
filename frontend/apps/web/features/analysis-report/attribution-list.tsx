import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@workspace/ui/components/accordion";
import { Info, TriangleAlert, UserRound } from "lucide-react";

import { AttributionDetails } from "./attribution-details";
import type { AttributionView } from "./report-view-model";

interface AttributionListProps {
  heading: string;
  attributions: AttributionView[];
}

// Columns shared by the header row and each Attribution row (design: address
// fills the rest, 380px confidence, 110px Mentions, 24px chevron, 24px gaps).
// The row itself has no chevron column: the trigger draws its own icon.
const headerGrid =
  "grid grid-cols-[minmax(0,1fr)_23.75rem_6.875rem_1.5rem] gap-x-6";
const rowGrid = "grid grid-cols-[minmax(0,1fr)_23.75rem_6.875rem] gap-x-6";

// Accordion `multiple`: rows are independent (the Analyst compares leads), all
// closed on entry. The trigger is a native button: Tab, Enter/Space,
// aria-expanded and a focus ring come from Base UI and the shared wrapper.
export function AttributionList({
  heading,
  attributions,
}: AttributionListProps) {
  return (
    <div className="flex flex-col gap-10">
      {attributions.length > 0 ? (
        <p className="flex items-center gap-2.5 border-l-4 border-border-strong px-4 py-2.5 leading-normal">
          <Info className="size-5 shrink-0" aria-hidden="true" />
          Powiązania są tropami do dalszej weryfikacji, a nie dowodami.
        </p>
      ) : null}
      <section
        aria-labelledby="attributions-heading"
        className="flex flex-col gap-3"
      >
        <h2
          id="attributions-heading"
          className="text-2xl leading-tight font-bold"
        >
          {heading}
        </h2>

        {attributions.length === 0 ? (
          <div className="flex flex-col gap-1 rounded-lg bg-muted p-6">
            <p className="font-semibold">
              Nie znaleziono powiązań w sprawdzonych źródłach.
            </p>
            <p>
              Brak powiązań nie oznacza, że osoba nie ma adresów kryptowalut.
            </p>
          </div>
        ) : (
          <div className="flex flex-col">
            <div
              aria-hidden="true"
              className={`${headerGrid} border-b border-l-4 border-border-strong border-l-transparent py-2 pr-4 pl-4 text-sm font-semibold text-muted-foreground`}
            >
              <span>Adres i sieć</span>
              <span>Stopień pewności</span>
              <span>Wzmianki</span>
              <span />
            </div>
            <Accordion multiple>
              {attributions.map((attribution) => (
                <AccordionItem
                  key={attribution.key}
                  value={attribution.key}
                  className="border-l-4 border-b-border border-l-transparent data-open:border-l-primary"
                >
                  <AccordionTrigger className="items-center gap-6 rounded-none border-0 py-4.5 pr-4 pl-4 text-base font-normal hover:no-underline aria-expanded:bg-primary-tint **:data-[slot=accordion-trigger-icon]:size-6 **:data-[slot=accordion-trigger-icon]:text-primary">
                    <AttributionRow attribution={attribution} />
                  </AccordionTrigger>
                  <AccordionContent className="max-w-200 pt-7 pr-4 pb-9 pl-4 text-base">
                    <AttributionDetails attribution={attribution} />
                  </AccordionContent>
                </AccordionItem>
              ))}
            </Accordion>
          </div>
        )}
      </section>
    </div>
  );
}

// Inside a button: phrasing content only (spans).
function AttributionRow({ attribution }: { attribution: AttributionView }) {
  const { confidence } = attribution;
  return (
    <span className={`${rowGrid} flex-1 items-center`}>
      <span className="flex min-w-0 flex-col gap-1">
        <span className="font-mono font-semibold break-all">
          {attribution.address}
        </span>
        <span className="text-muted-foreground">{attribution.network}</span>
        {attribution.inputAddressNote !== null ? (
          <span className="flex items-center gap-1.5 text-sm">
            <UserRound className="size-4 shrink-0" aria-hidden="true" />
            {attribution.inputAddressNote}
          </span>
        ) : null}
        {attribution.classificationWarning !== null ? (
          <span className="flex items-center gap-1.5 text-sm text-warning-text">
            <TriangleAlert className="size-4 shrink-0" aria-hidden="true" />
            {attribution.classificationWarning}
          </span>
        ) : null}
      </span>
      <span className="flex flex-col gap-2">
        <span className="flex flex-wrap items-baseline gap-x-2.5">
          <span className="text-xl font-bold">{confidence.text}</span>
          <span className="text-sm">{confidence.bandLabel}</span>
        </span>
        {/* One colour; the percent and band label above carry the meaning.
            Design: 200x6 track, grey fill with a border. */}
        <span
          aria-hidden="true"
          className="block h-1.5 w-50 overflow-hidden rounded-[3px] border border-border-strong bg-muted"
        >
          <span
            className="block h-full rounded-[3px] bg-primary"
            style={{ width: `${String(confidence.percent)}%` }}
          />
        </span>
      </span>
      <span>{attribution.mentionCountText}</span>
    </span>
  );
}
