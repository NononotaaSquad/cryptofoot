import { Badge } from "@workspace/ui/components/badge";
import { cn } from "@workspace/ui/lib/utils";
import {
  CircleCheck,
  CircleDashed,
  CircleX,
  LoaderCircle,
  TriangleAlert,
  type LucideIcon,
} from "lucide-react";

import type { AnalysisResponseStatus } from "@/lib/api/generated/model";

// Icon + word per status, so the status is never carried by color alone.
const statusIcons: Record<
  AnalysisResponseStatus | "NONE",
  { icon: LucideIcon; className: string }
> = {
  RUNNING: { icon: LoaderCircle, className: "bg-muted text-foreground" },
  COMPLETED: {
    icon: CircleCheck,
    className: "bg-success-tint text-success-text",
  },
  PARTIAL: {
    icon: TriangleAlert,
    className: "bg-warning-tint text-warning-text",
  },
  FAILED: { icon: CircleX, className: "bg-destructive/10 text-destructive" },
  NONE: { icon: CircleDashed, className: "bg-muted text-muted-foreground" },
};

interface AnalysisStatusBadgeProps {
  // "NONE" is a Case that has no Analysis yet (Case list).
  status: AnalysisResponseStatus | "NONE";
  label: string;
}

export function AnalysisStatusBadge({
  status,
  label,
}: AnalysisStatusBadgeProps) {
  const { icon: StatusIcon, className } = statusIcons[status];
  return (
    <Badge className={cn("h-6 px-2.5 text-sm", className)}>
      <StatusIcon data-icon="inline-start" aria-hidden="true" />
      {label}
    </Badge>
  );
}
