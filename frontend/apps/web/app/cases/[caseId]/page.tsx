import type { Metadata } from "next";

import { PageShell } from "@/components/page-shell";
import { NewResultsBadge } from "@/features/case-list/new-results-badge";
import { CaseDetailsView } from "@/features/case-details/case-details-view";

export const metadata: Metadata = {
  title: "Szczegóły sprawy",
};

export default async function Page({
  params,
  searchParams,
}: PageProps<"/cases/[caseId]">) {
  const { caseId } = await params;
  const { analysis } = await searchParams;

  return (
    <PageShell
      headerLink={{
        label: "← Lista spraw",
        href: "/cases",
        badge: <NewResultsBadge />,
      }}
    >
      <CaseDetailsView
        caseId={caseId}
        // A repeated `?analysis=` gives an array; the first one wins.
        analysisId={Array.isArray(analysis) ? analysis[0] : analysis}
      />
    </PageShell>
  );
}
