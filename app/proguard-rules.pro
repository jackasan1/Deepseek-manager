# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Tink (used by androidx.security-crypto)
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Keep data models used via reflection-free JSON parsing (defensive)
-keep class com.jackasan1.deepseekmanager.data.** { *; }
