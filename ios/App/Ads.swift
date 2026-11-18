import UIKit
import GoogleMobileAds
import AppTrackingTransparency

/// "광고 보고 문구 하나 더 받기"용 보상형 광고.
/// 광고 단위 ID는 Info.plist의 AdmobRewardedID (빌드 때 Secrets로 넣고, 없으면 Google 테스트 광고).
final class Ads: NSObject, FullScreenContentDelegate {
    static let shared = Ads()

    private var ad: RewardedAd?
    private var loading = false
    private var waiting: ((Result) -> Void)?
    private weak var presenter: UIViewController?
    private var finish: ((Result) -> Void)?
    private var earned = false

    enum Result { case reward, closed, unavailable }

    private var unitID: String {
        (Bundle.main.object(forInfoDictionaryKey: "AdmobRewardedID") as? String)
            .flatMap { $0.isEmpty || $0.hasPrefix("$(") ? nil : $0 } ?? "ca-app-pub-3940256099942544/1712485313"
    }

    func start() {
        MobileAds.shared.start { [weak self] _ in self?.load() }
    }

    func show(done: @escaping (Result) -> Void) {
        guard let vc = Self.topController() else { done(.unavailable); return }
        askTrackingIfNeeded { [weak self] in
            guard let self else { return }
            if let ad = self.ad {
                self.present(ad, from: vc, done: done)
            } else {
                self.waiting = done
                self.presenter = vc
                self.load()
            }
        }
    }

    /// 광고 버튼을 처음 누를 때만 묻는다. 거부해도 보상은 똑같다
    private func askTrackingIfNeeded(_ next: @escaping () -> Void) {
        if ATTrackingManager.trackingAuthorizationStatus == .notDetermined {
            ATTrackingManager.requestTrackingAuthorization { _ in
                DispatchQueue.main.async { next() }
            }
        } else {
            next()
        }
    }

    private func load() {
        guard ad == nil, !loading else { return }
        loading = true
        RewardedAd.load(with: unitID, request: Request()) { [weak self] loaded, _ in
            DispatchQueue.main.async {
                guard let self else { return }
                self.loading = false
                if let loaded {
                    self.ad = loaded
                    if let done = self.waiting, let vc = self.presenter {
                        self.waiting = nil
                        self.present(loaded, from: vc, done: done)
                    }
                } else if let done = self.waiting {
                    self.waiting = nil
                    done(.unavailable)
                }
            }
        }
    }

    private func present(_ ad: RewardedAd, from vc: UIViewController, done: @escaping (Result) -> Void) {
        self.ad = nil
        earned = false
        finish = done
        ad.fullScreenContentDelegate = self
        ad.present(from: vc) { [weak self] in self?.earned = true }
    }

    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        finish?(earned ? .reward : .closed)
        finish = nil
        load()
    }

    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        finish?(.unavailable)
        finish = nil
        load()
    }

    static func topController() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.first
        var vc = scene?.windows.first(where: { $0.isKeyWindow })?.rootViewController ?? scene?.windows.first?.rootViewController
        while let p = vc?.presentedViewController { vc = p }
        return vc
    }
}
