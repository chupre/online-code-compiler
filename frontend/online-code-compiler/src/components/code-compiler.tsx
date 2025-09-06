import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Play, Square, Code2 } from "lucide-react";
import AceEditor from "react-ace";
import "ace-builds/src-noconflict/mode-python";
import "ace-builds/src-noconflict/mode-c_cpp";
import "ace-builds/src-noconflict/theme-twilight";
import "ace-builds/src-noconflict/ext-language_tools";
import {execute} from "@/http/codeAPI.ts";

const defaultCode = {
    c: `#include <stdio.h>

int main() {
    printf("Hello, World!\\n");
    return 0;
}`,
    python: `print("Hello, World!")

# Your Python code here
def greet(name):
    return f"Hello, {name}!"

print(greet("Developer"))`,
};

export function CodeCompiler() {
    const [language, setLanguage] = useState<"c" | "python">("python");
    const [code, setCode] = useState(defaultCode.python);
    const [output, setOutput] = useState("");
    const [isRunning, setIsRunning] = useState(false);

    const handleLanguageChange = (value: "c" | "python") => {
        setLanguage(value);
        setCode(defaultCode[value]);
        setOutput("");
    };

    const handleRun = async () => {
        setIsRunning(true);
        setOutput("Running code...");

        execute(code, language.toUpperCase()).then((data) => {
            setOutput(data)
            setIsRunning(false)
        })
    };

    const handleStop = () => {
        setIsRunning(false);
        setOutput("Execution stopped.");
    };

    const editorMode = language === "python" ? "python" : "c_cpp";

    return (
        <div className="min-h-screen p-4 max-w-6xl mx-auto bg-[#161616] text-[#F8F8F8]">
            <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                    <Code2 className="h-6 w-6 text-[#CDA869]" />
                    <h1 className="text-xl font-bold">CODE COMPILER</h1>
                </div>

                <div className="flex items-center gap-3">
                    <label
                        htmlFor="language-select"
                        className="text-sm font-medium text-[#E2E2E2]"
                    >
                        LANGUAGE:
                    </label>
                    <Select value={language} onValueChange={handleLanguageChange}>
                        <SelectTrigger
                            id="language-select"
                            className="w-28 border bg-[#232323] border-[#5F5A60] text-[#F8F8F8] rounded-none"
                        >
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent className="border bg-[#232323] border-[#5F5A60] rounded-none">
                            <SelectItem
                                value="python"
                                className="hover:bg-white/10 text-[#F8F8F8] rounded-none"
                            >
                                PYTHON
                            </SelectItem>
                            <SelectItem
                                value="c"
                                className="hover:bg-white/20 text-[#F8F8F8] rounded-none"
                            >
                                C
                            </SelectItem>
                        </SelectContent>
                    </Select>
                </div>
            </div>

            <div className="flex items-center gap-2 mb-4">
                <Button
                    onClick={handleRun}
                    disabled={isRunning}
                    size="sm"
                    className={`px-4 py-2 text-sm font-semibold border border-[#5F5A60] rounded-none transition-colors
      ${isRunning
                        ? "bg-[#5F5A60] text-[#141414]"
                        : "bg-[#9CCC65] hover:bg-[#A6E22E] text-[#141414]"}`}
                >
                    <Play className="h-4 w-4" />
                </Button>

                <Button
                    onClick={handleStop}
                    disabled={!isRunning}
                    size="sm"
                    className={`px-4 py-2 text-sm font-semibold border border-[#5F5A60] rounded-none transition-colors
      ${!isRunning
                        ? "bg-[#5F5A60] text-[#F8F8F8]"
                        : "bg-[#EF5350] hover:bg-[#F92020] text-[#F8F8F8]"}`}
                >
                    <Square className="h-4 w-4 mr" />

                </Button>
            </div>

            <div className="space-y-4">
                <Card className="p-0 overflow-hidden border bg-[#232323] border-[#5F5A60] rounded-none">
                    <div className="p-2 border-b text-sm font-medium bg-[#141414] border-[#5F5A60] text-[#E2E2E2]">
                        CODE EDITOR
                    </div>
                    <div className="p-4 pt-0">
                        <AceEditor
                            placeholder="Enter your code here..."
                            mode={editorMode}
                            theme="twilight"
                            name="code-editor"
                            fontSize={14}
                            lineHeight={19}
                            height="350px"
                            width="100%"
                            showPrintMargin={true}
                            showGutter={false}
                            highlightActiveLine={true}
                            value={code}
                            onChange={(value) => setCode(value)}
                            setOptions={{
                                enableBasicAutocompletion: true,
                                enableLiveAutocompletion: true,
                                enableSnippets: false,
                                showLineNumbers: false,
                                tabSize: 2,
                            }}
                        />
                    </div>
                </Card>

                <Card className="p-0 overflow-hidden border bg-[#232323] border-[#5F5A60] rounded-none">
                    <div className="p-2 border-b text-sm font-medium bg-[#141414] border-[#5F5A60] text-[#E2E2E2]">
                        OUTPUT
                    </div>
                    <div className="p-4 pt-0">
                        <div className="min-h-[150px] max-h-64 overflow-auto p-4 pt-0 bg-[#141414] text-muted-foreground whitespace-pre-wrap">
                            {output || "Output will appear here..."}
                        </div>
                    </div>
                </Card>
            </div>
        </div>
    );
}
