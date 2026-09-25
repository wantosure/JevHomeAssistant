# Proguard rules for Jev Assistant
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.jev.assistant.jev.** { *; }
-keep class com.jev.assistant.device.** { *; }
