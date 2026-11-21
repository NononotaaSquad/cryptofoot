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

interface StartAnalysisErrorDialogProps {
  open: boolean;
  // Kept by the caller after closing, like `problem` above.
  error: unknown;
  onClose: () => void;
  onRetry: () => void;
}

// Failed start of an Analysis. The text comes only from toUserMessage, the
// one wording for backend errors in the app (frontend.md).
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

interface PollingErrorDialogProps {
  open: boolean;
  error: unknown;
  // Goes to the Case details; the dialog stays open until the page changes.
  onConfirm: () => void;
}

// Checking the status of a started Analysis failed. The Analysis exists, so
// the only way on is its Case: one button, no retry.
export function PollingErrorDialog({
  open,
  error,
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
          <AlertDialogTitle>Coś poszło nie tak</AlertDialogTitle>
          <AlertDialogDescription>
            {toUserMessage(error)} Analiza została rozpoczęta. Jej wynik
            zobaczysz w szczegółach sprawy.
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
