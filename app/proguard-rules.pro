# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class com.clockin.app.data.** { *; }

# Kotlin / coroutines
-keepattributes *Annotation*
-dontwarn org.jetbrains.annotations.**

# DataStore
-keep class androidx.datastore.*.** { *; }

# Glance App Widget
-keep class com.clockin.app.widget.** { *; }

# Crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
