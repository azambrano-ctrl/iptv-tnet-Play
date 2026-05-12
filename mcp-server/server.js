import express from "express";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/streamableHttp.js";
import { z } from "zod";
import fs from "fs";
import path from "path";

// Raíz del proyecto — ajusta esta ruta a tu PC
const PROJECT_ROOT = process.env.PROJECT_ROOT || process.cwd();

function resolveSafe(filePath) {
  const resolved = path.resolve(PROJECT_ROOT, filePath);
  if (!resolved.startsWith(path.resolve(PROJECT_ROOT))) {
    throw new Error("Acceso fuera del proyecto no permitido");
  }
  return resolved;
}

const server = new McpServer({
  name: "tnet-play-files",
  version: "1.0.0",
});

server.tool(
  "read_file",
  "Lee el contenido de un archivo del proyecto",
  { path: z.string().describe("Ruta relativa al archivo dentro del proyecto") },
  async ({ path: filePath }) => {
    const full = resolveSafe(filePath);
    if (!fs.existsSync(full)) return { content: [{ type: "text", text: `Archivo no encontrado: ${filePath}` }] };
    const content = fs.readFileSync(full, "utf-8");
    return { content: [{ type: "text", text: content }] };
  }
);

server.tool(
  "write_file",
  "Escribe o reemplaza el contenido de un archivo del proyecto",
  {
    path: z.string().describe("Ruta relativa al archivo"),
    content: z.string().describe("Contenido a escribir"),
  },
  async ({ path: filePath, content }) => {
    const full = resolveSafe(filePath);
    fs.mkdirSync(path.dirname(full), { recursive: true });
    fs.writeFileSync(full, content, "utf-8");
    return { content: [{ type: "text", text: `Archivo guardado: ${filePath}` }] };
  }
);

server.tool(
  "list_directory",
  "Lista archivos y carpetas en una ruta del proyecto",
  { path: z.string().default("").describe("Ruta relativa al directorio (vacío = raíz del proyecto)") },
  async ({ path: dirPath }) => {
    const full = resolveSafe(dirPath || ".");
    if (!fs.existsSync(full)) return { content: [{ type: "text", text: `Directorio no encontrado: ${dirPath}` }] };
    const entries = fs.readdirSync(full, { withFileTypes: true }).map(
      (e) => `${e.isDirectory() ? "[DIR]" : "[FILE]"} ${e.name}`
    );
    return { content: [{ type: "text", text: entries.join("\n") }] };
  }
);

server.tool(
  "delete_file",
  "Elimina un archivo del proyecto",
  { path: z.string().describe("Ruta relativa al archivo a eliminar") },
  async ({ path: filePath }) => {
    const full = resolveSafe(filePath);
    if (!fs.existsSync(full)) return { content: [{ type: "text", text: `Archivo no encontrado: ${filePath}` }] };
    fs.unlinkSync(full);
    return { content: [{ type: "text", text: `Eliminado: ${filePath}` }] };
  }
);

const app = express();
app.use(express.json());

app.all("/mcp", async (req, res) => {
  const transport = new StreamableHTTPServerTransport({
    sessionIdGenerator: undefined,
  });
  res.on("close", () => transport.close());
  await server.connect(transport);
  await transport.handleRequest(req, res, req.body);
});

const PORT = process.env.PORT || 3456;
app.listen(PORT, () => {
  console.log(`MCP Server corriendo en http://localhost:${PORT}/mcp`);
  console.log(`Proyecto: ${PROJECT_ROOT}`);
});
