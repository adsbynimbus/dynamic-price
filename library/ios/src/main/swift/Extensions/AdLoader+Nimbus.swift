//
//  AdLoader+Nimbus.swift
//  DynamicPrice
//
//  Created on 2/27/24
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

import GoogleMobileAds
import NimbusKit

public extension AdLoader {
    /// This method applies dynamic price targeting and calls `AdLoader.load()`.
    /// - Parameters:
    ///     - gamRequest: Instance of AdManagerRequest
    ///     - ad: NimbusAd to render if Nimbus wins
    ///     - mapping: Default is `NimbusGAMLinearPriceMapping.banner()`
    @available(*, deprecated, message: "AdLoader.applyDynamicPrice is no longer used and will be removed in the next feature release. Use NimbusAd.applyDynamicPrice(adManagerRequest) and AdLoader.load(adManagerRequest) instead.")
    func loadDynamicPrice(
        gamRequest: AdManagerRequest,
        ad: NimbusAd? = nil,
        mapping: NimbusGAMLinearPriceMapping = .banner()
    ) {
        ad?.applyDynamicPrice(into: gamRequest, mapping: mapping)

        load(gamRequest)
    }
}
