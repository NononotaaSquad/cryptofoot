import { Skeleton } from "@workspace/ui/components/skeleton";

export default function Loading() {
  return (
    <main
      className="mx-auto flex min-h-svh max-w-2xl flex-col justify-center gap-4 p-6"
      role="status"
      aria-busy="true"
    >
      <span className="sr-only">Ładowanie…</span>
      <Skeleton className="h-9 w-2/3" />
      <Skeleton className="h-5 w-full" />
      <Skeleton className="h-32 w-full" />
    </main>
  );
}
