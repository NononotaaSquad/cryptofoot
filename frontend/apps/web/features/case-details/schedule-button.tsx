"use client";

import { Button } from "@workspace/ui/components/button";
import {
  Popover,
  PopoverContent,
  PopoverDescription,
  PopoverHeader,
  PopoverTitle,
  PopoverTrigger,
} from "@workspace/ui/components/popover";
import {
  RadioGroup,
  RadioGroupItem,
} from "@workspace/ui/components/radio-group";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@workspace/ui/components/tooltip";
import { useQueryClient } from "@tanstack/react-query";
import { CheckIcon, ClockIcon } from "lucide-react";
import Link from "next/link";
import { useState } from "react";

import { toUserMessage } from "@/lib/api/client";
import {
  getGetScheduleQueryKey,
  useUpdateSchedule,
} from "@/lib/api/generated/case-controller/case-controller";

import {
  isScheduleOptionValue,
  toUpdateScheduleRequest,
  type ScheduleOptionValue,
  type ScheduleView,
} from "./schedule-view-model";

interface ScheduleButtonProps {
  caseId: string;
  view: ScheduleView;
  // The Schedule could not be read (and was never read before).
  readError: unknown;
  onRetryRead: () => void;
}

const groupLabelId = "schedule-frequency-label";

// The clock button next to "Rozpocznij nową analizę" and its popover (Q3, Q14).
export function ScheduleButton({
  caseId,
  view,
  readError,
  onRetryRead,
}: ScheduleButtonProps) {
  const queryClient = useQueryClient();
  const update = useUpdateSchedule();
  const [open, setOpen] = useState(false);
  // null: the option in force; a dropped choice is forgotten on close.
  const [choice, setChoice] = useState<ScheduleOptionValue | null>(null);

  const inForce =
    view.options.find((option) => option.selected)?.value ?? "OFF";
  const current = choice ?? inForce;
  const readFailed = readError !== null && readError !== undefined;

  const handleOpenChange = (next: boolean) => {
    setOpen(next);
    if (!next) {
      setChoice(null);
      update.reset();
    }
  };

  const save = () => {
    if (update.isPending) return;
    update.mutate(
      { caseId, data: toUpdateScheduleRequest(current) },
      {
        onSuccess: (response) => {
          queryClient.setQueryData(getGetScheduleQueryKey(caseId), response);
          handleOpenChange(false);
        },
      }
    );
  };

  return (
    <Popover open={open} onOpenChange={handleOpenChange}>
      <Tooltip>
        <TooltipTrigger
          render={
            <PopoverTrigger
              render={
                <Button
                  variant="outline"
                  size="icon-lg"
                  aria-label="Harmonogram"
                  className={
                    view.enabled
                      ? "relative size-12 border-primary bg-primary/10"
                      : "relative size-12"
                  }
                />
              }
            />
          }
        >
          <ClockIcon aria-hidden="true" className="size-5" />
          {view.enabled ? (
            <span
              aria-hidden="true"
              className="absolute -top-1.5 -right-1.5 flex size-4 items-center justify-center rounded-full bg-primary text-primary-foreground"
            >
              <CheckIcon className="size-3" />
            </span>
          ) : null}
        </TooltipTrigger>
        <TooltipContent>Harmonogram</TooltipContent>
      </Tooltip>

      <PopoverContent align="end" className="w-96 gap-4 p-5">
        <PopoverHeader>
          <PopoverTitle className="text-lg font-bold">Harmonogram</PopoverTitle>
          <PopoverDescription>
            Analiza tej sprawy uruchomi się sama w wybranych terminach.
          </PopoverDescription>
        </PopoverHeader>

        {readFailed ? (
          <div role="alert" className="flex flex-col items-start gap-3">
            <p>{toUserMessage(readError)}</p>
            <Button variant="outline" onClick={onRetryRead}>
              Spróbuj ponownie
            </Button>
          </div>
        ) : (
          <>
            <div className="flex flex-col gap-2">
              <p id={groupLabelId} className="font-semibold">
                Częstotliwość
              </p>
              <RadioGroup
                aria-labelledby={groupLabelId}
                value={current}
                onValueChange={(value) => {
                  if (isScheduleOptionValue(value)) setChoice(value);
                }}
              >
                {view.options.map((option) => (
                  <label
                    key={option.value}
                    className="flex cursor-pointer items-center gap-3 py-1"
                  >
                    <RadioGroupItem value={option.value} />
                    <span>{option.label}</span>
                  </label>
                ))}
              </RadioGroup>
              <p className="text-sm text-muted-foreground">
                Godziny według czasu polskiego.
              </p>
            </div>

            {view.nextLine !== null || view.lastAttemptLine !== null ? (
              <div className="flex flex-col gap-1 text-sm">
                {view.nextLine !== null ? <p>{view.nextLine}</p> : null}
                {view.lastAttemptLine !== null ? (
                  <p>
                    {view.lastAttemptLine.text}
                    {view.lastAttemptLine.analysisId !== null ? (
                      <>
                        {" "}
                        <Link
                          href={`/cases/${caseId}?analysis=${view.lastAttemptLine.analysisId}`}
                          className="rounded-sm font-semibold text-primary underline-offset-4 outline-none hover:underline focus-visible:ring-3 focus-visible:ring-ring/50"
                          onClick={() => {
                            handleOpenChange(false);
                          }}
                        >
                          Pokaż analizę
                        </Link>
                      </>
                    ) : null}
                  </p>
                ) : null}
              </div>
            ) : null}

            {update.isError ? (
              <p role="alert" className="text-destructive">
                {toUserMessage(update.error)}
              </p>
            ) : null}

            <div className="flex justify-end gap-3">
              <Button
                variant="outline"
                onClick={() => {
                  handleOpenChange(false);
                }}
              >
                Anuluj
              </Button>
              <Button onClick={save} disabled={update.isPending}>
                {update.isPending ? "Zapisywanie…" : "Zapisz"}
              </Button>
            </div>
          </>
        )}
      </PopoverContent>
    </Popover>
  );
}
