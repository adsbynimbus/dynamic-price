//
//  DynamicPriceView.swift
//  ios-samples
//
//  Created by Jason Sznol on 9/22/26.
//  Copyright © 2026 AdsByNimbus. All rights reserved.
//

import DynamicPrice
import GoogleMobileAds
import NimbusKit
import SwiftUI

private let refreshInterval: TimeInterval = 30

@MainActor
public class DynamicPriceView: UIView, AppEventDelegate {

    private let googleBanner: AdManagerBannerView
    private let nimbusRequestManager: NimbusRequestManager
    private let nimbusRequest: () -> NimbusRequest
    private var lastRequestTime: Date = Date.distantPast
    private var refreshTask: Task<Void, Error>?

    /// Set this delegate to receive events from the GAMBannerView
    public weak var delegate: BannerViewDelegate?

    /// Set to true for banner ads if they refresh when not on the screen
    public var useOnScreenCheck = false

    public init(
        adSize: AdSize,
        adUnitId: String,
        nimbusRequest: @autoclosure @escaping () -> NimbusRequest,
    ) {
        self.googleBanner = AdManagerBannerView(adSize: adSize)
        self.nimbusRequest = nimbusRequest
        self.nimbusRequestManager = NimbusRequestManager()
        super.init(frame: .zero)
        googleBanner.adUnitID = adUnitId
        googleBanner.appEventDelegate = self
        addSubview(googleBanner)
        googleBanner.translatesAutoresizingMaskIntoConstraints = false
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    public func loadAd() async {
        lastRequestTime = Date()

        // See DynamicPrice+Helper for NimbusRequestManager async extension
        let nimbusBid = try? await nimbusRequestManager.makeRequest(nimbusRequest())

        let request = AdManagerRequest()
        request.customTargeting = [:]

        nimbusBid?.applyDynamicPrice(request, mapping: DynamicPriceApp.mapping)

        googleBanner.load(request)
    }

    public nonisolated func adView(
        _ banner: BannerView,
        didReceiveAppEvent name: String,
        with info: String?
    ) {
        Task { @MainActor in banner.handleEventForNimbus(name: name, info: info) }
    }

    public override func willMove(toWindow newWindow: UIWindow?) {
        removeVisibilityListeners()

        guard let newWindow = newWindow else {
            self.refreshTask?.cancel()
            return
        }

        onVisibilityChanged(newWindow) { isVisible in
            Task { @MainActor [weak self = self] in
                isVisible ? self?.startRefresh() : self?.refreshTask?.cancel()
            }
        }

        startRefresh()
    }

    public override func willMove(toSuperview newSuperview: UIView?) {
        NSLayoutConstraint.deactivate(constraints)
    }

    public override func didMoveToSuperview() {
        guard let parent = superview else { return }
        translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            topAnchor.constraint(equalTo: parent.topAnchor),
            bottomAnchor.constraint(equalTo: parent.bottomAnchor),
            leftAnchor.constraint(equalTo: parent.leftAnchor),
            rightAnchor.constraint(equalTo: parent.rightAnchor),
            googleBanner.centerXAnchor.constraint(equalTo: centerXAnchor),
            googleBanner.centerYAnchor.constraint(equalTo: centerYAnchor),
        ])
    }

    private func startRefresh() {
        guard refreshTask == nil || refreshTask?.isCancelled == true else { return }

        refreshTask = Task {
            while !Task.isCancelled {
                // If not on screen, sleep for 200 milliseconds and then check again
                guard !useOnScreenCheck || isOnScreen else {
                    await Task.sleep(seconds: 0.2)
                    continue
                }

                // Task.sleep(seconds:) extension is located in DynamicPrice+Helper.swift
                await Task.sleep(seconds: refreshInterval - Date().timeIntervalSince(lastRequestTime))
                guard !Task.isCancelled else { break }

                await loadAd()
            }
        }
    }
}

extension UIView {

