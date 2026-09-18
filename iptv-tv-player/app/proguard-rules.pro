# Media3 / ExoPlayer keeps its own consumer rules; nothing extra needed for playback.

# kotlinx.serialization: keep generated serializers for our DTOs.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclasseswithmembers class com.iptvtv.player.data.remote.xtream.** {
    *** Companion;
}
-keepclasseswithmembers class com.iptvtv.player.data.remote.xtream.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.iptvtv.player.data.remote.xtream.**$$serializer { *; }

# Room entities accessed via reflection by generated code are already kept by Room's own rules.
