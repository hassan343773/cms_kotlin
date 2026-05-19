package com.cms.app.utils

object Constants {
    // Every device (emulator + phones) must hit the same host so admin/customer see one server.
    // Emulator → http://10.0.2.2:PORT/   Physical phone → http://<PC_LAN_IP>:PORT/  or shared ngrok URL + trailing /
    const val BASE_URL = "https://yoga-magnitude-rack.ngrok-free.dev/"

    /**
     * How often the app asks the server for complaint changes while you stay logged in.
     * Lower = faster cross-device updates, slightly more battery/network use. Try 5_000 for ~5s.
     */
    const val POLL_INTERVAL_MS = 10_000L

    // DataStore keys
    const val PREF_TOKEN    = "auth_token"
    const val PREF_USERNAME = "username"
    const val PREF_ROLE     = "role"
    const val PREF_USER_ID  = "user_id"
}
