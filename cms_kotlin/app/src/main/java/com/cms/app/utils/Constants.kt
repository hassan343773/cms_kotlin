package com.cms.app.utils

object Constants {
    // Change this to your backend URL
    // Android Emulator  → http://10.0.2.2:8080
    // Physical device   → http://<your-pc-local-ip>:8080
    const val BASE_URL = "https://yoga-magnitude-rack.ngrok-free.dev/"

    // DataStore keys
    const val PREF_TOKEN    = "auth_token"
    const val PREF_USERNAME = "username"
    const val PREF_ROLE     = "role"
    const val PREF_USER_ID  = "user_id"
}
