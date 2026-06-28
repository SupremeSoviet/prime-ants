from __future__ import annotations

import argparse
import json
import os
import threading
import time
import uuid
import urllib.error
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any, cast


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_HOST = "127.0.0.1"
DEFAULT_PORT = 11452
DEFAULT_MODEL = "glm-5.2"
DEFAULT_UPSTREAM = "https://api.z.ai/api/coding/paas/v4/chat/completions"
DEFAULT_AUTH_ENV_KEY = "ZAI_CODEX_PROXY_TOKEN"

THREADS: dict[str, list[dict[str, Any]]] = {}


def now_unix() -> int:
    return int(time.time())


def response_id() -> str:
    return "resp_" + uuid.uuid4().hex


def item_id(prefix: str) -> str:
    return prefix + "_" + uuid.uuid4().hex[:24]


def json_bytes(data: Any) -> bytes:
    return json.dumps(data, separators=(",", ":"), ensure_ascii=False).encode("utf-8")


def text_from_content(content: Any) -> str:
    if content is None:
        return ""
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        parts: list[str] = []
        for part in content:
            if isinstance(part, str):
                parts.append(part)
                continue
            if not isinstance(part, dict):
                continue
            value = part.get("text")
            if isinstance(value, str):
                parts.append(value)
                continue
            value = part.get("output_text")
            if isinstance(value, str):
                parts.append(value)
        return "\n".join(part for part in parts if part)
    if isinstance(content, dict):
        value = content.get("text")
        return value if isinstance(value, str) else ""
    return str(content)


def normalize_chat_role(role: Any) -> str:
    if role in ("system", "user", "assistant", "tool"):
        return str(role)
    if role == "developer":
        return "system"
    return "user"


def json_string(value: Any) -> str:
    if isinstance(value, str):
        return value
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"))


def response_input_to_messages(payload: dict[str, Any]) -> list[dict[str, Any]]:
    messages: list[dict[str, Any]] = []
    instructions = payload.get("instructions")
    if isinstance(instructions, str) and instructions.strip():
        messages.append({"role": "system", "content": instructions})

    inputs = payload.get("input")
    if isinstance(inputs, str):
        messages.append({"role": "user", "content": inputs})
        return messages
    if not isinstance(inputs, list):
        return messages

    for entry in inputs:
        if isinstance(entry, str):
            messages.append({"role": "user", "content": entry})
            continue
        if not isinstance(entry, dict):
            continue

        entry_type = entry.get("type")
        if entry_type == "function_call_output":
            output = entry.get("output", "")
            messages.append({
                "role": "tool",
                "tool_call_id": str(entry.get("call_id") or entry.get("id") or "call_unknown"),
                "content": json_string(output),
            })
            continue

        if entry_type == "function_call":
            call_id = str(entry.get("call_id") or entry.get("id") or "call_unknown")
            name = str(entry.get("name") or "unknown_tool")
            arguments = entry.get("arguments", "{}")
            messages.append({
                "role": "assistant",
                "content": None,
                "tool_calls": [{
                    "id": call_id,
                    "type": "function",
                    "function": {
                        "name": name,
                        "arguments": json_string(arguments),
                    },
                }],
            })
            continue

        role = entry.get("role")
        if isinstance(role, str):
            messages.append({"role": normalize_chat_role(role), "content": text_from_content(entry.get("content"))})

    return messages


def responses_tools_to_chat_tools(payload: dict[str, Any]) -> list[dict[str, Any]]:
    tools = payload.get("tools")
    if not isinstance(tools, list):
        return []
    result: list[dict[str, Any]] = []
    for tool in tools:
        if not isinstance(tool, dict):
            continue
        function = tool.get("function")
        if isinstance(function, dict):
            name = function.get("name") or tool.get("name")
            description = function.get("description") or tool.get("description", "")
            parameters = function.get("parameters") or tool.get("parameters") or tool.get("input_schema")
        else:
            name = tool.get("name")
            description = tool.get("description", "")
            parameters = tool.get("parameters") or tool.get("input_schema")
        parameters = parameters or {"type": "object", "properties": {}}
        if tool.get("type") in ("function", None) and isinstance(name, str):
            result.append({
                "type": "function",
                "function": {
                    "name": name,
                    "description": description if isinstance(description, str) else "",
                    "parameters": parameters,
                },
            })
            continue
    return result


