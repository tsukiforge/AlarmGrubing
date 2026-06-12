# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Retrofit Keep Rules
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeInvisibleParameterAnnotations
-keepclassmembers,allowobfuscation class * {
    @retrofit2.http.* <methods>;
}

# Moshi Keep Rules
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# Keep models which are serialized/deserialized
-keep class com.example.data.model.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.JsonProperty *;
}

# Room Keep Rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.android.AndroidDispatcherFactory {
    public <init>();
}
