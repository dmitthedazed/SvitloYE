# --- Android & Compose ---
-keepattributes SourceFile,LineNumberTable
-keep class androidx.compose.** { *; }

# --- Retrofit ---
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# --- GSON & Data Models ---
# Prevent R8 from stripping serialization/deserialization code
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes *Annotation*
-keepattributes Signature

# Keep all data classes used by GSON
-keep class com.occaecat.ztoeschedule.data.model.** { *; }

# --- WorkManager ---
-keep class androidx.work.Worker { *; }
-keep class androidx.work.ListenableWorker { *; }

# --- Kronos (NTP) ---
-keep class com.lyft.kronos.** { *; }

# --- Hilt / Dagger ---
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ActivityComponentManager
-keep class * extends dagger.hilt.android.internal.managers.BroadcastReceiverComponentManager
-keep class * extends dagger.hilt.android.internal.managers.FragmentComponentManager
-keep class * extends dagger.hilt.android.internal.managers.ServiceComponentManager
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager
