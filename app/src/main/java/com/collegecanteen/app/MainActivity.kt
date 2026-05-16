package com.collegecanteen.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.collegecanteen.app.data.CanteenRepository
import com.collegecanteen.app.data.SupabaseProvider
import com.collegecanteen.app.ui.CanteenApp
import com.collegecanteen.app.ui.CollegeCanteenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CollegeCanteenTheme {
                val repository = remember {
                    if (SupabaseProvider.isConfigured()) {
                        CanteenRepository(SupabaseProvider.client)
                    } else {
                        null
                    }
                }
                CanteenApp(repository = repository)
            }
        }
    }
}
