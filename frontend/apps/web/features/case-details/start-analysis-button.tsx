"use client";

import { Button } from "@workspace/ui/components/button";

import { AnalysisLoader } from "@/lib/analysis-run/analysis-loader";
import { PollingErrorDialog } from "@/lib/analysis-run/polling-error-dialog";
import { StartAnalysisErrorDialog } from "@/lib/analysis-run/start-analysis-error-dialog";
import { useAnalysisRun } from "@/lib/analysis-run/use-analysis-run";

interface StartAnalysisButtonProps {
  caseId: string;
}

// "Rozpocznij nową analizę" with the blocking loader and the two dialogs of
// the run, wired as on New case. Used in the Case header and in the empty
// state; the view renders only one of them at a time.
export function StartAnalysisButton({ caseId }: StartAnalysisButtonProps) {
  const run = useAnalysisRun();

  return (
    <>
      <Button
        size="xl"
        onClick={() => {
          run.start(caseId);
        }}
      >
        Rozpocznij nową analizę
      </Button>

      <StartAnalysisErrorDialog
        open={run.startErrorOpen}
        error={run.startError}
        onClose={run.closeStartError}
        onRetry={run.retryStart}
      />

      <PollingErrorDialog
        open={run.pollingFailed}
        onConfirm={run.confirmPollingFailure}
      />

      <AnalysisLoader
        open={run.isRunning}
        warnBeforeUnload={run.warnBeforeUnload}
      />
    </>
  );
}
