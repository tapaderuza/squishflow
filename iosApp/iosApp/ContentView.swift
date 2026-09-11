import UIKit
import SwiftUI
import FamilyControls
import ManagedSettings
import shared

/// The shared palette, matching Theme.kt on the Kotlin side.
enum Palette {
    static let ground = Color(red: 0.047, green: 0.055, blue: 0.051)   // Cream
    static let ink = Color(red: 0.949, green: 0.941, blue: 0.914)      // Ink
    static let muted = Color(red: 0.588, green: 0.600, blue: 0.569)    // Muted
    static let sage = Color(red: 0.553, green: 0.839, blue: 0.667)     // Sage
    static let coral = Color(red: 1.000, green: 0.502, blue: 0.424)    // Coral
    static let onLight = Color(red: 0.082, green: 0.090, blue: 0.075)  // OnLight
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        Main_iosKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

@MainActor
final class FocusProtectionModel: ObservableObject {
    @Published var selection = FamilyActivitySelection()
    @Published var isPickerPresented = false
    @Published var isAuthorized = false
    @Published var showSquishy = false
    @Published var errorMessage: String?

    private let store = ManagedSettingsStore(named: .init("squish.focus"))

    var selectedCount: Int {
        selection.applicationTokens.count +
        selection.categoryTokens.count +
        selection.webDomainTokens.count
    }

    func requestAuthorizationAndPick() async {
        do {
            try await AuthorizationCenter.shared.requestAuthorization(for: .individual)
            isAuthorized = AuthorizationCenter.shared.authorizationStatus == .approved
            if isAuthorized { isPickerPresented = true }
        } catch {
            errorMessage = "Could not turn on Screen Time. Check Family Controls in Settings."
        }
    }

    func beginFocus() {
        guard selectedCount > 0 else { return }
        store.shield.applications = selection.applicationTokens
        store.shield.applicationCategories = selection.categoryTokens.isEmpty
            ? nil
            : .specific(selection.categoryTokens)
        store.shield.webDomains = selection.webDomainTokens
        UserDefaults.standard.set(true, forKey: "squish_onboarded")
        UserDefaults.standard.set(["iOS Protected"], forKey: "squish_selected_categories")
        showSquishy = true
    }

    func removeProtection() {
        store.clearAllSettings()
    }
}

struct ContentView: View {
    @StateObject private var protection = FocusProtectionModel()

    /// Launched with `-squishy`, the app opens straight on the companion.
    ///
    /// Used by CI, which can build and boot a simulator but has no finger to
    /// press "timer only" with, and handy for anyone who wants to skip the
    /// protection setup during development.
    private let openOnCompanion = ProcessInfo.processInfo.arguments.contains("-squishy")

    var body: some View {
        ZStack {
            Palette.ground.ignoresSafeArea()

            if protection.showSquishy || openOnCompanion {
                ComposeView()
                    .ignoresSafeArea(.all, edges: .bottom)
            } else {
                setup
            }
        }
        .familyActivityPicker(
            isPresented: $protection.isPickerPresented,
            selection: $protection.selection
        )
    }

    private var setup: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("SQUISHFLOW")
                .font(.system(size: 16, weight: .black))
                .tracking(2.2)
                .foregroundStyle(Palette.ink)

            Text("YOUR FOCUS COMPANION")
                .font(.system(size: 9, weight: .bold))
                .tracking(1.7)
                .foregroundStyle(Palette.muted)

            Spacer()

            Text("What do you want\nto protect today?")
                .font(.system(size: 38, weight: .light))
                .foregroundStyle(Palette.ink)

            Text("Pick apps, categories or sites. iOS keeps the choice private: Squishflow never learns which ones you picked.")
                .font(.system(size: 15))
                .foregroundStyle(Palette.muted)
                .lineSpacing(5)
                .padding(.top, 16)

            if protection.selectedCount > 0 {
                Button {
                    protection.isPickerPresented = true
                } label: {
                    HStack {
                        Text("Protected selection")
                        Spacer()
                        Text("\(protection.selectedCount)")
                            .fontWeight(.bold)
                    }
                    .foregroundStyle(Palette.sage)
                    .padding(18)
                    .background(Color.white.opacity(0.055))
                    .clipShape(RoundedRectangle(cornerRadius: 20))
                }
                .padding(.top, 28)
            }

            Spacer()

            if let message = protection.errorMessage {
                Text(message)
                    .font(.system(size: 12))
                    .foregroundStyle(Palette.coral)
                    .padding(.bottom, 14)
            }

            Button {
                if protection.selectedCount == 0 {
                    Task { await protection.requestAuthorizationAndPick() }
                } else {
                    protection.beginFocus()
                }
            } label: {
                Text(protection.selectedCount == 0 ? "Choose apps" : "Start with Squishy")
                    .font(.system(size: 15, weight: .bold))
                    .frame(maxWidth: .infinity)
                    .frame(height: 58)
                    .foregroundStyle(Palette.onLight)
                    .background(Palette.ink)
                    .clipShape(Capsule())
            }

            Button {
                protection.showSquishy = true
            } label: {
                Text("Not now  \u{00B7}  timer only")
                    .font(.system(size: 13))
                    .foregroundStyle(Palette.muted)
                    .frame(maxWidth: .infinity)
                    .padding(.top, 16)
            }
            .buttonStyle(.plain)

            Text("Authorisation uses Face ID or Touch ID and can be revoked in Settings.")
                .font(.system(size: 11))
                .foregroundStyle(Palette.muted)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.top, 13)
        }
        .padding(.horizontal, 26)
        .padding(.top, 24)
        .padding(.bottom, 18)
    }
}
