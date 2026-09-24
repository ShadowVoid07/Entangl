# ProGuard / R8 rules for Entangl

# 1. SQLCipher
-keep class net.zetetic.database.sqlcipher.** { *; }
-dontwarn net.zetetic.database.sqlcipher.**

# 2. LazySodium & JNA Native Bindings
-keep class com.goterl.lazysodium.** { *; }
-dontwarn com.goterl.lazysodium.**
-keep class com.sun.jna.** { *; }
-dontwarn com.sun.jna.**
-keepclassmembers class * extends com.sun.jna.Structure {
    <fields>;
    <methods>;
}

# 3. LibSignal Client
-keep class org.signal.** { *; }
-dontwarn org.signal.**

# 4. Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# 5. Room Database & SQLCipher Helper
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class in.grayscales.entangl.data.local.entity.** { *; }
-keep class in.grayscales.entangl.data.local.dao.** { *; }

# 6. AndroidX Security Crypto & MasterKey
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# 7. Koin Dependency Injection
-keep class * extends org.koin.core.module.Module
-dontwarn org.koin.**

# 8. Cryptographic Models & Domain
-keep class in.grayscales.entangl.domain.model.** { *; }
-keep class in.grayscales.entangl.core.crypto.** { *; }
-keep class in.grayscales.entangl.data.network.TransportEnvelope { *; }
-keep class in.grayscales.entangl.data.network.TransportEnvelope$Companion { *; }
