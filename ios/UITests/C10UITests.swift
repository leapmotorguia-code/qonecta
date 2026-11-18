import XCTest

final class C10UITests: XCTestCase {
    func testManualRecordAndWidgetCards() {
        let app = XCUIApplication()
        app.launchEnvironment["C10_UI_TEST_STORAGE"] = "1"
        app.launch()
        let battery = app.textFields["field0"]
        XCTAssertTrue(battery.waitForExistence(timeout: 15))
        battery.tap(); battery.typeText("73")
        let range = app.textFields["field1"]
        range.tap(); range.typeText("91")
        app.swipeUp()
        let save = app.buttons["saveRecord"]
        if !save.isHittable { app.swipeUp() }
        XCTAssertTrue(save.waitForExistence(timeout: 5))
        save.tap()
        XCTAssertTrue(app.staticTexts["saveMessage"].label.contains("Guardado manualmente"))
        let shot = XCTAttachment(screenshot: app.screenshot())
        shot.name = "Registro manual y tarjetas"; shot.lifetime = .keepAlways
        add(shot)
    }
}
