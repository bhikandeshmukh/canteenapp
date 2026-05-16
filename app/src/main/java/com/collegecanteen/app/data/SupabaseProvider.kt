package com.collegecanteen.app.data

import com.collegecanteen.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseProvider {
    fun isConfigured(): Boolean =
        BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()

    val client: SupabaseClient by lazy {
        require(isConfigured()) {
            "Supabase URL and publishable key are missing. Add them to local.properties."
        }

        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        ) {
            install(Auth) {
                alwaysAutoRefresh = true
                autoLoadFromStorage = true
            }
            install(Postgrest)
        }
    }
}
