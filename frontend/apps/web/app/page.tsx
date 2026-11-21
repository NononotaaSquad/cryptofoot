import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@workspace/ui/components/card";

import { BackendStatusView } from "@/features/backend-status/backend-status-view";
import { env } from "@/lib/env";

export default function Page() {
  return (
    <main className="mx-auto flex min-h-svh max-w-2xl flex-col justify-center gap-6 p-6">
      <header className="flex flex-col gap-2">
        <h1 className="text-3xl font-semibold tracking-tight">
          Hackathon Template
        </h1>
        <p className="text-muted-foreground">
          Szablon działa. Możesz zacząć budować pierwszą funkcję.
        </p>
      </header>

      <BackendStatusView />

      <Card>
        <CardHeader>
          <CardTitle>Adres backendu</CardTitle>
          <CardDescription>
            Wartość NEXT_PUBLIC_API_URL wbudowana podczas kompilacji.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <code className="font-mono text-sm">{env.NEXT_PUBLIC_API_URL}</code>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Od czego zacząć</CardTitle>
        </CardHeader>
        <CardContent>
          <ul className="flex list-disc flex-col gap-2 pl-5">
            <li>
              Nową funkcję dodaj w folderze{" "}
              <code className="font-mono">
                apps/web/features/&lt;nazwa&gt;/
              </code>
              .
            </li>
            <li>
              Sprawdź zmiany poleceniem{" "}
              <code className="font-mono">npm run check</code> w katalogu{" "}
              <code className="font-mono">frontend/</code>.
            </li>
          </ul>
        </CardContent>
      </Card>
    </main>
  );
}
