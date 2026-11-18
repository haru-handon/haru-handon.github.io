import SwiftUI

/// 정지된 각도로 골드바 한 장을 그린다
struct BarSlab: View {
    let don: Int
    let yaw: Double
    let pitch: Double
    var relative: Double = 0
    var tiltX: Double = 0
    var shine: Double = -1
    @Environment(\.displayScale) private var displayScale

    var body: some View {
        Canvas { g, size in
            // 옆면 두께와 원근이 들어갈 자리를 남긴다
            let unit = min(size.width / GoldBarDrawing.plateW / 1.12, size.height / GoldBarDrawing.plateH / 1.1)
            let px = unit * displayScale
            let front = GoldBarDrawing.face(don: don, back: false, scale: px)
            let back = GoldBarDrawing.face(don: don, back: true, scale: px)
            g.withCGContext { cg in
                GoldBarDrawing.drawSlab(cg, center: CGPoint(x: size.width / 2, y: size.height / 2), unit: unit,
                                        front: front, back: back, yaw: yaw, pitch: pitch, dim: don == 0,
                                        relative: relative, tiltX: tiltX, shine: shine)
            }
        }
    }
}

func isBackAngle(_ deg: Double) -> Bool {
    let n = (deg.truncatingRemainder(dividingBy: 360) + 360).truncatingRemainder(dividingBy: 360)
    return n > 90 && n < 270
}

/// 금고 밖으로 나온 골드바. 가만히 있으면 천천히 흔들리고, 누르면 반짝, 옆으로 끌어 돌리면 뒷면이 보인다.
struct InteractiveGoldBar: View {
    let don: Int
    let enabled: Bool
    let shineAt: Date?
    let resetToken: Int
    let flipToken: Int
    let onTap: (UnitPoint) -> Void
    let onFlip: (Bool) -> Void

    /// 끌어서 돌린 각도와 마지막으로 멈춘 면 (0, ±180 …)
    @State private var angle: Double = 0
    @State private var base: Double = 0
    @State private var dragStart: Double?
    /// 놓은 뒤 가까운 면으로 돌아가는 움직임
    @State private var settle: (from: Double, to: Double, at: Date)?

    var body: some View {
        GeometryReader { geo in
        TimelineView(.animation(minimumInterval: nil, paused: !enabled && settle == nil && shineAt == nil)) { tl in
            let now = tl.date
            let t = now.timeIntervalSinceReferenceDate
            let still = dragStart != nil || !enabled
            let sway = still ? 0 : sin(t * 0.59) * 2.5
            let swayX = still ? 0 : cos(t * 0.43) * 2
            let a = currentAngle(now)
            BarSlab(don: don, yaw: a + sway, pitch: swayX, relative: a + sway - base, tiltX: swayX,
                    shine: shineValue(now))
        }
        .contentShape(Rectangle())
        .gesture(drag, including: enabled ? .all : .none)
        .simultaneousGesture(tap(geo.size), including: enabled ? .all : .none)
        }
        .onChange(of: resetToken) { _ in
            angle = 0
            base = 0
            settle = nil
            dragStart = nil
        }
        .onChange(of: flipToken) { _ in
            let from = currentAngle(Date())
            let target = base + 180
            settle = (from, target, Date())
            angle = target
            let wasBack = isBackAngle(base)
            base = target
            if isBackAngle(target) != wasBack { onFlip(isBackAngle(target)) }
        }
    }

    private func tap(_ size: CGSize) -> some Gesture {
        SpatialTapGesture().onEnded { v in
            onTap(UnitPoint(x: v.location.x / max(size.width, 1), y: v.location.y / max(size.height, 1)))
        }
    }

    private var drag: some Gesture {
        DragGesture(minimumDistance: 6)
            .onChanged { v in
                if dragStart == nil {
                    dragStart = currentAngle(Date())
                    settle = nil
                }
                angle = (dragStart ?? 0) + v.translation.width * 0.9
            }
            .onEnded { _ in
                let from = angle
                let target = (from / 180).rounded() * 180
                let wasBack = isBackAngle(base)
                base = target
                dragStart = nil
                settle = (from, target, Date())
                angle = target
                if isBackAngle(target) != wasBack { onFlip(isBackAngle(target)) }
            }
    }

    /// 지금 각도: 놓은 뒤에는 살짝 튕기며 가까운 면으로 돌아간다
    private func currentAngle(_ now: Date) -> Double {
        guard let s = settle else { return angle }
        let p = now.timeIntervalSince(s.at) / 0.6
        if p >= 1 { return s.to }
        // easeOutBack
        let c1 = 1.4, c3 = c1 + 1
        let e = 1 + c3 * pow(p - 1, 3) + c1 * pow(p - 1, 2)
        return s.from + (s.to - s.from) * e
    }

    private func shineValue(_ now: Date) -> Double {
        guard let at = shineAt else { return -1 }
        let p = now.timeIntervalSince(at) / 0.85
        if p < 0 || p > 1 { return -1 }
        return 1 - pow(1 - p, 3)
    }
}
