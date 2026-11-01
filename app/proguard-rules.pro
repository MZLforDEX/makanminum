# Add project specific ProGuard rules here.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

-keep class com.example.ui.AlarmRingingActivity { *; }
-keep class com.example.reminder.AlarmTriggerReceiver { *; }
-keep class com.example.reminder.NotificationActionReceiver { *; }
-keep class com.example.reminder.BootAndTimeReceiver { *; }
