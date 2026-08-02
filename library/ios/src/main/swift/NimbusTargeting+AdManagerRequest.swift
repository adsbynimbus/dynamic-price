//
//  DynamicPrice.swift
//  DynamicPrice
//
//  Created on 5/21/26.
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

import GoogleMobileAds
import NimbusKit

public extension AdManagerRequest {
    /// Add keywords for custom targeting from Nimbus ad to AdManagerRequest
    /// - Parameters:
    ///   - request: AdManagerRequest to add keywords to
    ///   - mapping: A mapping composed of multiple LinearPriceGranularities in ascending order.
    func applyDynamicPrice(nimbusAd: NimbusAd, mapping: PriceMapping) {
        if customTargeting == nil {
            customTargeting = [:]
        }

        customTargeting?["na_id"] = nimbusAd.auctionId
        customTargeting?["na_size"] = 
            "\(nimbusAd.adDimensions?.width ?? 0)x\(nimbusAd.adDimensions?.height ?? 0)"
        customTargeting?["na_network"] = nimbusAd.network
        customTargeting?["na_type"] = nimbusAd.auctionType.rawValue
        customTargeting?["na_bid" + (nimbusAd.auctionType == .video ? "_video" : "")] =
            Nimbus.shared.testMode ? "0" : mapping.getTarget(price: nimbusAd.bidRaw)
    }
}
