import { describe, expect, it, vi } from 'vitest'
import { EvidenceUploadQueue } from '@/inference/evidenceUploadQueue'

describe('EvidenceUploadQueue', () => {
  it('keeps realtime uploads bounded and serial', async () => {
    let active = 0
    let maxActive = 0
    const queue = new EvidenceUploadQueue<void>({ concurrency: 1, maxPending: 2 })

    const run = async () => {
      active += 1
      maxActive = Math.max(maxActive, active)
      await new Promise((resolve) => setTimeout(resolve, 5))
      active -= 1
    }

    expect(queue.enqueue({ id: 'one', run })).toBe(true)
    expect(queue.enqueue({ id: 'two', run })).toBe(true)
    expect(queue.enqueue({ id: 'three', run })).toBe(true)
    expect(queue.enqueue({ id: 'four', run })).toBe(false)

    await expect(queue.drain(500)).resolves.toBe(true)
    expect(maxActive).toBe(1)
    expect(queue.snapshot).toMatchObject({ queued: 0, uploading: 0, succeeded: 3, dropped: 1 })
  })

  it('retries failed tasks and deduplicates event ids', async () => {
    const run = vi.fn()
      .mockRejectedValueOnce(new Error('temporary'))
      .mockResolvedValueOnce(undefined)
    const queue = new EvidenceUploadQueue<void>({ retryDelayMs: 0, maxRetries: 1 })

    expect(queue.enqueue({ id: 'event-1', run })).toBe(true)
    expect(queue.enqueue({ id: 'event-1', run })).toBe(false)

    await expect(queue.drain(500)).resolves.toBe(true)
    expect(run).toHaveBeenCalledTimes(2)
    expect(queue.snapshot.failed).toBe(0)
    expect(queue.snapshot.succeeded).toBe(1)
  })

  it('stops accepting pending work and releases dropped resources', async () => {
    const dispose = vi.fn()
    const queue = new EvidenceUploadQueue<void>({ concurrency: 1, maxPending: 2 })
    let releaseActive: (() => void) | undefined
    const blocker = new Promise<void>((resolve) => {
      releaseActive = resolve
    })

    queue.enqueue({ id: 'active', run: () => blocker })
    queue.enqueue({ id: 'pending', run: async () => undefined, dispose })
    queue.stopAccepting()

    expect(dispose).toHaveBeenCalledTimes(1)
    expect(queue.snapshot.queued).toBe(0)
    releaseActive?.()
    await expect(queue.drain(500)).resolves.toBe(true)
  })
})
