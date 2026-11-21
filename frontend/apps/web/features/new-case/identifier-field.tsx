import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
} from "@workspace/ui/components/field";
import { Input } from "@workspace/ui/components/input";
import type { UseFormRegisterReturn } from "react-hook-form";

import type { IdentifierFieldDefinition } from "./identifier-fields";

interface IdentifierFieldProps {
  definition: IdentifierFieldDefinition;
  registration: UseFormRegisterReturn;
  error?: string;
}

// Label, input, hint and optional error of one field. The hint and the error
// are linked to the input through aria-describedby.
export function IdentifierField({
  definition,
  registration,
  error,
}: IdentifierFieldProps) {
  const inputId = `new-case-${definition.name}`;
  const hintId = `${inputId}-hint`;
  const errorId = `${inputId}-error`;
  const invalid = error !== undefined;

  return (
    <Field data-invalid={invalid || undefined}>
      <FieldLabel
        htmlFor={inputId}
        className="text-base leading-normal font-semibold"
      >
        {definition.label}
        {definition.narrowing ? null : (
          <span className="font-normal text-muted-foreground">
            (opcjonalnie)
          </span>
        )}
      </FieldLabel>
      <Input
        id={inputId}
        type="text"
        placeholder={definition.example}
        aria-describedby={invalid ? `${hintId} ${errorId}` : hintId}
        aria-invalid={invalid || undefined}
        autoComplete="off"
        spellCheck={false}
        {...registration}
      />
      <FieldDescription id={hintId} className="leading-normal">
        {definition.hint}
      </FieldDescription>
      {invalid ? <FieldError id={errorId}>{error}</FieldError> : null}
    </Field>
  );
}
