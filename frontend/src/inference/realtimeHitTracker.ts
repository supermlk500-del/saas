export class RealtimeHitTracker {
  private hitStreak = 0
  private persistedForCurrentStreak = false

  consume(hasDefect: boolean, requiredFrames: number) {
    if (!hasDefect) {
      this.hitStreak = 0
      this.persistedForCurrentStreak = false
      return { hitStreak: 0, shouldPersist: false }
    }

    this.hitStreak += 1
    const threshold = Math.max(1, requiredFrames)
    const shouldPersist = !this.persistedForCurrentStreak && this.hitStreak >= threshold
    if (shouldPersist) {
      this.persistedForCurrentStreak = true
    }
    return { hitStreak: this.hitStreak, shouldPersist }
  }

  reset() {
    this.hitStreak = 0
    this.persistedForCurrentStreak = false
  }
}
