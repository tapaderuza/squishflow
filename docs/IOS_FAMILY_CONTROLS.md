# iOS Family Controls — parked until Apple grants the entitlement

The iPhone build ships as timer + companion. Per-app protection needs the
`com.apple.developer.family-controls` distribution entitlement, which Apple
grants on request (typically days to weeks) and which a free Apple ID cannot
use at all. The SwiftUI setup screen, the entitlement and the two shield
extensions were removed from the app in September 2026 so a store build could
be uploaded; the earlier implementation is in git history (commit before
"Ship the iPhone app as timer and companion"). When the entitlement arrives:
restore that code, set `supportsAppProtection` to true on iOS, and follow the
steps below.

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