// swift-tools-version: 5.10
import PackageDescription

let package = Package(
    name: "dynamic-price",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "NimbusDynamicPrice",
            targets: ["NimbusDynamicPrice"]),
    ],
    dependencies: [
        .package(url: "https://github.com/adsbynimbus/nimbus-ios-sdk", from: "2.32.5"),
        .package(url: "https://github.com/googleads/swift-package-manager-google-mobile-ads", "12.0.0"..<"14.0.0"),
    ],
    targets: [
        .target(
            name: "NimbusDynamicPrice",
            dependencies: [
                .product(name: "GoogleMobileAds", package: "swift-package-manager-google-mobile-ads"),
                .product(name: "NimbusKit", package: "nimbus-ios-sdk"),
            ],
            path: "dynamic-price/ios/src/main"),
        .testTarget(
            name: "NimbusDynamicPriceTests",
            dependencies: ["NimbusDynamicPrice"],
            path: "dynamic-price/ios/src/test"),
    ],
)
