import SwiftUI

/// 금고. 몸체·안쪽 → 골드바 → 문 → 경첩 순으로 쌓는다. 골드바가 앞으로 나오면(lift > 0) 문보다 위로 올라온다.
struct VaultView<Bar: View>: View {
    let tier: Int
    let door: Double
    let dial: Double
    let lever: Double
    let lift: Double
    let showHint: Bool
    let labels: Set<Category>
    let freshLabel: Category?
    let onDoorTap: () -> Void
    @ViewBuilder let bar: () -> Bar

    var body: some View {
        GeometryReader { geo in
            let w = min(geo.size.width * 0.84, geo.size.height * 0.82 / 1.08)
            let h = w * 1.08
            let open = min(max(door / HomeModel.doorOpenDeg, 0), 1)
            ZStack {
                SafeBody(tier: tier, open: open)
                    .frame(width: w, height: h)

                bar()
                    .frame(width: w * 0.54, height: w * 0.54 * 1.6)
                    .scaleEffect(1 + 0.3 * lift)
                    .zIndex(lift > 0.02 ? 4 : 1)

                DoorView(tier: tier, angle: door, dial: dial, lever: lever, showHint: showHint,
                         labels: labels, freshLabel: freshLabel, width: w)
                    .padding(w * 0.055)
                    .frame(width: w, height: h)
                    .rotation3DEffect(.degrees(door), axis: (x: 0, y: 1, z: 0), anchor: .leading, perspective: 0.45)
                    .contentShape(Rectangle())
                    .onTapGesture(perform: onDoorTap)
                    .allowsHitTesting(abs(door) < 1)
                    .zIndex(2)
                    .accessibilityLabel("금고 열기")
                    .accessibilityAddTraits(.isButton)

                Hinges()
                    .frame(width: w, height: h)
                    .allowsHitTesting(false)
                    .zIndex(3)
            }
            .frame(width: geo.size.width, height: geo.size.height)
        }
    }
}

/* ---------- 몸체 ---------- */

private struct SafeBody: View {
    let tier: Int
    let open: Double

    var body: some View {
        Canvas { g, size in
            SafeBody.paint(&g, size: size, open: CGFloat(open))
        }
    }

    static func paint(_ g: inout GraphicsContext, size: CGSize, open: CGFloat) {
        let w: CGFloat = size.width
        let h: CGFloat = size.height
        // 다리
        let footW: CGFloat = w * 0.14
        let footH: CGFloat = 9
        let feet: [CGFloat] = [0.10, 0.76]
        for fx in feet {
            let foot = Path(roundedRect: CGRect(x: w * fx, y: h - footH, width: footW, height: footH * 2), cornerRadius: 4)
            g.fill(foot, with: .color(Color(hex: 0x15171A)))
        }
        let body = Path(roundedRect: CGRect(origin: .zero, size: size), cornerRadius: 16)
        let bodyGrad = Gradient(stops: [
            .init(color: Color(hex: 0x41464E), location: 0),
            .init(color: Color(hex: 0x262A2F), location: 0.55),
            .init(color: Color(hex: 0x1A1D21), location: 1),
        ])
        g.fill(body, with: .linearGradient(bodyGrad, startPoint: .zero, endPoint: CGPoint(x: w, y: h)))
        g.stroke(body, with: .color(Color(hex: 0x15171A)), lineWidth: 2)
        var top = Path()
        top.move(to: CGPoint(x: 16, y: 1))
        top.addLine(to: CGPoint(x: w - 16, y: 1))
        g.stroke(top, with: .color(.white.opacity(0.14)), lineWidth: 1)
        // 안쪽
        let inset: CGFloat = w * 0.075
        let inner = CGRect(x: inset, y: inset, width: w - inset * 2, height: h - inset * 2)
        let ip = Path(roundedRect: inner, cornerRadius: 9)
        let innerGrad = Gradient(stops: [
            .init(color: Color(hex: 0x2B1F0B), location: 0),
            .init(color: Color(hex: 0x0D0A05), location: 0.75),
            .init(color: Color(hex: 0x0D0A05), location: 1),
        ])
        let innerCenter = CGPoint(x: w / 2, y: inner.minY + inner.height * 0.85)
        g.fill(ip, with: .radialGradient(innerGrad, center: innerCenter, startRadius: 0, endRadius: inner.width * 0.9))
        g.stroke(ip, with: .color(.black), lineWidth: 1)
        if open > 0 {
            var g2 = g
            g2.opacity = Double(open)
            let glow = Gradient(colors: [Color(hex: 0xFBE7A6, alpha: 0.35), .clear])
            let glowCenter = CGPoint(x: w / 2, y: inner.minY + inner.height * 0.55)
            g2.fill(ip, with: .radialGradient(glow, center: glowCenter, startRadius: 0, endRadius: inner.width * 0.7))
        }
    }
}

