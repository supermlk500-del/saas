from datetime import date, datetime

from fastapi import HTTPException
from sqlalchemy import and_, desc, select
from sqlalchemy.orm import Session

from app.core.status import MachineStatus, SnapshotStatus
from app.models import (
    Machine,
    MachineCapabilityParam,
    MachineCapabilitySnapshot,
    MachineShiftAvailability,
    ScheduleRunLog,
)
from app.schemas.machine import (
    MachineCapabilityParamUpsert,
    MachineCreate,
    MachineShiftAvailabilityUpsert,
    MachineUpdate,
    SnapshotGenerateRequest,
)


class MachineService:
    @staticmethod
    def list_machines(db: Session) -> list[Machine]:
        stmt = select(Machine).order_by(Machine.id.desc())
        return list(db.scalars(stmt).all())

    @staticmethod
    def get_machine(db: Session, machine_id: int) -> Machine:
        machine = db.get(Machine, machine_id)
        if not machine:
            raise HTTPException(status_code=404, detail="机台不存在")
        return machine

    @staticmethod
    def list_machine_params(db: Session, machine_id: int) -> list[MachineCapabilityParam]:
        stmt = (
            select(MachineCapabilityParam)
            .where(MachineCapabilityParam.machine_id == machine_id)
            .order_by(desc(MachineCapabilityParam.updated_at), desc(MachineCapabilityParam.id))
        )
        return list(db.scalars(stmt).all())

    @staticmethod
    def list_machine_shifts(db: Session, machine_id: int) -> list[MachineShiftAvailability]:
        stmt = (
            select(MachineShiftAvailability)
            .where(MachineShiftAvailability.machine_id == machine_id)
            .order_by(desc(MachineShiftAvailability.effective_date), MachineShiftAvailability.shift_code)
        )
        return list(db.scalars(stmt).all())

    @staticmethod
    def create_machine(db: Session, payload: MachineCreate) -> Machine:
        exists = db.scalar(select(Machine).where(Machine.machine_code == payload.machine_code))
        if exists:
            raise HTTPException(status_code=400, detail="机台编号已存在")

        machine = Machine(
            machine_code=payload.machine_code,
            machine_name=payload.machine_name,
            machine_type=payload.machine_type,
            status=payload.status,
            remark=payload.remark,
            created_by=payload.created_by,
            updated_by=payload.created_by,
        )
        db.add(machine)
        db.commit()
        db.refresh(machine)
        return machine

    @staticmethod
    def update_machine(db: Session, machine_id: int, payload: MachineUpdate) -> Machine:
        machine = MachineService.get_machine(db, machine_id)
        for field, value in payload.model_dump(exclude_unset=True).items():
            setattr(machine, field, value)
        db.commit()
        db.refresh(machine)
        return machine

    @staticmethod
    def upsert_param(db: Session, machine_id: int, payload: MachineCapabilityParamUpsert) -> MachineCapabilityParam:
        MachineService.get_machine(db, machine_id)
        stmt = select(MachineCapabilityParam).where(
            and_(
                MachineCapabilityParam.machine_id == machine_id,
                MachineCapabilityParam.param_version == payload.param_version,
            )
        )
        record = db.scalar(stmt)
        if not record:
            record = MachineCapabilityParam(machine_id=machine_id, param_version=payload.param_version)
            db.add(record)

        record.speed_value = payload.speed_value
        record.speed_unit = payload.speed_unit
        record.changeover_loss = payload.changeover_loss
        record.stability_score = payload.stability_score
        record.is_active = payload.is_active
        record.updated_by = payload.operator
        if not record.created_by:
            record.created_by = payload.operator

        db.commit()
        db.refresh(record)
        return record

    @staticmethod
    def upsert_shift(db: Session, machine_id: int, payload: MachineShiftAvailabilityUpsert) -> MachineShiftAvailability:
        MachineService.get_machine(db, machine_id)
        stmt = select(MachineShiftAvailability).where(
            and_(
                MachineShiftAvailability.machine_id == machine_id,
                MachineShiftAvailability.shift_code == payload.shift_code,
                MachineShiftAvailability.effective_date == payload.effective_date,
            )
        )
        record = db.scalar(stmt)
        if not record:
            record = MachineShiftAvailability(
                machine_id=machine_id,
                shift_code=payload.shift_code,
                effective_date=payload.effective_date,
            )
            db.add(record)

        record.available_flag = payload.available_flag
        record.unavailable_reason = payload.unavailable_reason
        record.updated_by = payload.operator
        if not record.created_by:
            record.created_by = payload.operator

        db.commit()
        db.refresh(record)
        return record

    @staticmethod
    def list_snapshots(db: Session) -> list[MachineCapabilitySnapshot]:
        stmt = select(MachineCapabilitySnapshot).order_by(
            MachineCapabilitySnapshot.generated_at.desc(),
            MachineCapabilitySnapshot.id.desc(),
        )
        return list(db.scalars(stmt).all())

    @staticmethod
    def get_snapshot(db: Session, snapshot_no: str) -> list[MachineCapabilitySnapshot]:
        stmt = (
            select(MachineCapabilitySnapshot)
            .where(MachineCapabilitySnapshot.snapshot_no == snapshot_no)
            .order_by(MachineCapabilitySnapshot.machine_id)
        )
        rows = list(db.scalars(stmt).all())
        if not rows:
            raise HTTPException(status_code=404, detail="快照不存在")
        return rows

    @staticmethod
    def _log(
        db: Session,
        *,
        log_type: str,
        business_no: str,
        operation_name: str,
        operation_result: str,
        operator: str,
        error_message: str | None = None,
    ) -> None:
        db.add(
            ScheduleRunLog(
                log_type=log_type,
                business_no=business_no,
                operation_name=operation_name,
                operation_result=operation_result,
                error_message=error_message,
                operator=operator,
                operation_time=datetime.utcnow(),
            )
        )
        db.commit()

    @staticmethod
    def generate_snapshot(db: Session, payload: SnapshotGenerateRequest) -> dict:
        today = date.today()
        machines = MachineService.list_machines(db)
        if not machines:
            MachineService._log(
                db,
                log_type="machine_snapshot",
                business_no=payload.snapshot_no,
                operation_name="generate_snapshot",
                operation_result="failed",
                operator=payload.generated_by,
                error_message="无机台数据",
            )
            raise HTTPException(status_code=400, detail="无机台数据，无法生成快照")

        candidate_rows: list[dict] = []
        failures: list[str] = []

        for machine in machines:
            if machine.status in {MachineStatus.INACTIVE, MachineStatus.STOPPED}:
                continue

            param_stmt = (
                select(MachineCapabilityParam)
                .where(
                    and_(
                        MachineCapabilityParam.machine_id == machine.id,
                        MachineCapabilityParam.is_active.is_(True),
                    )
                )
                .order_by(desc(MachineCapabilityParam.updated_at), desc(MachineCapabilityParam.id))
            )
            param = db.scalar(param_stmt)
            if not param:
                failures.append(f"{machine.machine_code}:缺少有效能力参数")
                continue

            shift_stmt = select(MachineShiftAvailability).where(
                and_(
                    MachineShiftAvailability.machine_id == machine.id,
                    MachineShiftAvailability.effective_date == today,
                    MachineShiftAvailability.available_flag.is_(True),
                )
            )
            shift = db.scalar(shift_stmt)
            if not shift:
                failures.append(f"{machine.machine_code}:当日无可用班次")
                continue

            candidate_rows.append(
                {
                    "machine": machine,
                    "param": param,
                }
            )

        if failures:
            error_message = "; ".join(failures)
            MachineService._log(
                db,
                log_type="machine_snapshot",
                business_no=payload.snapshot_no,
                operation_name="generate_snapshot",
                operation_result="failed",
                operator=payload.generated_by,
                error_message=error_message,
            )
            raise HTTPException(status_code=400, detail=f"快照生成失败: {error_message}")

        if not candidate_rows:
            MachineService._log(
                db,
                log_type="machine_snapshot",
                business_no=payload.snapshot_no,
                operation_name="generate_snapshot",
                operation_result="failed",
                operator=payload.generated_by,
                error_message="无候选机台",
            )
            raise HTTPException(status_code=400, detail="无候选机台，无法生成快照")

        existing = db.scalar(select(MachineCapabilitySnapshot).where(MachineCapabilitySnapshot.snapshot_no == payload.snapshot_no))
        if existing:
            raise HTTPException(status_code=400, detail="快照版本号已存在")

        for row in candidate_rows:
            machine = row["machine"]
            param = row["param"]
            db.add(
                MachineCapabilitySnapshot(
                    snapshot_no=payload.snapshot_no,
                    machine_id=machine.id,
                    machine_code=machine.machine_code,
                    param_version=param.param_version,
                    snapshot_status=SnapshotStatus.GENERATED,
                    available_flag=True,
                    speed_value=param.speed_value,
                    changeover_loss=param.changeover_loss,
                    stability_score=param.stability_score,
                    generated_at=datetime.utcnow(),
                    generated_by=payload.generated_by,
                )
            )

        db.commit()
        MachineService._log(
            db,
            log_type="machine_snapshot",
            business_no=payload.snapshot_no,
            operation_name="generate_snapshot",
            operation_result="success",
            operator=payload.generated_by,
            error_message=None,
        )
        return {
            "snapshot_no": payload.snapshot_no,
            "candidate_count": len(candidate_rows),
            "status": SnapshotStatus.GENERATED,
        }
