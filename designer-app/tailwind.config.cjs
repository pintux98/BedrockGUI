/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        brand: {
          bg: "rgb(var(--ui-bg) / <alpha-value>)",
          surface: "rgb(var(--ui-surface) / <alpha-value>)",
          "surface-raised": "rgb(var(--ui-surface-raised) / <alpha-value>)",
          surface2: "rgb(var(--ui-surface-2) / <alpha-value>)",
          border: "rgb(var(--ui-border) / <alpha-value>)",
          borderStrong: "rgb(var(--ui-border-strong) / <alpha-value>)",
          text: "rgb(var(--ui-text) / <alpha-value>)",
          muted: "rgb(var(--ui-muted) / <alpha-value>)",
          accent: "rgb(var(--ui-accent) / <alpha-value>)",
          accentHover: "rgb(var(--ui-accent-hover) / <alpha-value>)",
          accentPressed: "rgb(var(--ui-accent-pressed) / <alpha-value>)",
          accentText: "rgb(var(--ui-accent-text) / <alpha-value>)",
          focus: "rgb(var(--ui-focus) / <alpha-value>)",
          success: "rgb(var(--ui-success) / <alpha-value>)",
          warning: "rgb(var(--ui-warning) / <alpha-value>)",
          danger: "rgb(var(--ui-danger) / <alpha-value>)"
        }
      }
    }
  },
  plugins: []
};

