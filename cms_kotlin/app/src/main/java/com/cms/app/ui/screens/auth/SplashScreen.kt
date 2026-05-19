package com.cms.app.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cms.app.R
import com.cms.app.viewmodel.AuthState
import com.cms.app.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    authViewModel: AuthViewModel,
    onNavigateToDashboard: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val authState by authViewModel.state.collectAsState()
    val splashStartMs = remember { System.currentTimeMillis() }

    val infiniteTransition = rememberInfiniteTransition(label = "handRotation")
    val handRotationY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "handRotationY"
    )

    LaunchedEffect(Unit) {
        authViewModel.checkAuth()
    }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                val elapsed = System.currentTimeMillis() - splashStartMs
                val remaining = (8000L - elapsed).coerceAtLeast(0L)
                delay(remaining)
                onNavigateToDashboard()
            }
            is AuthState.Unauthenticated -> {
                val elapsed = System.currentTimeMillis() - splashStartMs
                val remaining = (8000L - elapsed).coerceAtLeast(0L)
                delay(remaining)
                onNavigateToLogin()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Text(
            text = "7EIZ",
            color = Color.White,
            fontSize = 54.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
        )

        Image(
            painter = painterResource(id = R.drawable.hands_dotted),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(360.dp)
                .graphicsLayer {
                    rotationY = handRotationY
                    cameraDistance = 18f * density
                }
                .padding(6.dp),
            contentScale = ContentScale.Fit
        )

        Text(
            text = "Complaint System",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 32.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
        )
    }
}
