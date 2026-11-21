"use client";

import { useMutation } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState, useTransition } from "react";

import { useGetAnalysis } from "@/lib/api/generated/analysis-controller/analysis-controller";
import type { CreateCaseRequest } from "@/lib/api/generated/model";

import { nextPollInterval } from "./poll-interval";
import {
  type NewCaseRunInput,
  type StartedAnalysis,
  runCaseAnalysis,
  runNewCaseAnalysis,
  toNewCaseRunInput,
} from "./run-analysis";

type RunInput = NewCaseRunInput | { kind: "case"; caseId: string };

interface FailedStart {
  error: unknown;
  // Keep the Case ID and identifiers together after creation so both the
  // dialog and an unchanged form resubmission can retry only the start.
  retryInput: RunInput;
}

interface UseAnalysisRunOptions {
  // Called once per failed start, before the dialog opens; New case maps 400
  // field errors to its fields here.
  onStartError?: (error: unknown) => void;
}

export interface AnalysisRun {
  /** Creates the Case from the identifiers, then starts its Analysis. */
  startNewCase: (identifiers: CreateCaseRequest) => void;
  /** Starts an Analysis of an existing Case. */
  start: (caseId: string) => void;
  /** The loader is shown: starting, polling, or navigating to the report. */
  isRunning: boolean;
  /** The tab-close warning: only while the Analysis may still be running. */
  warnBeforeUnload: boolean;
  /** The failed-start dialog. `startError` stays after closing, so the text
   *  does not change while the closing animation runs. */
  startErrorOpen: boolean;
  startError: unknown;
  retryStart: () => void;
  closeStartError: () => void;
  /** The polling-error dialog; `confirmPollingFailure` opens Case details. */
  pollingFailed: boolean;
  confirmPollingFailure: () => void;
}

/**
 * Starts an Analysis, polls it until it ends, then opens Case details with
 * it selected. Renders nothing: the caller renders `AnalysisLoader`,
 * `StartAnalysisErrorDialog` and `PollingErrorDialog` from this state.
 */
export function useAnalysisRun({
  onStartError,
}: UseAnalysisRunOptions = {}): AnalysisRun {
  const router = useRouter();
  const [, startNavigation] = useTransition();
  const [started, setStarted] = useState<StartedAnalysis | null>(null);
  const [failedStart, setFailedStart] = useState<FailedStart | null>(null);
  const [startErrorOpen, setStartErrorOpen] = useState(false);
  // Written by `runNewCaseAnalysis` as soon as the Case exists; read when
  // the start fails right after.
  const createdCaseId = useRef<string | null>(null);
  // Guards against a second push for the same Analysis (Strict Mode runs
  // effects twice in development).
  const navigatedFor = useRef<string | null>(null);

  const run = useMutation({
    mutationFn: (input: RunInput) => {
      createdCaseId.current = null;
      if (input.kind === "case") return runCaseAnalysis(input.caseId);
      if (input.caseId !== undefined) return runCaseAnalysis(input.caseId);
      return runNewCaseAnalysis(input.identifiers, (caseId) => {
        createdCaseId.current = caseId;
      });
    },
    onSuccess: (result) => {
      setStarted(result);
    },
    onError: (error, input) => {
      const caseId = createdCaseId.current;
      setFailedStart({
        error,
        retryInput: caseId === null ? input : { ...input, caseId },
      });
      onStartError?.(error);
      setStartErrorOpen(true);
    },
  });

  const analysis = useGetAnalysis(started?.analysisId ?? "", {
    query: {
      enabled: started !== null,
      // The rule of S3, and no polling after an error: the dialog takes over.
      refetchInterval: (query) =>
        query.state.status === "error"
          ? false
          : nextPollInterval(query.state.data?.data.status),
      // The Analyst may switch tabs while waiting; the loader must still see
      // the end.
      refetchIntervalInBackground: true,
      refetchOnWindowFocus: false,
      // Retry stays the app default (3 tries for network errors and 5xx,
      // none for 4xx): a short blip does not end the wait.
    },
  });

  const status = analysis.data?.data.status;
  const ended = status !== undefined && nextPollInterval(status) === false;
  // Deliberate exception to the error rule of frontend.md (Q29): any polling
  // error, network or `ApiError`, gets the "Utracono połączenie" dialog, not
  // `toUserMessage`. Polling only observes; the Analysis may still be running
  // on the server, so Case details is the right place to look next.
  const pollingFailed = started !== null && analysis.isError;

  const openCaseDetails = (target: StartedAnalysis) => {
    if (navigatedFor.current === target.analysisId) return;
    navigatedFor.current = target.analysisId;
    // One transition for the push and the reset: the loader stays until the
    // new page is shown, also when Case details starts a run of its own
    // Case and stays mounted.
    startNavigation(() => {
      router.push(`/cases/${target.caseId}?analysis=${target.analysisId}`);
      setStarted(null);
      run.reset();
    });
  };

  useEffect(() => {
    if (started !== null && ended) openCaseDetails(started);
  });

  const isRunning = run.isPending || (started !== null && !pollingFailed);

  const mutate = (input: RunInput) => {
    // No second Analysis while one runs or while navigating to its Case.
    if (isRunning) return;
    navigatedFor.current = null;
    run.mutate(input);
  };

  return {
    startNewCase: (identifiers) => {
      const previous = failedStart?.retryInput;
      mutate(
        toNewCaseRunInput(
          identifiers,
          previous?.kind === "newCase" ? previous : undefined
        )
      );
    },
    start: (caseId) => {
      mutate({ kind: "case", caseId });
    },
    isRunning,
    warnBeforeUnload:
      run.isPending || (started !== null && !ended && !pollingFailed),
    startErrorOpen,
    startError: failedStart?.error,
    retryStart: () => {
      setStartErrorOpen(false);
      if (failedStart !== null) mutate(failedStart.retryInput);
    },
    closeStartError: () => {
      setStartErrorOpen(false);
    },
    pollingFailed,
    confirmPollingFailure: () => {
      if (started !== null) openCaseDetails(started);
    },
  };
}
