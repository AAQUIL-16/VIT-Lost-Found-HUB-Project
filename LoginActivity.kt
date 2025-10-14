package com.example.myfirstapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        setContent {
            val context = LocalContext.current
            val focusManager = LocalFocusManager.current
            val isDarkTheme = isSystemInDarkTheme()

            val textColor = if (isDarkTheme) Color(0xFF000000) else Color.White
            val buttonBg = if (isDarkTheme) Color(0xFF000000) else Color.White
            val roleButtonBgSelected = if (isDarkTheme) Color(0xFF000000) else Color.White
            val roleButtonBgUnselected =
                if (isDarkTheme) Color(0x33000000) else Color(0xFF006D77).copy(alpha = 0.3f)

            val backgroundGradient =
                Brush.verticalGradient(listOf(PremiumAqua, PremiumDarkAqua))

            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var showPassword by remember { mutableStateOf(false) }
            var selectedRole by remember { mutableStateOf("Student") }
            var isLoading by remember { mutableStateOf(false) }

            var showForgotDialog by remember { mutableStateOf(false) }
            var forgotEmail by remember { mutableStateOf("") }
            var sendingReset by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = backgroundGradient)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(modifier = Modifier.height(40.dp))

                    Text(
                        text = "VIT Lost & Found HUB",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { selectedRole = "Student" },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedRole == "Student") roleButtonBgSelected else roleButtonBgUnselected,
                                contentColor = if (selectedRole == "Student") Color(PremiumAqua) else textColor
                            )
                        ) { Text("Student") }

                        Button(
                            onClick = { selectedRole = "Admin" },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedRole == "Admin") roleButtonBgSelected else roleButtonBgUnselected,
                                contentColor = if (selectedRole == "Admin") Color(PremiumAqua) else textColor
                            )
                        ) { Text("Admin") }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = {
                            Text(
                                if (selectedRole == "Student") "Student Email (@vitstudent.ac.in)"
                                else "Admin Email (@vit.ac.in)",
                                color = textColor
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(color = textColor),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent, RoundedCornerShape(12.dp)),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = textColor,
                            unfocusedBorderColor = textColor,
                            cursorColor = textColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", color = textColor) },
                        textStyle = LocalTextStyle.current.copy(color = textColor),
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val icon = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(imageVector = icon, contentDescription = "Toggle Password Visibility", tint = textColor)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent, RoundedCornerShape(12.dp)),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = textColor,
                            unfocusedBorderColor = textColor,
                            cursorColor = textColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedRole == "Student") {
                        Text(
                            text = "Forgot Password?",
                            modifier = Modifier
                                .align(Alignment.End)
                                .clickable {
                                    showForgotDialog = true
                                    forgotEmail = ""
                                    sendingReset = false
                                },
                            color = textColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ✅ Login button with proper error handling
                    Button(
                        onClick = {
                            focusManager.clearFocus()

                            val trimmedEmail = email.trim().lowercase()

                            // Basic validation
                            if (trimmedEmail.isEmpty()) {
                                Toast.makeText(context, "Enter your email", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            if (password.isEmpty()) {
                                Toast.makeText(context, "Enter your password", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            if (selectedRole == "Student" && !trimmedEmail.endsWith("@vitstudent.ac.in")) {
                                Toast.makeText(context, "Only VIT student emails are allowed", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            if (selectedRole == "Admin" && !trimmedEmail.endsWith("@vit.ac.in")) {
                                Toast.makeText(context, "Only VIT admin emails are allowed", Toast.LENGTH_LONG).show()
                                return@Button
                            }

                            isLoading = true

                            // Firebase authentication
                            auth.signInWithEmailAndPassword(trimmedEmail, password)
                                .addOnCompleteListener { task ->
                                    isLoading = false

                                    if (task.isSuccessful) {
                                        val user = auth.currentUser
                                        if (user != null) {
                                            // Check if email is verified for students
                                            if (selectedRole == "Student" && !user.isEmailVerified) {
                                                Toast.makeText(
                                                    context,
                                                    "Please verify your email before logging in.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                auth.signOut()
                                                return@addOnCompleteListener
                                            }

                                            // Login successful - navigate to appropriate screen
                                            if (selectedRole == "Student") {
                                                startActivity(Intent(this@LoginActivity, StudentHomeActivity::class.java))
                                            } else {
                                                startActivity(Intent(this@LoginActivity, AdminHomeActivity::class.java))
                                            }
                                            finish()
                                        }
                                    } else {
                                        // Handle specific Firebase errors
                                        val exception = task.exception
                                        val errorMessage = exception?.message ?: "Login failed"

                                        when {
                                            errorMessage.contains("no user record", ignoreCase = true) ||
                                                    errorMessage.contains("there is no user", ignoreCase = true) ||
                                                    errorMessage.contains("user not found", ignoreCase = true) -> {
                                                Toast.makeText(
                                                    context,
                                                    "Email ID doesn't exist",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                            errorMessage.contains("password is invalid", ignoreCase = true) ||
                                                    errorMessage.contains("wrong password", ignoreCase = true) ||
                                                    errorMessage.contains("incorrect password", ignoreCase = true) -> {
                                                Toast.makeText(
                                                    context,
                                                    "Incorrect password",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                            errorMessage.contains("network error", ignoreCase = true) -> {
                                                Toast.makeText(
                                                    context,
                                                    "Network error. Please check your internet connection",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                            else -> {
                                                Toast.makeText(
                                                    context,
                                                    "Login failed: ${exception?.localizedMessage}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonBg,
                            contentColor = Color(0xFF006D77)
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF006D77),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Login", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedRole == "Student") {
                        Text(
                            text = "Don't have an account? Sign up",
                            modifier = Modifier.clickable {
                                startActivity(Intent(this@LoginActivity, SignupActivity::class.java))
                            },
                            color = textColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Forgot Password Dialog
                if (showForgotDialog) {
                    var forgotEmailError by remember { mutableStateOf("") }

                    AlertDialog(
                        onDismissRequest = {
                            showForgotDialog = false
                            forgotEmail = ""
                            sendingReset = false
                            forgotEmailError = ""
                        },
                        containerColor = Color(0xFF83C5BE),
                        title = {
                            Text(
                                "Forgot Password",
                                color = textColor,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column {
                                OutlinedTextField(
                                    value = forgotEmail,
                                    onValueChange = {
                                        forgotEmail = it
                                        forgotEmailError = ""
                                    },
                                    label = { Text("Enter your VIT email", color = textColor) },
                                    textStyle = LocalTextStyle.current.copy(color = textColor),
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = KeyboardType.Email
                                    ),
                                    singleLine = true,
                                    isError = forgotEmailError.isNotEmpty(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = textColor,
                                        unfocusedBorderColor = textColor,
                                        cursorColor = textColor,
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor,
                                        errorBorderColor = Color.Red
                                    )
                                )
                                if (forgotEmailError.isNotEmpty()) {
                                    Text(
                                        text = forgotEmailError,
                                        color = Color.Red,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val trimmed = forgotEmail.trim().lowercase()

                                    if (trimmed.isEmpty()) {
                                        forgotEmailError = "Email cannot be empty"
                                        return@TextButton
                                    }
                                    if (!trimmed.endsWith("@vit.ac.in") && !trimmed.endsWith("@vitstudent.ac.in")) {
                                        forgotEmailError = "Please enter a valid VIT email"
                                        return@TextButton
                                    }

                                    sendingReset = true
                                    auth.sendPasswordResetEmail(trimmed)
                                        .addOnCompleteListener { task ->
                                            sendingReset = false
                                            if (task.isSuccessful) {
                                                Toast.makeText(
                                                    context,
                                                    "Password reset email sent to $trimmed",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                showForgotDialog = false
                                                forgotEmail = ""
                                                forgotEmailError = ""
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    task.exception?.message ?: "Failed to send email",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                },
                                enabled = !sendingReset
                            ) {
                                Text(
                                    if (sendingReset) "Sending..." else "Send Reset Link",
                                    color = textColor
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showForgotDialog = false
                                    forgotEmail = ""
                                    sendingReset = false
                                    forgotEmailError = ""
                                }
                            ) { Text("Cancel", color = textColor) }
                        }
                    )
                }
            }
        }
    }
}
