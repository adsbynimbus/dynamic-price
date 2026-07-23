//
//  DynamicPriceRendererTests.swift
//  
//
//  Created by Jason Sznol on 7/22/26.
//

@testable import DynamicPrice
import GoogleMobileAds
import NimbusKit

import Testing

@Suite
struct DynamicPriceRendererTests {

    /// A simple thread‑safety check for `NimbusDynamicPriceHelper.adCache`.
    ///
    /// The test launches a large number of concurrent tasks that
    /// repeatedly write to and read from the cache. If any race condition
    /// exists, the test will crash or fail with an assertion.
    ///
    /// The testCount of 10 runs concurrently with requests that occur during app startup;
    /// using a higher value may evict some of the AdLoader entries from the adCache
    /// and cause failures in the final step.
    @Test("Dynamic Price adCache is accessed safely from multiple threads")
    func concurrentAccess() async throws {
        let testCount = 10

        let requests = (0..<testCount).map { _ in AdManagerRequest() }

        try await withThrowingTaskGroup(of: (Int, AdManagerRequest).self) {
            requestGroup in
            for request in requests.enumerated() {
                requestGroup.addTask(priority: .userInitiated) {
                    createNimbusAd(index: request.offset).applyDynamicPrice(into: request.element)
                    return request
                }
            }

            try await withThrowingTaskGroup(of: Void.self) { applyGroup in
                while !requestGroup.isEmpty,
                    let (index, request) = try await requestGroup.next()
                {
                    applyGroup.addTask(priority: .background) {
                        #expect(request.customTargeting!["na_type"] as? String == "static")
                        #expect(
                            request.customTargeting!["na_id"] as? String == "auctionId-\(index)")
                        #expect(
                            request.customTargeting!["na_bid"] as? String
                                == "\(Nimbus.shared.testMode ? 0 : 200)"
                        )
                    }

                    for index in (0..<testCount) {
                        applyGroup.addTask {
                            DynamicPriceRenderer.render(
                                data: "{\"na_id\":\"auctionId-\(index)\",\"ga_click\":\"https://click\(index)\"}"
                            ) { nimbusAd, clickTracker in
                                #expect(nimbusAd.value.auctionId == "auctionId-\(index)")
                                #expect(clickTracker == URL(string: "https://click\(index)"))
                            }
                        }
                    }
                }

                try await applyGroup.waitForAll()
            }
        }
    }
}
