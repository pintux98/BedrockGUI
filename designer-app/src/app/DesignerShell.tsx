import React, { useEffect } from "react";
import { Palette } from "../panels/Palette";
import { PropertiesPanel } from "../panels/PropertiesPanel";
import { FormTypePanel } from "../panels/FormTypePanel";
import { JavaPalette } from "../panels/JavaPalette";
import { Canvas } from "../canvas/Canvas";
import { ErrorBoundary } from "./ErrorBoundary";
import { TopBar } from "./TopBar";
import { DocumentationPanel } from "../panels/DocumentationPanel";
import { YamlEditorPanel } from "../panels/YamlEditorPanel";
import { useDesignerStore } from "../core/store";
import { DndHost } from "./DndHost";
import { ValidationPanel } from "../panels/ValidationPanel";
import { Wizard } from "../components/Wizard";
import { HistoryPanel } from "../panels/HistoryPanel";

export function DesignerShell() {
  const { platform, isWizardOpen } = useDesignerStore();
  return (
    <div className="h-full flex flex-col bg-brand-bg text-brand-text relative">
      <TopBar />
      {isWizardOpen && <Wizard />}
      <DndHost>
        <ErrorBoundary>
          <div className="flex flex-col flex-1 overflow-hidden">
            <div className="flex flex-1 overflow-hidden">
              <div className="w-96 border-r border-brand-border overflow-y-auto overflow-x-hidden flex flex-col">
                <FormTypePanel />
                {platform === "bedrock" ? <Palette /> : <JavaPalette />}
                <HistoryPanel />
              </div>
              <div className="flex-1 overflow-hidden">
                <Canvas />
              </div>
              <div className="w-[420px] border-l border-brand-border overflow-y-auto overflow-x-hidden flex flex-col">
                <PropertiesPanel />
                <YamlEditorPanel />
              </div>
            </div>
            <div className="flex border-t border-brand-border">
              <div className="flex-1">
                <ValidationPanel />
              </div>
              <div className="w-[420px] border-l border-brand-border h-8 bg-brand-surface">
                {/* Empty spacer to match ValidationPanel height */}
              </div>
            </div>
          </div>
        </ErrorBoundary>
      </DndHost>
    </div>
  );
}