private struct Hinges: View {
    var body: some View {
        Canvas { g, size in
            let hw: CGFloat = 9, hh = size.height * 0.13
            for y in [size.height * 0.16, size.height * (1 - 0.16 - 0.13)] {
                g.fill(Path(roundedRect: CGRect(x: -5, y: y, width: hw, height: hh), cornerRadius: 3),
                       with: .linearGradient(Gradient(colors: [Color(hex: 0x1B1E22), Color(hex: 0x4D535B), Color(hex: 0x1B1E22)]),
                                             startPoint: CGPoint(x: -5, y: 0), endPoint: CGPoint(x: -5 + hw, y: 0)))
            }
        }
    }
}

/* ---------- 문 ---------- */

private struct DoorView: View {
    let tier: Int
    let angle: Double
    let dial: Double
    let lever: Double
    let showHint: Bool
    let labels: Set<Category>
    let freshLabel: Category?
    let width: CGFloat

    var body: some View {
        ZStack(alignment: .topLeading) {
            if abs(angle) < 90 {
                DoorFace(tier: tier, dial: dial, lever: lever)
                if showHint {
                    DoorHint()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .offset(y: width * 0.27)
                }
                // 문 왼쪽 위에 붙은 라벨 (문이 열리면 문과 함께 돌아가 안 보인다)
                VaultLabels(done: labels, fresh: freshLabel, vertical: true)
                    .padding(.leading, width * 0.07)
                    .padding(.top, width * 0.085)
            } else {
                DoorBack().scaleEffect(x: -1, y: 1)
            }
        }
    }
}

private struct DoorHint: View {
    @State private var dim = false
    var body: some View {
        Text("터치해서 열기")
            .font(.system(size: 11, weight: .medium))
            .kerning(0.9)
            .foregroundStyle(Palette.goldHi)
            .opacity(dim ? 0.35 : 0.8)
            .onAppear {
                withAnimation(.easeInOut(duration: 1).repeatForever(autoreverses: true)) { dim = true }
            }
    }
}

/// 다이얼·손잡이가 돌아가는 문 앞면 (각도를 애니메이션으로 이어 그린다)
private struct DoorFace: View, Animatable {
    let tier: Int
    var dial: Double
    var lever: Double

    var animatableData: AnimatablePair<Double, Double> {
        get { AnimatablePair(dial, lever) }
        set { dial = newValue.first; lever = newValue.second }
    }

    var body: some View {
        Canvas { g, size in
            DoorFace.paint(&g, size: size, tier: tier, dial: CGFloat(dial), lever: CGFloat(lever))
        }
    }

    private static func circle(_ c: CGPoint, _ r: CGFloat) -> Path {
        Path(ellipseIn: CGRect(x: c.x - r, y: c.y - r, width: r * 2, height: r * 2))
    }

