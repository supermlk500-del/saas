export type EvidenceQueueTaskStatus = 'queued' | 'uploading' | 'succeeded' | 'failed' | 'dropped'

export type EvidenceQueueSnapshot = {
  queued: number
  uploading: number
  succeeded: number
  failed: number
  dropped: number
}

export type EvidenceQueueTask<T> = {
  id: string
  run: () => Promise<T>
  dispose?: () => void
}

type QueueRecord<T> = EvidenceQueueTask<T> & {
  attempts: number
  status: EvidenceQueueTaskStatus
}

export type EvidenceQueueStatusEvent<T> = {
  task: EvidenceQueueTask<T>
  status: EvidenceQueueTaskStatus
  attempts: number
  error?: unknown
  snapshot: EvidenceQueueSnapshot
}

export type EvidenceUploadQueueOptions<T> = {
  concurrency?: number
  maxPending?: number
  maxRetries?: number
  retryDelayMs?: number
  onStatus?: (event: EvidenceQueueStatusEvent<T>) => void
}

/**
 * Small, bounded, single-purpose queue for realtime evidence persistence.
 * The inference loop only enqueues immutable evidence tasks; network and
 * record-refresh work is deliberately kept outside the frame loop.
 */
export class EvidenceUploadQueue<T> {
  private readonly concurrency: number
  private readonly maxPending: number
  private readonly maxRetries: number
  private readonly retryDelayMs: number
  private readonly onStatus?: (event: EvidenceQueueStatusEvent<T>) => void
  private readonly pending: QueueRecord<T>[] = []
  private readonly knownTaskIds = new Set<string>()
  private readonly drainWaiters: Array<() => void> = []
  private active = 0
  private accepting = true
  private succeeded = 0
  private failed = 0
  private dropped = 0

  constructor(options: EvidenceUploadQueueOptions<T> = {}) {
    this.concurrency = Math.max(1, Math.min(2, Math.floor(options.concurrency ?? 1)))
    this.maxPending = Math.max(1, Math.floor(options.maxPending ?? 2))
    this.maxRetries = Math.max(0, Math.floor(options.maxRetries ?? 2))
    this.retryDelayMs = Math.max(0, Math.floor(options.retryDelayMs ?? 500))
    this.onStatus = options.onStatus
  }

  get snapshot(): EvidenceQueueSnapshot {
    return {
      queued: this.pending.length,
      uploading: this.active,
      succeeded: this.succeeded,
      failed: this.failed,
      dropped: this.dropped,
    }
  }

  get isIdle() {
    return this.pending.length === 0 && this.active === 0
  }

  enqueue(task: EvidenceQueueTask<T>) {
    if (this.knownTaskIds.has(task.id)) {
      task.dispose?.()
      return false
    }
    this.knownTaskIds.add(task.id)

    if (!this.accepting || this.pending.length >= this.maxPending) {
      this.dropped += 1
      this.emit({ task, status: 'dropped', attempts: 0 })
      task.dispose?.()
      return false
    }

    const record: QueueRecord<T> = { ...task, attempts: 0, status: 'queued' }
    this.pending.push(record)
    this.emit({ task: record, status: 'queued', attempts: 0 })
    void this.pump()
    return true
  }

  stopAccepting() {
    this.accepting = false
    while (this.pending.length) {
      const task = this.pending.shift()
      if (!task) continue
      task.status = 'dropped'
      this.dropped += 1
      task.dispose?.()
      this.emit({ task, status: 'dropped', attempts: task.attempts })
    }
    this.resolveDrainWaitersIfIdle()
  }

  resumeAccepting() {
    this.accepting = true
  }

  async drain(timeoutMs = 3_000) {
    if (this.isIdle) return true

    const drained = new Promise<boolean>((resolve) => {
      this.drainWaiters.push(() => resolve(true))
    })
    const timeout = new Promise<boolean>((resolve) => {
      globalThis.setTimeout(() => resolve(false), Math.max(0, timeoutMs))
    })
    return Promise.race([drained, timeout])
  }

  reset() {
    if (!this.isIdle) {
      throw new Error('Cannot reset a non-idle evidence upload queue')
    }
    this.accepting = true
    this.knownTaskIds.clear()
    this.succeeded = 0
    this.failed = 0
    this.dropped = 0
  }

  private async pump() {
    while (this.active < this.concurrency && this.pending.length) {
      const task = this.pending.shift()
      if (!task) return
      this.active += 1
      void this.run(task).finally(() => {
        this.active -= 1
        this.resolveDrainWaitersIfIdle()
        void this.pump()
      })
    }
  }

  private async run(task: QueueRecord<T>) {
    task.status = 'uploading'
    this.emit({ task, status: 'uploading', attempts: task.attempts })

    try {
      for (let retry = 0; retry <= this.maxRetries; retry += 1) {
        task.attempts = retry + 1
        try {
          await task.run()
          task.status = 'succeeded'
          this.succeeded += 1
          this.emit({ task, status: 'succeeded', attempts: task.attempts })
          return
        } catch (error) {
          if (retry >= this.maxRetries) {
            task.status = 'failed'
            this.failed += 1
            this.emit({ task, status: 'failed', attempts: task.attempts, error })
            return
          }
          await this.delay(this.retryDelayMs * (retry + 1))
        }
      }
    } finally {
      task.dispose?.()
    }
  }

  private delay(delayMs: number) {
    return new Promise<void>((resolve) => globalThis.setTimeout(resolve, delayMs))
  }

  private emit(event: Omit<EvidenceQueueStatusEvent<T>, 'snapshot'>) {
    this.onStatus?.({ ...event, snapshot: this.snapshot })
  }

  private resolveDrainWaitersIfIdle() {
    if (!this.isIdle) return
    while (this.drainWaiters.length) {
      this.drainWaiters.shift()?.()
    }
  }
}
