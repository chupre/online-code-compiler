import {$host} from "@/http/index.ts";

export const executeC = async (code: string) => {
    const {data} = await $host.post("/api/execute/c", {code})
    return data
}