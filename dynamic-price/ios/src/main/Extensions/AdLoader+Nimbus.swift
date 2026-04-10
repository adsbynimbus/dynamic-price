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
    private static var nimbusAdKey: Void?
    
    private(set) var nimbusAd: NimbusAd? {
        get {
            objc_getAssociatedObject(
                self,
                &Self.nimbusAdKey
            ) as? NimbusAd
        }
        set {
            objc_setAssociatedObject(
                self,
                &Self.nimbusAdKey,
                newValue,
                .OBJC_ASSOCIATION_RETAIN_NONATOMIC
            )
        }
    }
    
    /// This method applies dynamic price targeting and calls `GADAdLoader.load()`.
    /// - Parameters:
    ///     - request: Instance of AdManagerRequest
    ///     - ad: NimbusAd to render if Nimbus wins
    ///     - mapping: Mapping to line items in Ad Manager
    func loadDynamicPrice(
        request: AdManagerRequest,
        ad: NimbusAd? = nil,
        mapping: LinearPriceMapping,
    ) {
        if !request.hasDynamicPrice {
            ad?.applyDynamicPrice(into: request, mapping: mapping)
        }
        
        nimbusAd = ad
        load(request)
    }
}
