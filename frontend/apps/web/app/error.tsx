"use client";

import {
  Alert,
  AlertDescription,
  AlertTitle,
} from "@workspace/ui/components/alert";
import { Button } from "@workspace/ui/components/button";
import { useEffect } from "react";

import { isApiError, toUserMessage } from "@/lib/api/client";

// Server Component errors reach this boundary serialized (and with a generic
// message in production), so only errors thrown on the client keep their
// ApiError identity. Any other error here may be a render bug, not a network
// failure, so it gets the generic text instead of toUserMessage's
// "no connection" message.
export default function ErrorPage({
  error,
  retry,
}: {
  error: unknown;
  retry: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  const message = isApiError(error)
    ? toUserMessage(error)
    : "Wystąpił nieoczekiwany błąd. Spróbuj ponownie za chwilę.";

  return (
    <main className="mx-auto flex min-h-svh max-w-xl flex-col justify-center gap-4 p-6">
      <Alert variant="destructive">
        <AlertTitle>Coś poszło nie tak</AlertTitle>
        <AlertDescription>{message}</AlertDescription>
      </Alert>
      <Button
        className="self-start"
        onClick={() => {
          retry();
        }}
      >
        Spróbuj ponownie
      </Button>
    </main>
  );
}
