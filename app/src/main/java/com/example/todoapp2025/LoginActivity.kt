package com.example.todoapp2025

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LoginScreen(
                onLogin = {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("isGuest", false)
                    startActivity(intent)
                    finish()
                },
                onGuest = {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("isGuest", true)
                    startActivity(intent)
                    finish()
                }
            )
        }
    }
}

@Composable
fun LoginScreen(
    onLogin: () -> Unit,
    onGuest: () -> Unit
) {
    var email by remember { mutableStateOf("test@user.com") }
    var password by remember { mutableStateOf("test") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text("Login", style = MaterialTheme.typography.headlineMedium)

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(Modifier.height(24.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onLogin() }
            ) {
                Text("Login")
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = { onGuest() }) {
                Text("Continue as Guest")
            }
        }
    }
}
