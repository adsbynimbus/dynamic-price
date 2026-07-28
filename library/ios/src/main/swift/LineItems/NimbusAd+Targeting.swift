//
//  NimbusAd+Targeting.swift
//  DynamicPrice
//
//  Created on 8/12/22.
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

import GoogleMobileAds
@_exported import NimbusKit

public extension NimbusAd {

    /// Add keywords for custom targeting from Nimbus ad to AdManagerRequest
    /// - Parameters:
    ///   - request: AdManagerRequest to add keywords to
    ///   - mapping: A mapping composed of multiple LinearPriceGranularities in ascending order. Default: NimbusGAMLinearPriceMapping.banner()
    func applyDynamicPrice(into request: AdManagerRequest, mapping: NimbusGAMLinearPriceMapping = .banner()) {
        applyDynamicPrice(into: request, keywords: mapping.getKeywords(ad: self))
        DynamicPriceRenderer[auctionId] = .init(self)
    }
}

/// :nodoc:
extension NimbusAd {
    func applyDynamicPrice(into request: AdManagerRequest, keywords: String?) {
        if request.customTargeting == nil {
            request.customTargeting = [:]
        }
        let isVideo = auctionType == .video
        let bidKey = isVideo ? "na_bid_video" : "na_bid"
        request.customTargeting?["na_id"] = auctionId
        request.customTargeting?["na_size"] = "\(adDimensions?.width ?? 0)x\(adDimensions?.height ?? 0)"
        request.customTargeting?["na_network"] = network
        request.customTargeting?["na_type"] = isVideo ? "video" : "static"
        request.customTargeting?[bidKey] = Nimbus.shared.testMode ? "0" : keywords
    }
}
