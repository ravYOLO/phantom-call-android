-keep class com.phantomcall.app.shell.IUserService** { *; }
-keepattributes *Annotation*,InnerClasses,Signature
-keepclasseswithmembers class kotlinx.serialization.json.** { *; }
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-keepclasseswithmembers class <1> {
    kotlinx.serialization.KSerializer serializer(...);
}