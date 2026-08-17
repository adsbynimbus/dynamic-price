//
//  NimbusAd+TargetingTests.swift
//  DynamicPriceTests
//
//  Created on 8/12/22.
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

import GoogleMobileAds
import NimbusKit
import Testing

import DynamicPrice

@Suite(.serialized) struct ApplyDynamicPriceTests {

    @Test func `applyDynamicPrice sets customTargeting for static ads`() async {
        let ad = createNimbusAd(type: .static)
        let request = AdManagerRequest()
        ad.applyDynamicPrice(request, mapping: mapping)

        #expect(request.customTargeting?["na_id"] as? String == ad.id)
        #expect(request.customTargeting?["na_bid"] as? String == mapping.getTarget(ad))
        #expect(request.customTargeting?["na_network"] as? String == ad.bid.ext?.omp?.buyer)
        #expect(request.customTargeting?["na_size"] as? String == "\(ad.bid.w!)x\(ad.bid.h!)")
        #expect(request.customTargeting?["na_type"] as? String == "static")
        #expect(request.customTargeting?["na_render"] as? String == "static")
        #expect(request.customTargeting?["na_bid_video"] == nil)
    }

    @Test func `applyDynamicPrice sets customTargeting for video ads`() async {
        let ad = createNimbusAd(type: .video)
        let request = AdManagerRequest()

        ad.applyDynamicPrice(request, mapping: mapping)

        #expect(request.customTargeting?["na_id"] as? String == ad.id)
        #expect(request.customTargeting?["na_network"] as? String == ad.bid.ext?.omp?.buyer)
        #expect(request.customTargeting?["na_size"] as? String == "0x0")
        #expect(request.customTargeting?["na_bid_video"] as? String == mapping.getTarget(ad))
        #expect(request.customTargeting?["na_type"] as? String == "video")
        #expect(request.customTargeting?["na_render"] as? String == "video")
    }

    @Test func `applyDynamicPrice does not modify other customTargeting values`() async {
        let ad = createNimbusAd(type: .static)
        let request = AdManagerRequest()
        request.customTargeting = [:]
        request.customTargeting?["test_key"] = "test_value"

        ad.applyDynamicPrice(request, mapping: mapping)

        #expect(request.customTargeting?["na_id"] as? String == ad.id)
        #expect(request.customTargeting?["na_network"] as? String == ad.bid.ext?.omp?.buyer)
        #expect(request.customTargeting?["na_size"] as? String == "\(ad.bid.w!)x\(ad.bid.h!)")
        #expect(request.customTargeting?["na_bid"] as? String == "200")
        #expect(request.customTargeting?["na_type"] as? String == "static")
        #expect(request.customTargeting?["na_render"] as? String == "static")
        #expect(request.customTargeting?["test_key"] as? String == "test_value")
    }

    @MainActor
    @Test func `applyDynamicPrice uses na_bid=0 when Nimbus.shared.testMode = true`() async {
        Nimbus.configuration.testMode = true
        let ad = createNimbusAd(type: .static)
        let request = AdManagerRequest()
        request.customTargeting = [:]

        ad.applyDynamicPrice(request, mapping: mapping)

        #expect(request.customTargeting?["na_bid"] as? String == "0")
        #expect(request.customTargeting?["na_bid_video"] == nil)

        Nimbus.configuration.testMode = false
    }

    @MainActor
    @Test func `applyDynamicPrice uses na_bid_video=0 when Nimbus.shared.testMode = true`() async {
        Nimbus.configuration.testMode = true
        let ad = createNimbusAd(type: .video)
        let request = AdManagerRequest()
        request.customTargeting = [:]

        ad.applyDynamicPrice(request, mapping: mapping)

        #expect(request.customTargeting?["na_bid_video"] as? String == "0")
        #expect(request.customTargeting?["na_bid"] == nil)

        Nimbus.configuration.testMode = false
    }
}
