"use client";

import { Button } from "@workspace/ui/components/button";
import { Input } from "@workspace/ui/components/input";
import { Label } from "@workspace/ui/components/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@workspace/ui/components/select";
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
  intervalUnitLabels,
  intervalUnits,
  type IntervalUnit,
} from "./schedule-labels";
import {
  checkCustomInterval,
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
const intervalValueId = "schedule-interval-value";
const intervalUnitId = "schedule-interval-unit";
const intervalErrorId = "schedule-interval-error";

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
  // null: the saved interval; edits are forgotten on close.
  const [customText, setCustomText] = useState<string | null>(null);
  const [customUnit, setCustomUnit] = useState<IntervalUnit | null>(null);
  const [customError, setCustomError] = useState<string | null>(null);

  const inForce =
    view.options.find((option) => option.selected)?.value ?? "OFF";
  const current = choice ?? inForce;
  const readFailed = readError !== null && readError !== undefined;
  const text = customText ?? view.customInterval.value;
  const unit = customUnit ?? view.customInterval.unit;

  const handleOpenChange = (next: boolean) => {
    setOpen(next);
    if (!next) {
      setChoice(null);
      setCustomText(null);
      setCustomUnit(null);
      setCustomError(null);
      update.reset();
    }
  };

  const save = () => {
    if (update.isPending) return;
    let custom:
      { intervalValue: number; intervalUnit: IntervalUnit } | undefined;
    if (current === "CUSTOM_INTERVAL") {
      const checked = checkCustomInterval(text, unit);
      if (!checked.ok) {
        setCustomError(checked.message);
        return;
      }
      custom = {
        intervalValue: checked.intervalValue,
        intervalUnit: checked.intervalUnit,
      };
    }
    setCustomError(null);
    // Only the answer of the server replaces the cached Schedule: a failed
    // save never shows the setting as saved.
    update.mutate(
      { caseId, data: toUpdateScheduleRequest(current, custom) },
      {
        onSuccess: (response) => {
          queryClient.setQueryData(getGetScheduleQueryKey(caseId), response);
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
                  if (isScheduleOptionValue(value)) {
                    setChoice(value);
                    update.reset();
                  }
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

              {current === "CUSTOM_INTERVAL" ? (
                <div className="flex flex-col gap-2 rounded-lg bg-muted p-3">
                  <div className="flex items-end gap-3">
                    <div className="flex flex-col gap-1">
                      <Label htmlFor={intervalValueId}>Co ile</Label>
                      <Input
                        id={intervalValueId}
                        type="text"
                        inputMode="numeric"
                        autoComplete="off"
                        className="w-28"
                        value={text}
                        aria-invalid={customError !== null}
                        aria-describedby={
                          customError === null ? undefined : intervalErrorId
                        }
                        onChange={(event) => {
                          setCustomText(event.target.value);
                          setCustomError(null);
                          update.reset();
                        }}
                      />
                    </div>
                    <div className="flex flex-col gap-1">
                      <Label htmlFor={intervalUnitId}>Jednostka</Label>
                      <Select
                        items={intervalUnits.map((value) => ({
                          value,
                          label: intervalUnitLabels[value],
                        }))}
                        value={unit}
                        onValueChange={(value) => {
                          if (value !== null) {
                            setCustomUnit(value);
                            setCustomError(null);
                            update.reset();
                          }
                        }}
                      >
                        <SelectTrigger id={intervalUnitId} className="w-32">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          {intervalUnits.map((value) => (
                            <SelectItem key={value} value={value}>
                              {intervalUnitLabels[value]}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                  </div>
                  {customError === null ? null : (
                    <p
                      id={intervalErrorId}
                      role="alert"
                      className="text-sm text-destructive"
                    >
                      {customError}
                    </p>
                  )}
                </div>
              ) : null}

              <p className="text-sm text-muted-foreground">
                Interwał to stały odstęp między analizami, liczony od zapisania
                ustawienia, np. „co 1 minutę”. To nie jest jednorazowe
                uruchomienie o wskazanej godzinie. Opcje z godziną (np. „o
                06:00”) uruchamiają analizę w tych godzinach czasu polskiego.
              </p>
              <p className="text-sm text-muted-foreground">
                Częste analizy zwiększają liczbę zapytań do źródeł.
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
                Nie zapisano harmonogramu. {toUserMessage(update.error)}
              </p>
            ) : null}
            {update.isSuccess ? (
              <p role="status" className="font-semibold">
                Zapisano harmonogram.
              </p>
            ) : null}

            <div className="flex justify-end gap-3">
              <Button
                variant="outline"
                onClick={() => {
                  handleOpenChange(false);
                }}
              >
                {update.isSuccess ? "Zamknij" : "Anuluj"}
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
