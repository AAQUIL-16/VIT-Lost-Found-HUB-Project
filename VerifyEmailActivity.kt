package com.example.myfirstapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

class VerifyEmailActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val email = intent.getStringExtra("email") ?: ""

        setContent {
            var message by remember { mutableStateOf("") }
            var isLoading by remember { mutableStateOf(false) }
            var isResending by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(PremiumAqua, PremiumDarkAqua)
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Verify Your Email",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "A verification link has been sent to:\n$email\n\nPlease verify your email, then click Continue.",
                        color = Color.White,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(30.dp))

                    // --- Continue Button ---
                    Button(
                        onClick = {
                            isLoading = true
                            message = ""

                            auth.currentUser?.reload()?.addOnCompleteListener {
                                val user = auth.currentUser
                                if (user != null && user.isEmailVerified) {
                                    Toast.makeText(
                                        this@VerifyEmailActivity,
                                        "Email verified successfully!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    val intent = Intent(this@VerifyEmailActivity, LoginActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    message = "Please verify your email before continuing."
                                }
                                isLoading = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = PremiumAqua
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = PremiumAqua,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text("Continue", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // --- Resend Email Button ---
                    TextButton(
                        onClick = {
                            isResending = true
                            message = ""
                            auth.currentUser?.sendEmailVerification()
                                ?.addOnSuccessListener {
                                    Toast.makeText(
                                        this@VerifyEmailActivity,
                                        "Verification email resent! Check your inbox.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                ?.addOnFailureListener {
                                    message = "Failed to resend: ${it.message}"
                                }
                                ?.addOnCompleteListener {
                                    isResending = false
                                }
                        }
                    ) {
                        if (isResending) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text("Resend Verification Email", color = Color.White)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    if (message.isNotEmpty()) {
                        Text(
                            text = message,
                            color = Color.Red,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
