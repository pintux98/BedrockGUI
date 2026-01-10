declare global {
  interface Window {
    gtag?: (...args: any[]) => void;
    dataLayer?: any[];
  }
}

export function gaEnabled() {
  return Boolean(import.meta.env.VITE_GA_MEASUREMENT_ID) && typeof window !== "undefined" && typeof window.gtag === "function";
}

export function gaPageView(path?: string) {
  if (!gaEnabled()) return;
  const page_location = window.location.href;
  const page_path = path ?? window.location.pathname + window.location.search + window.location.hash;
  window.gtag!("event", "page_view", { page_location, page_path });
}

export function gaEvent(name: string, params?: Record<string, any>) {
  if (!gaEnabled()) return;
  window.gtag!("event", name, params ?? {});
}

