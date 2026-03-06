import React from "react";

let cached: Record<string, string> | null = null;
let inflight: Promise<Record<string, string>> | null = null;

export async function getJavaAssetsIndex(): Promise<Record<string, string>> {
  if (cached) return cached;
  if (inflight) return inflight;
  inflight = fetch("/java-assets-index.json", { cache: "force-cache" })
    .then((r) => (r.ok ? r.json() : {}))
    .catch(() => ({}))
    .then((m) => {
      cached = (m && typeof m === "object" ? (m as Record<string, string>) : {}) ?? {};
      inflight = null;
      return cached;
    });
  return inflight;
}

export function useJavaAssetsIndex() {
  const [index, setIndex] = React.useState<Record<string, string> | null>(cached);
  React.useEffect(() => {
    if (cached) {
      setIndex(cached);
      return;
    }
    getJavaAssetsIndex().then((m) => setIndex(m));
  }, []);
  return index;
}

