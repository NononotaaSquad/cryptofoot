"use client";

// Replaces the root layout when it fails, so it renders its own document and
// gets no global CSS or providers: keep it dependency-free.
export default function GlobalError({ retry }: { retry: () => void }) {
  return (
    <html lang="pl">
      <body style={{ fontFamily: "system-ui, sans-serif", padding: "2rem" }}>
        <title>Błąd aplikacji</title>
        <h1>Coś poszło nie tak</h1>
        <p>Wystąpił nieoczekiwany błąd. Spróbuj ponownie za chwilę.</p>
        <button
          type="button"
          onClick={() => {
            retry();
          }}
        >
          Spróbuj ponownie
        </button>
      </body>
    </html>
  );
}
