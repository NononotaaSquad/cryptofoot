import { env } from "@/lib/env";

import type { ApiError } from "./api-error";
import { toApiError } from "./api-error";

export type { ApiFieldError } from "./api-error";
export { ApiError, isApiError } from "./api-error";
export { toUserMessage } from "./error-message";

// Orval picks this up and types every hook's error as `ApiError` instead of
// the raw backend body, because customFetch throws `ApiError` on non-2xx.
export type ErrorType<TBody> = TBody extends unknown ? ApiError : never;

async function parseBody(response: Response): Promise<unknown> {
  if (response.status === 204) return undefined;

  const text = await response.text();
  if (text === "") return undefined;

  const contentType = response.headers.get("content-type") ?? "";
  if (!/[/+]json\b/.test(contentType)) return text;

  try {
    return JSON.parse(text) as unknown;
  } catch {
    // Truncated or mislabelled JSON (e.g. a gateway error page): keep the raw
    // text so a non-2xx status still becomes an ApiError.
    return text;
  }
}

/**
 * Orval custom fetch mutator: the only hand-written `fetch` in the app.
 *
 * Non-2xx responses throw an `ApiError`. Network failures (backend down, CORS)
 * reject with the native error; tell them apart with `isApiError(error)`.
 */
export const customFetch = async <T>(
  url: string,
  options: RequestInit
): Promise<T> => {
  const response = await fetch(`${env.NEXT_PUBLIC_API_URL}${url}`, options);
  const body = await parseBody(response);

  if (!response.ok) {
    throw toApiError(response.status, body);
  }

  const result = {
    data: body,
    status: response.status,
    headers: response.headers,
  };

  // Generic boundary: Orval's generated code declares T as the
  // `{ data, status, headers }` envelope for this endpoint; the shape of
  // `data` is the backend's contract and is not checked at runtime.
  return result as T;
};
