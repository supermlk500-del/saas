from pydantic import BaseModel


class TimelineEvent(BaseModel):
    event_time: str
    event_type: str
    event_title: str
    detail: str
    ref_no: str


class TimelineFilesResponse(BaseModel):
    files: list[str]
