"use client";

import { Button } from "@workspace/ui/components/button";
import { Skeleton } from "@workspace/ui/components/skeleton";

import { toUserMessage } from "@/lib/api/client";
import { useListCases } from "@/lib/api/generated/case-controller/case-controller";

import { CaseListHeader, CaseRow } from "./case-row";
import { toCaseRows } from "./case-row-view-model";

// Case list: the heading, the column header and one row per Case. Search,
// the New Results / Reviewed groups, empty states and refresh are #86.
export function CaseListView() {
  // Always read again when the Analyst comes to the list, so the latest
  // Analysis of each Case is not a minute old.
  const casesQuery = useListCases({ query: { refetchOnMount: "always" } });

  return (
    <div className="flex flex-col gap-10">
      <div className="flex flex-col gap-3">
        <h1 className="text-[2.5rem] leading-tight font-bold">Lista spraw</h1>
        <p className="text-base leading-normal">
          Tu znajdziesz wszystkie sprawy. Wybierz sprawę, aby zobaczyć jej
          analizy.
        </p>
      </div>

      {casesQuery.isPending ? (
        <ListSkeleton />
      ) : casesQuery.isError ? (
        <ErrorState
          error={casesQuery.error}
          onRetry={() => {
            void casesQuery.refetch();
          }}
        />
      ) : (
        <section aria-label="Sprawy" className="flex flex-col">
          <CaseListHeader />
          <ul>
            {toCaseRows(casesQuery.data.data).map((row) => (
              <CaseRow key={row.caseId} row={row} />
            ))}
          </ul>
        </section>
      )}
    </div>
  );
}

function ListSkeleton() {
  return (
    <div aria-busy="true" className="flex flex-col gap-3">
      <span className="sr-only" role="status">
        Wczytywanie spraw…
      </span>
      <Skeleton className="h-20 w-full" />
      <Skeleton className="h-20 w-full" />
      <Skeleton className="h-20 w-full" />
    </div>
  );
}

function ErrorState({
  error,
  onRetry,
}: {
  error: unknown;
  onRetry: () => void;
}) {
  return (
    <div
      role="alert"
      className="flex flex-col items-start gap-4 rounded-lg bg-muted px-8 py-10"
    >
      <h2 className="text-2xl leading-tight font-bold">Coś poszło nie tak</h2>
      <p className="leading-normal">{toUserMessage(error)}</p>
      <Button size="xl" onClick={onRetry}>
        Spróbuj ponownie
      </Button>
    </div>
  );
}
