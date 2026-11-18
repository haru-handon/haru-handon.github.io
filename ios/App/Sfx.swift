import AVFoundation
import UIKit

enum Sound: String, CaseIterable {
    case tick = "sfx_tick", clunk = "sfx_clunk", chime = "sfx_chime", ting = "sfx_ting", flip = "sfx_flip", fanfare = "sfx_fanfare"
}

/// 짧은 효과음 (안드로이드와 같은 wav). 무음 스위치를 따르는 ambient 세션.
final class Sfx {
    static let shared = Sfx()
    var enabled = true
    private var pools: [Sound: [AVAudioPlayer]] = [:]
    private var next: [Sound: Int] = [:]

    init() {
        try? AVAudioSession.sharedInstance().setCategory(.ambient, options: [.mixWithOthers])
        for s in Sound.allCases {
            guard let url = Bundle.main.url(forResource: s.rawValue, withExtension: "wav") else { continue }
            let count = s == .tick ? 4 : 2
            pools[s] = (0..<count).compactMap { _ in
                let p = try? AVAudioPlayer(contentsOf: url)
                p?.prepareToPlay()
                return p
            }
        }
    }

    func play(_ s: Sound, volume: Float = 1, jitter: Bool = false) {
        guard enabled, let pool = pools[s], !pool.isEmpty else { return }
        let i = (next[s] ?? 0) % pool.count
        next[s] = i + 1
        let p = pool[i]
        p.volume = volume
        if jitter {
            p.enableRate = true
            p.rate = Float.random(in: 0.94...1.06)
        }
        p.currentTime = 0
        p.play()
    }
}

enum Haptics {
    static func tick() { UIImpactFeedbackGenerator(style: .light).impactOccurred() }
    static func thud() { UIImpactFeedbackGenerator(style: .heavy).impactOccurred() }
    static func confirm() { UINotificationFeedbackGenerator().notificationOccurred(.success) }
}
