import React, { useEffect, useState } from "react";
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
import { ResizablePanel } from "../components/ResizablePanel";

export function DesignerShell() {
  const { platform, isWizardOpen } = useDesignerStore();
  const [isDesktop, setIsDesktop] = useState(true);
  const [yamlCollapsed, setYamlCollapsed] = useState(false);
  const [historyCollapsed, setHistoryCollapsed] = useState(false);
  const [mobileTab, setMobileTab] = useState<"tools" | "canvas" | "properties">("canvas");

  useEffect(() => {
    const check = () => setIsDesktop(window.innerWidth >= 1024);
    check();
    window.addEventListener("resize", check);
    return () => window.removeEventListener("resize", check);
  }, []);

  return (
    <div className="h-full w-full flex flex-col bg-brand-bg text-brand-text relative">
      <TopBar />
      {isWizardOpen && <Wizard />}
      <div className="flex-1 overflow-hidden relative flex flex-col min-h-0">
        <DndHost>
          <ErrorBoundary>
            <div className="flex flex-col lg:flex-row flex-1 overflow-hidden min-h-0">
            {/* Left Panel */}
            {isDesktop ? (
              <ResizablePanel 
                initialSize={384} 
                minSize={250} 
                maxSize={500} 
                side="left" 
                persistenceKey="left_panel_width"
                className="border-r border-brand-border h-full"
              >
                <div className="flex-1 flex flex-col h-full overflow-hidden">
                  <div className="flex-1 overflow-y-auto overflow-x-hidden custom-scrollbar min-h-0">
                    <FormTypePanel />
                    {platform === "bedrock" ? <Palette /> : <JavaPalette />}
                  </div>
                  <ResizablePanel
                    initialSize={200}
                    minSize={32}
                    maxSize={500}
                    side="bottom"
                    persistenceKey="history_panel_height"
                    className={`shrink-0 border-t border-brand-border transition-all duration-300 ${historyCollapsed ? "!h-8 !min-h-[32px]" : ""}`}
                    forceCollapse={historyCollapsed}
                  >
                    <HistoryPanel onCollapseChange={setHistoryCollapsed} />
                  </ResizablePanel>
                </div>
              </ResizablePanel>
            ) : (
              // Mobile View: Tools Tab
              <div className={`w-full flex-1 flex flex-col overflow-hidden ${mobileTab === "tools" ? "flex" : "hidden"}`}>
                <div className="flex-1 overflow-y-auto overflow-x-hidden custom-scrollbar">
                  <FormTypePanel />
                  {platform === "bedrock" ? <Palette /> : <JavaPalette />}
                  <HistoryPanel />
                </div>
              </div>
            )}

            {/* Center Canvas */}
            <div className={`flex-1 flex flex-col min-w-0 min-h-0 relative bg-neutral-900/50 ${!isDesktop && mobileTab !== "canvas" ? "hidden" : "flex"}`}>
              <div className="flex-1 overflow-hidden relative">
                <Canvas />
              </div>
              <div className="border-t border-brand-border shrink-0">
                <ValidationPanel />
              </div>
            </div>

            {/* Right Panel */}
            {isDesktop ? (
              <ResizablePanel 
                initialSize={420} 
                minSize={300} 
                maxSize={600} 
                side="right" 
                persistenceKey="right_panel_width"
                className="border-l border-brand-border h-full"
              >
                <div className="flex-1 flex flex-col h-full overflow-hidden">
                  <div className="flex-1 overflow-y-auto overflow-x-hidden custom-scrollbar min-h-0">
                    <PropertiesPanel />
                  </div>
                  <ResizablePanel
                    initialSize={250}
                    minSize={32} // Collapsed header height
                    maxSize={600}
                    side="bottom"
                    persistenceKey="yaml_panel_height"
                    className={`shrink-0 transition-all duration-300 ${yamlCollapsed ? "!h-8 !min-h-[32px]" : ""}`}
                    forceCollapse={yamlCollapsed}
                  >
                    <YamlEditorPanel onCollapseChange={setYamlCollapsed} />
                  </ResizablePanel>
                </div>
              </ResizablePanel>
            ) : (
              // Mobile View: Properties Tab
              <div className={`w-full flex-1 flex flex-col overflow-hidden min-h-0 ${mobileTab === "properties" ? "flex" : "hidden"}`}>
                <div className="flex-1 overflow-y-auto overflow-x-hidden custom-scrollbar flex flex-col">
                  <div className="shrink-0 min-h-max">
                    <PropertiesPanel />
                  </div>
                  <div className="h-[300px] border-t border-brand-border shrink-0">
                    <YamlEditorPanel defaultExpanded={true} />
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Mobile Bottom Navigation */}
          {!isDesktop && (
            <div className="h-14 bg-[#2b2b2b] border-t border-brand-border shrink-0 flex items-center justify-around px-2 z-50">
              <button 
                onClick={() => setMobileTab("tools")}
                className={`flex flex-col items-center justify-center w-20 py-1 rounded transition-colors ${mobileTab === "tools" ? "text-brand-accent bg-[#3a3a3a]" : "text-gray-400"}`}
              >
                <span className="text-lg">🛠️</span>
                <span className="text-[10px] font-bold mt-0.5">Tools</span>
              </button>
              <button 
                onClick={() => setMobileTab("canvas")}
                className={`flex flex-col items-center justify-center w-20 py-1 rounded transition-colors ${mobileTab === "canvas" ? "text-brand-accent bg-[#3a3a3a]" : "text-gray-400"}`}
              >
                <span className="text-lg">🎨</span>
                <span className="text-[10px] font-bold mt-0.5">Canvas</span>
              </button>
              <button 
                onClick={() => setMobileTab("properties")}
                className={`flex flex-col items-center justify-center w-20 py-1 rounded transition-colors ${mobileTab === "properties" ? "text-brand-accent bg-[#3a3a3a]" : "text-gray-400"}`}
              >
                <span className="text-lg">⚙️</span>
                <span className="text-[10px] font-bold mt-0.5">Props</span>
              </button>
            </div>
          )}
          </ErrorBoundary>
        </DndHost>
      </div>
    </div>
  );
}
