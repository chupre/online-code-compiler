import {$host} from "@/http/index.ts";

export const execute = async (code: string, language: string) => {
    const {data} = await $host.post("/api/execute", {code, language})
    return data
}

export const stopExecution = async (id: bigint) => {
    await $host.post(`/api/execute/${id}`)
}