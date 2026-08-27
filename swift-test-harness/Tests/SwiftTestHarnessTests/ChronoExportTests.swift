#if canImport(Testing)
import Testing
import Chrono

@Suite("Chrono Swift Export Suite")
struct ChronoExportTests {
    @Test("Swift module loads cleanly")
    func swiftModuleLoads() {
        #expect(Bool(true), "Chrono swift module imported cleanly")
    }
}
#elseif canImport(XCTest)
import XCTest
import Chrono

final class ChronoExportTests: XCTestCase {
    func testSwiftModuleLoads() {
        XCTAssertTrue(true, "Chrono swift module imported cleanly")
    }
}
#endif
