import { Button } from "@workspace/ui/components/button";
import {
  Pagination,
  PaginationContent,
  PaginationItem,
} from "@workspace/ui/components/pagination";
import { ChevronLeftIcon, ChevronRightIcon } from "lucide-react";

import {
  hasPages,
  pageLabel,
  rangeLabel,
  type Page,
} from "./case-list-pagination";

const buttonClass =
  "h-10 px-4 text-base data-disabled:cursor-not-allowed data-disabled:opacity-50";

// Previous / next for one group of the Case list. Buttons, not the shared
// PaginationLink: the page is screen state, not an address. A button that
// reaches an end stays focusable (aria-disabled), so the keyboard focus is
// not dropped to the page body.
export function CaseListPager({
  groupName,
  page,
  onPageChange,
}: {
  // Names the navigator: two of them can be on the screen at once.
  groupName: string;
  page: Page<unknown>;
  // `byPointer`: a mouse or touch press (a click from Enter or Space has
  // `detail` 0), for the caller to decide about scrolling.
  onPageChange: (page: number, byPointer: boolean) => void;
}) {
  if (!hasPages(page)) return null;
  const isFirst = page.page === 0;
  const isLast = page.page === page.pageCount - 1;

  return (
    <Pagination
      aria-label={`Strony sekcji ${groupName}`}
      className="mx-0 flex-wrap items-center justify-between gap-x-6 gap-y-3 pt-4"
    >
      <p aria-live="polite" className="text-muted-foreground">
        {rangeLabel(page)}
      </p>
      <PaginationContent className="flex-wrap gap-3">
        <PaginationItem>
          <Button
            variant="outline"
            size="lg"
            focusableWhenDisabled
            disabled={isFirst}
            className={buttonClass}
            onClick={(event) => {
              onPageChange(page.page - 1, event.detail > 0);
            }}
          >
            <ChevronLeftIcon aria-hidden="true" />
            Poprzednia
          </Button>
        </PaginationItem>
        {/* On a narrow screen the page number takes its own line above the
            two buttons, so they stay side by side. */}
        <PaginationItem className="order-first basis-full sm:order-none sm:basis-auto">
          <span className="px-1 font-semibold">{pageLabel(page)}</span>
        </PaginationItem>
        <PaginationItem>
          <Button
            variant="outline"
            size="lg"
            focusableWhenDisabled
            disabled={isLast}
            className={buttonClass}
            onClick={(event) => {
              onPageChange(page.page + 1, event.detail > 0);
            }}
          >
            Następna
            <ChevronRightIcon aria-hidden="true" />
          </Button>
        </PaginationItem>
      </PaginationContent>
    </Pagination>
  );
}
