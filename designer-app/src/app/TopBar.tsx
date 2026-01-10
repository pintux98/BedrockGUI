import React, { useState, useEffect } from "react";
import { useExporter } from "../exporters/useExporter";
import { useImporter } from "../importers/useImporter";
import { useDesignerStore } from "../core/store";
import { DesignerState } from "../core/types";

export function TopBar() {
  const { exportYaml } = useExporter();
  const { importYaml } = useImporter();
  const { undo, redo, undoStack, redoStack, menuName, setMenuName, loadState } = useDesignerStore();
  
  const [showProjects, setShowProjects] = useState(false);
  const [projects, setProjects] = useState<string[]>([]);
  const dropdownRef = React.useRef<HTMLDivElement>(null);

  useEffect(() => {
    updateProjectList();
    const handleSave = () => saveProject();
    window.addEventListener("save-project", handleSave);
    return () => window.removeEventListener("save-project", handleSave);
  }, [menuName]); // Re-bind when menuName changes to capture current name

  const updateProjectList = () => {
    const list: string[] = [];
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (key?.startsWith("project_")) {
        list.push(key.replace("project_", ""));
      }
    }
    setProjects(list);
  };

  const saveProject = () => {
    const state = useDesignerStore.getState();
    const data = JSON.stringify(state);
    localStorage.setItem(`project_${menuName}`, data);
    updateProjectList();
    alert(`Project '${menuName}' saved!`);
  };

  const loadProject = (name: string) => {
    const data = localStorage.getItem(`project_${name}`);
    if (data) {
      try {
        const state = JSON.parse(data);
        loadState(state);
        setShowProjects(false);
      } catch (e) {
        console.error("Failed to load project", e);
      }
    }
  };

  const deleteProject = (name: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (confirm(`Delete project '${name}'?`)) {
      localStorage.removeItem(`project_${name}`);
      updateProjectList();
    }
  };

  return (
    <div className="h-14 flex items-center justify-between px-4 bg-[#2b2b2b] border-b-4 border-[#1e1e1e] shadow-md z-10 shrink-0">
      <div className="flex items-center gap-4">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 bg-brand-accent border-2 border-white flex items-center justify-center text-white font-bold text-xl select-none">
              B
          </div>
          <div className="font-bold text-xl text-white tracking-widest drop-shadow-md hidden md:block">
              BEDROCK<span className="text-brand-accent">GUI</span>
          </div>
        </div>
        
        <div className="h-8 w-[2px] bg-[#3f3f3f] mx-2"></div>
        
        <div className="flex items-center gap-2">
          <button 
            className="ui-btn ui-btn-secondary px-3 py-1 text-xs"
            onClick={undo}
            disabled={!undoStack.length}
            title="Undo (Ctrl+Z)"
          >
            ↶ Undo
          </button>
          <button 
            className="ui-btn ui-btn-secondary px-3 py-1 text-xs"
            onClick={redo}
            disabled={!redoStack.length}
            title="Redo (Ctrl+Y)"
          >
            ↷ Redo
          </button>
        </div>

        <div className="h-8 w-[2px] bg-[#3f3f3f] mx-2"></div>

        {/* Project Management */}
        <div className="relative" ref={dropdownRef}>
          <div 
            className="flex items-center gap-2 bg-[#3a3a3a] px-3 py-1 rounded cursor-pointer hover:bg-[#4a4a4a] border border-[#555]"
            onClick={() => setShowProjects(!showProjects)}
          >
            <div className="text-sm font-bold text-white">{menuName}</div>
            <div className="text-xs text-gray-400">▼</div>
          </div>

          {showProjects && (
            <div className="absolute top-full left-0 mt-2 w-64 bg-[#2b2b2b] border border-[#555] shadow-xl rounded z-50">
              <div className="p-2 border-b border-[#444]">
                 <input 
                   className="w-full bg-[#1e1e1e] border border-[#555] px-2 py-1 text-sm text-white focus:border-brand-accent outline-none"
                   value={menuName}
                   onChange={(e) => setMenuName(e.target.value)}
                   placeholder="Project Name"
                   onClick={(e) => e.stopPropagation()}
                 />
              </div>
              <div className="p-1 max-h-48 overflow-y-auto">
                <div className="text-xs text-gray-500 px-2 py-1 uppercase font-bold">Saved Projects</div>
                {projects.length === 0 && <div className="text-xs text-gray-500 px-2 py-2 italic">No saved projects</div>}
                {projects.map(p => (
                  <div 
                    key={p} 
                    className="flex items-center justify-between px-2 py-1 hover:bg-[#3a3a3a] cursor-pointer group"
                    onClick={() => loadProject(p)}
                  >
                    <span className={`text-sm ${p === menuName ? "text-brand-accent font-bold" : "text-gray-300"}`}>{p}</span>
                    <button 
                      className="text-gray-500 hover:text-red-400 hidden group-hover:block px-1"
                      onClick={(e) => deleteProject(p, e)}
                    >
                      ✕
                    </button>
                  </div>
                ))}
              </div>
              <div className="p-2 border-t border-[#444] flex gap-2">
                <button 
                  className="flex-1 ui-btn ui-btn-primary text-xs py-1"
                  onClick={saveProject}
                >
                  Save Project
                </button>
                <button 
                  className="flex-1 ui-btn ui-btn-secondary text-xs py-1"
                  onClick={() => {
                     setMenuName("New Project");
                     // Reset state logic could go here if implemented
                     setShowProjects(false);
                  }}
                >
                  New
                </button>
              </div>
            </div>
          )}
        </div>
        {/* External Links */}
        <div className="flex items-center gap-2 mx-2">
          <a
            href="https://pintux.gitbook.io/pintux-support/bedrockgui/bedrockgui-v2"
            target="_blank"
            rel="noreferrer"
            className="ui-btn-secondary px-3 py-1.5 text-brand-muted hover:text-white transition-colors relative group flex items-center gap-2"
            title="Documentation"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
              <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
            </svg>
            <span className="text-xs font-medium hidden lg:inline">Docs</span>
          </a>
          
          <div className="h-6 w-[1px] bg-[#3f3f3f] mx-1"></div>

          <a
            href="https://www.spigotmc.org/resources/bedrockgui-spigot-and-bungeecord-support.119592/"
            target="_blank"
            rel="noreferrer"
            className="ui-btn-secondary px-3 py-1.5 text-[#ff9f43] hover:text-[#ffb775] transition-colors relative group flex items-center gap-2"
            title="SpigotMC"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
               <path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z" />
            </svg>
            <span className="text-xs font-medium hidden lg:inline">Spigot</span>
          </a>

          <a
            href="https://modrinth.com/plugin/bedrockgui"
            target="_blank"
            rel="noreferrer"
            className="ui-btn-secondary px-3 py-1.5 text-[#1bd96a] hover:text-[#4ffc92] transition-colors relative group flex items-center gap-2"
            title="Modrinth"
          >
             <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
               <path d="M12.002 0a12 12 0 1 0 0 24 12 12 0 0 0 0-24Zm0 21.6a9.6 9.6 0 1 1 0-19.2 9.6 9.6 0 0 1 0 19.2Zm0-15.6a6 6 0 1 0 0 12 6 6 0 0 0 0-12Z" />
             </svg>
             <span className="text-xs font-medium hidden lg:inline">Modrinth</span>
          </a>

          <a
            href="https://github.com/pintux98/BedrockGUI"
            target="_blank"
            rel="noreferrer"
            className="ui-btn-secondary px-3 py-1.5 text-white hover:text-gray-300 transition-colors relative group flex items-center gap-2"
            title="GitHub"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z" />
            </svg>
            <span className="text-xs font-medium hidden lg:inline">GitHub</span>
          </a>

          <a
            href="https://ko-fi.com/pintux"
            target="_blank"
            rel="noreferrer"
            className="ui-btn-secondary px-3 py-1.5 text-[#ff5e5b] hover:text-[#ff8f8d] transition-colors relative group flex items-center gap-2"
            title="Ko-Fi"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
              <path d="M23.881 8.948c-.773-4.085-4.859-4.593-4.859-4.593H.723c-.604 0-.679.798-.679.798s-.082 7.324-.022 11.822c.164 2.424 2.586 2.672 2.586 2.672s8.267-.023 11.966-.049c2.438-.426 2.683-2.566 2.658-3.734 4.352.24 7.422-2.831 6.649-6.916zm-11.062 3.511c-1.246 1.453-4.011 3.976-4.011 3.976s-.121.119-.31.023c-.076-.057-.108-.09-.108-.09-.443-.441-3.368-3.049-4.034-3.954-.709-.965-1.041-2.7-.091-3.71.951-1.01 3.005-1.086 4.363.407 0 0 1.565-1.782 3.468-.963 1.904.82 1.832 3.011.723 4.311zm6.173.478c-.928.116-1.682.028-1.682.028V7.284h1.77s1.971.551 1.971 2.638c0 1.913-.985 2.667-2.059 3.015z"/>
            </svg>
            <span className="text-xs font-medium hidden lg:inline">Ko-Fi</span>
          </a>
        </div>
      </div>

      <div className="flex gap-2">
        <button
          className="ui-btn ui-btn-primary px-4 py-1 text-sm uppercase tracking-wide"
          onClick={() => exportYaml()}
        >
          Export
        </button>
        <label className="ui-btn ui-btn-secondary px-4 py-1 text-sm uppercase tracking-wide cursor-pointer">
          Import
          <input
            type="file"
            accept=".yml,.yaml"
            className="hidden"
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) importYaml(file);
            }}
          />
        </label>
      </div>
    </div>
  );
}
