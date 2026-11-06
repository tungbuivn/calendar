# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# --- Custom Views khởi tạo từ XML layout ---
# Cần constructor (Context, AttributeSet) để Android inflate được trong layout.
-keepclasseswithmembers class com.calendar.tbt.FamilyTreeView {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class com.calendar.tbt.FlowLayout {
    public <init>(android.content.Context, android.util.AttributeSet);
}

# --- Widget receiver & provider ---
# Android khởi tạo receiver từ AndroidManifest theo tên lớp đầy đủ.
-keep class com.calendar.tbt.CalendarWidget { *; }
-keep class com.calendar.tbt.App { *; }

# --- Giữ tên lớp cho stack trace dễ đọc trong Play Console ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
