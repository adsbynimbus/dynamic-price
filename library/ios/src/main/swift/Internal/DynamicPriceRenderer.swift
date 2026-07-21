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
struct DynamicPriceRenderer: Codable {
    static let jsonDecoder = JSONDecoder()
    
    let auctionId: String
    let googleClickEventUrl: URL
    
    enum CodingKeys: String, CodingKey {
        case auctionId = "na_id"
        case googleClickEventUrl = "ga_click"
    }
    
    init(auctionId: String, googleClickEventUrl: URL) {
        self.auctionId = auctionId
        self.googleClickEventUrl = googleClickEventUrl
    }
    
    init?(info: String?) {
        guard let data = info?.data(using: .utf8),
            let _self = try? Self.jsonDecoder.decode(DynamicPriceRenderer.self, from: data) else {
            return nil
        }

        self = _self
    }
}
