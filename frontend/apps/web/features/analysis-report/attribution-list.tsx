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

// Accordion `multiple`: rows are independent (the Analyst compares leads), all
// closed on entry. The trigger is a native button: Tab, Enter/Space,
// aria-expanded and a focus ring come from Base UI and the shared wrapper.
export function AttributionList({
  heading,
  attributions,
}: AttributionListProps) {
  return (
    <section
      aria-labelledby="attributions-heading"
      className="flex flex-col gap-4"
    >
      {attributions.length > 0 ? (
        <p className="flex items-center gap-2 text-muted-foreground">
          <Info className="size-4 shrink-0" aria-hidden="true" />
          Powiązania są tropami do dalszej weryfikacji, a nie dowodami.
        </p>
      ) : null}
      <h2 id="attributions-heading" className="text-xl font-bold">
        {heading}
      </h2>

      {attributions.length === 0 ? (
        <div className="flex flex-col gap-1 rounded-lg bg-muted p-6">
          <p className="font-semibold">
            Nie znaleziono powiązań w sprawdzonych źródłach.
          </p>
          <p>Brak powiązań nie oznacza, że osoba nie ma adresów kryptowalut.</p>
        </div>
      ) : (
        <Accordion multiple className="rounded-lg border border-border">
          {attributions.map((attribution) => (
            <AccordionItem key={attribution.key} value={attribution.key}>
              <AccordionTrigger className="items-center gap-4 px-4 py-4 text-base hover:no-underline">
                <AttributionRow attribution={attribution} />
              </AccordionTrigger>
              <AccordionContent className="px-4 pb-6 text-base">
                <AttributionDetails attribution={attribution} />
              </AccordionContent>
            </AccordionItem>
          ))}
        </Accordion>
      )}
    </section>
  );
}

// Inside a button: phrasing content only (spans).
function AttributionRow({ attribution }: { attribution: AttributionView }) {
  const { confidence } = attribution;
  return (
    <span className="flex flex-1 flex-wrap items-start gap-x-8 gap-y-2">
      <span className="flex min-w-0 flex-1 basis-72 flex-col gap-1">
        <span className="font-mono break-all">{attribution.address}</span>
        <span className="text-sm text-muted-foreground">
          {attribution.network}
        </span>
        {attribution.inputAddressNote !== null ? (
          <span className="flex items-center gap-1.5 text-sm font-normal">
            <UserRound className="size-4 shrink-0" aria-hidden="true" />
            {attribution.inputAddressNote}
          </span>
        ) : null}
        {attribution.classificationWarning !== null ? (
          <span className="flex items-center gap-1.5 text-sm font-normal text-warning-text">
            <TriangleAlert className="size-4 shrink-0" aria-hidden="true" />
            {attribution.classificationWarning}
          </span>
        ) : null}
      </span>
      <span className="flex w-56 flex-col gap-1.5">
        <span>
          <span className="font-bold">{confidence.text}</span>{" "}
          <span className="font-normal">{confidence.bandLabel}</span>
        </span>
        {/* One colour; the percent and band label above carry the meaning. */}
        <span aria-hidden="true" className="block h-2 rounded-full bg-muted">
          <span
            className="block h-full rounded-full bg-primary"
            style={{ width: `${String(confidence.percent)}%` }}
          />
        </span>
      </span>
      <span className="w-24 text-sm font-normal text-muted-foreground">
        {attribution.mentionCountText}
      </span>
    </span>
  );
}
