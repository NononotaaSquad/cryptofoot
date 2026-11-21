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
        <h2 className="text-xl font-bold">{heading}</h2>
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
      className="flex flex-col gap-4"
    >
      <h2 className="text-xl font-bold">
        <CollapsibleTrigger className="-mx-2 inline-flex items-center gap-3 rounded-md px-2 py-1 outline-none hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50">
          {heading}
          <span className="text-base font-medium text-primary">
            {open ? "Zwiń" : "Rozwiń"}
          </span>
          <Chevron className="size-5 text-primary" aria-hidden="true" />
        </CollapsibleTrigger>
      </h2>
      <CollapsibleContent className="flex flex-col gap-4">
        {children}
      </CollapsibleContent>
    </Collapsible>
  );
}
