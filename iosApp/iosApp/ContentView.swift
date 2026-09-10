import UIKit
import SwiftUI
import FamilyControls
import ManagedSettings
import shared

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
            errorMessage = "No se pudo activar Screen Time. Revisa Family Controls en Ajustes."
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

    var body: some View {
        ZStack {
            Color(red: 0.047, green: 0.055, blue: 0.051).ignoresSafeArea()

            if protection.showSquishy {
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
            Text("SQUISH.")
                .font(.system(size: 16, weight: .black))
                .tracking(2.2)
                .foregroundStyle(Color(red: 0.95, green: 0.94, blue: 0.91))

            Text("FOCUS RESET")
                .font(.system(size: 9, weight: .bold))
                .tracking(1.7)
                .foregroundStyle(.secondary)

            Spacer()

            Text("¿Qué quieres\nproteger hoy?")
                .font(.system(size: 38, weight: .light))
                .foregroundStyle(Color(red: 0.95, green: 0.94, blue: 0.91))

            Text("Elige aplicaciones, categorías o webs. iOS mantiene tu selección privada: Squishflow no puede ver qué apps has escogido.")
                .font(.system(size: 15))
                .foregroundStyle(.secondary)
                .lineSpacing(5)
                .padding(.top, 16)

            if protection.selectedCount > 0 {
                Button {
                    protection.isPickerPresented = true
                } label: {
                    HStack {
                        Text("Selección protegida")
                        Spacer()
                        Text("\(protection.selectedCount)")
                            .fontWeight(.bold)
                    }
                    .foregroundStyle(Color(red: 0.55, green: 0.84, blue: 0.67))
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
                    .foregroundStyle(Color(red: 1, green: 0.50, blue: 0.42))
                    .padding(.bottom, 14)
            }

            Button {
                if protection.selectedCount == 0 {
                    Task { await protection.requestAuthorizationAndPick() }
                } else {
                    protection.beginFocus()
                }
            } label: {
                Text(protection.selectedCount == 0 ? "Elegir aplicaciones" : "Empezar con Squishy")
                    .font(.system(size: 15, weight: .bold))
                    .frame(maxWidth: .infinity)
                    .frame(height: 58)
                    .foregroundStyle(Color(red: 0.07, green: 0.08, blue: 0.07))
                    .background(Color(red: 0.95, green: 0.94, blue: 0.91))
                    .clipShape(Capsule())
            }

            Text("La autorización se realiza con Face ID o Touch ID y puede revocarse en Ajustes.")
                .font(.system(size: 11))
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.top, 13)
        }
        .padding(.horizontal, 26)
        .padding(.top, 24)
        .padding(.bottom, 18)
    }
}
