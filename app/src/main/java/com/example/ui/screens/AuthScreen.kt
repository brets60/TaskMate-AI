package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.auth.FirebaseAuthManager
import com.example.data.model.UserEntity
import com.example.ui.Screen
import com.example.ui.TaskMateViewModel
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

enum class AuthMode {
    SIGN_IN,
    REGISTER
}

data class PasswordStrength(
    val hasMinLength: Boolean,
    val hasUppercase: Boolean,
    val hasLowercase: Boolean,
    val hasDigit: Boolean,
    val hasSpecialChar: Boolean
) {
    val score: Int
        get() {
            var s = 0
            if (hasMinLength) s++
            if (hasUppercase) s++
            if (hasLowercase) s++
            if (hasDigit) s++
            if (hasSpecialChar) s++
            return s
        }

    val isStrong: Boolean
        get() = hasMinLength && hasUppercase && hasLowercase && hasDigit && hasSpecialChar

    val label: String
        get() = when (score) {
            0, 1 -> "Very Weak"
            2 -> "Weak"
            3 -> "Fair"
            4 -> "Good"
            5 -> "Strong & Secure"
            else -> "Strong"
        }

    val color: Color
        get() = when (score) {
            0, 1 -> PriorityUrgent
            2 -> Color(0xFFF97316)
            3 -> AccentAmber
            4 -> PrimaryBlue
            5 -> AccentEmerald
            else -> AccentEmerald
        }
}

