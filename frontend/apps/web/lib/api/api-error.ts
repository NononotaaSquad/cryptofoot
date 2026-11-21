import { z } from "zod";

/** One invalid input: `field` is a path like "address.city" or "items[0]", null for the whole request. */
export const apiFieldErrorSchema = z.object({
  field: z.string().nullable(),
  message: z.string(),
});

export type ApiFieldError = z.infer<typeof apiFieldErrorSchema>;

/** Error body returned by the backend `RestExceptionHandler`. */
export const apiErrorBodySchema = z.object({
  code: z.number().int(),
  // Status code and name, e.g. "404 NOT_FOUND", "422 UNPROCESSABLE_CONTENT".
  status: z.string(),
  message: z.string(),
  // Filled for 400 validation failures; Polish messages, one per invalid field.
  errors: z.array(apiFieldErrorSchema).default([]),
});

export type ApiErrorBody = z.infer<typeof apiErrorBodySchema>;

interface ApiErrorInit {
  httpStatus: number;
  message: string;
  statusName?: string | undefined;
  fieldErrors?: ApiFieldError[] | undefined;
}

/** A non-2xx response from the backend. */
export class ApiError extends Error {
  override readonly name = "ApiError";
  readonly httpStatus: number;
  /** Backend `status` such as "404 NOT_FOUND"; undefined if the body had none. */
  readonly statusName: string | undefined;
  /** Invalid fields of a 400 validation failure; empty otherwise. */
  readonly fieldErrors: ApiFieldError[];

  constructor({ httpStatus, message, statusName, fieldErrors }: ApiErrorInit) {
    super(message);
    this.httpStatus = httpStatus;
    this.statusName = statusName;
    this.fieldErrors = fieldErrors ?? [];
  }
}

export function isApiError(value: unknown): value is ApiError {
  return value instanceof ApiError;
}

/**
 * Maps a failed response to an `ApiError`. `body` is whatever the client
 * parsed: an object matching `apiErrorBodySchema` (the normal backend error),
 * a JSON value of another shape (e.g. a proxy or gateway error), a plain-text
 * string, or `undefined` for an empty body. `httpStatus` is the response
 * status and is always the source of truth for `ApiError.httpStatus`.
 */
export function toApiError(httpStatus: number, body: unknown): ApiError {
  const parseApiErrorData = apiErrorBodySchema.safeParse(body);

  if (parseApiErrorData.success) {
    return new ApiError({
      httpStatus,
      message: parseApiErrorData.data.message,
      statusName: parseApiErrorData.data.status,
      fieldErrors: parseApiErrorData.data.errors,
    });
  }

  return new ApiError({
    httpStatus,
    // Developer-facing (logs, console); the user text comes from toUserMessage.
    message: `Unexpected response body (HTTP ${String(httpStatus)})`,
  });
}
