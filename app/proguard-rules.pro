# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in $ANDROID_HOME/tools/proguard/proguard-android.txt

# Keep OpenCV classes
-keep class org.opencv.** { *; }

# Keep Room entities
-keep class com.srg.inventory.data.** { *; }
