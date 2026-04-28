package com.cms.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.cms.app.ui.screens.auth.*
import com.cms.app.ui.screens.complaints.*
import com.cms.app.ui.screens.dashboard.DashboardScreen
import com.cms.app.viewmodel.AuthViewModel
import com.cms.app.viewmodel.ComplaintViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    complaintViewModel: ComplaintViewModel
) {
    val isAdmin by authViewModel.currentUser.collectAsState()

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
                onNavigateToComplaints = { navController.navigate(Routes.COMPLAINTS) },
                onNavigateToDetail = { navController.navigate(Routes.COMPLAINT_DETAIL) },
                onNavigateToCreate = { navController.navigate(Routes.CREATE_COMPLAINT) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.DASHBOARD) { inclusive = true }
                    }
                }
            )
        }

        // ── Complaints List ───────────────────────────────────────────────────
        composable(Routes.COMPLAINTS) {
            ComplaintsScreen(
                isAdmin = isAdmin?.isAdmin == true,
                complaintViewModel = complaintViewModel,
                onNavigateToDetail = { navController.navigate(Routes.COMPLAINT_DETAIL) },
                onNavigateToCreate = { navController.navigate(Routes.CREATE_COMPLAINT) },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Complaint Detail ──────────────────────────────────────────────────
        composable(Routes.COMPLAINT_DETAIL) {
            ComplaintDetailScreen(
                isAdmin = isAdmin?.isAdmin == true,
                complaintViewModel = complaintViewModel,
                onNavigateToEdit = { navController.navigate(Routes.EDIT_COMPLAINT) },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Create Complaint ──────────────────────────────────────────────────
        composable(Routes.CREATE_COMPLAINT) {
            CreateEditComplaintScreen(
                isEdit = false,
                complaintViewModel = complaintViewModel,
                onSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Edit Complaint ────────────────────────────────────────────────────
        composable(Routes.EDIT_COMPLAINT) {
            CreateEditComplaintScreen(
                isEdit = true,
                complaintViewModel = complaintViewModel,
                onSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
