from pathlib import Path

from fastapi import APIRouter, HTTPException

from app.schemas.timeline import TimelineEvent, TimelineFilesResponse
from app.services.timeline_service import (
    discover_timeline_files,
    load_summary_for_timeline,
    load_timeline_events,
)

router = APIRouter(prefix="/timeline", tags=["timeline-replay"])
OUTPUT_DIR = Path("数据")


@router.get("/files", response_model=TimelineFilesResponse)
def list_timeline_files():
    files = discover_timeline_files(OUTPUT_DIR)
    return {"files": [x.name for x in files]}


@router.get("/events", response_model=list[TimelineEvent])
def timeline_events(file: str):
    csv_file = OUTPUT_DIR / file
    if not csv_file.exists():
        raise HTTPException(status_code=404, detail="timeline file not found")
    return load_timeline_events(csv_file)


@router.get("/summary")
def timeline_summary(file: str):
    summary = load_summary_for_timeline(OUTPUT_DIR, file)
    if summary is None:
        raise HTTPException(status_code=404, detail="timeline summary not found")
    return summary
