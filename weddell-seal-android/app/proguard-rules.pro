# Release R8 rules. Debug builds are not minified.

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room TypeConverters persist FileStatus / FileType via Enum.name / valueOf.
-keepclassmembers enum weddellseal.markrecap.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# OpenCSV references Apache Commons Logging; we do not ship it.
-dontwarn org.apache.commons.logging.**

# Drop verbose log calls in release so coordinate strings are not written to logcat.
# Log.w / Log.e are kept for field failures.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
