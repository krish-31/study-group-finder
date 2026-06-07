package com.studygroup.finder.core.navigation

/**
 * Sealed class representing every navigation destination in the app.
 *
 * Routes that require arguments contain a `{param}` placeholder in [route]
 * and expose a helper function to build the concrete route string.
 */
sealed class Screen(val route: String) {

    // ── Auth ────────────────────────────────────────
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")

    // ── Main ────────────────────────────────────────
    data object Home : Screen("home")
    data object Profile : Screen("profile")
    data object EditProfile : Screen("edit_profile")

    // ── Groups ──────────────────────────────────────
    data object GroupList : Screen("group_list")

    data object GroupDetail : Screen("group_detail/{groupId}") {
        const val ARG_GROUP_ID = "groupId"
        fun createRoute(groupId: String): String = "group_detail/$groupId"
    }

    data object CreateGroup : Screen("create_group")

    data object JoinRequests : Screen("join_requests/{groupId}") {
        const val ARG_GROUP_ID = "groupId"
        fun createRoute(groupId: String): String = "join_requests/$groupId"
    }

    // ── Search ──────────────────────────────────────
    data object Search : Screen("search")

    // ── Chat ────────────────────────────────────────
    data object Chat : Screen("chat/{groupId}") {
        const val ARG_GROUP_ID = "groupId"
        fun createRoute(groupId: String): String = "chat/$groupId"
    }

    // ── Sessions ────────────────────────────────────
    data object ScheduleSession : Screen("schedule_session/{groupId}") {
        const val ARG_GROUP_ID = "groupId"
        fun createRoute(groupId: String): String = "schedule_session/$groupId"
    }

    data object SessionDetail : Screen("session_detail")

    // ── Notifications ───────────────────────────────
    data object Notifications : Screen("notifications")

    // ── Reviews ─────────────────────────────────────
    data object Reviews : Screen("reviews/{groupId}") {
        const val ARG_GROUP_ID = "groupId"
        fun createRoute(groupId: String): String = "reviews/$groupId"
    }

    // ── Tracking ────────────────────────────────────
    data object ActivityTracking : Screen("activity_tracking")

    // ── Admin ───────────────────────────────────────
    data object AdminPanel : Screen("admin_panel")
}
