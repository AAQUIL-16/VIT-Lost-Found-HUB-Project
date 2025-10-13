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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

class SignupActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkTheme = isSystemInDarkTheme()

            // --- Define Dark & Light Mode Colors ---
            val textColor = if (isDarkTheme) Color(0xFF000000) else Color.White
            val buttonBg = if (isDarkTheme) Color(0xFF000000) else Color.White
            val backgroundGradient = if (isDarkTheme)
                Brush.verticalGradient(listOf(Color(0xFF121212), Color(0xFF000000)))
            else
                Brush.verticalGradient(listOf(PremiumAqua, PremiumDarkAqua))

            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var confirmPassword by remember { mutableStateOf("") }
            var showPassword by remember { mutableStateOf(false) }
            var showConfirmPassword by remember { mutableStateOf(false) }
            var message by remember { mutableStateOf("") }

            val scrollState = rememberScrollState()

            // Background gradient
            Box( modifier = Modifier .fillMaxSize() .background( brush = Brush.verticalGradient( colors = listOf(PremiumAqua, PremiumDarkAqua) ) ) .padding(24.dp) ){
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(Modifier.height(32.dp))

                    Text(
                        text = "VIT Lost & Found Hub",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(20.dp))

                    // --- Fixed Role Label ---
                    Text(
                        text = "Student",
                        color = PremiumAqua,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(buttonBg, RoundedCornerShape(12.dp))
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    )

                    Spacer(Modifier.height(20.dp))

                    // --- Email Field ---
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Student Email (@vitstudent.ac.in)", color = textColor) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = message.contains("email"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = textColor,
                            focusedBorderColor = textColor,
                            unfocusedBorderColor = textColor
                        )
                    )
                    if (message.contains("email")) {
                        Text(
                            text = message,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .align(Alignment.Start)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // --- Password Field ---
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", color = textColor) },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            val icon = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = "Toggle Password Visibility",
                                    tint = textColor
                                )
                            }
                        },
                        isError = message.contains("Password"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = textColor,
                            focusedBorderColor = textColor,
                            unfocusedBorderColor = textColor
                        )
                    )
                    if (message.contains("Password")) {
                        Text(
                            text = message,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .align(Alignment.Start)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // --- Confirm Password Field ---
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password", color = textColor) },
                        visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            val icon = if (showConfirmPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = "Toggle Confirm Password Visibility",
                                    tint = textColor
                                )
                            }
                        },
                        isError = message.contains("match"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = textColor,
                            focusedBorderColor = textColor,
                            unfocusedBorderColor = textColor
                        )
                    )
                    if (message.contains("match")) {
                        Text(
                            text = message,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .align(Alignment.Start)
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // --- Sign Up Button ---
                    Button(
                        onClick = {
                            message = ""
                            when {
                                email.isBlank() -> message = "Enter your email"
                                !email.endsWith("@vitstudent.ac.in") -> message = "Only VIT student emails are allowed"
                                password.isBlank() -> message = "Enter your password"
                                confirmPassword.isBlank() -> message = "Enter confirm password"
                                password.length < 8 -> message = "Password must be at least 8 characters"
                                !password.matches(Regex(".[A-Z].")) -> message = "Password must contain an uppercase letter"
                                !password.matches(Regex(".[a-z].")) -> message = "Password must contain a lowercase letter"
                                !password.matches(Regex(".[0-9].")) -> message = "Password must contain a number"
                                !password.matches(Regex(".[!@#\$%^&+=?-].")) -> message = "Password must contain a special character"
                                password != confirmPassword -> message = "Password didn't match"
                                else -> {
                                    auth.createUserWithEmailAndPassword(email, password)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                auth.currentUser?.sendEmailVerification()
                                                    ?.addOnCompleteListener { emailTask ->
                                                        if (emailTask.isSuccessful) {
                                                            Toast.makeText(
                                                                this@SignupActivity,
                                                                "Verification email sent! Please verify before logging in.",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                            val intent = Intent(
                                                                this@SignupActivity,
                                                                VerifyEmailActivity::class.java
                                                            )
                                                            intent.putExtra("role", "Student")
                                                            intent.putExtra("email", email)
                                                            startActivity(intent)
                                                            finish()
                                                        } else {
                                                            message = emailTask.exception?.message
                                                                ?: "Failed to send verification email"
                                                        }
                                                    }
                                            } else {
                                                message =
                                                    task.exception?.message ?: "Signup failed"
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
                            contentColor = PremiumAqua
                        )
                    ) {
                        Text("Sign Up", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(12.dp))

                    // --- Back to Login ---
                    Text(
                        text = "Back to Login",
                        color = textColor,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable {
                            startActivity(Intent(this@SignupActivity, LoginActivity::class.java))
                            finish()
                        },
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
