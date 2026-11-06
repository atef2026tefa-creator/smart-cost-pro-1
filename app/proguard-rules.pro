# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp { *; }
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.HiltAndroidApp class *
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.lifecycle.HiltViewModel class *
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.qualifiers.Qualifier class *

# Retrofit / OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * { @retrofit2.http.* <methods>; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.aistudio.smartcostpro.app.**$$serializer { *; }
-keepclassmembers class com.aistudio.smartcostpro.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.aistudio.smartcostpro.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room — only keep what R8 can't infer (entities & DAOs are accessed by name).
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Models (DTO + Domain) — accessed via reflection by kotlinx.serialization
-keep class com.aistudio.smartcostpro.app.data.remote.dto.** { *; }
-keep class com.aistudio.smartcostpro.app.domain.model.** { *; }

# Stage 3 (per code review): removed the catch-all
#   `-keep class androidx.compose.** { *; }`
# That rule crippled R8 / resource shrinking and ballooned the APK. Compose
# ships its own consumer-rules that keep what's actually needed; we trust them.
