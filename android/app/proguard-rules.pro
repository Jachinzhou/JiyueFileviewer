# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /tools/proguard/proguard-android.txt

# Keep Gson serialized classes
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.jachin.jiyue.model.** { *; }
