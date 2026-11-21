import { isApiError } from "./api-error";

/**
 * The one place that turns a failed backend call into a Polish sentence for
 * the user. Every feature and `app/error.tsx` show errors through this.
 *
 * | Case                              | Message                                                                      |
 * | --------------------------------- | ---------------------------------------------------------------------------- |
 * | not an `ApiError` (no response)   | Nie udało się połączyć z serwerem. Sprawdź połączenie i spróbuj ponownie.    |
 * | 4xx with a valid backend body     | backend `message` + " (błąd 404)" - the backend writes it in Polish          |
 * | 5xx                               | Serwer napotkał błąd. Spróbuj ponownie za chwilę. (błąd 500)                 |
 * | `ApiError` without a valid body   | Nieoczekiwana odpowiedź serwera. (błąd 502)                                  |
 *
 * `statusName === undefined` means the body did not match the backend schema.
 * `restrict-template-expressions` forbids numbers in template strings: use
 * `String(error.httpStatus)`.
 */
export function toUserMessage(error: unknown): string {
  if (!isApiError(error)) {
    return "Nie udało się połączyć z serwerem. Sprawdź połączenie i spróbuj ponownie.";
  }

  if (error.statusName === undefined) {
    return `Nieoczekiwana odpowiedź serwera. (błąd ${String(error.httpStatus)})`;
  }

  if (error.httpStatus >= 400 && error.httpStatus < 500) {
    return error.message + ` (błąd ${String(error.httpStatus)})`;
  }

  return `Serwer napotkał błąd. Spróbuj ponownie za chwilę. (błąd ${String(error.httpStatus)})`;
}
