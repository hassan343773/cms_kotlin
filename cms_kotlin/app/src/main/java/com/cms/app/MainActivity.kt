package com.cms.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.cms.app.data.repository.AuthRepository
import com.cms.app.data.services.RetrofitClient
import com.cms.app.navigation.AppNavGraph
import com.cms.app.ui.theme.CMSTheme
import com.cms.app.ui.theme.SurfaceBg
import com.cms.app.utils.SessionManager
import com.cms.app.viewmodel.AuthViewModel
import com.cms.app.viewmodel.AuthViewModelFactory
import com.cms.app.viewmodel.ComplaintViewModel
import com.cms.app.viewmodel.ComplaintViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize session manager & Retrofit with auth interceptor
        sessionManager = SessionManager(applicationContext)
        RetrofitClient.init(sessionManager)

        // Create AuthViewModel with factory (needs repository)
        val authRepository = AuthRepository(sessionManager)
        authViewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(authRepository)
        )[AuthViewModel::class.java]

        setContent {
            CMSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SurfaceBg
                ) {
                    val navController = rememberNavController()
                    val complaintViewModel: ComplaintViewModel = viewModel(
                        factory = ComplaintViewModelFactory(application)
                    )

                    AppNavGraph(
                        navController      = navController,
                        authViewModel      = authViewModel,
                        complaintViewModel = complaintViewModel
                    )
                }
            }
        }
    }
}
