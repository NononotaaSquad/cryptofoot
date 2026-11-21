import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@workspace/ui/components/table";

import { CollapsibleSection } from "./collapsible-section";
import type { RejectedAddressView } from "./report-view-model";

interface RejectedAddressesProps {
  heading: string;
  rejectedAddresses: RejectedAddressView[];
}

export function RejectedAddresses({
  heading,
  rejectedAddresses,
}: RejectedAddressesProps) {
  return (
    <CollapsibleSection
      heading={heading}
      emptyMessage={
        rejectedAddresses.length === 0
          ? "Nie znaleziono adresów odrzuconych."
          : null
      }
    >
      <Table className="text-base">
        <TableHeader>
          <TableRow>
            <TableHead>Adres</TableHead>
            <TableHead>Powód odrzucenia</TableHead>
            <TableHead>Źródło</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {rejectedAddresses.map((rejected) => (
            <TableRow key={rejected.key}>
              <TableCell className="font-mono break-all whitespace-normal">
                {rejected.address}
              </TableCell>
              <TableCell className="whitespace-normal">
                {rejected.reasonLabel}
              </TableCell>
              <TableCell className="whitespace-normal">
                {rejected.sourceNames}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </CollapsibleSection>
  );
}
