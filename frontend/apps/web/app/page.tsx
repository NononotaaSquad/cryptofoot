import { PageShell } from "@/components/page-shell";
import { NewResultsBadge } from "@/features/case-list/new-results-badge";
import { NewCaseView } from "@/features/new-case/new-case-view";

export default function Page() {
  return (
    <PageShell
      headerLink={{
        label: "Lista spraw →",
        href: "/cases",
        badge: <NewResultsBadge />,
      }}
    >
      <NewCaseView />
    </PageShell>
  );
}
