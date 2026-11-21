import type { Metadata } from "next";

import { PageShell } from "@/components/page-shell";

export const metadata: Metadata = {
  title: "Szczegóły sprawy",
};

// Stub until the Case details view is built. The `caseId` param and the
// `analysis` search param are not read yet.
export default function Page() {
  return (
    <PageShell headerLink={{ label: "← Lista spraw", href: "/cases" }}>
      <h1 className="text-[2.5rem] leading-tight font-bold">
        Szczegóły sprawy
      </h1>
    </PageShell>
  );
}
