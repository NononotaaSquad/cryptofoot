"use client";

import { Button } from "@workspace/ui/components/button";
import { Input } from "@workspace/ui/components/input";
import { Label } from "@workspace/ui/components/label";
import { Skeleton } from "@workspace/ui/components/skeleton";
import { ChevronDown, ChevronUp } from "lucide-react";
import Link from "next/link";
import { useMemo, useRef, useState, type ReactNode } from "react";

import { useReviewedMarks } from "@/features/case-details/reviewed-store";
import { toUserMessage } from "@/lib/api/client";

import { buildCaseList, normalizeQuery, reviewedOpen } from "./case-list-model";
import { CaseListHeader, CaseRow } from "./case-row";
import type { CaseRowView } from "./case-row-view-model";
import { useCaseList } from "./use-case-list";

const searchId = "case-list-search";
const searchHintId = `${searchId}-hint`;
const reviewedListId = "case-list-reviewed";

// Case list: search, the statistics, the New Results and Reviewed groups and
// the two empty states. The query, the expansion and the focus live here, so a
// background refresh of the list never resets them.
export function CaseListView() {
  const casesQuery = useCaseList();
  const [query, setQuery] = useState("");
  // The Analyst's own choice for the Reviewed group, kept while searching.
  const [reviewedChoice, setReviewedChoice] = useState(false);
  // A choice made during a search; null: follow the automatic rule.
  const [searchOverride, setSearchOverride] = useState<boolean | null>(null);
  const searchRef = useRef<HTMLInputElement>(null);

  const items = useMemo(() => casesQuery.data?.data ?? [], [casesQuery.data]);
  const caseIds = useMemo(
    () => items.flatMap((item) => (item.id === undefined ? [] : [item.id])),
    [items]
  );
  const marks = useReviewedMarks(caseIds);
  const model = useMemo(
    () => buildCaseList(items, query, marks),
    [items, query, marks]
  );

  if (casesQuery.isPending) {
    return (
      <Page>
        <ListSkeleton />
      </Page>
    );
  }

  // No data at all: not an empty list. The query typed so far is kept.
  if (casesQuery.data === undefined) {
    return (
      <Page>
        <ErrorState
          error={casesQuery.error}
          onRetry={() => {
            void casesQuery.refetch();
          }}
        />
      </Page>
    );
  }

  if (items.length === 0) {
    return (
      <Page>
        <EmptyPanel>
          <p className="text-xl leading-normal font-semibold">
            Nie masz jeszcze żadnej sprawy.
          </p>
          <StartLink href="/">← Rozpocznij od nowej sprawy</StartLink>
        </EmptyPanel>
      </Page>
    );
  }

  const trimmed = normalizeQuery(query);
  const searching = trimmed !== "";
  const open = reviewedOpen({
    searching,
    hasMatches: model.reviewedRows.length > 0,
    choice: reviewedChoice,
    searchOverride,
  });

  const clear = () => {
    setQuery("");
    setSearchOverride(null);
    searchRef.current?.focus();
  };

  return (
    <Page>
      <div className="flex max-w-140 flex-col gap-2">
        <Label htmlFor={searchId} className="text-base font-semibold">
          Szukaj sprawy
        </Label>
        <Input
          ref={searchRef}
          id={searchId}
          type="text"
          autoComplete="off"
          placeholder="np. zenek_fix"
          aria-describedby={searchHintId}
          value={query}
          onChange={(event) => {
            setQuery(event.target.value);
            if (normalizeQuery(event.target.value) === "") {
              setSearchOverride(null);
            }
          }}
        />
        <p id={searchHintId} className="text-sm text-muted-foreground">
          Wpisz numer sprawy, imię i nazwisko, nazwę użytkownika, e-mail lub
          inny identyfikator.
        </p>
      </div>

      {casesQuery.isError ? (
        <RefreshError
          error={casesQuery.error}
          onRetry={() => {
            void casesQuery.refetch();
          }}
        />
      ) : null}

      {model.matching === 0 ? (
        <EmptyPanel>
          <p className="text-xl leading-normal font-semibold break-words">
            Nie znaleziono sprawy dla „{trimmed}”.
          </p>
          <p className="leading-normal">
            Sprawdź pisownię lub wpisz inny identyfikator.
          </p>
          <Button
            variant="link"
            className="h-auto px-0 text-base font-semibold"
            onClick={clear}
          >
            Wyczyść
          </Button>
        </EmptyPanel>
      ) : (
        <>
          <p className="flex flex-wrap gap-x-6 gap-y-1">
            <span>
              Liczba spraw: <strong>{model.matching}</strong>
            </span>
            <span>
              Nowe wyniki: <strong>{model.newRows.length}</strong>
            </span>
          </p>

          {model.newRows.length > 0 ? (
            <Group
              title={`Nowe wyniki (${String(model.newRows.length)})`}
              rows={model.newRows}
              emphasized
            />
          ) : null}

          {model.reviewedRows.length > 0 ? (
            <section aria-labelledby="case-list-reviewed-title">
              <div className="flex items-center justify-between gap-3 py-3 pr-4">
                <h2
                  id="case-list-reviewed-title"
                  className="text-2xl leading-tight font-bold"
                >
                  {`Przejrzane sprawy (${String(model.reviewedRows.length)})`}
                </h2>
                <Button
                  variant="link"
                  className="h-auto px-0 text-base font-semibold"
                  aria-expanded={open}
                  aria-controls={reviewedListId}
                  onClick={() => {
                    if (searching) setSearchOverride(!open);
                    else setReviewedChoice(!open);
                  }}
                >
                  {open ? "Zwiń" : "Rozwiń"}
                  {open ? (
                    <ChevronUp aria-hidden="true" />
                  ) : (
                    <ChevronDown aria-hidden="true" />
                  )}
                </Button>
              </div>
              {open ? (
                <div id={reviewedListId} className="mt-3 flex flex-col">
                  <RowList rows={model.reviewedRows} />
                </div>
              ) : null}
            </section>
          ) : null}
        </>
      )}
    </Page>
  );
}