    var isOnScreen: Bool {
        guard let window else { return false }

        return !window.bounds.intersection(convert(bounds, to: nil)).isEmpty
    }

    @inlinable
    func removeVisibilityListeners() {
        NotificationCenter.default.removeObserver(
            self, name: UIWindow.didBecomeVisibleNotification, object: window)
        NotificationCenter.default.removeObserver(
            self, name: UIWindow.didBecomeHiddenNotification, object: window)
        NotificationCenter.default.removeObserver(
            self, name: UIApplication.didBecomeActiveNotification, object: UIApplication.shared)
        NotificationCenter.default.removeObserver(
            self, name: UIApplication.didEnterBackgroundNotification, object: UIApplication.shared)
    }

    @inlinable
    func onVisibilityChanged(_ target: UIWindow, _ onChange: @escaping @Sendable (Bool) -> Void) {
        NotificationCenter.default.addObserver(
            forName: UIWindow.didBecomeVisibleNotification,
            object: target,
            queue: nil
        ) { _ in onChange(true) }
        NotificationCenter.default.addObserver(
            forName: UIWindow.didBecomeHiddenNotification,
            object: target,
            queue: nil
        ) { _ in onChange(false) }
        NotificationCenter.default.addObserver(
            forName: UIApplication.didBecomeActiveNotification,
            object: UIApplication.shared,
            queue: nil
        ) { _ in onChange(true) }
        NotificationCenter.default.addObserver(
            forName: UIApplication.didEnterBackgroundNotification,
            object: UIApplication.shared,
            queue: nil
        ) { _ in onChange(false) }
    }

    public func parentViewController() -> UIViewController {
        var responder: UIResponder? = self
        while !(responder is UIViewController) {
            responder = responder?.next
            if nil == responder {
                break
            }
        }
        return (responder as? UIViewController)!
    }
}

struct DynamicPriceInlineAd: UIViewRepresentable {
    typealias UIViewType = DynamicPriceView
    let adSize: AdSize
    let nimbusRequest: NimbusRequest

    init(adSize: AdSize, nimbusRequest: NimbusRequest) {
        self.adSize = adSize
        self.nimbusRequest = nimbusRequest
    }

    func makeUIView(context: Context) -> DynamicPriceView {
        let banner = DynamicPriceView(
            adSize: adSize,
            adUnitId: DynamicPriceApp.adUnitId,
            nimbusRequest: nimbusRequest,
        )
        banner.delegate = context.coordinator
        return banner
    }

    func updateUIView(_ uiView: DynamicPriceView, context: Context) { }

    func makeCoordinator() -> DynamicPriceInlineAdCoordinator {
        DynamicPriceInlineAdCoordinator(self)
    }

    class DynamicPriceInlineAdCoordinator: NSObject, BannerViewDelegate {
        let inlineAd: DynamicPriceInlineAd
        var name: String

        init(_ inlineAd: DynamicPriceInlineAd) {
            self.inlineAd = inlineAd
            let sizeDesc = String(describing: inlineAd.adSize)
            self.name = sizeDesc.replacingOccurrences(of: "AdSize", with: "")
        }

        func bannerViewDidReceiveAd(_ bannerView: BannerView) {
            // If using anchored adaptive banner, the following block will resize the ad to match
            // the parent's frame and will scale up the ad to fit/
            if let banner = bannerView as? AdManagerBannerView, let parent = banner.superview {
                banner.resize(adSizeFor(cgSize: parent.frame.size))
            }
            print("DynamicPrice: \(name) loaded")
        }

        func bannerViewDidFailToReceiveAdWithError(_ bannerView: BannerView, error: Error) {
            print("DynamicPrice: \(name) load failed \(error.localizedDescription)")
        }

        func bannerViewDidRecordImpression(_ bannerView: BannerView) {
            print("DynamicPrice: \(name) impression")
        }

        func bannerViewDidRecordClick(_ bannerView: BannerView) {
            print("DynamicPrice: \(name) clicked")
        }
    }
}
