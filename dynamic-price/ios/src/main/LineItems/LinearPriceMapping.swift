//
//  LinearPriceMapping.swift
//  DynamicPrice
//
//  Created on 12/10/20.
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

import NimbusKit

/// Provides a mapping from a Nimbus response to keywords
public protocol DynamicPriceMapping {

    /**
     The keyword to be inserted
     
     - Parameters:
     - ad: An ad from Nimbus
     
     - Returns: The keywords to set
     */
    func getKeywords(ad: NimbusAd) -> String?
}

/// A mapping composed of multiple LinearPriceGranularities in ascending order
public struct LinearPriceMapping: DynamicPriceMapping, Sendable {
    
    /// The granularities used in this mapping
    let granularities: [LinearPriceGranularity]
    
    /**
     Constructs a new `LinearPriceMapping`
     
     - Parameters:
     -  granularities: the granularities to use
     */
    public init(granularities: [LinearPriceGranularity]) {
        self.granularities = granularities.sorted()
    }
    
    /**
     Linearly searches the granularity mappings and returns the keywords to be inserted
     
     - Parameters:
     - ad: An ad from Nimbus
     
     - Returns: The keywords to set
     */
    public func getKeywords(ad: NimbusAd) -> String? {
        for granularity in granularities {
            if ad.bidInCents < granularity.max {
                return granularity.getKeywords(ad: ad)
            }
        }
        return granularities.last?.getKeywords(ad: ad)
    }
}
