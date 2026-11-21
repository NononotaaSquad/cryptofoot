"use client";

import { Button } from "@workspace/ui/components/button";
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@workspace/ui/components/card";
import { Skeleton } from "@workspace/ui/components/skeleton";
import { RefreshCw } from "lucide-react";

import { toUserMessage } from "@/lib/api/client";
import { useHello } from "@/lib/api/generated/test-controller/test-controller";

// Reference feature: the smallest end-to-end path through the generated
// client (spec -> Orval hook -> customFetch -> ApiError). Copy its shape.
export function BackendStatusView() {
  const { data, isPending, isError, error, refetch, isFetching } = useHello();

  return (
    <Card>
      <CardHeader>
        <CardTitle>Status backendu</CardTitle>
        <CardDescription>Odpowiedź z GET /api/1/test</CardDescription>
        <CardAction>
          <Button
            variant="outline"
            size="sm"
            onClick={() => void refetch()}
            disabled={isFetching}
          >
            <RefreshCw data-icon="inline-start" aria-hidden="true" />
            Odśwież
          </Button>
        </CardAction>
      </CardHeader>
      <CardContent aria-live="polite">
        {isPending ? (
          <Skeleton className="h-5 w-32" />
        ) : isError ? (
          <p className="text-sm text-destructive">{toUserMessage(error)}</p>
        ) : !data.data.message ? (
          <p className="text-sm text-muted-foreground">
            Backend odpowiedział, ale bez treści.
          </p>
        ) : (
          <code className="font-mono text-sm">{data.data.message}</code>
        )}
      </CardContent>
    </Card>
  );
}
