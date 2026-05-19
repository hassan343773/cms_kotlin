package com.cms.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cms.app.ui.screens.auth.*
import com.cms.app.ui.screens.complaints.*
import com.cms.app.ui.screens.dashboard.DashboardScreen
import com.cms.app.ui.screens.notifications.NotificationsScreen
import com.cms.app.ui.screens.profile.MyProfileScreen
import com.cms.app.ui.screens.profile.ProfileDetailScreen
import com.cms.app.ui.screens.profile.ProfileDirectoryScreen
import com.cms.app.ui.screens.team.TeamTasksScreen
import com.cms.app.viewmodel.AuthViewModel
import com.cms.app.viewmodel.ComplaintViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    complaintViewModel: ComplaintViewModel
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val useGlobalComplaintQueue = currentUser?.canViewGlobalComplaintQueue == true
    val canModerateStatus = currentUser?.canModerateComplaintStatus == true
    val canDeleteComplaint = currentUser?.canDeleteComplaints == true
    val canManageAssignmentAndProgress = currentUser?.canManageAssignmentAndProgress == true
    val canBrowseProfileDirectory = currentUser?.canBrowseProfileDirectory == true

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                authViewModel.syncUserFromStorage()
                complaintViewModel.refreshComplaintsFromLastMode()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {

        // ── Splash ────────────────────────────────────────────────────────────
        composable(Routes.SPLASH) {
            SplashScreen(
                authViewModel = authViewModel,
                onNavigateToDashboard = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // ── Login ─────────────────────────────────────────────────────────────
        composable(Routes.LOGIN) {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        // ── Register ──────────────────────────────────────────────────────────
        composable(Routes.REGISTER) {
            RegisterScreen(
                authViewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // ── Dashboard ─────────────────────────────────────────────────────────
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                authViewModel = authViewModel,
                complaintViewModel = complaintViewModel,
                canBrowseProfileDirectory = canBrowseProfileDirectory,
                onNavigateToComplaints = { navController.navigate(Routes.COMPLAINTS) },
                onNavigateToTeamTasks = { navController.navigate(Routes.TEAM_TASKS) },
                onNavigateToDetail = { navController.navigate(Routes.COMPLAINT_DETAIL) },
                onNavigateToCreate = { navController.navigate(Routes.CREATE_COMPLAINT) },
                onNavigateToMyProfile = { navController.navigate(Routes.MY_PROFILE) },
                onNavigateToProfileDirectory = { navController.navigate(Routes.PROFILE_DIRECTORY) },
                onNavigateToNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.DASHBOARD) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(
                complaintViewModel = complaintViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // ── Complaints List ───────────────────────────────────────────────────
        composable(Routes.COMPLAINTS) {
            ComplaintsScreen(
                useGlobalComplaintQueue = useGlobalComplaintQueue,
                complaintViewModel = complaintViewModel,
                onNavigateToDetail = { navController.navigate(Routes.COMPLAINT_DETAIL) },
                onNavigateToCreate = { navController.navigate(Routes.CREATE_COMPLAINT) },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Complaint Detail ──────────────────────────────────────────────────
        composable(Routes.COMPLAINT_DETAIL) {
            ComplaintDetailScreen(
                canModerateStatus = canModerateStatus,
                canDeleteComplaint = canDeleteComplaint,
                canManageAssignmentAndProgress = canManageAssignmentAndProgress,
                currentUserName = currentUser?.username ?: "User",
                complaintViewModel = complaintViewModel,
                onNavigateToEdit = { navController.navigate(Routes.EDIT_COMPLAINT) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MY_PROFILE) {
            MyProfileScreen(
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PROFILE_DIRECTORY) {
            val complaints by complaintViewModel.complaints.collectAsState()
            val extras by complaintViewModel.extrasByComplaintId.collectAsState()
            ProfileDirectoryScreen(
                authViewModel = authViewModel,
                complaints = complaints,
                extrasByComplaintId = extras,
                onBack = { navController.popBackStack() },
                onOpenProfile = { username, role ->
                    navController.navigate(Routes.profileDetailRoute(username, role))
                }
            )
        }

        composable(
            route = Routes.PROFILE_DETAIL,
            arguments = listOf(
                navArgument("username") { type = NavType.StringType },
                navArgument("role") { type = NavType.StringType }
            )
        ) { entry ->
            val username = entry.arguments?.getString("username").orEmpty()
            val role = entry.arguments?.getString("role").orEmpty()
            ProfileDetailScreen(
                authViewModel = authViewModel,
                targetUsername = username,
                targetRole = role,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.TEAM_TASKS) {
            TeamTasksScreen(
                complaintViewModel = complaintViewModel,
                useGlobalComplaintQueue = useGlobalComplaintQueue,
                onBack = { navController.popBackStack() },
                onOpenComplaint = { c ->
                    complaintViewModel.setSelectedComplaint(c)
                    navController.navigate(Routes.COMPLAINT_DETAIL)
                }
            )
        }

        // ── Create Complaint ──────────────────────────────────────────────────
        composable(Routes.CREATE_COMPLAINT) {
            CreateEditComplaintScreen(
                isEdit = false,
                canManageAssignmentAndProgress = canManageAssignmentAndProgress,
                complaintViewModel = complaintViewModel,
                onSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Edit Complaint ────────────────────────────────────────────────────
        composable(Routes.EDIT_COMPLAINT) {
            CreateEditComplaintScreen(
                isEdit = true,
                canManageAssignmentAndProgress = canManageAssignmentAndProgress,
                complaintViewModel = complaintViewModel,
                onSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
