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

interface PollingErrorDialogProps {
  open: boolean;
  // Opens Case details; the dialog stays until the navigation shows it.
  onConfirm: () => void;
}

// Shown when asking for the state of the Analysis failed (Q28, Q29). The
// copy is fixed on purpose, not `toUserMessage`: the Analysis may still be
// running, so the Analyst is sent to Case details rather than asked to retry.
// Every way of closing (button, Escape) leads to Case details, so the
// Analyst is never left on a page whose run is unknown.
export function PollingErrorDialog({
  open,
  onConfirm,
}: PollingErrorDialogProps) {
  return (
    <AlertDialog
      open={open}
      onOpenChange={(nextOpen) => {
        if (!nextOpen) onConfirm();
      }}
    >
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Utracono połączenie</AlertDialogTitle>
          <AlertDialogDescription>
            Analiza może nadal trwać. Sprawdź jej wynik w szczegółach sprawy.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <Button size="xl" onClick={onConfirm}>
            Rozumiem
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
