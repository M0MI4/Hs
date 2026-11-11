# Add project specific ProGuard rules here.

# Keep data models used by Firestore, Room, and Moshi
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** { *; }

# Keep Room database and DAO classes
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Moshi / Retrofit serialization
-keepclassmembers class * {
    @com.squareup.moshi.* <fields>;
}

# Keep Firebase Firestore and Auth serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @com.google.firebase.firestore.DocumentId <fields>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}

# Keep Coil Image loaders
-dontwarn coil.**

