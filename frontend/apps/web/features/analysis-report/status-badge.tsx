import { Badge } from "@workspace/ui/components/badge";
import { cn } from "@workspace/ui/lib/utils";
import {
  CircleAlert,
  CircleCheck,
  CircleDashed,
  CircleX,
  Loader,
  type LucideIcon,
} from "lucide-react";

import type { AnalysisResponseStatus } from "@/lib/api/generated/model";

// Icon + word per status, so the status is never carried by color alone.
const statusIcons: Record<
  AnalysisResponseStatus | "NONE",
  { icon: LucideIcon; className: string }
> = {
  RUNNING: { icon: Loader, className: "bg-muted text-foreground" },
  COMPLETED: {
    icon: CircleCheck,
    className: "bg-success-tint text-success-text",
  },
  PARTIAL: {
    icon: CircleAlert,
    className: "bg-warning-tint text-warning-text",
  },
  FAILED: { icon: CircleX, className: "bg-error-tint text-error-dark" },
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
    // Design: 4px radius, 16px icon, 600 weight, padding 4/10/4/8.
    <Badge
      className={cn(
        "h-auto gap-1.5 py-1 pr-2.5 pl-2 text-sm font-semibold has-data-[icon=inline-start]:pl-2 [&>svg]:size-4!",
        className
      )}
    >
      <StatusIcon data-icon="inline-start" aria-hidden="true" />
      {label}
    </Badge>
  );
}
