package com.lockself.app

import com.lockself.app.ui.navigation.NavRoute
import com.lockself.app.util.AppPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationAndPreferencesTest {

    @Test
    fun testNavRouteCreation() {
        val detailRoute = NavRoute.RecordDetail.createRoute(42L)
        assertEquals("record_detail/42", detailRoute)

        val editNewRoute = NavRoute.EditRecord.createRoute(recordId = null, type = "NOTE")
        assertEquals("edit_record?recordId=&type=NOTE", editNewRoute)

        val editExistingRoute = NavRoute.EditRecord.createRoute(recordId = 101L, type = "PASSWORD")
        assertEquals("edit_record?recordId=101&type=PASSWORD", editExistingRoute)

        val historyRoute = NavRoute.PasswordHistory.createRoute(77L)
        assertEquals("password_history/77", historyRoute)
    }

    @Test
    fun testAppPreferencesOptionArrays() {
        val lockOptions = AppPreferences.LOCK_TIMEOUT_OPTIONS
        assertEquals(9, lockOptions.size)
        assertTrue(lockOptions.any { it.first == 900 }) // Default 15 minutes
        assertTrue(lockOptions.any { it.first == -1 })  // Never

        val clipboardOptions = AppPreferences.CLIPBOARD_TIMEOUT_OPTIONS
        assertEquals(7, clipboardOptions.size)
        assertTrue(clipboardOptions.any { it.first == 600 }) // Default 10 minutes
        assertTrue(clipboardOptions.any { it.first == -1 })  // Never
    }
}
