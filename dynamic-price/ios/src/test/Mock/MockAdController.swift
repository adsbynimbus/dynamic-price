//
//  MockAdController.swift
//  NimbusGAMKitTests
//  Created on 3/1/24
//  Copyright © 2024 Nimbus Advertising Solutions Inc. All rights reserved.
//

import NimbusKit
import UIKit

class MockAdController: AdController {
    var delegate: AdControllerDelegate?
    
    var friendlyObstructions: [UIView]?
    
    var isClickProtectionEnabled: Bool = true
    
    var volume: Int = 0
    
    var adView: UIView?
    
    var adDuration: CGFloat = 0.0
    
    func start() {
    }
    
    func stop() {
        
    }
    
    func destroy() {
        
    }
    
    init() {
        
    }
}