    static func paint(_ g: inout GraphicsContext, size: CGSize, tier: Int, dial: CGFloat, lever: CGFloat) {
        let w: CGFloat = size.width
        let h: CGFloat = size.height
        let face = Path(roundedRect: CGRect(origin: .zero, size: size), cornerRadius: 11)
        let doorGrad = Gradient(colors: [Palette.doorHi[tier], Palette.doorLo[tier]])
        g.fill(face, with: .linearGradient(doorGrad, startPoint: .zero, endPoint: CGPoint(x: w, y: h)))
        let innerRim = Path(roundedRect: CGRect(x: 3.5, y: 3.5, width: w - 7, height: h - 7), cornerRadius: 7.5)
        g.stroke(innerRim, with: .color(.black.opacity(0.35)), lineWidth: 5)
        g.stroke(face, with: .color(Palette.rim[tier]), lineWidth: 2)
        var top = Path()
        top.move(to: CGPoint(x: 11, y: 2))
        top.addLine(to: CGPoint(x: w - 11, y: 2))
        g.stroke(top, with: .color(.white.opacity(0.15)), lineWidth: 1)
        // 리벳
        let rivetFracs: [CGFloat] = [0.07, 0.93]
        for fx in rivetFracs {
            for fy in rivetFracs {
                g.fill(circle(CGPoint(x: w * fx, y: h * fy), 2), with: .color(Color(hex: 0x9AA0A8)))
            }
        }
        let cx: CGFloat = w / 2
        // 다이얼 위 표시
        let py: CGFloat = h * 0.12
        var mark = Path()
        mark.move(to: CGPoint(x: cx - 6, y: py))
        mark.addLine(to: CGPoint(x: cx + 6, y: py))
        mark.addLine(to: CGPoint(x: cx, y: py + 9))
        mark.closeSubpath()
        g.fill(mark, with: .color(Palette.gold))

        // 다이얼
        let c = CGPoint(x: cx, y: h * 0.43)
        let outer: CGFloat = w * 0.27
        g.fill(circle(c, outer), with: .color(Color(hex: 0x15171A)))
        g.stroke(circle(c, outer * 0.97), with: .color(Color(hex: 0x2A2E33)), lineWidth: 2)
        let ring: CGFloat = outer * 0.88
        var d = g
        d.translateBy(x: c.x, y: c.y)
        d.rotate(by: .degrees(Double(dial)))
        d.translateBy(x: -c.x, y: -c.y)
        let ringGrad = Gradient(colors: [Color(hex: 0x4A5058), Color(hex: 0x2B2F34)])
        d.fill(circle(c, ring), with: .radialGradient(ringGrad, center: c, startRadius: 0, endRadius: ring))
        for i in 0..<36 {
            let a: CGFloat = (CGFloat(i) * 10 - 90) * .pi / 180
            let major: Bool = i % 3 == 0
            let r1: CGFloat = ring * 0.97
            let r2: CGFloat = ring * (major ? 0.76 : 0.85)
            var tick = Path()
            tick.move(to: CGPoint(x: c.x + cos(a) * r1, y: c.y + sin(a) * r1))
            tick.addLine(to: CGPoint(x: c.x + cos(a) * r2, y: c.y + sin(a) * r2))
            d.stroke(tick, with: .color(Color(hex: 0xE2E5E9)), lineWidth: major ? 1.6 : 1)
        }
        d.stroke(circle(c, ring * 0.64), with: .color(Color(hex: 0x0F1012)), lineWidth: 1)
        let knob: CGFloat = ring * 0.56
        let knobGrad = Gradient(stops: [
            .init(color: Color(hex: 0xA3A9B1), location: 0),
            .init(color: Color(hex: 0x5D636B), location: 0.38),
            .init(color: Color(hex: 0x2A2E33), location: 1),
        ])
        let knobCenter = CGPoint(x: c.x - knob * 0.25, y: c.y - knob * 0.4)
        d.fill(circle(c, knob), with: .radialGradient(knobGrad, center: knobCenter, startRadius: 0, endRadius: knob * 1.3))
        let pointer = Path(roundedRect: CGRect(x: c.x - 2, y: c.y - knob * 0.9, width: 4, height: knob * 0.28), cornerRadius: 2)
        d.fill(pointer, with: .color(Palette.gold))

        // 손잡이
        let lw: CGFloat = w * 0.34
        let lh: CGFloat = h * 0.07
        let ly: CGFloat = h * (1 - 0.11 - 0.07)
        var l = g
        l.translateBy(x: cx, y: ly + lh / 2)
        l.rotate(by: .degrees(Double(-28 * lever)))
        l.translateBy(x: -cx, y: -(ly + lh / 2))
        let leverGrad = Gradient(stops: [
            .init(color: Color(hex: 0x9AA0A8), location: 0),
            .init(color: Color(hex: 0x4D535B), location: 0.6),
            .init(color: Color(hex: 0x2A2E33), location: 1),
        ])
        let handle = Path(roundedRect: CGRect(x: cx - lw / 2, y: ly, width: lw, height: lh), cornerRadius: lh / 2)
        l.fill(handle, with: .linearGradient(leverGrad, startPoint: CGPoint(x: 0, y: ly), endPoint: CGPoint(x: 0, y: ly + lh)))
    }
}

