"use client";

import { Button } from "@workspace/ui/components/button";
import { Input } from "@workspace/ui/components/input";
import { Label } from "@workspace/ui/components/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@workspace/ui/components/select";
import { Skeleton } from "@workspace/ui/components/skeleton";
import { ChevronDown, ChevronUp } from "lucide-react";
import Link from "next/link";
import {
  useMemo,
  useRef,
  useState,
  type ReactNode,
  type RefObject,
} from "react";

import { useReviewedMarks } from "@/features/case-details/reviewed-store";
import { toUserMessage } from "@/lib/api/client";

import { buildCaseList, normalizeQuery, reviewedOpen } from "./case-list-model";
import { CaseListPager } from "./case-list-pager";
import {
  defaultPageSize,
  firstPages,
  isPageSize,
  offersPageSize,
  pageCaseList,
  pageSizes,
  pagesMoved,
  searchChanged,
  type GroupPages,
  type Page as RowsPage,
  type PageSize,
} from "./case-list-pagination";
import { CaseListHeader, CaseRow } from "./case-row";
import type { CaseRowView } from "./case-row-view-model";
import { useCaseList } from "./use-case-list";

const searchId = "case-list-search";
const searchHintId = `${searchId}-hint`;
const reviewedListId = "case-list-reviewed";
const pageSizeId = "case-list-page-size";
const newResultsName = "Nowe wyniki";
const reviewedName = "Przejrzane sprawy";
const pageSizeItems = pageSizes.map((size) => ({
  value: String(size),
  label: String(size),
}));

