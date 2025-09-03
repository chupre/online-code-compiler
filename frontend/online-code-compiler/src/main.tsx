import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import {CodeCompiler} from "@/components/code-compiler.tsx";

createRoot(document.getElementById('root')!).render(
  <StrictMode>
      <main className="min-h-screen bg-[#161616]">
          <CodeCompiler />
      </main>
  </StrictMode>,
)
