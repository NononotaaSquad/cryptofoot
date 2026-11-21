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

export type ValidationProblem = "empty" | "fullNameOnly";

const copy: Record<ValidationProblem, { title: string; text: string }> = {
  empty: {
    title: "Podaj co najmniej jeden identyfikator",
    text: "Aby rozpocząć analizę, wpisz nick, login, adres e-mail, identyfikator z komunikatora, adres profilu lub adres kryptowalutowy.",
  },
  fullNameOnly: {
    title: "Samo imię i nazwisko nie wystarczy",
    text: "Dodaj co najmniej jeden identyfikator, na przykład nick lub adres e-mail.",
  },
};

interface ValidationDialogProps {
  open: boolean;
  // Kept by the caller after closing, so the copy does not change while the
  // closing animation runs.
  problem: ValidationProblem;
  onClose: () => void;
}

// Controlled pop-up. Base UI moves focus inside, traps it, closes on Escape
// and returns focus to the element that had it before opening.
export function ValidationDialog({
  open,
  problem,
  onClose,
}: ValidationDialogProps) {
  return (
    <AlertDialog
      open={open}
      onOpenChange={(nextOpen) => {
        if (!nextOpen) onClose();
      }}
    >
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{copy[problem].title}</AlertDialogTitle>
          <AlertDialogDescription>{copy[problem].text}</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <Button size="xl" onClick={onClose}>
            Rozumiem
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
