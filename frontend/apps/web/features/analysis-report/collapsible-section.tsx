import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@workspace/ui/components/collapsible";
import { ChevronDown, ChevronUp } from "lucide-react";
import { type ReactNode, useState } from "react";

interface CollapsibleSectionProps {
  /** "Adresy wykluczone (N)" */
  heading: string;
  /** Shown instead of the trigger when the section is empty (Q41). */
  emptyMessage: string | null;
  children: ReactNode;
}

/**
 * A report section collapsed on entry ("Rozwiń" / "Zwiń"). The trigger is a
 * native button inside the heading (Tab, Enter/Space, aria-expanded) with the
 * shared focus ring. An empty section has nothing to expand: it shows its
 * "(0)" heading and the message directly.
 */
export function CollapsibleSection({
  heading,
  emptyMessage,
  children,
}: CollapsibleSectionProps) {
  const [open, setOpen] = useState(false);

  if (emptyMessage !== null) {
    return (
      <section className="flex flex-col gap-2">
        <h2 className="text-2xl leading-tight font-bold">{heading}</h2>
        <p className="text-muted-foreground">{emptyMessage}</p>
      </section>
    );
  }

  const Chevron = open ? ChevronUp : ChevronDown;
  return (
    <Collapsible
      open={open}
      onOpenChange={setOpen}
      render={<section />}
      className="flex flex-col gap-3"
    >
      {/* Design: heading on the left, "Rozwiń" + chevron on the right. */}
      <h2 className="text-2xl leading-tight font-bold">
        <CollapsibleTrigger className="group flex w-full items-center justify-between gap-3 py-3 pr-4 text-left outline-none">
          {heading}
          <span className="inline-flex items-center gap-1.5 text-base font-semibold text-primary group-hover:underline">
            {open ? "Zwiń" : "Rozwiń"}
            <Chevron className="size-6" aria-hidden="true" />
          </span>
        </CollapsibleTrigger>
      </h2>
      <CollapsibleContent className="flex flex-col gap-4">
        {children}
      </CollapsibleContent>
    </Collapsible>
  );
}
