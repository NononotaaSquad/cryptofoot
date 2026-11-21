import { Button } from "@workspace/ui/components/button";
import Link from "next/link";

export default function NotFound() {
  return (
    <main className="mx-auto flex min-h-svh max-w-xl flex-col justify-center gap-4 p-6">
      <h1 className="text-2xl font-semibold">Nie znaleziono strony</h1>
      <p className="text-muted-foreground">
        Strona, której szukasz, nie istnieje lub została przeniesiona.
      </p>
      <Button
        className="self-start"
        nativeButton={false}
        render={<Link href="/" />}
      >
        Wróć na stronę główną
      </Button>
    </main>
  );
}
