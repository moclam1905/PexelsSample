# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}

# Kotlinx Serialization
-keepclassmembers class **$$serializer { *; }
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.Transient <fields>;
}

# Hilt - These rules are usually not needed if using the Hilt Gradle plugin correctly,
# but can be added for completeness or if issues arise.
# -keep class dagger.hilt.android.internal.** { *; }
# -keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$ViewComponentBuilder { *; }
# -keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
# -keep @dagger.hilt.釁 { *; }
# -keep @dagger.hilt.android.釁 { *; }
# -keep @dagger.hilt.android.HiltAndroidApp { *; }
# -keep @dagger.hilt.android.AndroidEntryPoint { *; }
# -keep @dagger.hilt.android.lifecycle.HiltViewModel { *; }
# -keep @javax.inject.Inject class * { *; }
# -keep @javax.inject.Singleton class * { *; }