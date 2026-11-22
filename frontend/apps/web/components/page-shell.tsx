import type { Route } from "next";
import Image from "next/image";
import Link from "next/link";
import type { ReactNode } from "react";

import kasLogoMark from "@/public/kas-logo-mark.png";

interface PageShellProps {
  // `badge` sits right after the label (the Case counter).
  headerLink: { label: string; href: Route; badge?: ReactNode };
  children: ReactNode;
}

// Header, centred content column (1200 px) and footer shared by every view.
export function PageShell({ headerLink, children }: PageShellProps) {
  return (
    <div className="flex min-h-svh flex-col">
      <header className="border-b border-border bg-background">
        <div className="mx-auto flex h-18 max-w-300 items-center justify-between gap-4 px-6 xl:px-0">
          <div className="flex items-center gap-4">
            <Image
              src={kasLogoMark}
              alt="Krajowa Administracja Skarbowa"
              className="h-12 w-16 object-contain"
              preload
            />
            <span className="text-xl font-bold text-navy">Projekt OŁCyŚ</span>
          </div>
          <Link
            href={headerLink.href}
            className="rounded-sm text-base font-semibold text-primary underline-offset-4 outline-none hover:underline focus-visible:ring-3 focus-visible:ring-ring/50"
          >
            {headerLink.label}
            {headerLink.badge}
          </Link>
        </div>
      </header>

      <main className="mx-auto w-full max-w-300 flex-1 px-6 pt-10 pb-14 xl:px-0">
        {children}
      </main>

      <footer className="border-t border-border">
        <p className="mx-auto max-w-300 px-6 py-5 text-sm leading-normal text-muted-foreground xl:px-0">
          Wszelkie prawa zastrzeżone. Krajowa Administracja Skarbowa, projekt
          OŁCyŚ.
        </p>
      </footer>
    </div>
  );
}
