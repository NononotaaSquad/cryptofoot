import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@workspace/ui/components/table";
import { cn } from "@workspace/ui/lib/utils";
import { ChevronDown, ChevronUp } from "lucide-react";
import { useId, useState } from "react";

import { MentionLink } from "./attribution-details";
import { CollapsibleSection } from "./collapsible-section";
import type { ExcludedAddressView } from "./report-view-model";

interface ExcludedAddressesProps {
  heading: string;
  excludedAddresses: ExcludedAddressView[];
}

export function ExcludedAddresses({
  heading,
  excludedAddresses,
}: ExcludedAddressesProps) {
  return (
    <CollapsibleSection
      heading={heading}
      emptyMessage={
        excludedAddresses.length === 0
          ? "Nie znaleziono adresów wykluczonych."
          : null
      }
    >
      <p>
        Te adresy pojawiły się w źródłach, ale należą do serwisów, z których
        korzysta wiele osób. Nie są powiązaniami.
      </p>
      <Table className="text-base">
        <TableHeader>
          <TableRow>
            <TableHead className="w-1/2">Adres i sieć</TableHead>
            <TableHead>Powód wykluczenia</TableHead>
            <TableHead>Źródło klasyfikacji</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {excludedAddresses.map((excluded) => (
            <ExcludedRow key={excluded.key} excluded={excluded} />
          ))}
        </TableBody>
      </Table>
    </CollapsibleSection>
  );
}

// Per-row state and a real button: a Collapsible panel (a div) is not valid
// inside <tbody>, so the Mentions go in a following full-width row (Q43).
function ExcludedRow({ excluded }: { excluded: ExcludedAddressView }) {
  const [open, setOpen] = useState(false);
  const mentionsId = useId();
  const Chevron = open ? ChevronUp : ChevronDown;

  return (
    <>
      <TableRow className={cn(open && "border-b-0")}>
        <TableCell className="align-top whitespace-normal">
          <button
            type="button"
            aria-expanded={open}
            aria-controls={mentionsId}
            onClick={() => {
              setOpen(!open);
            }}
            className="flex items-start gap-2 rounded-md text-left outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          >
            <Chevron
              className="mt-0.5 size-4 shrink-0 text-muted-foreground"
              aria-hidden="true"
            />
            <span className="font-mono break-all">{excluded.address}</span>
          </button>
          <span className="block pl-6 text-sm text-muted-foreground">
            {excluded.network}
          </span>
        </TableCell>
        <TableCell className="align-top whitespace-normal">
          {excluded.reasonLabel}
        </TableCell>
        <TableCell className="align-top whitespace-normal">
          {excluded.classificationSource}
        </TableCell>
      </TableRow>
      <TableRow id={mentionsId} hidden={!open} className="hover:bg-transparent">
        <TableCell colSpan={3} className="pb-4 pl-8 whitespace-normal">
          <h3 className="mb-1 font-semibold">Wzmianki</h3>
          <ul className="flex flex-col gap-1">
            {excluded.mentions.map((mention) => (
              <li key={mention.key}>
                <span className="font-medium">{mention.sourceName}</span>
                {" – "}
                <MentionLink mention={mention} />
              </li>
            ))}
          </ul>
        </TableCell>
      </TableRow>
    </>
  );
}
