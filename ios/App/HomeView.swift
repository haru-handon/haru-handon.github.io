import SwiftUI

struct HomeView: View {
    @ObservedObject var model: HomeModel

    var body: some View {
        ZStack {
            Palette.ink.ignoresSafeArea()
            RadialGradient(colors: [Palette.inkLift.opacity(0.9), .clear], center: .init(x: 0.5, y: 0.38),
                           startRadius: 0, endRadius: 420)
                .ignoresSafeArea()

            VStack(spacing: 0) {
                Header(model: model)
                TierLine(don: model.shownDon)
                stage
                FortuneCard(model: model)
                Foot(model: model)
            }

            if model.dogamOpen {
                CollectionView(model: model)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .zIndex(10)
            }
            if let don = model.celebration {
                NyangCelebration(don: don) { withAnimation { model.celebration = nil } }
                    .transition(.opacity)
                    .zIndex(11)
            }
            if model.fakeAd {
                AdStandIn { model.grantBonus() }
                    .zIndex(12)
            }
        }
        .animation(.easeOut(duration: 0.25), value: model.dogamOpen)
        .preferredColorScheme(.dark)
    }

    private var stage: some View {
        ZStack {
            VaultView(tier: tierIndex(model.shownDon), door: model.door, dial: model.dial, lever: model.lever,
                      lift: model.lift, showHint: model.phase == .closed, labels: model.labels,
                      freshLabel: model.freshLabel, onDoorTap: model.openVault) {
                InteractiveGoldBar(don: model.shownDon, enabled: model.phase == .open, shineAt: model.shineAt,
                                   resetToken: model.barResetToken, flipToken: model.flipToken,
                                   onTap: { p in
                                       model.tapBar(at: UnitPoint(x: 0.5 + (p.x - 0.5) * 0.55, y: 0.5 + (p.y - 0.5) * 0.75))
                                   },
                                   onFlip: { _ in model.barFlipped() })
            }
            RadialGradient(colors: [Palette.goldHi.opacity(0.55), .clear], center: .center, startRadius: 0, endRadius: 260)
                .opacity(model.flash ? 1 : 0)
                .allowsHitTesting(false)
            SparkLayer(burst: model.burst)
            PlusOne(token: model.plusOne)
                .frame(maxHeight: .infinity, alignment: .top)
                .padding(.top, 8)
            ToastView(toast: model.toast)
                .frame(maxHeight: .infinity, alignment: .top)
                .padding(.top, 4)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

/* ---------- 위쪽 ---------- */

private struct Header: View {
    @ObservedObject var model: HomeModel

    var body: some View {
        HStack(alignment: .bottom) {
            VStack(alignment: .leading, spacing: 4) {
                Text("하루 한돈").font(.serif(21)).foregroundStyle(Palette.goldHi)
                HStack(spacing: 10) {
                    Text("연속 출석 \(model.streak)일").font(.system(size: 11)).foregroundStyle(Palette.muted)
                    Button(action: model.toggleSound) {
                        Text(model.soundOn ? "효과음 켬" : "효과음 끔")
                            .font(.system(size: 11))
                            .foregroundStyle(model.soundOn ? Palette.gold : Palette.faint)
                            .padding(.vertical, 4)
                    }
                    Button { model.dogamOpen = true } label: {
                        Text("도감 \(model.collected)/\(Fortunes.all.count)")
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundStyle(Palette.goldHi)
                            .padding(.horizontal, 8).padding(.vertical, 2)
                            .overlay(RoundedRectangle(cornerRadius: 10).stroke(Palette.goldLo, lineWidth: 1))
                    }
                }
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 0) {
                Text(donLabel(model.shownDon))
                    .font(.serif(24))
                    .foregroundStyle(Palette.goldHi)
                    .contentTransition(.numericText())
                Text("\(gramLabel(model.shownDon)) · \(model.shownDon)돈")
                    .font(.system(size: 11.5)).foregroundStyle(Palette.muted)
            }
        }
        .padding(.horizontal, 22)
        .padding(.top, 16)
    }
}

private struct TierLine: View {
    let don: Int
    var body: some View {
        let tier = tierOf(don)
        let next = nextTier(don)
        let progress = next.map { Double(don - tier.min) / Double($0.min - tier.min) } ?? 1
        HStack(spacing: 10) {
            Text(tier.name).font(.system(size: 12, weight: .semibold)).foregroundStyle(Palette.gold)
            GeometryReader { g in
                ZStack(alignment: .leading) {
                    Capsule().fill(Palette.line)
                    Capsule().fill(LinearGradient(colors: [Palette.goldLo, Palette.goldHi], startPoint: .leading, endPoint: .trailing))
                        .frame(width: g.size.width * min(max(progress, 0), 1))
                }
            }
            .frame(height: 4)
            Text(next.map { "\($0.short)까지 \($0.min - don)돈" } ?? "최고 단계")
                .font(.system(size: 12)).foregroundStyle(Palette.muted)
        }
        .padding(.horizontal, 22)
        .padding(.vertical, 10)
    }
}

/* ---------- 오늘의 문구 ---------- */

private struct FortuneCard: View {
    @ObservedObject var model: HomeModel

    static func dateText(_ d: Date) -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "ko_KR")
        f.dateFormat = "M.d E"
        return f.string(from: d)
    }

    var body: some View {
        let today = Date()
        let fortune = model.fortuneVisible ? model.fortune : nil
        let bonus = model.fortuneVisible ? model.bonus : nil
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 8) {
                Text("오늘의 재물운").font(.system(size: 11, weight: .semibold)).kerning(0.9).foregroundStyle(Palette.gold)
                if let f = fortune {
                    CategoryChip(label: f.category.label)
                    if model.fortuneIsNew { NewBadge() }
                }
                Spacer()
                Text(Self.dateText(today))
                    .font(.system(size: 11)).foregroundStyle(Palette.faint)
            }
            if let f = fortune {
                Text(f.text).font(.serif(15.5)).lineSpacing(5).foregroundStyle(Palette.text)
                    .fixedSize(horizontal: false, vertical: true)
                if let b = bonus {
                    Rectangle().fill(Palette.line).frame(height: 1).padding(.vertical, 4)
                    HStack(spacing: 8) {
                        Text("하나 더").font(.system(size: 11, weight: .semibold)).foregroundStyle(Palette.gold)
                        CategoryChip(label: b.category.label)
                        if model.bonusIsNew { NewBadge() }
                    }
                    Text(b.text).font(.serif(15.5)).lineSpacing(5).foregroundStyle(Palette.text)
                        .fixedSize(horizontal: false, vertical: true)
                } else {
                    HStack {
                        Spacer()
                        Button(action: model.askBonus) {
                            HStack(spacing: 6) {
                                Text("▶").font(.system(size: 9)).foregroundStyle(Palette.gold)
                                Text("광고 보고 문구 하나 더 받기").font(.system(size: 12, weight: .semibold)).foregroundStyle(Palette.goldHi)
                            }
                            .padding(.horizontal, 12).padding(.vertical, 6)
                            .overlay(Capsule().stroke(Palette.goldLo, lineWidth: 1))
                        }
                    }
                    .padding(.top, 6)
                }
            } else {
                Text("금고를 열면 오늘의 문구가 나와요.").font(.system(size: 13)).foregroundStyle(Palette.faint)
            }
        }
        .padding(.horizontal, 16).padding(.vertical, 14)
        .frame(maxWidth: .infinity, minHeight: 68, alignment: .topLeading)
        .background(Color.white.opacity(0.035), in: RoundedRectangle(cornerRadius: 14))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(Palette.line, lineWidth: 1))
        .padding(.horizontal, 18)
    }
}

