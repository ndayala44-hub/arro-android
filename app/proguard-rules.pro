# kotlinx.serialization: keep generated serializers for the app's persisted models.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class app.arro.core.** {
    *** Companion;
}
-keepclasseswithmembers class app.arro.core.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class app.arro.core.**$$serializer { *; }

# ML Kit and CameraX ship their own consumer rules.
