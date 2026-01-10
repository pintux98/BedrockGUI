import React from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import { gaPageView } from "./analytics";
import "./styles.css";

try {
  localStorage.removeItem("bedrockgui.designer.assets.v1");
} catch {}

const container = document.getElementById("root")!;
const root = createRoot(container);
root.render(<App />);

try {
  gaPageView();
} catch {}