struct CategoryChip: View {
    let label: String
    var body: some View {
        Text(label).font(.system(size: 10.5)).foregroundStyle(Palette.goldHi)
            .padding(.horizontal, 6)
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(Palette.goldLo, lineWidth: 1))
    }
}

struct NewBadge: View {
    var body: some View {
        Text("NEW").font(.system(size: 9.5, weight: .heavy)).foregroundStyle(Color(hex: 0x2A1C04))
            .padding(.horizontal, 6)
            .background(Palette.gold, in: RoundedRectangle(cornerRadius: 8))
    }
}

/* ---------- 아래 ---------- */

private struct Foot: View {
    @ObservedObject var model: HomeModel

    var body: some View {
        let open = model.phase == .open
        VStack(spacing: 8) {
            Text(open ? "금괴를 잡고 옆으로 돌리면 뒷면이 보여요" : "하루 한 번 금고를 열면 금 1돈과 문구 하나를 받아요")
                .font(.system(size: 11.5)).foregroundStyle(Palette.faint)
            HStack(spacing: 10) {
                Button { model.dogamOpen = true } label: {
                    HStack(spacing: 8) {
                        Text("문구 도감").font(.system(size: 13, weight: .bold)).foregroundStyle(Palette.goldHi)
                        Text("\(model.collected)/\(Fortunes.all.count)").font(.system(size: 12)).foregroundStyle(Palette.gold)
                        Text("›").font(.system(size: 15, weight: .bold)).foregroundStyle(Palette.goldHi)
                    }
                    .padding(.leading, 18).padding(.trailing, 14).padding(.vertical, 10)
                    .background(Palette.goldLo.opacity(0.18), in: Capsule())
                    .overlay(Capsule().stroke(Palette.gold, lineWidth: 1))
                }
                if open {
                    Button(action: model.closeVault) {
                        Text("금괴 넣기").font(.system(size: 13)).foregroundStyle(Palette.text)
                            .padding(.horizontal, 24).padding(.vertical, 10)
                            .background(Palette.panel, in: Capsule())
                            .overlay(Capsule().stroke(Palette.line, lineWidth: 1))
                    }
                }
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: 92)
        .padding(.horizontal, 22)
    }
}

/* ---------- 효과 ---------- */

struct ToastView: View {
    let toast: HomeModel.Toast?
    @State private var visible: HomeModel.Toast?

