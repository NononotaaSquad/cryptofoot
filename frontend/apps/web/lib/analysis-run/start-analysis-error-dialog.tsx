"use client";

import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@workspace/ui/components/alert-dialog";
import { Button } from "@workspace/ui/components/button";

import { toUserMessage } from "@/lib/api/client";

interface StartAnalysisErrorDialogProps {
  open: boolean;
  // Kept by the caller after closing, so the text does not change while the
  // closing animation runs.
  error: unknown;
  onClose: () => void;
  onRetry: () => void;
}

// Failed start of an Analysis, including a 409 (one is already running). The
// text comes only from toUserMessage, the one wording for backend errors in
// the app (frontend.md).
export function StartAnalysisErrorDialog({
  open,
  error,
  onClose,
  onRetry,
}: StartAnalysisErrorDialogProps) {
  return (
    <AlertDialog
      open={open}
      onOpenChange={(nextOpen) => {
        if (!nextOpen) onClose();
      }}
    >
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Coś poszło nie tak</AlertDialogTitle>
          <AlertDialogDescription>
            {toUserMessage(error)}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <Button size="xl" onClick={onRetry}>
            Spróbuj ponownie
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
