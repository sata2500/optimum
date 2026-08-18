-keep class tech.salev.optimum.data.model.** { *; }
-keep class tech.salev.optimum.service.** { *; }
-keep class tech.salev.optimum.** { *; }

# Hilt — modern 2.60.x; consumer rules dahil, bu blok yeterli
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# Room — 2.7.2 kendi consumer rules'larını içeriyor; bu ek kurallar yeterli
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class tech.salev.optimum.**$$serializer { *; }

# Vico Charts
-keep class com.patrykandpatrick.vico.** { *; }

# ────────────────────────────────────────────────────────────────────────────
# R8 / Crashlytics — AGP 9.0 + R8 Full Mode
# Bu kurallar Firebase Crashlytics'in üretim çökmelerini doğru decode etmesi
# ve Kotlin reflection'ın çalışması için gereklidir.
# ────────────────────────────────────────────────────────────────────────────

# Crashlytics stack trace'leri için kaynak dosya ve satır numarası bilgisi
-keepattributes SourceFile,LineNumberTable
# Kotlin Serialization ve genel reflection için imza metadata'sı
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault

# mapping.txt: Her release build sonrası Firebase Crashlytics'e yükleyin.
# Android Studio → Build → Generate Signed Bundle → mapping.txt otomatik üretilir.
# Firebase Console → Crashlytics → Upload mapping file (veya Gradle plugin ile otomatik).

