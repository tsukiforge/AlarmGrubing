package com.example

import com.example.ui.HealthTimeChecker
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testSameDaySchedule() {
        val startTime = "08:00"
        val endTime = "12:00"
        val scheduledDays = listOf("Sen", "Sel", "Rab") // Monday, Tuesday, Wednesday

        // Test Case 1: Within time and scheduled day
        assertTrue(
            "Should be active on Monday at 09:30",
            HealthTimeChecker.isScheduleActive(startTime, endTime, scheduledDays, "Sen", 570) // 09:30 is 570 min
        )

        // Test Case 2: Out of time range on scheduled day
        assertFalse(
            "Should be inactive on Monday at 13:00",
            HealthTimeChecker.isScheduleActive(startTime, endTime, scheduledDays, "Sen", 780) // 13:00 is 780 min
        )

        // Test Case 3: Within time range but unscheduled day
        assertFalse(
            "Should be inactive on Thursday at 09:30",
            HealthTimeChecker.isScheduleActive(startTime, endTime, scheduledDays, "Kam", 570)
        )
    }

    @Test
    fun testOvernightMidnightSchedule() {
        val startTime = "22:00" // 1320 min
        val endTime = "05:00"   // 300 min
        val scheduledDays = listOf("Sel") // Tuesday only

        // Test Case 1: Same day within range (Tuesday 23:00)
        assertTrue(
            "Should be active on Tuesday at 23:00 (Starts Tuesday)",
            HealthTimeChecker.isScheduleActive(startTime, endTime, scheduledDays, "Sel", 1380)
        )

        // Test Case 2: Spans past midnight (Wednesday 02:00)
        assertTrue(
            "Should be active on Wednesday at 02:00 (Started Tuesday)",
            HealthTimeChecker.isScheduleActive(startTime, endTime, scheduledDays, "Rab", 120)
        )

        // Test Case 3: Out of range past end time (Wednesday 06:00)
        assertFalse(
            "Should be inactive on Wednesday at 06:00",
            HealthTimeChecker.isScheduleActive(startTime, endTime, scheduledDays, "Rab", 360)
        )
    }

    @Test
    fun testAlmost24HourSchedule() {
        val startTime = "08:00" // 480 min
        val endTime = "07:59"   // 479 min
        val scheduledDays = listOf("Jum") // Friday only

        // Test Case 1: Same day early active (Friday 08:30)
        assertTrue(
            "Should be active on Friday at 08:30",
            HealthTimeChecker.isScheduleActive(startTime, endTime, scheduledDays, "Jum", 510)
        )

        // Test Case 2: Near end of same day (Friday 23:50)
        assertTrue(
            "Should be active on Friday at 23:50",
            HealthTimeChecker.isScheduleActive(startTime, endTime, scheduledDays, "Jum", 1430)
        )

        // Test Case 3: Next day morning before end time (Saturday 07:30)
        assertTrue(
            "Should be active on Saturday at 07:30",
            HealthTimeChecker.isScheduleActive(startTime, endTime, scheduledDays, "Sab", 450)
        )

        // Test Case 4: Next day morning after end time (Saturday 08:15)
        assertFalse(
            "Should be inactive on Saturday at 08:15",
            HealthTimeChecker.isScheduleActive(startTime, endTime, scheduledDays, "Sab", 495)
        )
    }
}
