# iOS Family Controls release setup

The implementation sources are already in this repository. Apple requires these
capabilities and extension targets to be created and signed in Xcode after approving
the distribution entitlement.

## Apple Developer portal

Request Family Controls (Distribution) for:

- com.alvaropassalacqua.squishflow
- com.alvaropassalacqua.squishflow.ShieldConfiguration
- com.alvaropassalacqua.squishflow.ShieldAction

Enable App Group group.com.alvaropassalacqua.squishflow for all three identifiers.

## Xcode targets

Open iosApp/iosApp.xcodeproj on macOS using Xcode 16 or newer.

Add a Shield Configuration Extension target:

- Product name: ShieldConfigurationExtension
- Bundle identifier: com.alvaropassalacqua.squishflow.ShieldConfiguration
- Source: ShieldConfigurationExtension/ShieldConfigurationExtension.swift
- Info: ShieldConfigurationExtension/Info.plist
- Entitlements: ShieldConfigurationExtension/ShieldConfigurationExtension.entitlements

Add a Shield Action Extension target:

- Product name: ShieldActionExtension
- Bundle identifier: com.alvaropassalacqua.squishflow.ShieldAction
- Source: ShieldActionExtension/ShieldActionExtension.swift
- Info: ShieldActionExtension/Info.plist
- Entitlements: ShieldActionExtension/ShieldActionExtension.entitlements

For both targets:

- Deployment target: iOS 16.0
- Family Controls capability
- App Groups capability with group.com.alvaropassalacqua.squishflow
- Embed each appex in the main iosApp target under Embed App Extensions
- Use automatic signing with the same Apple Developer team as the main app

The main target already has SquishFocus.entitlements, iOS 16 deployment, native
authorization, FamilyActivityPicker, and ManagedSettings shielding implemented.