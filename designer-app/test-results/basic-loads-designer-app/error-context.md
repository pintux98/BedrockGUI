# Page snapshot

```yaml
- generic [ref=e3]:
  - generic [ref=e4]: "[plugin:vite:import-analysis] Failed to resolve import \"@hookform/resolvers/zod\" from \"src/actions/ActionEditor.tsx\". Does the file exist?"
  - generic [ref=e5]: C:/Users/pintu/Desktop/Server/BedrockGUI/designer-app/src/actions/ActionEditor.tsx:3:28
  - generic [ref=e6]: "18 | import { useMemo } from \"react\"; 19 | import { useForm } from \"react-hook-form\"; 20 | import { zodResolver } from \"@hookform/resolvers/zod\"; | ^ 21 | import { ActionRegistry } from \"./registry\"; 22 | export function ActionEditor({"
  - generic [ref=e7]: at TransformPluginContext._formatError (file:///C:/Users/pintu/Desktop/Server/BedrockGUI/designer-app/node_modules/vite/dist/node/chunks/dep-BK3b2jBa.js:49258:41) at TransformPluginContext.error (file:///C:/Users/pintu/Desktop/Server/BedrockGUI/designer-app/node_modules/vite/dist/node/chunks/dep-BK3b2jBa.js:49253:16) at normalizeUrl (file:///C:/Users/pintu/Desktop/Server/BedrockGUI/designer-app/node_modules/vite/dist/node/chunks/dep-BK3b2jBa.js:64307:23) at process.processTicksAndRejections (node:internal/process/task_queues:105:5) at async file:///C:/Users/pintu/Desktop/Server/BedrockGUI/designer-app/node_modules/vite/dist/node/chunks/dep-BK3b2jBa.js:64439:39 at async Promise.all (index 5) at async TransformPluginContext.transform (file:///C:/Users/pintu/Desktop/Server/BedrockGUI/designer-app/node_modules/vite/dist/node/chunks/dep-BK3b2jBa.js:64366:7) at async PluginContainer.transform (file:///C:/Users/pintu/Desktop/Server/BedrockGUI/designer-app/node_modules/vite/dist/node/chunks/dep-BK3b2jBa.js:49099:18) at async loadAndTransform (file:///C:/Users/pintu/Desktop/Server/BedrockGUI/designer-app/node_modules/vite/dist/node/chunks/dep-BK3b2jBa.js:51978:27) at async viteTransformMiddleware (file:///C:/Users/pintu/Desktop/Server/BedrockGUI/designer-app/node_modules/vite/dist/node/chunks/dep-BK3b2jBa.js:62106:24
  - generic [ref=e8]:
    - text: Click outside, press Esc key, or fix the code to dismiss.
    - text: You can also disable this overlay by setting
    - code [ref=e9]: server.hmr.overlay
    - text: to
    - code [ref=e10]: "false"
    - text: in
    - code [ref=e11]: vite.config.ts
    - text: .
```