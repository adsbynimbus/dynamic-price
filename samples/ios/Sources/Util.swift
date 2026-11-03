import Foundation

extension Task where Success == Never, Failure == Never {
    @inlinable
    static func sleep(seconds: TimeInterval) async {
        if seconds > 0 {
            try? await Task.sleep(nanoseconds: UInt64(seconds) * 1_000_000_000)
        }
    }
}
