import XCTest
@testable import C10Widget

final class VehicleDataTests: XCTestCase {
    func testResignedGroupMappingDoesNotUseOtherAppGroups() {
        let original = "group.com.leapmotorguia.c10widget"
        XCTAssertEqual(VehicleStore.resolvedGroup(configured: original, remapped: ["group.other", original + ".TEAM"]), original + ".TEAM")
        XCTAssertEqual(VehicleStore.resolvedGroup(configured: original, remapped: ["group.other"]), original)
    }
    func testSpanishDecimalAndInvalidInputs() {
        XCTAssertEqual(VehicleData.number(" 0,23 "), 0.23)
        XCTAssertNil(VehicleData.number("")); XCTAssertNil(VehicleData.number("nan"))
        XCTAssertNil(VehicleData.number("1,2,3"))
        var data = VehicleData(); data.battery = 101
        XCTAssertFalse(data.isValid)
        data.battery = 50; data.regenPower = -1
        XCTAssertFalse(data.isValid)
    }
    func testPhysicalUnitsAndSeparateCosts() {
        var data = VehicleData()
        data.electricConsumption = 20; data.electricityPrice = 0.25
        data.petrolConsumption = 6; data.petrolPrice = 1.5
        data.regenPower = 36; data.regenSeconds = 10
        XCTAssertEqual(data.electricCost, 5)
        XCTAssertEqual(data.petrolCost, 9)
        XCTAssertEqual(data.recoveredKWh, 0.1, accuracy: 0.000001)
    }
    func testStalenessAndMissingData() {
        let now = Date(timeIntervalSince1970: 10000)
        var data = VehicleData()
        XCTAssertTrue(data.isStale(at: now))
        data.updatedAt = now.addingTimeInterval(-3599)
        XCTAssertFalse(data.isStale(at: now))
        data.updatedAt = now.addingTimeInterval(-3601)
        XCTAssertTrue(data.isStale(at: now))
        data.updatedAt = now.addingTimeInterval(1)
        XCTAssertTrue(data.isStale(at: now))
    }
    func testStorageRoundTripAndMisconfiguredGroup() throws {
        let suite = "c10.tests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        var data = VehicleData(); data.battery = 73; data.electricRange = 91; data.updatedAt = .now
        try VehicleStore.save(data, to: defaults)
        XCTAssertEqual(VehicleStore.read(from: defaults), data)
        XCTAssertThrowsError(try VehicleStore.save(data, to: nil))
        defaults.set(Data("broken".utf8), forKey: VehicleStore.key)
        XCTAssertNil(VehicleStore.read(from: defaults).updatedAt)
    }
}
