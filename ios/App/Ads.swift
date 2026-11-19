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
    private var started = false
    private var retried = false

    enum Result { case reward, closed, unavailable }

    private var unitID: String {
        (Bundle.main.object(forInfoDictionaryKey: "AdmobRewardedID") as? String)
            .flatMap { $0.isEmpty || $0.hasPrefix("$(") ? nil : $0 } ?? "ca-app-pub-3940256099942544/1712485313"
    }

    /// 추적 허용 질문(처음 한 번) → 답을 받은 뒤 광고 SDK 시작 → 광고 미리 불러오기.
    /// 그 전에는 광고 SDK가 아무 데이터도 모으지 않는다.
    func startAfterTrackingPrompt() {
        askTrackingIfNeeded { [weak self] in
            guard let self, !self.started else { return }
            self.started = true
            MobileAds.shared.start { _ in
                DispatchQueue.main.async { self.load() }
            }
        }
    }

    func show(done: @escaping (Result) -> Void) {
        guard let vc = Self.topController() else { done(.unavailable); return }
        guard started else {
            // 아직 추적 질문·SDK 시작 전이면 먼저 끝내고, 광고가 불러와지면 이어서 보여 준다
            waiting = done
            presenter = vc
            startAfterTrackingPrompt()
            return
        }
        if let ad = self.ad {
            present(ad, from: vc, done: done)
        } else {
            waiting = done
            presenter = vc
            load()
        }
    }

    /// 앱을 처음 켤 때 한 번 묻는다. 거부해도 앱과 광고 보상은 똑같다
    private func askTrackingIfNeeded(_ next: @escaping () -> Void) {
        if ATTrackingManager.trackingAuthorizationStatus == .notDetermined {
            ATTrackingManager.requestTrackingAuthorization { status in
                DispatchQueue.main.async {
                    // 드물게 앱이 아직 활성 상태가 아니면 notDetermined로 바로 돌아온다 → 잠시 뒤 한 번 더
                    if status == .notDetermined && !self.retried {
                        self.retried = true
                        DispatchQueue.main.asyncAfter(deadline: .now() + 1) { self.askTrackingIfNeeded(next) }
                    } else {
                        next()
                    }
                }
            }
        } else {
            next()
        }
    }

    private func load() {
        guard started, ad == nil, !loading else { return }
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
