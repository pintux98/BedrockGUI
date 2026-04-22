import React from "react";
import { DesignerShell } from "./app/DesignerShell";
import { MobileWarning } from "./components/MobileWarning";
import { ToastHost } from "./components/ToastHost";
import { ConfirmDialog } from "./components/ConfirmDialog";

export default function App() {
  return (
    <>
      <MobileWarning />
      <DesignerShell />
      <ToastHost />
      <ConfirmDialog />
    </>
  );
}

