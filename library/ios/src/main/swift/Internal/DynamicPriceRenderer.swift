//
//  DynamicPriceRenderer.swift
//  DynamicPrice
//
//  Created on 7/21/26.
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

import Foundation
import NimbusKit

/// :nodoc:
internal struct DynamicPriceRenderer: Codable {

    class CachedAd: NSObject {
        let value: NimbusAd

        init(_ ad: NimbusAd) {
            self.value = ad
        }
    }

    static let adCache = {
        let cache = NSCache<NSString, CachedAd>()
        cache.countLimit = 12
        return cache
    }()
    static let jsonDecoder = JSONDecoder()

    static func render(data: String?, block: @escaping (CachedAd, URL) -> Void) -> NimbusAd? {
        guard let renderer = DynamicPriceRenderer(info: data),
              let nimbusAd = DynamicPriceRenderer[renderer.auctionId] else {
            return nil
        }
        
        block(nimbusAd, renderer.googleClickTracker)

        return nimbusAd.value
    }

    let auctionId: String
    let googleClickTracker: URL

    enum CodingKeys: String, CodingKey {
        case auctionId = "na_id"
        case googleClickTracker = "ga_click"
    }
    
    init(auctionId: String, googleClickTracker: URL) {
        self.auctionId = auctionId
        self.googleClickTracker = googleClickTracker
    }
    
    init?(info: String?) {
        guard let data = info?.data(using: .utf8),
            let _self = try? Self.jsonDecoder.decode(DynamicPriceRenderer.self, from: data) else {
            return nil
        }

        self = _self
    }

    static subscript (_ id: String) -> CachedAd? {
        get {
            Self.adCache.object(forKey: id as NSString)
        }
        set(newValue) {
            if let newValue {
                Self.adCache.setObject(newValue, forKey: id as NSString)
            } else {
                Self.adCache.removeObject(forKey: id as NSString)
            }
        }
    }
}
