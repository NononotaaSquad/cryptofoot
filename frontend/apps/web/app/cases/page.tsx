import type { Metadata } from "next";

import { PageShell } from "@/components/page-shell";

export const metadata: Metadata = {
  title: "Lista spraw",
};

// Stub until the Case list view is built.
export default function Page() {
  return (
    <PageShell headerLink={{ label: "← Nowa sprawa", href: "/" }}>
      <h1 className="text-[2.5rem] leading-tight font-bold">Lista spraw</h1>
    </PageShell>
  );
}
