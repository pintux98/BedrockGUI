import React from "react";
import { DesignerShell } from "./app/DesignerShell";
import { MobileWarning } from "./components/MobileWarning";

export default function App() {
  return (
    <>
      <MobileWarning />
      <DesignerShell />
    </>
  );
}

