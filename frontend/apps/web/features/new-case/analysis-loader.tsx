"use client";

import {
  Dialog,
  DialogContent,
  DialogTitle,
} from "@workspace/ui/components/dialog";
import { Loader2Icon } from "lucide-react";
import { useEffect } from "react";

interface AnalysisLoaderProps {
  open: boolean;
  // The tab-close warning; the caller turns it off once the Analysis has
  // ended, while the loader may still cover the page during navigation.
  warnBeforeUnload: boolean;
}

function warnBeforeUnloadHandler(event: BeforeUnloadEvent) {
  event.preventDefault();
  // Legacy part of the MDN pattern: some browsers still show the prompt only
  // when `returnValue` is set; `preventDefault()` alone covers the rest.
  // eslint-disable-next-line @typescript-eslint/no-deprecated -- kept on purpose for those browsers (MDN, "Window: beforeunload event").
  event.returnValue = "";
}

/**
 * Full-screen blocking loader shown while the Analysis runs.
 *
 * Built on the modal Base UI Dialog rather than a plain overlay with `inert`:
 * a modal dialog already traps focus, locks the page scroll, blocks pointer
 * events outside it and hides the rest of the page from assistive technology,
 * including the page shell header that lies outside this component, which an
 * `inert` overlay would have to reach by hand. The dialog is controlled and
 * every close request (Escape, outside press) is cancelled, so it closes
 * only when `open` turns false. It has no close button on purpose: the
 * Analyst must not leave while the Analysis runs. The dialog takes focus and
 * its title is its accessible name, so a screen reader announces the text.
 */
export function AnalysisLoader({
  open,
  warnBeforeUnload,
}: AnalysisLoaderProps) {
  useEffect(() => {
    if (!warnBeforeUnload) return;
    window.addEventListener("beforeunload", warnBeforeUnloadHandler);
    return () => {
      window.removeEventListener("beforeunload", warnBeforeUnloadHandler);
    };
  }, [warnBeforeUnload]);

  return (
    <Dialog
      open={open}
      disablePointerDismissal
      onOpenChange={(_nextOpen, eventDetails) => {
        eventDetails.cancel();
      }}
    >
      <DialogContent
        showCloseButton={false}
        className="top-0 left-0 flex size-full max-w-none translate-x-0 translate-y-0 flex-col items-center justify-center gap-6 rounded-none bg-background p-6 text-base ring-0 sm:max-w-none"
      >
        <Loader2Icon
          aria-hidden="true"
          className="size-12 animate-spin text-primary motion-reduce:animate-none"
        />
        <DialogTitle className="text-center text-xl leading-normal font-bold">
          Trwa analiza. Nie zamykaj tej strony.
        </DialogTitle>
      </DialogContent>
    </Dialog>
  );
}