    var body: some View {
        ZStack {
            if let t = visible {
                Text(t.text)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(Palette.goldHi)
                    .padding(.horizontal, 16).padding(.vertical, 8)
                    .background(Color(hex: 0x1B1A16, alpha: 0.92), in: Capsule())
                    .overlay(Capsule().stroke(Palette.goldLo, lineWidth: 1))
                    .transition(.move(edge: .top).combined(with: .opacity))
                    .id(t.id)
            }
        }
        .animation(.easeOut(duration: 0.25), value: visible)
        .onChange(of: toast) { t in
            visible = t
            guard let t else { return }
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.6) {
                if visible == t { visible = nil }
            }
        }
        .allowsHitTesting(false)
    }
}

struct PlusOne: View {
    let token: Int
    @State private var start: Date?

    var body: some View {
        TimelineView(.animation(paused: start == nil)) { tl in
            let p = start.map { tl.date.timeIntervalSince($0) / 1.6 } ?? 2
            if p <= 1 {
                Text("+1돈")
                    .font(.serif(28))
                    .foregroundStyle(Palette.goldHi)
                    .shadow(color: Palette.gold.opacity(0.8), radius: 10)
                    .offset(y: 40 - 50 * (1 - pow(1 - p, 3)))
                    .opacity(p < 0.15 ? p / 0.15 : max(0, 1 - (p - 0.6) / 0.4))
            }
        }
        .onChange(of: token) { _ in start = Date() }
        .allowsHitTesting(false)
    }
}

/// 금빛 불꽃 몇 개가 튀었다 사라진다
struct SparkLayer: View {
    let burst: HomeModel.Burst?

    var body: some View {
        TimelineView(.animation(paused: burst == nil)) { tl in
            Canvas { g, size in
                guard let b = burst else { return }
                let p = tl.date.timeIntervalSince(burstStart[b.id] ?? tl.date) / 0.9
                guard p < 1 else { return }
                let c = CGPoint(x: size.width * b.at.x, y: size.height * b.at.y)
                for i in 0..<b.count {
                    SparkLayer.spark(&g, center: c, index: i, seed: abs(b.id.hashValue % 97), p: CGFloat(p))
                }
            }
        }
        .onChange(of: burst) { b in if let b { burstStart[b.id] = Date() } }
        .allowsHitTesting(false)
    }
}

private var burstStart: [UUID: Date] = [:]

extension SparkLayer {
    static func spark(_ g: inout GraphicsContext, center c: CGPoint, index i: Int, seed s: Int, p: CGFloat) {
        let seed: CGFloat = CGFloat(i) * 2.399 + CGFloat(s)
        let ang: CGFloat = seed.truncatingRemainder(dividingBy: .pi * 2)
        let ease: CGFloat = 1 - pow(1 - p, 3)
        let dist: CGFloat = (60 + (seed * 37).truncatingRemainder(dividingBy: 70)) * ease
        let r: CGFloat = 3.5 - 2.5 * p
        let x: CGFloat = c.x + cos(ang) * dist
        let y: CGFloat = c.y + sin(ang) * dist + 30 * p * p
        var gg = g
        gg.opacity = Double(1 - p)
        let dot = Path(ellipseIn: CGRect(x: x - r, y: y - r, width: r * 2, height: r * 2))
        gg.fill(dot, with: .color(i % 2 == 0 ? Palette.goldHi : Palette.gold))
    }
}

/// 디버그 캡처 전용: 실제 보상형 광고가 뜨는 자리
struct AdStandIn: View {
    let onRewarded: () -> Void
    @State private var left = 4

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Color(hex: 0x050506).ignoresSafeArea()
            VStack(spacing: 8) {
                Text("광고").font(.system(size: 22, weight: .bold)).foregroundStyle(.white.opacity(0.85))
                Text("실제 앱에서는 이 자리에\n15~30초짜리 보상형 광고가 나와요")
                    .font(.system(size: 13)).multilineTextAlignment(.center).foregroundStyle(.white.opacity(0.5))
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            Text("\(left)").font(.system(size: 13)).foregroundStyle(.white)
                .frame(width: 34, height: 34)
                .overlay(Circle().stroke(.white.opacity(0.5), lineWidth: 1.5))
                .padding(.top, 12).padding(.trailing, 20)
        }
        .task {
            while left > 0 {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                left -= 1
            }
            onRewarded()
        }
    }
}
