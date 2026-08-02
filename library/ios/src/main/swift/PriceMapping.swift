//
//  PriceMapping.swift
//  DynamicPrice
//
//  Created on 5/21/26.
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

public protocol PriceMapping {
    func getTarget(price: Double) -> String
}

/// A PriceMapping using a linear step function to generate the target value
public struct LinearPriceGranularity {
    public let min: Int
    public let max: Int
    public let step: Int
}

/// A PriceMapping composed of multiple LinearPriceGranularities in sorted order
public struct LinearPriceMapping : PriceMapping {
    public let granularities: [LinearPriceGranularity]

    public init(_ mapping: [LinearPriceGranularity]) {
        self.granularities = mapping.sorted(by: { $0.min < $1.min })

        if granularities.count > 1 {
            let _ = granularities.dropFirst(1).reduce(granularities[0]) {
                guard $0.max == $1.min else {
                    fatalError("Use contiguous granularities: \($0.max) != \($1.min)")
                }
                return $1
            }
        }
    }

    public func getTarget(price: Double) -> String {
        let priceInCents = Int(price * 100)
        let bucket = granularities.first(where: { priceInCents < $0.max }) ?? granularities.last!
        return "\(min(max(priceInCents - priceInCents % bucket.step, bucket.min), bucket.max))"
    }
}
