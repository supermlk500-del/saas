from fastapi import APIRouter, Depends
from sqlalchemy import delete, select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models import (
    Machine,
    MachineCapabilityParam,
    MachineCapabilitySnapshot,
    MachineShiftAvailability,
    SchedulePlanItem,
    SchedulePlanVersion,
    ScheduleUnassignedTask,
)
from app.schemas.machine import (
    MachineCapabilityParamUpsert,
    MachineCreate,
    MachineRead,
    MachineResetResult,
    MachineShiftAvailabilityUpsert,
    MachineUpdate,
    SnapshotGenerateRequest,
    SnapshotRead,
)
from app.services.machine_service import MachineService

router = APIRouter(prefix="/machines", tags=["blackbox-machine"])


@router.get("", response_model=list[MachineRead])
def list_machines(db: Session = Depends(get_db)):
    return MachineService.list_machines(db)


@router.post("", response_model=MachineRead)
def create_machine(payload: MachineCreate, db: Session = Depends(get_db)):
    return MachineService.create_machine(db, payload)


@router.get("/{machine_id}", response_model=MachineRead)
def get_machine(machine_id: int, db: Session = Depends(get_db)):
    return MachineService.get_machine(db, machine_id)


@router.put("/{machine_id}", response_model=MachineRead)
def update_machine(machine_id: int, payload: MachineUpdate, db: Session = Depends(get_db)):
    return MachineService.update_machine(db, machine_id, payload)


@router.post("/{machine_id}/capability-param")
def upsert_capability_param(
    machine_id: int,
    payload: MachineCapabilityParamUpsert,
    db: Session = Depends(get_db),
):
    return MachineService.upsert_param(db, machine_id, payload)


@router.post("/{machine_id}/shift-availability")
def upsert_shift_availability(
    machine_id: int,
    payload: MachineShiftAvailabilityUpsert,
    db: Session = Depends(get_db),
):
    return MachineService.upsert_shift(db, machine_id, payload)


@router.post("/snapshots/generate")
def generate_snapshot(payload: SnapshotGenerateRequest, db: Session = Depends(get_db)):
    return MachineService.generate_snapshot(db, payload)


@router.get("/snapshots", response_model=list[SnapshotRead])
def list_snapshots(db: Session = Depends(get_db)):
    return MachineService.list_snapshots(db)


@router.get("/snapshots/{snapshot_no}", response_model=list[SnapshotRead])
def get_snapshot(snapshot_no: str, db: Session = Depends(get_db)):
    return MachineService.get_snapshot(db, snapshot_no)


@router.post("/reset", response_model=MachineResetResult)
def reset_machines(db: Session = Depends(get_db)):
    plan_ids = list(db.scalars(select(SchedulePlanVersion.id)).all())
    cleared_plan_items = 0
    cleared_unassigned = 0
    if plan_ids:
        cleared_plan_items = db.execute(
            delete(SchedulePlanItem).where(SchedulePlanItem.plan_version_id.in_(plan_ids))
        ).rowcount or 0
        cleared_unassigned = db.execute(
            delete(ScheduleUnassignedTask).where(ScheduleUnassignedTask.plan_version_id.in_(plan_ids))
        ).rowcount or 0
    cleared_plan_versions = db.execute(delete(SchedulePlanVersion)).rowcount or 0

    machine_ids = list(db.scalars(select(Machine.id)).all())
    cleared_params = 0
    cleared_snapshots = 0
    cleared_shifts = 0
    cleared_machines = 0
    if machine_ids:
        cleared_params = db.execute(
            delete(MachineCapabilityParam).where(MachineCapabilityParam.machine_id.in_(machine_ids))
        ).rowcount or 0
        cleared_snapshots = db.execute(
            delete(MachineCapabilitySnapshot).where(MachineCapabilitySnapshot.machine_id.in_(machine_ids))
        ).rowcount or 0
        cleared_shifts = db.execute(
            delete(MachineShiftAvailability).where(MachineShiftAvailability.machine_id.in_(machine_ids))
        ).rowcount or 0
        cleared_machines = db.execute(delete(Machine).where(Machine.id.in_(machine_ids))).rowcount or 0
    db.commit()
    return {
        "cleared_machines": cleared_machines,
        "cleared_snapshots": cleared_snapshots,
        "cleared_params": cleared_params,
        "cleared_shifts": cleared_shifts,
        "cleared_plan_versions": cleared_plan_versions,
        "cleared_plan_items": cleared_plan_items,
        "cleared_unassigned": cleared_unassigned,
    }
