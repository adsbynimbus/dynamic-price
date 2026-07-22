//
//  DynamicPriceRenderer.swift
//  DynamicPrice
//
//  Created on 23/04/23.
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

    static let jsonDecoder = JSONDecoder()
    static let adCache = NSCache<NSString, CachedAd>()

    static func render(data: String?, block: @escaping (CachedAd, URL) -> Void) {
        Task {
            guard let renderer = DynamicPriceRenderer(info: data),
                  let nimbusAd = DynamicPriceRenderer[renderer.auctionId] else {
                return
            }

            await MainActor.run {
                block(nimbusAd, renderer.googleClickTracker)
            }
        }
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
