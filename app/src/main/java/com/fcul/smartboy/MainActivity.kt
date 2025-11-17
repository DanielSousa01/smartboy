package com.fcul.smartboy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.fcul.smartboy.ui.auth.AuthActivity
import com.fcul.smartboy.ui.theme.SmartBoyTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage

@ExperimentalMaterial3ExpressiveApi
class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase
    private lateinit var storage: FirebaseStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        if (auth.currentUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        val vm = MainViewmodel(auth = auth)

        enableEdgeToEdge()
        setContent {
            SmartBoyTheme {
                SmartBoyApp(vm)
            }
        }
    }
}