@file:Suppress("DEPRECATION")

package com.cms.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cms.app.ui.components.FormLabel
import com.cms.app.ui.theme.*
import com.cms.app.viewmodel.AuthState
import com.cms.app.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val authState by authViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("CUSTOMER") }

    var usernameError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var confirmError by remember { mutableStateOf("") }

    val isLoading = authState is AuthState.Loading
    val roles = listOf(
        "CUSTOMER" to Icons.Rounded.Person,
        "ADMIN"    to Icons.Rounded.AdminPanelSettings,
        "SUPPORT"  to Icons.Rounded.HeadsetMic,
        "MANAGER"  to Icons.Rounded.ManageAccounts,
        "CEO"      to Icons.Rounded.Stars
    )

    LaunchedEffect(authState) {
        when (val s = authState) {
            is AuthState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                authViewModel.clearError()
            }
            else -> {}
        }
    }

    fun validate(): Boolean {
        usernameError = if (username.length < 3) "Minimum 3 characters" else ""
        passwordError = if (password.length < 6) "Minimum 6 characters" else ""
        confirmError  = if (password != confirmPassword) "Passwords do not match" else ""
        return usernameError.isEmpty() && passwordError.isEmpty() && confirmError.isEmpty()
    }

    Scaffold(snackbarHost = {
        SnackbarHost(snackbarHostState) { data ->
            Snackbar(snackbarData = data, containerColor = DangerRed, contentColor = Color.White,
                shape = RoundedCornerShape(10.dp))
        }
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceBg)
                .verticalScroll(rememberScrollState())
                .padding(padding)
        ) {
            // ── Gradient Header ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(Brush.linearGradient(listOf(Secondary, Color(0xFF9061F9))))
                    .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 28.dp)
            ) {
                Column {
                    IconButton(
                        onClick = onNavigateToLogin,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Create Account", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text("Join to manage your complaints", fontSize = 15.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }

            // ── Form ─────────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(24.dp)) {
                Spacer(Modifier.height(6.dp))

                FormLabel("Username")
                OutlinedTextField(
                    value = username, onValueChange = { username = it; usernameError = "" },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Choose a username", color = TextHint) },
                    leadingIcon = { Icon(Icons.Rounded.Person, null, tint = TextHint) },
                    isError = usernameError.isNotEmpty(),
                    supportingText = { if (usernameError.isNotEmpty()) Text(usernameError, color = DangerRed) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Secondary, unfocusedBorderColor = BorderColor)
                )

                Spacer(Modifier.height(14.dp))
                FormLabel("Password")
                OutlinedTextField(
                    value = password, onValueChange = { password = it; passwordError = "" },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Create a password", color = TextHint) },
                    leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = TextHint) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null, tint = TextHint)
                        }
                    },
                    isError = passwordError.isNotEmpty(),
                    supportingText = { if (passwordError.isNotEmpty()) Text(passwordError, color = DangerRed) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Secondary, unfocusedBorderColor = BorderColor)
                )

                Spacer(Modifier.height(14.dp))
                FormLabel("Confirm Password")
                OutlinedTextField(
                    value = confirmPassword, onValueChange = { confirmPassword = it; confirmError = "" },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Repeat your password", color = TextHint) },
                    leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = TextHint) },
                    isError = confirmError.isNotEmpty(),
                    supportingText = { if (confirmError.isNotEmpty()) Text(confirmError, color = DangerRed) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Secondary, unfocusedBorderColor = BorderColor)
                )

                // Role Selection Grid
                FormLabel("Select Role")
                LazyVerticalGrid(
                    columns             = GridCells.Fixed(2),
                    modifier            = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp),
                    userScrollEnabled     = false
                ) {
                    items(roles) { (role, icon) ->
                        RoleOption(
                            role       = role,
                            icon       = icon,
                            isSelected = selectedRole == role,
                            onClick    = { selectedRole = role }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Selected role indicator
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Secondary.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        null,
                        tint     = Secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text       = "Selected: $selectedRole",
                        fontSize   = 13.sp,
                        color      = Secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { if (validate()) authViewModel.register(username.trim(), password, selectedRole) { onRegisterSuccess() } },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Create Account", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Already have an account? ", color = TextSecondary, fontSize = 14.sp)
                    TextButton(onClick = onNavigateToLogin, contentPadding = PaddingValues(0.dp)) {
                        Text("Login", color = Secondary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
@Composable
private fun RoleOption(
    role: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Secondary.copy(alpha = 0.10f) else CardBg)
            .border(
                width  = if (isSelected) 1.5.dp else 0.5.dp,
                color  = if (isSelected) Secondary else BorderColor,
                shape  = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint     = if (isSelected) Secondary else TextHint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text       = role,
                fontSize   = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color      = if (isSelected) Secondary else TextSecondary
            )
        }
    }
}
