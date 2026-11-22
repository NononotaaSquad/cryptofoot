"use client";

import { useReviewedMarks } from "@/features/case-details/reviewed-store";

import { countNewResults } from "./case-list-model";
import { useCaseList } from "./use-case-list";

// The red circle next to the "Lista spraw" link: how many Cases have New
// Results, over all Cases (no filter). Shown only once the list is known and
// the count is positive; a failed or pending read is not "0".
export function NewResultsBadge() {
  const { data } = useCaseList();
  const items = data?.data;
  const marks = useReviewedMarks(
    (items ?? []).flatMap((item) => (item.id === undefined ? [] : [item.id]))
  );
  if (items === undefined) return null;
  const count = countNewResults(items, marks);
  if (count === 0) return null;

  return (
    <>
      <span
        aria-hidden="true"
        className="ml-2 inline-flex h-5 min-w-5 items-center justify-center rounded-full bg-destructive px-1.5 align-middle text-xs font-bold text-white"
      >
        {count}
      </span>
      <span className="sr-only">
        {" "}
        (sprawy z nowymi wynikami: {String(count)})
      </span>
    </>
  );
}
