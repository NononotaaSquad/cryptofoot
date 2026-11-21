import { describe, expect, it } from "vitest";

import { toApiError } from "./api-error";
import { toUserMessage } from "./error-message";

function backendBody(code: number, status: string, message: string) {
  return { code, status, message };
}

describe("toUserMessage", () => {
  it.each([
    ["a fetch TypeError", new TypeError("Failed to fetch")],
    ["an unknown value", "boom"],
    ["undefined", undefined],
  ])("reports no connection for %s", (_label, error) => {
    expect(toUserMessage(error)).toBe(
      "Nie udało się połączyć z serwerem. Sprawdź połączenie i spróbuj ponownie."
    );
  });

  it("shows the backend message for a 4xx with a valid body", () => {
    const error = toApiError(
      404,
      backendBody(404, "404 NOT_FOUND", "Nie znaleziono zasobu.")
    );

    expect(toUserMessage(error)).toBe("Nie znaleziono zasobu. (błąd 404)");
  });

  it("ignores the backend message for a 5xx", () => {
    const error = toApiError(
      500,
      backendBody(500, "500 INTERNAL_SERVER_ERROR", "Internal Server Error")
    );

    expect(toUserMessage(error)).toBe(
      "Serwer napotkał błąd. Spróbuj ponownie za chwilę. (błąd 500)"
    );
  });

  it.each([
    [502, "<html>Bad Gateway</html>"],
    [404, undefined],
    [400, { error: "other shape" }],
  ])(
    "reports an unexpected response for %i without a valid body",
    (httpStatus, body) => {
      expect(toUserMessage(toApiError(httpStatus, body))).toBe(
        `Nieoczekiwana odpowiedź serwera. (błąd ${String(httpStatus)})`
      );
    }
  );
});
