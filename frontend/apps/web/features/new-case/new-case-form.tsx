"use client";

import { Alert, AlertDescription } from "@workspace/ui/components/alert";
import { Button } from "@workspace/ui/components/button";
import { type ReactNode, useState } from "react";
import { useForm } from "react-hook-form";

import { AnalysisLoader } from "@/lib/analysis-run/analysis-loader";
import { PollingErrorDialog } from "@/lib/analysis-run/polling-error-dialog";
import { StartAnalysisErrorDialog } from "@/lib/analysis-run/start-analysis-error-dialog";
import { useAnalysisRun } from "@/lib/analysis-run/use-analysis-run";
import { isApiError } from "@/lib/api/client";

import { IdentifierField } from "./identifier-field";
import {
  type IdentifierFieldDefinition,
  type NewCaseFieldName,
  type NewCaseFormValues,
  emptyNewCaseFormValues,
  fullNameField,
  narrowingIdentifierFields,
} from "./identifier-fields";
import { type ValidationProblem, ValidationDialog } from "./new-case-dialogs";
import { decideMinimumData } from "./new-case-rules";

const formFieldDefinitions: readonly IdentifierFieldDefinition[] = [
  ...narrowingIdentifierFields,
  fullNameField,
];

// Matches a backend field path to a form field name, without a cast.
function toFormFieldName(field: string): NewCaseFieldName | undefined {
  return formFieldDefinitions.find((definition) => definition.name === field)
    ?.name;
}

interface FieldGroupProps {
  id: string;
  heading: string;
  description: string;
  children: ReactNode;
}

function FieldGroup({ id, heading, description, children }: FieldGroupProps) {
  const descriptionId = `${id}-description`;

  return (
    // Block layout with margins: a legend must be the first child of its
    // fieldset, and browsers lay out a legend inside a flex fieldset unevenly.
    <fieldset aria-describedby={descriptionId}>
      <legend className="w-full">
        <h2 className="text-2xl leading-tight font-bold">{heading}</h2>
      </legend>
      <p id={descriptionId} className="mt-2 leading-normal">
        {description}
      </p>
      <div className="mt-6 grid items-start gap-x-5 gap-y-6 sm:grid-cols-2">
        {children}
      </div>
    </fieldset>
  );
}

export function NewCaseForm() {
  const { register, handleSubmit, reset, setError, clearErrors, formState } =
    useForm<NewCaseFormValues>({ defaultValues: emptyNewCaseFormValues });
  const [dialogOpen, setDialogOpen] = useState(false);
  const [problem, setProblem] = useState<ValidationProblem>("empty");
  const [formErrors, setFormErrors] = useState<string[]>([]);

  // A failed start keeps the typed values; a 400 puts its field errors next
  // to their fields and the rest above the form.
  const run = useAnalysisRun({
    onStartError: (error) => {
      const unmatched: string[] = [];
      if (isApiError(error)) {
        for (const fieldError of error.fieldErrors) {
          const name =
            fieldError.field === null
              ? undefined
              : toFormFieldName(fieldError.field);
          if (name === undefined) {
            unmatched.push(fieldError.message);
          } else {
            setError(name, { type: "server", message: fieldError.message });
          }
        }
      }
      setFormErrors(unmatched);
    },
  });

  const clearServerErrors = () => {
    clearErrors();
    setFormErrors([]);
  };

  const onSubmit = (values: NewCaseFormValues) => {
    const decision = decideMinimumData(values);
    if (decision.result !== "ok") {
      setProblem(decision.result);
      setDialogOpen(true);
      return;
    }
    clearServerErrors();
    run.startNewCase(decision.identifiers);
  };

  const renderField = (definition: IdentifierFieldDefinition) => (
    <IdentifierField
      key={definition.name}
      definition={definition}
      registration={register(definition.name)}
      error={formState.errors[definition.name]?.message}
    />
  );

  return (
    <>
      {/* One grid cell of the view: the error block stays above the form,
          not in a column of its own. */}
      <div className="flex flex-col gap-6">
        {formErrors.length > 0 ? (
          <Alert variant="destructive" className="px-4 py-3 text-base">
            <AlertDescription className="text-base">
              <ul className="flex flex-col gap-1">
                {formErrors.map((message, index) => (
                  // Messages may repeat; the list is rebuilt, never reordered.
                  <li key={`${String(index)}:${message}`}>{message}</li>
                ))}
              </ul>
            </AlertDescription>
          </Alert>
        ) : null}

        <form
          noValidate
          onSubmit={(event) => void handleSubmit(onSubmit)(event)}
          className="flex flex-col gap-10"
        >
          <FieldGroup
            id="new-case-identifiers"
            heading="Identyfikatory"
            description="Podaj co najmniej jeden."
          >
            {narrowingIdentifierFields.map(renderField)}
          </FieldGroup>

          <hr className="border-border" />

          <FieldGroup
            id="new-case-additional-data"
            heading="Dane dodatkowe"
            description="Te dane pomagają zawęzić wyniki, ale nie musisz ich podawać."
          >
            {renderField(fullNameField)}
          </FieldGroup>

          <div className="flex flex-wrap items-center gap-6">
            <Button type="submit" size="xl">
              Rozpocznij analizę
            </Button>
            <Button
              type="button"
              variant="link"
              className="h-auto px-0 text-base font-semibold"
              onClick={() => {
                reset();
                setFormErrors([]);
              }}
            >
              Wyczyść
            </Button>
          </div>
        </form>
      </div>

      <ValidationDialog
        open={dialogOpen}
        problem={problem}
        onClose={() => {
          setDialogOpen(false);
        }}
      />

      <StartAnalysisErrorDialog
        open={run.startErrorOpen}
        error={run.startError}
        onClose={run.closeStartError}
        onRetry={() => {
          clearServerErrors();
          run.retryStart();
        }}
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
