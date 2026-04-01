// swift-tools-version: 5.10
import PackageDescription

let package = Package(
    name: "dynamic-price",
    platforms: [.iOS(.v15)],
    dependencies: [
        .package(url: "https://github.com/adsbynimbus/nimbus-ios-sdk", from: "2.32.2"),
        .package(url: "https://github.com/googleads/swift-package-manager-google-mobile-ads", from: "12.0.0"),
    ],
    targets: [
        .target(
            name: "DynamicPrice",
            dependencies: [
                .product(name: "GoogleMobileAds", package: "swift-package-manager-google-mobile-ads"),
                .product(name: "NimbusKit", package: "nimbus-ios-sdk"),
            ],
            path: "dynamic-price/ios/Sources/DynamicPrice"),
        .testTarget(
            name: "DynamicPriceTests",
            dependencies: ["DynamicPrice"],
            path: "dynamic-price/ios/Tests/DynamicPriceTests"),
    ],
)
