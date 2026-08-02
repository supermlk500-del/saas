import { describe, expect, it } from 'vitest'
import { RealtimeHitTracker } from '@/inference/realtimeHitTracker'

describe('RealtimeHitTracker', () => {
  it('persists once after continuous hit threshold', () => {
    const tracker = new RealtimeHitTracker()

    expect(tracker.consume(true, 3)).toEqual({ hitStreak: 1, shouldPersist: false })
    expect(tracker.consume(true, 3)).toEqual({ hitStreak: 2, shouldPersist: false })
    expect(tracker.consume(true, 3)).toEqual({ hitStreak: 3, shouldPersist: true })
    expect(tracker.consume(true, 3)).toEqual({ hitStreak: 4, shouldPersist: false })
  })

  it('starts a new persist window after a normal frame', () => {
    const tracker = new RealtimeHitTracker()

    tracker.consume(true, 1)
    expect(tracker.consume(false, 1)).toEqual({ hitStreak: 0, shouldPersist: false })
    expect(tracker.consume(true, 1)).toEqual({ hitStreak: 1, shouldPersist: true })
  })
})
