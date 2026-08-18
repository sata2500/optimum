package tech.salev.optimum

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.salev.optimum.util.TimeUtils

class TimeUtilsTest {

    @Test
    fun testTimeSlotOverlapping_sameRange() {
        assertTrue(TimeUtils.isTimeSlotOverlapping("14:00", "14:30", "14:00", "14:30"))
    }

    @Test
    fun testTimeSlotOverlapping_subSlotInsideLargerLog() {
        // 2-minute slot inside 30-minute log
        assertTrue(TimeUtils.isTimeSlotOverlapping("14:02", "14:04", "14:00", "14:30"))
        assertTrue(TimeUtils.isTimeSlotOverlapping("14:00", "14:02", "14:00", "14:30"))
        assertTrue(TimeUtils.isTimeSlotOverlapping("14:28", "14:30", "14:00", "14:30"))
    }

    @Test
    fun testTimeSlotOverlapping_slotOutsideLog() {
        assertFalse(TimeUtils.isTimeSlotOverlapping("14:30", "14:32", "14:00", "14:30"))
        assertFalse(TimeUtils.isTimeSlotOverlapping("13:58", "14:00", "14:00", "14:30"))
        assertFalse(TimeUtils.isTimeSlotOverlapping("15:00", "15:30", "14:00", "14:30"))
    }

    @Test
    fun testTimeSlotOverlapping_midnightBoundary() {
        // Log ending at midnight 00:00
        assertTrue(TimeUtils.isTimeSlotOverlapping("23:30", "00:00", "23:00", "00:00"))
        assertTrue(TimeUtils.isTimeSlotOverlapping("23:58", "00:00", "23:00", "00:00"))
        assertFalse(TimeUtils.isTimeSlotOverlapping("00:00", "00:30", "23:00", "00:00"))
    }
}
