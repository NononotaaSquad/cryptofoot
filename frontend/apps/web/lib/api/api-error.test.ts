import { describe, expect, it } from "vitest";

import { ApiError, isApiError, toApiError } from "./api-error";

describe("ApiError", () => {
  it("carries the HTTP status, backend status name and message", () => {
    const error = new ApiError({
      httpStatus: 409,
      statusName: "CONFLICT",
      message: "Resource already exists",
    });

    expect(error).toBeInstanceOf(Error);
    expect(error.name).toBe("ApiError");
    expect(error.httpStatus).toBe(409);
    expect(error.statusName).toBe("CONFLICT");
    expect(error.message).toBe("Resource already exists");
  });

  it("leaves the status name undefined when it is not given", () => {
    const error = new ApiError({ httpStatus: 502, message: "Bad Gateway" });

    expect(error.statusName).toBeUndefined();
  });
});

describe("isApiError", () => {
  it("accepts an ApiError", () => {
    expect(isApiError(new ApiError({ httpStatus: 500, message: "x" }))).toBe(
      true
    );
  });

  it.each([
    ["a plain Error", new Error("network down")],
    ["a TypeError from fetch", new TypeError("Failed to fetch")],
    ["an error-shaped object", { httpStatus: 404, message: "x" }],
    ["undefined", undefined],
  ])("rejects %s", (_label, value) => {
    expect(isApiError(value)).toBe(false);
  });
});

describe("toApiError", () => {
  it("maps a valid backend error body", () => {
    const error = toApiError(404, {
      code: 404,
      status: "404 NOT_FOUND",
      message: "User not found",
    });

    expect(isApiError(error)).toBe(true);
    expect(error.message).toBe("User not found");
    expect(error.httpStatus).toBe(404);
    expect(error.statusName).toBe("404 NOT_FOUND");
    expect(error.fieldErrors).toEqual([]);
  });

  it("keeps the field errors of a validation failure", () => {
    const error = toApiError(400, {
      code: 400,
      status: "400 BAD_REQUEST",
      message: "Popraw błędy w formularzu.",
      errors: [
        { field: "name", message: "To pole jest wymagane." },
        { field: null, message: "Nieprawidłowe dane." },
      ],
    });

    expect(error.message).toBe("Popraw błędy w formularzu.");
    expect(error.fieldErrors).toEqual([
      { field: "name", message: "To pole jest wymagane." },
      { field: null, message: "Nieprawidłowe dane." },
    ]);
  });

  it("rejects a body whose field errors have the wrong shape", () => {
    const error = toApiError(400, {
      code: 400,
      status: "400 BAD_REQUEST",
      message: "x",
      errors: [{ field: 1 }],
    });

    expect(error.statusName).toBeUndefined();
    expect(error.fieldErrors).toEqual([]);
  });

  it("takes the HTTP status from the response, not from the body", () => {
    const error = toApiError(500, {
      code: 404,
      status: "404 NOT_FOUND",
      message: "User not found",
    });

    expect(error.httpStatus).toBe(500);
  });

  it.each([
    ["a JSON body of another shape", 502, { error: "upstream timeout" }],
    ["a plain-text body", 503, "<html><body>Service Unavailable</body></html>"],
    ["an empty body (undefined)", 500, undefined],
  ])("falls back to a developer message for %s", (_label, httpStatus, body) => {
    const error = toApiError(httpStatus, body);

    expect(isApiError(error)).toBe(true);
    expect(error.httpStatus).toBe(httpStatus);
    expect(error.statusName).toBeUndefined();
    expect(error.message).toBe(
      `Unexpected response body (HTTP ${String(httpStatus)})`
    );
  });
});
