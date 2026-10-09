# ObjectBox ProGuard rules
-keepclassmembers class * {
    @io.objectbox.annotation.Entity <fields>;
}
-keep class io.objectbox.** { *; }