def build_chat_request(payload: dict[str, Any], model: str) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    previous_id = payload.get("previous_response_id")
    previous = THREADS.get(previous_id, []) if isinstance(previous_id, str) else []
    messages = [dict(message) for message in previous]
    messages.extend(response_input_to_messages(payload))
    if not messages:
        messages.append({"role": "user", "content": ""})

    body: dict[str, Any] = {
        "model": payload.get("model") if isinstance(payload.get("model"), str) else model,
        "messages": messages,
        "stream": True,
    }
    max_tokens = payload.get("max_output_tokens")
    if isinstance(max_tokens, int) and max_tokens > 0:
        body["max_tokens"] = max_tokens
    temperature = payload.get("temperature")
    if isinstance(temperature, (int, float)):
        body["temperature"] = temperature
    tools = responses_tools_to_chat_tools(payload)
    if tools:
        body["tools"] = tools
    if isinstance(payload.get("parallel_tool_calls"), bool):
        body["parallel_tool_calls"] = payload["parallel_tool_calls"]
    return messages, body


def model_catalog(model: str) -> dict[str, Any]:
    base_instructions = (
        "You are Codex, a coding agent. Follow the full session instructions, "
        "use available tools carefully, keep edits scoped, and verify your work."
    )
    return {
        "models": [{
            "slug": model,
            "display_name": "Z.AI GLM 5.2",
            "description": "GLM 5.2 through a local Codex Responses compatibility proxy.",
            "default_reasoning_level": "high",
            "supported_reasoning_levels": [
                {"effort": "low", "description": "Lighter reasoning"},
                {"effort": "medium", "description": "Balanced reasoning"},
                {"effort": "high", "description": "Deeper reasoning"},
                {"effort": "xhigh", "description": "Maximum local loop budget"},
            ],
            "shell_type": "shell_command",
            "visibility": "list",
            "supported_in_api": True,
            "priority": 0,
            "additional_speed_tiers": [],
            "service_tiers": [],
            "availability_nux": None,
            "upgrade": None,
            "base_instructions": base_instructions,
            "model_messages": {
                "instructions_template": "{{ base_instructions }}\n\n{{ personality }}",
                "instructions_variables": {
                    "base_instructions": base_instructions,
                    "personality_default": "",
                    "personality_friendly": "",
                },
            },
            "supports_reasoning_summaries": False,
            "default_reasoning_summary": "none",
            "support_verbosity": False,
            "default_verbosity": "low",
            "apply_patch_tool_type": "freeform",
            "web_search_tool_type": "text_and_image",
            "truncation_policy": {"mode": "tokens", "limit": 10000},
            "supports_parallel_tool_calls": True,
            "supports_image_detail_original": False,
            "supports_search_tool": False,
            "use_responses_lite": False,
            "context_window": 1000000,
            "max_context_window": 1000000,
            "effective_context_window_percent": 90,
            "experimental_supported_tools": [],
            "input_modalities": ["text"],
        }]
    }


