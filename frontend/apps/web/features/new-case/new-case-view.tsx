import { AnalysisGuidance } from "./analysis-guidance";
import { NewCaseForm } from "./new-case-form";

export function NewCaseView() {
  return (
    <div className="flex flex-col gap-8">
      <div className="flex max-w-182 flex-col gap-2.5">
        <h1 className="text-[2.5rem] leading-tight font-bold">Nowa sprawa</h1>
        <p className="leading-normal">
          Wprowadź poniżej znane dane do analizy obywatela. Na tej podstawie
          przeszukamy publiczne źródła i przygotujemy raport.
        </p>
      </div>
      {/* At 1200 px: form 728 px, gap 56 px, guidance 416 px (26rem). */}
      <div className="grid items-start gap-14 lg:grid-cols-[minmax(0,1fr)_minmax(18rem,26rem)]">
        <NewCaseForm />
        <AnalysisGuidance />
      </div>
    </div>
  );
}