function Page({ children }: { children: ReactNode }) {
  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-col gap-2.5">
        <h1 className="text-[2.5rem] leading-tight font-bold">Lista spraw</h1>
        <p className="text-base leading-normal">
          Tu znajdziesz wszystkie sprawy. Wybierz sprawę, aby zobaczyć jej
          analizy.
        </p>
      </div>
      {children}
    </div>
  );
}

function Group({
  title,
  rows,
  emphasized = false,
}: {
  title: string;
  rows: CaseRowView[];
  emphasized?: boolean;
}) {
  return (
    <section aria-label={title} className="flex flex-col gap-3">
      <h2 className="text-2xl leading-tight font-bold">{title}</h2>
      <RowList rows={rows} emphasized={emphasized} />
    </section>
  );
}

function RowList({
  rows,
  emphasized = false,
}: {
  rows: CaseRowView[];
  emphasized?: boolean;
}) {
  return (
    <div className="flex flex-col gap-1">
      <CaseListHeader />
      <ul className="flex flex-col gap-1">
        {rows.map((row) => (
          <CaseRow key={row.caseId} row={row} emphasized={emphasized} />
        ))}
      </ul>
    </div>
  );
}

function EmptyPanel({ children }: { children: ReactNode }) {
  return (
    <div className="flex flex-col items-start gap-3 rounded-lg bg-muted px-8 py-8">
      {children}
    </div>
  );
}

function StartLink({ href, children }: { href: "/"; children: ReactNode }) {
  return (
    <Link
      href={href}
      className="rounded-sm text-base font-semibold text-primary underline-offset-4 outline-none hover:underline focus-visible:ring-3 focus-visible:ring-ring/50"
    >
      {children}
    </Link>
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

// A failed background refresh: the rows stay as they were, the notice does
// not block them.
function RefreshError({
  error,
  onRetry,
}: {
  error: unknown;
  onRetry: () => void;
}) {
  return (
    <div
      role="alert"
      className="flex flex-wrap items-center gap-x-4 gap-y-2 rounded-lg bg-destructive/10 px-4 py-3"
    >
      <p className="leading-normal">
        Nie udało się odświeżyć listy. Widzisz poprzednie dane.{" "}
        {toUserMessage(error)}
      </p>
      <Button
        variant="link"
        className="h-auto px-0 text-base font-semibold"
        onClick={onRetry}
      >
        Spróbuj ponownie
      </Button>
    </div>
  );
}
