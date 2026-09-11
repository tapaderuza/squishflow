import SwiftUI
import shared

/// The shared palette, matching Theme.kt on the Kotlin side.
enum Palette {
    static let ground = Color(red: 0.047, green: 0.055, blue: 0.051)   // Cream
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        Main_iosKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

/// The whole iPhone app is the shared Compose app.
///
/// An earlier version put a Screen Time setup screen in front of it. Family
/// Controls needs a distribution entitlement Apple grants on request, the
/// current build ships without it, and a setup screen for a feature that
/// cannot turn on is worse than no screen. Per-app protection on iPhone
/// returns with that entitlement; see docs/IOS_FAMILY_CONTROLS.md.
struct ContentView: View {
    var body: some View {
        ZStack {
            Palette.ground.ignoresSafeArea()
            ComposeView()
                .ignoresSafeArea(.all, edges: .bottom)
        }
    }
}
