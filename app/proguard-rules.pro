# SpendLess ProGuard Rules

# ── Kotlin ─────────────────────────────────────────────────────────────────────
-keepclassmembers class **$WhenMappings { <fields>; }
-keep class kotlin.Metadata { *; }

# ── Room ────────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface *
-keepclassmembers @androidx.room.Dao interface * { *; }

# ── Hilt ────────────────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ActivityComponentManager { *; }

# ── Kotlinx Serialization ──────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer { *; }

# ── Jetpack Compose ────────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }

# ── WorkManager ────────────────────────────────────────────────────────────────
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.Worker { <init>(...); }
-keepclassmembers class * extends androidx.work.CoroutineWorker { <init>(...); }

# ── App Entities ───────────────────────────────────────────────────────────────
-keep class com.spendless.app.core.data.database.entities.** { *; }
-keep enum com.spendless.app.core.data.database.entities.** { *; }
