"""AI assistant services for scheduling explanations.

Configuration is loaded from environment variables or `.env`:

- `LLM_ENABLED=true`
- `LLM_API_KEY=<YOUR_KEY_HERE>`
- `LLM_BASE_URL=https://api.deepseek.com/v1`
- `LLM_MODEL=deepseek-chat`

The assistant is intentionally read-only. It explains unassigned orders and
summarizes KPI reports, but never writes schedule plans or participates in the
solver loop.
"""
