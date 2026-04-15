//
//  DynamicPrice+Helper.swift
//  DynamicPrice
//
//  Created by Jason Sznol on 4/16/26.
//  Copyright © 2026 AdsByNimbus. All rights reserved.
//

@preconcurrency import NimbusDynamicPrice
import SwiftUI

extension DynamicPriceApp {
    nonisolated static let adUnitId = Bundle.main.infoDictionary?["AdManager AdUnit Id"] as! String

    /**
     * Default Mapping for Banner ad units
     *
     * $0.01 increments: $0.01 - $3.00   (ex. na_bid = {1, 2, 3, 4 ... 300})
     * $0.05 increments: $3.00 - $8.00   (ex. na_bid = {300, 305, 310, 315 ... 800})
     * $0.50 increments: $8.00 - $20.00  (ex. na_bid = {800, 850, 900, 950 ... 2000})
     * $1.00 increments: $20.00 - $35.00 (ex. na_bid = {2000, 2100, 2200, 2300 ... 3500})
     */
    nonisolated static let mapping = NimbusGAMLinearPriceMapping(granularities: [
        NimbusGAMLinearPriceGranularity(min: 0, max: 300, step: 1),
        NimbusGAMLinearPriceGranularity(min: 300, max: 800, step: 5),
        NimbusGAMLinearPriceGranularity(min: 800, max: 2000, step: 50),
        NimbusGAMLinearPriceGranularity(min: 2000, max: 3500, step: 100),
    ])
}

extension NimbusRequestManager {
    final class RequestListener: NimbusRequestManagerDelegate, Sendable {
        nonisolated(unsafe) var continuation: UnsafeContinuation<NimbusAd, Error>?

        func didCompleteNimbusRequest(request: NimbusRequest, ad: NimbusAd) {
            continuation?.resume(returning: ad)
            continuation = nil
        }

        func didFailNimbusRequest(request: NimbusRequest, error: NimbusError) {
            continuation?.resume(throwing: error)
            continuation = nil
        }
    }
    
    func makeRequest(_ request: NimbusRequest) async throws -> NimbusAd {
        let listener = RequestListener()
        delegate = listener
        let response = try await withTaskCancellationHandler {
            try await withUnsafeThrowingContinuation { continuation in
                listener.continuation = continuation
                performRequest(request: request)
            }
        } onCancel: {
            listener.continuation?.resume(throwing: CancellationError())
            listener.continuation = nil
        }
        return response
    }
}

extension Task where Success == Never, Failure == Never {
    @inlinable
    static func sleep(seconds: TimeInterval) async {
        if seconds > 0 {
            try? await Task.sleep(nanoseconds: UInt64(seconds) * 1_000_000_000)
        }
    }
}
