from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.routing import APIRoute

from app.api.api_v1.router import api_router
from app.core.config import settings

app = FastAPI(title=settings.app_name)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(api_router)


@app.get("/")
def root():
    endpoints: list[dict] = []
    for route in app.routes:
        if not isinstance(route, APIRoute):
            continue
        path = route.path
        if not path.startswith("/api/v1"):
            continue
        tags = list(route.tags or [])
        endpoints.append(
            {
                "path": path,
                "methods": sorted(m for m in route.methods if m not in {"HEAD", "OPTIONS"}),
                "name": route.name,
                "tag": tags[0] if tags else None,
            }
        )
    endpoints.sort(key=lambda x: (x["tag"] or "", x["path"]))

    return {
        "service": settings.app_name,
        "mode": "api-only",
        "docs": "/docs",
        "openapi": "/openapi.json",
        "api_prefix": settings.api_prefix,
        "endpoint_count": len(endpoints),
        "endpoints": endpoints,
    }


@app.get("/health")
def health_check():
    return {"status": "ok", "service": settings.app_name, "mode": "api-only"}
