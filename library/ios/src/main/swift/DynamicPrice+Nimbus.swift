//
//  DynamicPrice+Nimbus.swift
//  DynamicPrice
//
//  Created on 7/22/26.
//  Copyright © 2026 Nimbus Advertising Solutions Inc. All rights reserved.
//

import NimbusKit

public extension NimbusRequest {

    /// Helper method for adding in-article or in-feed video inventory to an MREC or larger banner request.
    ///
    /// - Parameter inArticle: A Boolean value indicating whether the video is placed inside an article.
    ///   If `true`, uses `.inArticle` placement; otherwise, uses `.inFeed`.
    func addInlineVideo(inArticle: Bool = false) {
        guard let banner = impressions.first?.banner, banner.height > 100 else { return }
        /*
           .interstitial() sets all the expected video parameters,the fullscreen indication is
           removed by setting position to .unknown
         */
        impressions[0].video = .interstitial()
        impressions[0].video?.position = .unknown
        impressions[0].video?.placementType = inArticle ? .inArticle : .inFeed
    }
}
