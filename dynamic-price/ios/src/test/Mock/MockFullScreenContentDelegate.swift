//
//  MockFullScreenContentDelegate.swift
//  DynamicPriceTests
//
//  Created on 2/29/24
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

import Foundation
import GoogleMobileAds

final class MockFullScreenContentDelegate: NSObject, FullScreenContentDelegate {
    enum State: Equatable {
        case didFailToPresent(ad: FullScreenPresentingAd, error: Error)
        case adDidRecordImpression(ad: FullScreenPresentingAd)
        case adDidRecordClick(ad: FullScreenPresentingAd)
        case adWillPresentFullScreenContent(ad: FullScreenPresentingAd)
        case adWillDismissFullScreenContent(ad: FullScreenPresentingAd)
        case adDidDismissFullScreenContent(ad: FullScreenPresentingAd)
        
        static func == (
            lhs: MockFullScreenContentDelegate.State,
            rhs: MockFullScreenContentDelegate.State
        ) -> Bool {
            switch (lhs, rhs) {
            case (.didFailToPresent(let lhsAd, let lhsError), .didFailToPresent(let rhsAd, let rhsError)):
                return lhsAd === rhsAd && (lhsError as NSError) == (rhsError as NSError)
            case (.adDidRecordImpression(let lhsAd), .adDidRecordImpression(let rhsAd)):
                return lhsAd === rhsAd
            case (.adDidRecordClick(let lhsAd), .adDidRecordClick(let rhsAd)):
                return lhsAd === rhsAd
            case (.adWillPresentFullScreenContent(let lhsAd), .adWillPresentFullScreenContent(let rhsAd)):
                return lhsAd === rhsAd
            case (.adWillDismissFullScreenContent(let lhsAd), .adWillDismissFullScreenContent(let rhsAd)):
                return lhsAd === rhsAd
            case (.adDidDismissFullScreenContent(let lhsAd), .adDidDismissFullScreenContent(let rhsAd)):
                return lhsAd === rhsAd
            default:
                return false
            }
        }
    }
    
    var state: State?
    
    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        state = .didFailToPresent(ad: ad, error: error)
    }
    
    func adDidRecordImpression(_ ad: FullScreenPresentingAd) {
        state = .adDidRecordImpression(ad: ad)
    }
    
    func adDidRecordClick(_ ad: FullScreenPresentingAd) {
        state = .adDidRecordClick(ad: ad)
    }
    
    func adWillPresentFullScreenContent(_ ad: FullScreenPresentingAd) {
        state = .adWillPresentFullScreenContent(ad: ad)
    }
    
    func adWillDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        state = .adWillDismissFullScreenContent(ad: ad)
    }
    
    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        state = .adDidDismissFullScreenContent(ad: ad)
    }
}