fun evaluatePasswordStrength(password: String): PasswordStrength {
    return PasswordStrength(
        hasMinLength = password.length >= 8,
        hasUppercase = password.any { it.isUpperCase() },
        hasLowercase = password.any { it.isLowerCase() },
        hasDigit = password.any { it.isDigit() },
        hasSpecialChar = password.any { !it.isLetterOrDigit() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: TaskMateViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val auth = remember { FirebaseAuth.getInstance() }

    var firebaseUser by remember { mutableStateOf<FirebaseUser?>(auth.currentUser) }
    var currentMode by remember { 
        mutableStateOf(if (auth.currentUser != null) AuthMode.SIGN_IN else AuthMode.REGISTER) 
    }

    BackHandler(enabled = firebaseUser != null) {
        viewModel.navigateBack()
    }

    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val passwordStrength = remember(passwordInput) {
        evaluatePasswordStrength(passwordInput)
    }

    // Auto-detect auth state changes
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { fbAuth ->
            firebaseUser = fbAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (firebaseUser != null) "Account & Security" else if (currentMode == AuthMode.REGISTER) "Register First" else "Sign In", 
                        fontWeight = FontWeight.Bold, 
                        color = TextPrimaryLight 
                    ) 
                },
                navigationIcon = {
                    if (firebaseUser != null) {
                        IconButton(onClick = { viewModel.navigateBack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimaryLight)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(68.dp),
                shape = CircleShape,
                color = PastelBlueBg
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (firebaseUser != null) Icons.Default.CheckCircle else Icons.Default.LockReset,
                        contentDescription = null,
                        tint = if (firebaseUser != null) AccentEmerald else PrimaryBlueDark,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Text(
                text = if (firebaseUser != null) "Welcome Back!" else if (currentMode == AuthMode.SIGN_IN) "Log In to TaskMate" else "Create Your Account",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryLight,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (firebaseUser != null) {
                    "Your tasks and reminders are securely backed up."
                } else if (currentMode == AuthMode.SIGN_IN) {
                    "Enter your credentials or continue with Google to access your tasks."
                } else {
                    "Register with email and a strong password to sync your tasks securely."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryLight,
                textAlign = TextAlign.Center
            )

            errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PastelRoseBg),
                    border = BorderStroke(1.dp, PastelRoseBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = PriorityUrgent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = error, style = MaterialTheme.typography.bodySmall, color = PastelRoseText)
                    }
                }
            }

            successMessage?.let { success ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PastelMintBg),
                    border = BorderStroke(1.dp, PastelMintBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentEmerald)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = success, style = MaterialTheme.typography.bodySmall, color = PastelMintText)
                    }
                }
            }

            if (firebaseUser != null) {
                // Authenticated State Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, BorderLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(60.dp),
                            shape = CircleShape,
                            color = PastelMintBg
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = AccentEmerald,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Signed In Active",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PastelMintText
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = firebaseUser?.displayName ?: "TaskMate User",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryLight
                        )

                        Text(
                            text = firebaseUser?.email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryLight
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { viewModel.navigateTo(Screen.Home) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Go to Dashboard", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                successMessage = null
                                FirebaseAuthManager.signOut(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onSignOutComplete = {
                                        isLoading = false
                                        firebaseUser = null
                                        currentMode = AuthMode.REGISTER
                                        viewModel.clearBackStack()
                                        Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
                                    },
                                    scope = coroutineScope
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sign Out", color = PriorityUrgent)
                        }
                    }
                }
            } else {
                // Mode Toggle Tabs: Log In / Register
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceVariantLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (currentMode == AuthMode.SIGN_IN) SurfaceLight else Color.Transparent,
                            border = if (currentMode == AuthMode.SIGN_IN) BorderStroke(1.dp, BorderLight) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    currentMode = AuthMode.SIGN_IN
                                    errorMessage = null
                                    successMessage = null
                                }
                        ) {
                            Text(
                                text = "Log In",
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = TextAlign.Center,
                                fontWeight = if (currentMode == AuthMode.SIGN_IN) FontWeight.Bold else FontWeight.Medium,
                                color = if (currentMode == AuthMode.SIGN_IN) PrimaryBlueDark else TextSecondaryLight
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (currentMode == AuthMode.REGISTER) SurfaceLight else Color.Transparent,
                            border = if (currentMode == AuthMode.REGISTER) BorderStroke(1.dp, BorderLight) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    currentMode = AuthMode.REGISTER
                                    errorMessage = null
                                    successMessage = null
                                }
                        ) {
                            Text(
                                text = "Register",
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = TextAlign.Center,
                                fontWeight = if (currentMode == AuthMode.REGISTER) FontWeight.Bold else FontWeight.Medium,
                                color = if (currentMode == AuthMode.REGISTER) PrimaryBlueDark else TextSecondaryLight
                            )
                        }
                    }
                }

                // Credentials Form Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, BorderLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (currentMode == AuthMode.REGISTER) {
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text("Full Name") },
                                placeholder = { Text("e.g. Alex Rivera") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondaryLight) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("name_input_field"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = BackgroundLight,
                                    focusedContainerColor = SurfaceLight,
                                    unfocusedBorderColor = BorderLight,
                                    focusedBorderColor = PrimaryBlue
                                )
                            )
                        }

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Email Address") },
                            placeholder = { Text("you@example.com") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TextSecondaryLight) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_input_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = BackgroundLight,
                                focusedContainerColor = SurfaceLight,
                                unfocusedBorderColor = BorderLight,
                                focusedBorderColor = PrimaryBlue
                            )
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text(if (currentMode == AuthMode.REGISTER) "Create Strong Password" else "Password") },
                            placeholder = { Text("••••••••") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondaryLight) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = TextSecondaryLight
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = if (currentMode == AuthMode.REGISTER) ImeAction.Next else ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                                onDone = { focusManager.clearFocus() }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = BackgroundLight,
                                focusedContainerColor = SurfaceLight,
                                unfocusedBorderColor = BorderLight,
                                focusedBorderColor = PrimaryBlue
                            )
                        )

                        // Registration Specific: Strong Password Meter & Requirements
                        if (currentMode == AuthMode.REGISTER) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceVariantLight)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Password Strength:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondaryLight,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = passwordStrength.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = passwordStrength.color
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                LinearProgressIndicator(
                                    progress = { (passwordStrength.score / 5f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = passwordStrength.color,
                                    trackColor = BorderLight
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Strong Password Requirements:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimaryLight
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                PasswordRequirementRow(
                                    satisfied = passwordStrength.hasMinLength,
                                    text = "At least 8 characters"
                                )
                                PasswordRequirementRow(
                                    satisfied = passwordStrength.hasUppercase,
                                    text = "At least 1 uppercase letter (A-Z)"
                                )
                                PasswordRequirementRow(
                                    satisfied = passwordStrength.hasLowercase,
                                    text = "At least 1 lowercase letter (a-z)"
                                )
                                PasswordRequirementRow(
                                    satisfied = passwordStrength.hasDigit,
                                    text = "At least 1 number (0-9)"
                                )
                                PasswordRequirementRow(
                                    satisfied = passwordStrength.hasSpecialChar,
                                    text = "At least 1 special symbol (!@#\$%^&*)"
                                )
                            }

                            OutlinedTextField(
                                value = confirmPasswordInput,
                                onValueChange = { confirmPasswordInput = it },
                                label = { Text("Confirm Password") },
                                placeholder = { Text("••••••••") },
                                leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = TextSecondaryLight) },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("confirm_password_input_field"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = BackgroundLight,
                                    focusedContainerColor = SurfaceLight,
                                    unfocusedBorderColor = BorderLight,
                                    focusedBorderColor = PrimaryBlue
                                )
                            )
                        }

                        // Submit Button (Register or Sign In)
                        Button(
                            onClick = {
                                errorMessage = null
                                successMessage = null
                                focusManager.clearFocus()

                                val email = emailInput.trim()
                                val password = passwordInput

                                if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                    errorMessage = "Please enter a valid email address."
                                    return@Button
                                }

                                if (currentMode == AuthMode.REGISTER) {
                                    if (!passwordStrength.isStrong) {
                                        errorMessage = "Password does not meet the strong password requirements."
                                        return@Button
                                    }
                                    if (password != confirmPasswordInput) {
                                        errorMessage = "Passwords do not match."
                                        return@Button
                                    }

                                    isLoading = true
                                    val name = nameInput.trim().ifEmpty { email.substringBefore("@") }
                                    FirebaseAuthManager.registerWithEmail(
                                        email = email,
                                        password = password,
                                        displayName = name,
                                        onSuccess = { user ->
                                            isLoading = false
                                            firebaseUser = user
                                            viewModel.updateUserSettings(
                                                UserEntity(
                                                    userId = user.uid,
                                                    displayName = name,
                                                    email = user.email ?: email
                                                )
                                            )
                                            Toast.makeText(context, "Account created successfully! Welcome $name", Toast.LENGTH_SHORT).show()
                                            viewModel.clearBackStack()
                                            viewModel.navigateTo(Screen.Home)
                                        },
                                        onError = { err ->
                                            isLoading = false
                                            errorMessage = err
                                        },
                                        scope = coroutineScope
                                    )
                                } else {
                                    if (password.isBlank()) {
                                        errorMessage = "Please enter your password."
                                        return@Button
                                    }

                                    isLoading = true
                                    FirebaseAuthManager.signInWithEmail(
                                        email = email,
                                        password = password,
                                        onSuccess = { user ->
                                            isLoading = false
                                            firebaseUser = user
                                            val displayName = user.displayName ?: email.substringBefore("@")
                                            viewModel.updateUserSettings(
                                                UserEntity(
                                                    userId = user.uid,
                                                    displayName = displayName,
                                                    email = user.email ?: email
                                                )
                                            )
                                            Toast.makeText(context, "Signed in as $displayName", Toast.LENGTH_SHORT).show()
                                            viewModel.clearBackStack()
                                            viewModel.navigateTo(Screen.Home)
                                        },
                                        onError = { err ->
                                            isLoading = false
                                            errorMessage = err
                                        },
                                        scope = coroutineScope
                                    )
                                }
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_auth_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (currentMode == AuthMode.REGISTER) Icons.Default.PersonAdd else Icons.Default.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (currentMode == AuthMode.REGISTER) "Register with Strong Password" else "Log In",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        // Divider
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = BorderLight)
                            Text(
                                text = " or ",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryLight,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = BorderLight)
                        }

                        // One-tap Google Sign-In Button
                        OutlinedButton(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                successMessage = null
                                FirebaseAuthManager.onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = { user ->
                                        isLoading = false
                                        firebaseUser = user
                                        viewModel.updateUserSettings(
                                            UserEntity(
                                                userId = user.uid,
                                                displayName = user.displayName ?: "Google User",
                                                email = user.email ?: ""
                                            )
                                        )
                                        Toast.makeText(context, "Signed in as ${user.displayName ?: user.email}", Toast.LENGTH_SHORT).show()
                                        viewModel.clearBackStack()
                                        viewModel.navigateTo(Screen.Home)
                                    },
                                    onAuthError = { err ->
                                        isLoading = false
                                        errorMessage = err
                                    },
                                    scope = coroutineScope,
                                    onAuthCancelled = {
                                        isLoading = false
                                    }
                                )
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderLight),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("google_signin_button")
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Continue with Google", fontWeight = FontWeight.SemiBold, color = TextPrimaryLight, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordRequirementRow(
    satisfied: Boolean,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = if (satisfied) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (satisfied) AccentEmerald else TextSecondaryLight,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (satisfied) AccentEmerald else TextSecondaryLight,
            fontWeight = if (satisfied) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
