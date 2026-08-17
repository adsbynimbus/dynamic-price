//
//  Mapping.swift
//  
//
//  Created by Jason Sznol on 8/13/26.
//

import NimbusKit

/// Provides a mapping from a Nimbus response to Dynamic Price target
public protocol Mapping {
    func getTarget(_ ad: NimbusAd) -> String
}

/// A mapping using a linear step function to generate the target dynamic price value
public struct LinearPriceGranularity: Comparable, Sendable {

    /// The minimum bid in cents
    public let min: Int

    /// The maximum bid in cents
    public let max: Int

    /// The step size for each line item mapping
    public let step: Int

    /**
     Constructs a new `LinearPriceGranularity`

     - Parameters:
     - min:The minimum bid in cents
     - max: The maximum bid in cents
     - step: The step size for each line item mapping
     */
    public init(min: Int, max: Int, step: Int) {
        self.min = min
        self.max = max
        self.step = step
    }

    /// :nodoc:
    public static func < (lhs: LinearPriceGranularity, rhs: LinearPriceGranularity) -> Bool {
        lhs.min < rhs.min
    }
}

/// A mapping composed of multiple LinearPriceGranularities in ascending order
public struct LinearPriceMapping: Mapping, Sendable {

    /// The granularities used in this mapping
    public let granularities: [LinearPriceGranularity]

    /**
     Constructs a new `LinearPriceMapping`

     - Parameters:
     -  granularities: the granularities to use
     */
    public init(granularities: [LinearPriceGranularity]) {
        self.granularities = granularities.sorted()
    }

    /// Constructs a new `LinearPriceMapping`
    public init(_ granularities: LinearPriceGranularity...) {
        self.init(granularities: granularities)
    }

    /**
     Linearly searches the granularity mappings and returns the keywords to be inserted

     - Parameters:
     - ad: An ad from Nimbus

     - Returns: The keywords to set
     */
    public func getTarget(_ ad: NimbusAd) -> String {
        let range = granularities.first { ad.bidInCents < $0.max } ?? granularities.last!
        return String(min(max(ad.bidInCents - (ad.bidInCents % range.step), range.min), range.max))
    }
}
