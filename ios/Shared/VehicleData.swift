import Foundation

struct VehicleData: Codable, Equatable {
    var battery: Double = 0
    var electricRange: Double = 0
    var electricConsumption: Double = 0
    var electricityPrice: Double = 0
    var petrolConsumption: Double = 0
    var petrolPrice: Double = 0
    var regenPower: Double = 0
    var regenSeconds: Double = 0
    var costsRecorded: Bool = false
    var regenerationRecorded: Bool = false
    var updatedAt: Date? = nil

    var electricCost: Double { electricConsumption * electricityPrice }
    var petrolCost: Double { petrolConsumption * petrolPrice }
    var recoveredKWh: Double { regenPower * regenSeconds / 3600 }
    func isStale(at date: Date) -> Bool {
        guard let updatedAt else { return true }
        return date.timeIntervalSince(updatedAt) > 3600 || date < updatedAt
    }
    var isValid: Bool {
        let values = [battery, electricRange, electricConsumption, electricityPrice,
                      petrolConsumption, petrolPrice, regenPower, regenSeconds]
        return values.allSatisfy { $0.isFinite && $0 >= 0 } && battery <= 100
            && electricRange <= 2000 && electricConsumption <= 200
            && electricityPrice <= 100 && petrolConsumption <= 100
            && petrolPrice <= 100 && regenPower <= 500 && regenSeconds <= 86400
    }
    static func number(_ raw: String) -> Double? {
        let normalized = raw.trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: ",", with: ".")
        guard !normalized.isEmpty, let value = Double(normalized), value.isFinite else { return nil }
        return value
    }
}

enum VehicleStore {
    static let key = "c10.manual.snapshot.v1"
    static var groupID: String {
        resolvedGroup(configured: Bundle.main.object(forInfoDictionaryKey: "C10AppGroup") as? String
            ?? "group.com.leapmotorguia.c10widget", remapped: Bundle.main.object(forInfoDictionaryKey: "ALTAppGroups") as? [String] ?? [])
    }
    static func resolvedGroup(configured: String, remapped: [String]) -> String {
        remapped.first { $0 == configured || $0.hasPrefix(configured + ".") } ?? configured
    }
    static func defaults() -> UserDefaults? {
        #if DEBUG
        if ProcessInfo.processInfo.environment["C10_UI_TEST_STORAGE"] == "1" {
            return UserDefaults(suiteName: "c10.ui.tests")
        }
        #endif
        guard FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: groupID) != nil else { return nil }
        return UserDefaults(suiteName: groupID)
    }
    static func read(from defaults: UserDefaults?) -> VehicleData {
        guard let raw = defaults?.data(forKey: key),
              let value = try? JSONDecoder().decode(VehicleData.self, from: raw), value.isValid else { return VehicleData() }
        return value
    }
    static func save(_ value: VehicleData, to defaults: UserDefaults?) throws {
        guard value.isValid else { throw StoreError.invalid }
        guard let defaults else { throw StoreError.groupUnavailable }
        defaults.set(try JSONEncoder().encode(value), forKey: key)
    }
    enum StoreError: LocalizedError {
        case invalid, groupUnavailable
        var errorDescription: String? {
            switch self {
            case .invalid: return "Revisa los números: deben ser positivos y la batería de 0 a 100 %."
            case .groupUnavailable: return "El grupo compartido no está disponible. Hay que configurar la firma de la app y el widget con el mismo App Group."
            }
        }
    }
}
