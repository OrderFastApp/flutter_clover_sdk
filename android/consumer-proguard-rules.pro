# Clover Android SDK: los Parcelables de jobs (impresión, etc.) viajan en Intents
# hacia com.clover.engine. R8 no debe renombrar esas clases o el engine falla con
# ClassNotFoundException al deserializar (nombres tipo Z0.g en el log).

-keep class com.clover.sdk.** { *; }
-keepclassmembers class com.clover.sdk.** { *; }
-dontwarn com.clover.sdk.**
