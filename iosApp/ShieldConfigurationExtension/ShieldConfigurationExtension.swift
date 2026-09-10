import ManagedSettings
import ManagedSettingsUI
import UIKit

final class ShieldConfigurationExtension: ShieldConfigurationDataSource {
    private var squishShield: ShieldConfiguration {
        ShieldConfiguration(
            backgroundBlurStyle: .systemUltraThinMaterialDark,
            backgroundColor: UIColor(red: 0.047, green: 0.055, blue: 0.051, alpha: 1),
            icon: UIImage(systemName: "circle.hexagongrid.fill"),
            title: ShieldConfiguration.Label(
                text: "Pausa consciente",
                color: UIColor(red: 0.95, green: 0.94, blue: 0.91, alpha: 1)
            ),
            subtitle: ShieldConfiguration.Label(
                text: "Has elegido proteger este momento. Vuelve a Squishflow y aprieta a Squishy tres veces.",
                color: UIColor(red: 0.60, green: 0.61, blue: 0.58, alpha: 1)
            ),
            primaryButtonLabel: ShieldConfiguration.Label(
                text: "Mantener el foco",
                color: UIColor(red: 0.07, green: 0.08, blue: 0.07, alpha: 1)
            ),
            primaryButtonBackgroundColor: UIColor(red: 0.55, green: 0.84, blue: 0.67, alpha: 1),
            secondaryButtonLabel: ShieldConfiguration.Label(
                text: "Solicitar acceso",
                color: UIColor(red: 0.95, green: 0.94, blue: 0.91, alpha: 0.72)
            )
        )
    }

    override func configuration(shielding application: Application) -> ShieldConfiguration {
        squishShield
    }

    override func configuration(
        shielding application: Application,
        in category: ActivityCategory
    ) -> ShieldConfiguration {
        squishShield
    }

    override func configuration(shielding webDomain: WebDomain) -> ShieldConfiguration {
        squishShield
    }

    override func configuration(
        shielding webDomain: WebDomain,
        in category: ActivityCategory
    ) -> ShieldConfiguration {
        squishShield
    }

    override func configuration(shielding category: ActivityCategory) -> ShieldConfiguration {
        squishShield
    }
}
