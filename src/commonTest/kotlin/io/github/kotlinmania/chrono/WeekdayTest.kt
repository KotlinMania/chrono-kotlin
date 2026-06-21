// port-lint: source src/weekday.rs
package io.github.kotlinmania.chrono

import kotlin.test.Test
import kotlin.test.assertEquals

class WeekdayTest {
    @Test
    fun testDaysSince() {
        for (i in 0..6) {
            val baseDay = Weekday.tryFrom(i).getOrThrow()

            assertEquals(baseDay.numDaysFromMonday(), baseDay.daysSince(Weekday.Mon))
            assertEquals(baseDay.numDaysFromSunday(), baseDay.daysSince(Weekday.Sun))

            assertEquals(0, baseDay.daysSince(baseDay))

            assertEquals(1, baseDay.daysSince(baseDay.pred()))
            assertEquals(2, baseDay.daysSince(baseDay.pred().pred()))
            assertEquals(3, baseDay.daysSince(baseDay.pred().pred().pred()))
            assertEquals(
                4,
                baseDay.daysSince(
                    baseDay
                        .pred()
                        .pred()
                        .pred()
                        .pred(),
                ),
            )
            assertEquals(
                5,
                baseDay.daysSince(
                    baseDay
                        .pred()
                        .pred()
                        .pred()
                        .pred()
                        .pred(),
                ),
            )
            assertEquals(
                6,
                baseDay.daysSince(
                    baseDay
                        .pred()
                        .pred()
                        .pred()
                        .pred()
                        .pred()
                        .pred(),
                ),
            )

            assertEquals(6, baseDay.daysSince(baseDay.succ()))
            assertEquals(5, baseDay.daysSince(baseDay.succ().succ()))
            assertEquals(4, baseDay.daysSince(baseDay.succ().succ().succ()))
            assertEquals(
                3,
                baseDay.daysSince(
                    baseDay
                        .succ()
                        .succ()
                        .succ()
                        .succ(),
                ),
            )
            assertEquals(
                2,
                baseDay.daysSince(
                    baseDay
                        .succ()
                        .succ()
                        .succ()
                        .succ()
                        .succ(),
                ),
            )
            assertEquals(
                1,
                baseDay.daysSince(
                    baseDay
                        .succ()
                        .succ()
                        .succ()
                        .succ()
                        .succ()
                        .succ(),
                ),
            )
        }
    }
}
