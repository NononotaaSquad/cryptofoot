import type { Metadata } from "next";

import { PageShell } from "@/components/page-shell";
import { CaseListView } from "@/features/case-list/case-list-view";

export const metadata: Metadata = {
  title: "Lista spraw",
};

export default function Page() {
  return (
    <PageShell headerLink={{ label: "← Nowa sprawa", href: "/" }}>
      <CaseListView />
    </PageShell>
  );
}
