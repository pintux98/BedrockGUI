import "@testing-library/jest-dom/vitest";

if (!Element.prototype.scrollIntoView) {
  Object.defineProperty(Element.prototype, "scrollIntoView", {
    value: () => {},
    writable: true
  });
}