// Case list: search, the statistics, the New Results and Reviewed groups, their
// pages and the two empty states. The query, the expansion, the pages and the
// focus live here, so a background refresh of the list never resets them.
export function CaseListView() {
  const casesQuery = useCaseList();
  const [query, setQuery] = useState("");
  // The Analyst's own choice for the Reviewed group, kept while searching.
  const [reviewedChoice, setReviewedChoice] = useState(false);
  // A choice made during a search; null: follow the automatic rule.
  const [searchOverride, setSearchOverride] = useState<boolean | null>(null);
  // Rows drawn per page in each group and the page of each group (#104):
  // screen state only, the query still holds every Case.
  const [pageSize, setPageSize] = useState<PageSize>(defaultPageSize);
  const [pages, setPages] = useState<GroupPages>(firstPages);
  const searchRef = useRef<HTMLInputElement>(null);
  const newHeadingRef = useRef<HTMLHeadingElement>(null);
  const reviewedHeadingRef = useRef<HTMLHeadingElement>(null);

  const items = useMemo(() => casesQuery.data?.data ?? [], [casesQuery.data]);
  const caseIds = useMemo(
    () => items.flatMap((item) => (item.id === undefined ? [] : [item.id])),
    [items]
  );
  const marks = useReviewedMarks(caseIds);
  // Search, groups and order over the full answer; the pages come last.
  const model = useMemo(
    () => buildCaseList(items, query, marks),
    [items, query, marks]
  );
  const paged = pageCaseList(model, pages, pageSize);

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
          <p className="text-lg leading-normal">
            Nie masz jeszcze żadnej sprawy.
          </p>
          <StartLink href="/">← Rozpocznij od nowej sprawy</StartLink>
        </EmptyPanel>
      </Page>
    );
  }

  // A refresh or a mark made a group shorter: keep the last page that still
  // exists, so a later refresh does not jump back to a page the Analyst left.
  if (pagesMoved(pages, paged)) {
    setPages({
      newResults: paged.newResults.page,
      reviewed: paged.reviewed.page,
    });
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
    setPages(firstPages);
    searchRef.current?.focus();
  };

  // The pressed button keeps the focus. After a mouse or touch press the
  // group's heading comes back into view when it scrolled away, so the new
  // page is read from its first row. After Enter or Space the focused button
  // is kept on screen instead, although the new page is shorter or longer
  // (the range is announced to screen readers).
  const goToPage = (
    group: keyof GroupPages,
    page: number,
    byPointer: boolean,
    heading: RefObject<HTMLHeadingElement | null>
  ) => {
    setPages((current) => ({ ...current, [group]: page }));
    if (!byPointer) {
      // Next frame: the new page is on screen by then. Centred, so the focus
      // ring is not cut by the window edge.
      requestAnimationFrame(() => {
        const focused = document.activeElement;
        if (!(focused instanceof HTMLElement)) return;
        const { top, bottom } = focused.getBoundingClientRect();
        if (top < 0 || bottom > window.innerHeight) {
          focused.scrollIntoView({ block: "center" });
        }
      });
      return;
    }
    const element = heading.current;
    if (element !== null && element.getBoundingClientRect().top < 0) {
      element.scrollIntoView({ block: "start" });
    }
  };

  return (
    <Page>
      <div className="flex max-w-120 flex-col gap-2">
        <Label htmlFor={searchId} className="text-base font-semibold">
          Szukaj sprawy
        </Label>
        <Input
          ref={searchRef}
          id={searchId}
          type="text"
          autoComplete="off"
          placeholder="np. krypto_janek"
          aria-describedby={searchHintId}
          value={query}
          onChange={(event) => {
            const next = event.target.value;
            if (searchChanged(query, next)) setPages(firstPages);
            setQuery(next);
            if (normalizeQuery(next) === "") {
              setSearchOverride(null);
            }
          }}
          className="h-12 px-4"
        />
        <p id={searchHintId} className="text-sm text-muted-foreground">
          Wpisz numer sprawy, imię i nazwisko, nick, e-mail lub inny
          identyfikator.
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
          <p className="text-lg leading-normal break-words">
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
          <div className="flex flex-wrap items-end justify-between gap-x-6 gap-y-4">
            {/* All matching Cases, never only the page on screen. */}
            <p className="flex flex-wrap gap-x-6 gap-y-1">
              <span>
                Liczba spraw: <strong>{model.matching}</strong>
              </span>
              <span>
                Nowe wyniki: <strong>{model.newRows.length}</strong>
              </span>
            </p>
            {offersPageSize(model) ? (
              <PageSizeSelect
                value={pageSize}
                onChange={(size) => {
                  setPageSize(size);
                  setPages(firstPages);
                }}
              />
            ) : null}
          </div>

          {model.newRows.length > 0 ? (
            <section
              aria-labelledby="case-list-new-title"
              className="flex flex-col gap-6"
            >
              <h2
                ref={newHeadingRef}
                id="case-list-new-title"
                className="scroll-mt-4 text-2xl leading-tight font-bold"
              >
                {`${newResultsName} (${String(model.newRows.length)})`}
              </h2>
              <PagedRows
                groupName={newResultsName}
                page={paged.newResults}
                emphasized
                onPageChange={(page, byPointer) => {
                  goToPage("newResults", page, byPointer, newHeadingRef);
                }}
              />
            </section>
          ) : null}

          {model.reviewedRows.length > 0 ? (
            <section aria-labelledby="case-list-reviewed-title">
              <div className="flex items-center justify-between gap-4">
                <h2
                  ref={reviewedHeadingRef}
                  id="case-list-reviewed-title"
                  className="scroll-mt-4 text-2xl leading-tight font-bold"
                >
                  {`${reviewedName} (${String(model.reviewedRows.length)})`}
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
              {/* Collapsed: neither the rows nor their page controls. */}
              {open ? (
                <div id={reviewedListId} className="mt-6 flex flex-col">
                  <PagedRows
                    groupName={reviewedName}
                    page={paged.reviewed}
                    onPageChange={(page, byPointer) => {
                      goToPage("reviewed", page, byPointer, reviewedHeadingRef);
                    }}
                  />
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
      <div className="flex flex-col gap-3">
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

// The rows of one page of a group and, with more than one page, its navigator.
function PagedRows({
  groupName,
  page,
  emphasized = false,
  onPageChange,
}: {
  groupName: string;
  page: RowsPage<CaseRowView>;
  emphasized?: boolean;
  onPageChange: (page: number, byPointer: boolean) => void;
}) {
  return (
    <div className="flex flex-col">
      <CaseListHeader />
      <ul>
        {page.rows.map((row) => (
          <CaseRow key={row.caseId} row={row} emphasized={emphasized} />
        ))}
      </ul>
      <CaseListPager
        groupName={groupName}
        page={page}
        onPageChange={onPageChange}
      />
    </div>
  );
}

// One size for both groups; the label says what the limit applies to.
function PageSizeSelect({
  value,
  onChange,
}: {
  value: PageSize;
  onChange: (size: PageSize) => void;
}) {
  return (
    <div className="flex flex-col gap-2">
      <Label htmlFor={pageSizeId} className="text-base font-semibold">
        Wierszy na stronę w każdej sekcji
      </Label>
      <Select
        items={pageSizeItems}
        value={String(value)}
        onValueChange={(next) => {
          // `null` would mean "nothing selected"; every option is a size.
          const size = Number(next);
          if (next !== null && isPageSize(size)) onChange(size);
        }}
      >
        <SelectTrigger
          id={pageSizeId}
          className="h-10 w-28 px-3 text-base data-[size=default]:h-10"
        >
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {pageSizeItems.map((item) => (
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
