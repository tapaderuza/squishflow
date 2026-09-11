# RevenueCat ships its own consumer rules. The accessibility service is
# referenced from the manifest by name and must keep it.
-keep class com.alvaropassalacqua.squishflow.FocusGuardService { *; }
-keep class com.alvaropassalacqua.squishflow.MainActivity { *; }

# Kotlin coroutines debug metadata is not needed and inflates the mapping.
-dontwarn kotlinx.coroutines.debug.**
