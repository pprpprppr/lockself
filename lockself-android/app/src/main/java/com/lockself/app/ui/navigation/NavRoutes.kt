package com.lockself.app.ui.navigation

sealed class NavRoute(val route: String) {
    object Lock : NavRoute("lock")
    object Setup : NavRoute("setup")
    object VaultMain : NavRoute("vault_main")
    object Settings : NavRoute("settings")

    object RecordDetail : NavRoute("record_detail/{recordId}") {
        fun createRoute(recordId: Long) = "record_detail/$recordId"
    }

    object EditRecord : NavRoute("edit_record?recordId={recordId}&type={type}") {
        fun createRoute(recordId: Long? = null, type: String = "PASSWORD"): String {
            val idParam = recordId?.toString() ?: ""
            return "edit_record?recordId=$idParam&type=$type"
        }
    }

    object PasswordHistory : NavRoute("password_history/{recordId}") {
        fun createRoute(recordId: Long) = "password_history/$recordId"
    }
}
