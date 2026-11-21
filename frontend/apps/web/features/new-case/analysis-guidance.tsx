const steps = [
  "Utworzymy nową sprawę. Znajdziesz ją później na liście spraw.",
  "Przeszukamy publicznie dostępne źródła w internecie pod kątem podanych identyfikatorów.",
  "Analiza chwilę potrwa. Nie zamykaj w tym czasie strony.",
  "Gdy analiza się zakończy, raport otworzy się sam.",
];

export function AnalysisGuidance() {
  return (
    <aside
      aria-labelledby="analysis-guidance-heading"
      className="flex flex-col gap-5 rounded-lg bg-muted p-7"
    >
      <h2
        id="analysis-guidance-heading"
        className="text-xl leading-tight font-bold"
      >
        Co się stanie, gdy rozpoczniesz analizę
      </h2>
      <ol className="flex flex-col gap-4">
        {steps.map((step, index) => (
          <li key={step} className="flex gap-3 leading-normal">
            <span aria-hidden="true" className="w-5 shrink-0 font-bold">
              {index + 1}.
            </span>
            <span>{step}</span>
          </li>
        ))}
      </ol>
      <hr className="border-border-strong" />
      <div className="flex flex-col gap-1 leading-normal">
        <p className="font-bold">Pamiętaj</p>
        <p>
          Aby rozpocząć analizę, podaj co najmniej jeden identyfikator. Samo
          imię i nazwisko nie wystarczy.
        </p>
      </div>
    </aside>
  );
}