class ZaiProxyHandler(BaseHTTPRequestHandler):
    server_version = "ZaiCodexProxy/0.1"

    def log_message(self, format: str, *args: Any) -> None:
        cast(ZaiProxyServer, self.server).write_log(format % args)

    def send_json(self, status: int, data: Any) -> None:
        body = json_bytes(data)
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self) -> None:
        path = self.path.split("?", 1)[0]
        proxy = cast(ZaiProxyServer, self.server)
        if path in ("/health", "/v1/health"):
            self.send_json(200, {
                "status": "ok",
                "model": proxy.model,
                "has_api_key": bool(proxy.api_key),
                "has_proxy_auth_token": bool(proxy.auth_token),
            })
            return
        if path in ("/models", "/v1/models"):
            self.send_json(200, model_catalog(proxy.model))
            return
        self.send_json(404, {"error": {"message": f"Unknown path: {path}"}})

    def do_POST(self) -> None:
        path = self.path.split("?", 1)[0]
        if path not in ("/responses", "/v1/responses"):
            self.send_json(404, {"error": {"message": f"Unknown path: {path}"}})
            return
        proxy = cast(ZaiProxyServer, self.server)
        if not proxy.api_key:
            self.send_json(500, {"error": {"message": "ZAI_API_KEY is not set"}})
            return
        length = int(self.headers.get("Content-Length", "0"))
        try:
            payload = json.loads(self.rfile.read(length).decode("utf-8"))
        except Exception as exception:  # noqa: BLE001
            self.send_json(400, {"error": {"message": f"Invalid JSON: {exception}"}})
            return
        if not isinstance(payload, dict):
            self.send_json(400, {"error": {"message": "Request body must be a JSON object"}})
            return

        stream = payload.get("stream", True) is not False
        if stream:
            self.handle_streaming_response(payload)
        else:
            self.handle_json_response(payload)

    def upstream_request(self, body: dict[str, Any]) -> urllib.response.addinfourl:
        proxy = cast(ZaiProxyServer, self.server)
        request = urllib.request.Request(
            proxy.upstream_url,
            data=json_bytes(body),
            headers={
                "Authorization": f"Bearer {proxy.api_key}",
                "Content-Type": "application/json",
                "Accept": "text/event-stream",
                "Accept-Language": "en-US,en",
            },
            method="POST",
        )
        return urllib.request.urlopen(request, timeout=proxy.upstream_timeout)

    def handle_json_response(self, payload: dict[str, Any]) -> None:
        response = self.collect_upstream(payload)
        self.send_json(200, response)

    def handle_streaming_response(self, payload: dict[str, Any]) -> None:
        proxy = cast(ZaiProxyServer, self.server)
        response_id_value = response_id()
        created = now_unix()
        model = payload.get("model") if isinstance(payload.get("model"), str) else proxy.model
        self.send_response(200)
        self.send_header("Content-Type", "text/event-stream; charset=utf-8")
        self.send_header("Cache-Control", "no-cache")
        self.send_header("Connection", "close")
        self.end_headers()
        self.close_connection = True

        write_lock = threading.Lock()

        def emit(event: str, data: Any) -> None:
            with write_lock:
                self.wfile.write(f"event: {event}\n".encode("utf-8"))
                self.wfile.write(b"data: ")
                self.wfile.write(json_bytes(data))
                self.wfile.write(b"\n\n")
                self.wfile.flush()

        # GLM-5.2 can spend 10-40s on prompt processing + reasoning before the
        # first forwardable byte (huge agent prompts), and that whole window is
        # inside the blocking upstream urlopen/read. The proxy would otherwise send
        # codex nothing during it and codex drops the stream with IncompleteRead. A
        # background heartbeat writes an SSE comment every few seconds, from before
        # the upstream call through the whole response, so codex's read timer never
        # expires. All socket writes are serialized with emit() via write_lock.
        stop_heartbeat = threading.Event()

        def heartbeat() -> None:
            while not stop_heartbeat.wait(5.0):
                try:
                    with write_lock:
                        self.wfile.write(b": keep-alive\n\n")
                        self.wfile.flush()
                except OSError:
                    break

        base_response = self.empty_response(response_id_value, created, model, "in_progress")
        emit("response.created", {"type": "response.created", "response": base_response})
        emit("response.in_progress", {"type": "response.in_progress", "response": base_response})
        heartbeat_thread = threading.Thread(target=heartbeat, daemon=True)
        heartbeat_thread.start()
        try:
            response = self.collect_upstream(payload, response_id_value, created, emit)
        except Exception as exception:  # noqa: BLE001
            stop_heartbeat.set()
            heartbeat_thread.join(timeout=1.0)
            failed = self.empty_response(response_id_value, created, model, "failed")
            failed["error"] = {"message": str(exception)}
            emit("response.failed", {"type": "response.failed", "response": failed})
            return
        stop_heartbeat.set()
        heartbeat_thread.join(timeout=1.0)
        emit("response.completed", {"type": "response.completed", "response": response})
        with write_lock:
            self.wfile.write(b"data: [DONE]\n\n")
            self.wfile.flush()

    def collect_upstream(
        self,
        payload: dict[str, Any],
        response_id_value: str | None = None,
        created: int | None = None,
        emit: Any | None = None,
    ) -> dict[str, Any]:
        response_id_value = response_id_value or response_id()
        created = created or now_unix()
        proxy = cast(ZaiProxyServer, self.server)
        model = payload.get("model") if isinstance(payload.get("model"), str) else proxy.model
        messages, body = build_chat_request(payload, proxy.model)
        proxy.write_request_summary(payload, body)

        content_item_id = item_id("msg")
        content_started = False
        content = ""
        reasoning_content = ""
        tool_calls: dict[int, dict[str, Any]] = {}
        output_items: list[dict[str, Any]] = []

        def emit_text_start() -> None:
            nonlocal content_started
            if content_started:
                return
            content_started = True
            if emit is None:
                return
            emit("response.output_item.added", {
                "type": "response.output_item.added",
                "output_index": 0,
                "item": {"id": content_item_id, "type": "message", "status": "in_progress", "role": "assistant", "content": []},
            })
            emit("response.content_part.added", {
                "type": "response.content_part.added",
                "output_index": 0,
                "content_index": 0,
                "item_id": content_item_id,
                "part": {"type": "output_text", "text": "", "annotations": []},
            })

        def emit_tool_start(index: int, call: dict[str, Any]) -> None:
            if emit is None or call.get("started"):
                return
            call["started"] = True
            output_index = len([value for value in tool_calls.values() if value.get("started")]) - 1
            call["output_index"] = max(output_index, 0)
            emit("response.output_item.added", {
                "type": "response.output_item.added",
                "output_index": call["output_index"],
                "item": {
                    "id": call["item_id"],
                    "type": "function_call",
                    "status": "in_progress",
                    "call_id": call["call_id"],
                    "name": call.get("name") or "unknown_tool",
                    "arguments": "",
                },
            })

        try:
            upstream = self.upstream_request(body)
            # Stream incrementally (not upstream.read()) so deltas reach codex as
            # they arrive; the background heartbeat in handle_streaming_response
            # covers the long reasoning/TTFT gaps where no delta is forwarded.
            for raw_line in upstream:
                line = raw_line.decode("utf-8", errors="replace").strip()
                if not line or line.startswith(":") or not line.startswith("data:"):
                    continue
                data_text = line[5:].strip()
                if data_text == "[DONE]":
                    break
                try:
                    chunk = json.loads(data_text)
                except json.JSONDecodeError:
                    continue
                choices = chunk.get("choices")
                if not isinstance(choices, list) or not choices:
                    continue
                delta = choices[0].get("delta", {})
                if not isinstance(delta, dict):
                    continue
                text_delta = delta.get("content")
                if isinstance(text_delta, str) and text_delta:
                    emit_text_start()
                    content += text_delta
                    if emit is not None:
                        emit("response.output_text.delta", {
                            "type": "response.output_text.delta",
                            "output_index": 0,
                            "content_index": 0,
                            "item_id": content_item_id,
                            "delta": text_delta,
                        })
                reasoning_delta = delta.get("reasoning_content")
                if isinstance(reasoning_delta, str) and reasoning_delta:
                    reasoning_content += reasoning_delta
                delta_tool_calls = delta.get("tool_calls")
                if isinstance(delta_tool_calls, list):
                    for tool_delta in delta_tool_calls:
                        if not isinstance(tool_delta, dict):
                            continue
                        index = int(tool_delta.get("index", 0))
                        call = tool_calls.setdefault(index, {
                            "item_id": item_id("fc"),
                            "call_id": tool_delta.get("id") or f"call_{uuid.uuid4().hex[:24]}",
                            "name": "",
                            "arguments": "",
                            "started": False,
                        })
                        if isinstance(tool_delta.get("id"), str):
                            call["call_id"] = tool_delta["id"]
                        function = tool_delta.get("function")
                        if isinstance(function, dict):
                            if isinstance(function.get("name"), str):
                                call["name"] += function["name"]
                            if function.get("arguments") is not None:
                                argument_delta = json_string(function.get("arguments"))
                                if not call.get("started") and call.get("name"):
                                    emit_tool_start(index, call)
                                call["arguments"] += argument_delta
                                if emit is not None:
                                    emit("response.function_call_arguments.delta", {
                                        "type": "response.function_call_arguments.delta",
                                        "output_index": call.get("output_index", index),
                                        "item_id": call["item_id"],
                                        "delta": argument_delta,
                                    })
                        if not call.get("started") and call.get("name"):
                            emit_tool_start(index, call)
        except urllib.error.HTTPError as exception:
            body_text = exception.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"Z.AI upstream HTTP {exception.code}: {body_text[:500]}") from exception

        if not content and reasoning_content and not tool_calls:
            content = reasoning_content

        proxy.write_log(
            f"collected content_chars={len(content)} reasoning_chars={len(reasoning_content)} tool_calls={len(tool_calls)}"
        )

        if content:
            emit_text_start()

        if content_started:
            text_part = {"type": "output_text", "text": content, "annotations": []}
            item = {"id": content_item_id, "type": "message", "status": "completed", "role": "assistant", "content": [text_part]}
            if emit is not None:
                emit("response.output_text.done", {
                    "type": "response.output_text.done",
                    "output_index": 0,
                    "content_index": 0,
                    "item_id": content_item_id,
                    "text": content,
                })
                emit("response.content_part.done", {
                    "type": "response.content_part.done",
                    "output_index": 0,
                    "content_index": 0,
                    "item_id": content_item_id,
                    "part": text_part,
                })
                emit("response.output_item.done", {
                    "type": "response.output_item.done",
                    "output_index": 0,
                    "item": item,
                })
            output_items.append(item)

        for index in sorted(tool_calls):
            call = tool_calls[index]
            if not call.get("started"):
                emit_tool_start(index, call)
            item = {
                "id": call["item_id"],
                "type": "function_call",
                "status": "completed",
                "call_id": call["call_id"],
                "name": call.get("name") or "unknown_tool",
                "arguments": call.get("arguments") or "{}",
            }
            if emit is not None:
                emit("response.function_call_arguments.done", {
                    "type": "response.function_call_arguments.done",
                    "output_index": call.get("output_index", index),
                    "item_id": call["item_id"],
                    "arguments": item["arguments"],
                })
                emit("response.output_item.done", {
                    "type": "response.output_item.done",
                    "output_index": call.get("output_index", index),
                    "item": item,
                })
            output_items.append(item)

        if not output_items:
            text_part = {"type": "output_text", "text": "", "annotations": []}
            output_items.append({"id": content_item_id, "type": "message", "status": "completed", "role": "assistant", "content": [text_part]})

        response = {
            "id": response_id_value,
            "object": "response",
            "created_at": created,
            "status": "completed",
            "model": model,
            "output": output_items,
            "parallel_tool_calls": payload.get("parallel_tool_calls", True),
            "usage": None,
        }
        THREADS[response_id_value] = self.messages_after_response(messages, output_items)
        return response

    def messages_after_response(self, messages: list[dict[str, Any]], output_items: list[dict[str, Any]]) -> list[dict[str, Any]]:
        next_messages = [dict(message) for message in messages]
        text_content = ""
        tool_calls: list[dict[str, Any]] = []
        for item in output_items:
            if item.get("type") == "message":
                parts = item.get("content", [])
                if isinstance(parts, list):
                    text_content += text_from_content(parts)
            if item.get("type") == "function_call":
                tool_calls.append({
                    "id": item.get("call_id"),
                    "type": "function",
                    "function": {
                        "name": item.get("name"),
                        "arguments": item.get("arguments") or "{}",
                    },
                })
        if tool_calls:
            next_messages.append({"role": "assistant", "content": None, "tool_calls": tool_calls})
        else:
            next_messages.append({"role": "assistant", "content": text_content})
        return next_messages[-80:]

    def empty_response(self, response_id_value: str, created: int, model: str, status: str) -> dict[str, Any]:
        return {
            "id": response_id_value,
            "object": "response",
            "created_at": created,
            "status": status,
            "model": model,
            "output": [],
            "parallel_tool_calls": True,
            "usage": None,
        }


