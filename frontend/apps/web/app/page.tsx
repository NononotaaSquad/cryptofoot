import { PageShell } from "@/components/page-shell";
import { NewCaseView } from "@/features/new-case/new-case-view";

export default function Page() {
  return (
    <PageShell headerLink={{ label: "Lista spraw →", href: "/cases" }}>
      <NewCaseView />
    </PageShell>
  );
}
