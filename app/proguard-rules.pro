# Add project specific ProGuard rules here.

-keepattributes LineNumberTable,SourceFile
-renamesourcefileattribute SourceFile

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# OkHttp (platform)
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn okhttp3.internal.platform.**

