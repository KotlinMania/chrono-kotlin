// port-lint: source src/weekday.rs
package io.github.kotlinmania.chrono

import io.github.kotlinmania.serdetest.Token
import io.github.kotlinmania.serdetest.assertDeTokens
import io.github.kotlinmania.serdetest.assertDeTokensError
import io.github.kotlinmania.serdetest.assertSerTokens
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

    // Upstream test_formatting_alignment is unported: Rust Formatter::pad width/alignment specifiers do not apply to Kotlin toString().

    @Test
    fun testSerdeSerialize() {
        val cases =
            listOf(
                Weekday.Mon to "Mon",
                Weekday.Tue to "Tue",
                Weekday.Wed to "Wed",
                Weekday.Thu to "Thu",
                Weekday.Fri to "Fri",
                Weekday.Sat to "Sat",
                Weekday.Sun to "Sun",
            )

        for ((weekday, expectedStr) in cases) {
            assertSerTokens(weekday, listOf(Token.Str(expectedStr)))
        }
    }

    @Test
    fun testSerdeDeserialize() {
        val cases =
            listOf(
                "mon" to Weekday.Mon,
                "MONDAY" to Weekday.Mon,
                "MonDay" to Weekday.Mon,
                "mOn" to Weekday.Mon,
                "tue" to Weekday.Tue,
                "tuesday" to Weekday.Tue,
                "wed" to Weekday.Wed,
                "wednesday" to Weekday.Wed,
                "thu" to Weekday.Thu,
                "thursday" to Weekday.Thu,
                "fri" to Weekday.Fri,
                "friday" to Weekday.Fri,
                "sat" to Weekday.Sat,
                "saturday" to Weekday.Sat,
                "sun" to Weekday.Sun,
                "sunday" to Weekday.Sun,
            )

        for ((str, expectedWeekday) in cases) {
            assertDeTokens(expectedWeekday, Weekday, listOf(Token.Str(str)))
        }

        val errors = listOf("not a weekday", "monDAYs", "mond", "thur", "thurs")

        for (str in errors) {
            assertDeTokensError(
                Weekday,
                listOf(Token.Str(str)),
                "short or long weekday names expected",
            )
        }
    }

    // Upstream test_rkyv_validation is unported: rkyv zero-copy archival is a Rust-specific serialization crate.
}