private struct DoorBack: View {
    var body: some View {
        Canvas { g, size in
            let w = size.width, h = size.height
            let face = Path(roundedRect: CGRect(origin: .zero, size: size), cornerRadius: 11)
            g.fill(face, with: .linearGradient(Gradient(colors: [Color(hex: 0x2A2E33), Color(hex: 0x141619)]),
                                               startPoint: CGPoint(x: w, y: 0), endPoint: CGPoint(x: 0, y: h)))
            g.stroke(face, with: .color(Color(hex: 0x0C0D0F)), lineWidth: 2)
            let bw = w * 0.16, bh = h * 0.05
            for fy in [0.25, 0.70] {
                g.fill(Path(roundedRect: CGRect(x: w - bw * 0.8, y: h * fy, width: bw, height: bh), cornerRadius: 3),
                       with: .linearGradient(Gradient(colors: [Color(hex: 0x9AA0A8), Color(hex: 0x4D535B)]),
                                             startPoint: CGPoint(x: 0, y: h * fy), endPoint: CGPoint(x: 0, y: h * fy + bh)))
            }
        }
    }
}

/* ---------- 라벨 ---------- */

/// 놋쇠 라벨. 다 모은 카테고리만 순서대로 붙는다. [fresh]는 방금 붙은 라벨 — 툭 하고 붙는다.
struct VaultLabels: View {
    let done: Set<Category>
    let fresh: Category?
    var vertical = true

    var body: some View {
        let earned = Category.allCases.filter { done.contains($0) }
        if vertical {
            VStack(alignment: .leading, spacing: 3) { ForEach(earned) { LabelPlate(text: $0.label, fresh: $0 == fresh) } }
        } else {
            HStack(spacing: 6) { ForEach(earned) { LabelPlate(text: $0.label, fresh: $0 == fresh) } }
        }
    }
}

struct LabelPlate: View {
    let text: String
    let fresh: Bool
    @State private var shown = false

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 4)
                .fill(LinearGradient(stops: [
                    .init(color: Color(hex: 0xF7DE8C), location: 0), .init(color: Color(hex: 0xD6A647), location: 0.45),
                    .init(color: Color(hex: 0x9C7123), location: 1),
                ], startPoint: .top, endPoint: .bottom))
            RoundedRectangle(cornerRadius: 4).stroke(Color(hex: 0x6E4E12), lineWidth: 1)
            HStack {
                Circle().fill(Color(hex: 0x7A5718)).frame(width: 3, height: 3)
                Spacer()
                Circle().fill(Color(hex: 0x7A5718)).frame(width: 3, height: 3)
            }
            .padding(.horizontal, 3)
            Text(text).font(.serif(9.5)).foregroundStyle(Color(hex: 0x4A3208))
        }
        .frame(width: 40, height: 16)
        .scaleEffect(fresh && !shown ? 1.8 : 1)
        .opacity(fresh && !shown ? 0 : 1)
        .onAppear {
            guard fresh else { return }
            withAnimation(.spring(response: 0.45, dampingFraction: 0.45).delay(0.15)) { shown = true }
        }
    }
}
