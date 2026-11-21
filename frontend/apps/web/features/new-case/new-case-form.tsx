"use client";

import { Button } from "@workspace/ui/components/button";
import { type ReactNode, useState } from "react";
import { useForm } from "react-hook-form";

import { IdentifierField } from "./identifier-field";
import {
  type IdentifierFieldDefinition,
  type NewCaseFormValues,
  emptyNewCaseFormValues,
  fullNameField,
  narrowingIdentifierFields,
} from "./identifier-fields";
import { type ValidationProblem, ValidationDialog } from "./new-case-dialogs";
import { decideMinimumData } from "./new-case-rules";

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
  const { register, handleSubmit, reset, formState } =
    useForm<NewCaseFormValues>({ defaultValues: emptyNewCaseFormValues });
  const [dialogOpen, setDialogOpen] = useState(false);
  const [problem, setProblem] = useState<ValidationProblem>("empty");

  const onSubmit = (values: NewCaseFormValues) => {
    const decision = decideMinimumData(values);
    if (decision.result !== "ok") {
      setProblem(decision.result);
      setDialogOpen(true);
      return;
    }
    // Task 03 starts the Analysis with decision.identifiers.
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
      <form
        noValidate
        onSubmit={(event) => void handleSubmit(onSubmit)(event)}
        className="flex flex-col gap-10"
      >
        <FieldGroup
          id="new-case-identifiers"
          heading="Identyfikatory"
          description="Podaj co najmniej jeden. W każdym polu możesz podać kilka, oddzielając je przecinkami."
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
            }}
          >
            Wyczyść
          </Button>
        </div>
      </form>

      <ValidationDialog
        open={dialogOpen}
        problem={problem}
        onClose={() => {
          setDialogOpen(false);
        }}
      />
    </>
  );
}
