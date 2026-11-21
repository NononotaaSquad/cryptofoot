import "@workspace/ui/globals.css";

import type { Metadata } from "next";
import type { ReactNode } from "react";

// Imported for its side effect: the root layout is evaluated for every route,
// so a build without NEXT_PUBLIC_API_URL fails here, whatever the pages import.
import "@/lib/env";

import { Providers } from "./providers";

export const metadata: Metadata = {
  title: {
    default: "Hackathon Template",
    template: "%s | Hackathon Template",
  },
  description: "Aplikacja zbudowana na szablonie hackathonowym.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: ReactNode;
}>) {
  return (
    <html lang="pl" suppressHydrationWarning>
      <body className="min-h-svh bg-background font-sans text-foreground antialiased">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
