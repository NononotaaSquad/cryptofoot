"use client";

import { Label } from "@workspace/ui/components/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@workspace/ui/components/select";

import { analysisLabel } from "@/features/analysis-report/report-labels";
import type { AnalysisResponse } from "@/lib/api/generated/model";

interface AnalysisSelectProps {
  // Newest first, as the view passes them.
  analyses: readonly AnalysisResponse[];
  // `null` when the `analysis` param names no Analysis of this Case (Q36).
  selectedId: string | null;
  onSelect: (analysisId: string) => void;
}

const triggerId = "case-details-analysis";
const hintId = `${triggerId}-hint`;

// History of Analyses of the Case: one option per Analysis. The selection is
// the `?analysis=` param, written by the caller.
export function AnalysisSelect({
  analyses,
  selectedId,
  onSelect,
}: AnalysisSelectProps) {
  const items = analyses.flatMap((analysis) =>
    analysis.id === undefined
      ? []
      : [{ value: analysis.id, label: analysisLabel(analysis) }]
  );

  return (
    <div className="flex flex-col gap-2">
      {/* A label on the trigger button names the combobox; Base UI adds its
          own aria-labelledby only with Select.Label, which the wrapper lacks. */}
      <Label htmlFor={triggerId} className="text-base font-semibold">
        Wyświetlana analiza
      </Label>
      <Select
        items={items}
        value={selectedId}
        onValueChange={(value) => {
          // `null` would mean "nothing selected"; the history has no such
          // option, so it is ignored.
          if (value !== null) onSelect(value);
        }}
      >
        <SelectTrigger
          id={triggerId}
          aria-describedby={hintId}
          className="h-12 w-full max-w-120 px-4 text-base data-[size=default]:h-12"
        >
          <SelectValue placeholder="Wybierz analizę" />
        </SelectTrigger>
        <SelectContent>
          {items.map((item) => (
            <SelectItem
              key={item.value}
              value={item.value}
              className="py-2 text-base"
            >
              {item.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      <p id={hintId} className="text-sm leading-normal text-muted-foreground">
        Wybierz wcześniejszą analizę, aby zobaczyć jej raport. Liczba analiz:{" "}
        {analyses.length}.
      </p>
    </div>
  );
}
