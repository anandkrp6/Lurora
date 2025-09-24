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

# Hilt ProGuard rules
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.AndroidEntryPoint { *; }

# Media3 ProGuard rules
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }

# Compose ProGuard rules
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# Keep data classes and enums
-keep class com.bytecoder.lurora.backend.models.** { *; }
-keep class com.bytecoder.lurora.storage.** { *; }

# Room ProGuard rules
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**