class ZaiProxyServer(ThreadingHTTPServer):
    def __init__(self, address: tuple[str, int], handler: type[BaseHTTPRequestHandler], args: argparse.Namespace) -> None:
        super().__init__(address, handler)
        self.model = args.model
        self.upstream_url = args.upstream_url
        self.upstream_timeout = args.upstream_timeout
        self.api_key = os.environ.get(args.env_key, "")
        self.auth_token = os.environ.get(args.auth_env_key, "")
        self.log_dir = Path(args.log_dir)
        self.log_dir.mkdir(parents=True, exist_ok=True)
        self.log_path = self.log_dir / "proxy.log"

    def write_log(self, message: str) -> None:
        line = f"{time.strftime('%Y-%m-%dT%H:%M:%S')} {message}\n"
        with self.log_path.open("a", encoding="utf-8") as handle:
            handle.write(line)

    def write_request_summary(self, payload: dict[str, Any], body: dict[str, Any]) -> None:
        tools = body.get("tools", [])
        summary = {
            "model": body.get("model"),
            "messages": len(body.get("messages", [])),
            "tools": len(tools) if isinstance(tools, list) else 0,
            "previous_response_id": payload.get("previous_response_id"),
            "stream": payload.get("stream", True),
        }
        self.write_log("request " + json.dumps(summary, separators=(",", ":")))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Local Codex Responses proxy for Z.AI GLM Coding Plan.")
    parser.add_argument("--host", default=DEFAULT_HOST)
    parser.add_argument("--port", type=int, default=DEFAULT_PORT)
    parser.add_argument("--model", default=DEFAULT_MODEL)
    parser.add_argument("--env-key", default="ZAI_API_KEY")
    parser.add_argument("--auth-env-key", default=DEFAULT_AUTH_ENV_KEY)
    parser.add_argument("--upstream-url", default=DEFAULT_UPSTREAM)
    parser.add_argument("--upstream-timeout", type=int, default=1800)
    parser.add_argument("--log-dir", default=str(ROOT / "build/zai-codex-proxy"))
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    server = ZaiProxyServer((args.host, args.port), ZaiProxyHandler, args)
    server.write_log(
        f"starting host={args.host} port={args.port} model={args.model} "
        f"has_api_key={bool(server.api_key)} has_proxy_auth_token={bool(server.auth_token)}"
    )
    print(f"Z.AI Codex proxy listening on http://{args.host}:{args.port}/v1")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        server.write_log("stopped")
        return 0
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
