package squad.nononotaa.testsupport

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class MutableClock extends Clock {

    Instant now

    MutableClock(Instant now) {
        this.now = now
    }

    void advance(Duration duration) {
        now = now.plus(duration)
    }

    @Override
    ZoneId getZone() {
        ZoneId.of('UTC')
    }

    @Override
    Clock withZone(ZoneId zone) {
        this
    }

    @Override
    Instant instant() {
        now
    }
}
