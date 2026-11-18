import UIKit

/// 압인 골드바 그림. 안드로이드 GoldBar.kt 와 같은 방식:
/// 앞·뒷면 그림을 한 번 만들어 두고, 둥근 테두리를 따라 옆면을 원근으로 그린 뒤 큰 면에 그림을 입힌다.
/// 좌표 단위: 판 200 × 300, 두께 30.
enum GoldBarDrawing {
    static let plateW: CGFloat = 200
    static let plateH: CGFloat = 300
    static let thick: CGFloat = 30
    static let camDist: CGFloat = 900
    static let corner: CGFloat = 14

    /* ---------- 색 ---------- */

    struct RGB {
        var r: CGFloat, g: CGFloat, b: CGFloat
        init(_ hex: UInt32) {
            r = CGFloat((hex >> 16) & 0xFF) / 255
            g = CGFloat((hex >> 8) & 0xFF) / 255
            b = CGFloat(hex & 0xFF) / 255
        }
        init(r: CGFloat, g: CGFloat, b: CGFloat) { self.r = r; self.g = g; self.b = b }
        func lerp(_ o: RGB, _ t: CGFloat) -> RGB { RGB(r: r + (o.r - r) * t, g: g + (o.g - g) * t, b: b + (o.b - b) * t) }
        var cg: CGColor { CGColor(srgbRed: r, green: g, blue: b, alpha: 1) }
        func cg(_ a: CGFloat) -> CGColor { CGColor(srgbRed: r, green: g, blue: b, alpha: a) }
    }

    static let edgeDark = RGB(0x573805)
    static let edgeLight = RGB(0xEDC152)
    static let engrave = RGB(0xA87818)
    static let engraveShadow = RGB(0x6B4708)
    static let engraveLight = RGB(0xFFF8DC)

    /* ---------- 글꼴 ---------- */

    static func serif(_ size: CGFloat, bold: Bool = false) -> UIFont {
        UIFont(name: bold ? "NanumMyeongjoBold" : "NanumMyeongjoExtraBold", size: size)
            ?? UIFont(name: "AppleMyungjo", size: size)
            ?? .systemFont(ofSize: size, weight: .heavy)
    }

    /// 金·福 같은 한자 (나눔명조에 없어서 히라기노 명조)
    static func hanja(_ size: CGFloat) -> UIFont {
        UIFont(name: "HiraMinProN-W6", size: size) ?? serif(size)
    }

    static func sans(_ size: CGFloat) -> UIFont { .systemFont(ofSize: size, weight: .semibold) }
    static func sansMedium(_ size: CGFloat) -> UIFont { .systemFont(ofSize: size, weight: .medium) }
    static func mono(_ size: CGFloat) -> UIFont { .monospacedSystemFont(ofSize: size, weight: .medium) }

    /* ---------- 면 그림 ---------- */

    private static var faceCache: [String: CGImage] = [:]

    /// 앞면 또는 뒷면 그림 (판 단위 × scale 픽셀)
    static func face(don: Int, back: Bool, scale s: CGFloat) -> CGImage {
        let key = "\(don)-\(back)-\(Int(s * 100))"
        if let hit = faceCache[key] { return hit }
        let size = CGSize(width: ceil(plateW * s), height: ceil(plateH * s))
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        format.opaque = false
        let img = UIGraphicsImageRenderer(size: size, format: format).image { rc in
            let ctx = rc.cgContext
            ctx.scaleBy(x: s, y: s)
            drawPlate(ctx)
            if back {
                drawBackPattern(ctx)
                let field = fieldGradient()
                fillGradient(ctx, path: UIBezierPath(roundedRect: CGRect(x: 44, y: 30, width: 112, height: 18), cornerRadius: 4).cgPath,
                             gradient: field, from: CGPoint(x: 0, y: plateH), to: CGPoint(x: plateW, y: 0))
                fillGradient(ctx, path: CGPath(ellipseIn: CGRect(x: 44, y: 72, width: 112, height: 112), transform: nil),
                             gradient: field, from: CGPoint(x: 0, y: plateH), to: CGPoint(x: plateW, y: 0))
                fillGradient(ctx, path: UIBezierPath(roundedRect: CGRect(x: 30, y: 194, width: 140, height: 82), cornerRadius: 6).cgPath,
                             gradient: field, from: CGPoint(x: 0, y: plateH), to: CGPoint(x: plateW, y: 0))
                engraveCircle(ctx, CGPoint(x: 100, y: 128), 52, 2.2)
                engraveCircle(ctx, CGPoint(x: 100, y: 128), 45, 0.8)
                engraveLine(ctx, CGPoint(x: 60, y: 252), CGPoint(x: 140, y: 252), 1)
                engraveText("OHSANG VAULT", sans(8.5), kern: 3, cx: 100, baseline: 43)
                engraveText("福", hanja(64), kern: 0, cx: 100, baseline: 151)
                engraveText("행복하세요", serif(17), kern: 1, cx: 100, baseline: 218)
                engraveText("돈 많이 버세요", serif(12, bold: true), kern: 1, cx: 100, baseline: 240)
                engraveText(serialLabel(don), mono(8), kern: 1.2, cx: 100, baseline: 267)
            } else {
                engraveCircle(ctx, CGPoint(x: 100, y: 58), 25, 2)
                engraveCircle(ctx, CGPoint(x: 100, y: 58), 20, 0.8)
                engraveLine(ctx, CGPoint(x: 50, y: 157), CGPoint(x: 150, y: 157), 1.2)
                engraveText("金", hanja(25), kern: 0, cx: 100, baseline: 67)
                engraveText("FINE GOLD", sans(10), kern: 4, cx: 100, baseline: 108)
                engraveText("999.9", serif(31), kern: 1, cx: 100, baseline: 143)
                engraveText(donLabel(don), serif(25), kern: 0, cx: 100, baseline: 191)
                engraveText(gramLabel(don), sansMedium(10.5), kern: 1.5, cx: 100, baseline: 211)
                engraveText("하루 한돈", serif(10, bold: true), kern: 3, cx: 100, baseline: 248)
                engraveText(serialLabel(don), mono(8), kern: 1.2, cx: 100, baseline: 266)
            }
        }
        let cg = img.cgImage!
        if faceCache.count > 12 { faceCache.removeAll() }
        faceCache[key] = cg
        return cg
    }

    private static func gradient(_ stops: [(CGFloat, UInt32)]) -> CGGradient {
        CGGradient(colorsSpace: CGColorSpace(name: CGColorSpace.sRGB),
                   colors: stops.map { RGB($0.1).cg } as CFArray,
                   locations: stops.map { $0.0 })!
    }

    private static func plateGradient() -> CGGradient {
        gradient([(0, 0xFFF2C2), (0.16, 0xEBC25A), (0.3, 0xB7871F), (0.44, 0xF6DC8C),
                  (0.58, 0xD7A538), (0.76, 0x9A6B14), (0.9, 0xD9AC45), (1, 0xF3D27A)])
    }

    private static func fieldGradient() -> CGGradient {
        gradient([(0, 0xB98523), (0.3, 0xE7BD55), (0.5, 0xFBE5A0), (0.68, 0xE2B44A), (1, 0xA7771B)])
    }

    static func fillGradient(_ ctx: CGContext, path: CGPath, gradient: CGGradient, from: CGPoint, to: CGPoint) {
        ctx.saveGState()
        ctx.addPath(path)
        ctx.clip()
        ctx.drawLinearGradient(gradient, start: from, end: to, options: [.drawsBeforeStartLocation, .drawsAfterEndLocation])
        ctx.restoreGState()
    }

    static func strokeGradient(_ ctx: CGContext, path: CGPath, width: CGFloat, gradient: CGGradient, from: CGPoint, to: CGPoint) {
        ctx.saveGState()
        ctx.addPath(path)
        ctx.setLineWidth(width)
        ctx.replacePathWithStrokedPath()
        ctx.clip()
        ctx.drawLinearGradient(gradient, start: from, end: to, options: [.drawsBeforeStartLocation, .drawsAfterEndLocation])
        ctx.restoreGState()
    }

    static func platePath() -> CGPath {
        UIBezierPath(roundedRect: CGRect(x: 0, y: 0, width: plateW, height: plateH), cornerRadius: corner).cgPath
    }

    private static func drawPlate(_ ctx: CGContext) {
        let w = plateW, h = plateH
        fillGradient(ctx, path: platePath(), gradient: plateGradient(), from: .zero, to: CGPoint(x: w, y: h))
        ctx.saveGState()
        ctx.setStrokeColor(RGB(0xFFF7D8).cg(0.6))
        ctx.setLineWidth(1)
        ctx.addPath(UIBezierPath(roundedRect: CGRect(x: 1, y: 1, width: w - 2, height: h - 2), cornerRadius: 13.5).cgPath)
        ctx.strokePath()
        ctx.restoreGState()
        strokeGradient(ctx, path: UIBezierPath(roundedRect: CGRect(x: 11, y: 11, width: w - 22, height: h - 22), cornerRadius: 8).cgPath,
                       width: 3, gradient: gradient([(0, 0xFFFAE6), (0.5, 0xD9A940), (1, 0x6E4A0B)]),
                       from: .zero, to: CGPoint(x: w, y: h))
        let field = UIBezierPath(roundedRect: CGRect(x: 14, y: 14, width: w - 28, height: h - 28), cornerRadius: 6).cgPath
        fillGradient(ctx, path: field, gradient: fieldGradient(), from: CGPoint(x: 0, y: h), to: CGPoint(x: w, y: 0))
        strokeGradient(ctx, path: field, width: 1.6, gradient: gradient([(0, 0xFFF6D2), (0.5, 0xC8952E), (1, 0x5F3F08)]),
                       from: CGPoint(x: w, y: h), to: .zero)
    }

    /// 뒷면: 비스듬히 반복되는 金 무늬
    private static func drawBackPattern(_ ctx: CGContext) {
        ctx.saveGState()
        ctx.clip(to: CGRect(x: 15, y: 15, width: 170, height: 270))
        ctx.translateBy(x: 100, y: 150)
        ctx.rotate(by: -30 * .pi / 180)
        ctx.translateBy(x: -100, y: -150)
        let font = hanja(10)
        let light = NSAttributedString(string: "金", attributes: [.font: font, .foregroundColor: UIColor(cgColor: engraveLight.cg(0.45))])
        let dark = NSAttributedString(string: "金", attributes: [.font: font, .foregroundColor: UIColor(cgColor: RGB(0x9A6B14).cg(0.4))])
        var y: CGFloat = -80
        while y < 380 {
            var x: CGFloat = -110
            while x < 310 {
                light.draw(at: CGPoint(x: x + 0.6, y: y + 0.6))
                dark.draw(at: CGPoint(x: x, y: y))
                x += 24
            }
            y += 24
        }
        ctx.restoreGState()
    }

    /// 새긴 글자: 아래 그림자, 위 하이라이트, 가운데 금색
    private static func engraveText(_ text: String, _ font: UIFont, kern: CGFloat, cx: CGFloat, baseline: CGFloat) {
        func attr(_ c: CGColor) -> NSAttributedString {
            NSAttributedString(string: text, attributes: [.font: font, .kern: kern, .foregroundColor: UIColor(cgColor: c)])
        }
        let size = attr(engrave.cg).size()
        let x = cx - (size.width - kern) / 2
        let y = baseline - font.ascender
        attr(engraveShadow.cg(0.55)).draw(at: CGPoint(x: x + 0.7, y: y + 0.7))
        attr(engraveLight.cg(0.85)).draw(at: CGPoint(x: x - 0.5, y: y - 0.5))
        attr(engrave.cg).draw(at: CGPoint(x: x, y: y))
    }

    private static func engraveCircle(_ ctx: CGContext, _ c: CGPoint, _ r: CGFloat, _ w: CGFloat) {
        for (dx, col) in [(CGFloat(0.7), engraveShadow.cg(0.55)), (-0.5, engraveLight.cg(0.85)), (0, engrave.cg)] {
            ctx.setStrokeColor(col)
            ctx.setLineWidth(w)
            ctx.strokeEllipse(in: CGRect(x: c.x + dx - r, y: c.y + dx - r, width: r * 2, height: r * 2))
        }
    }

    private static func engraveLine(_ ctx: CGContext, _ a: CGPoint, _ b: CGPoint, _ w: CGFloat) {
        for (dx, col) in [(CGFloat(0.7), engraveShadow.cg(0.55)), (-0.5, engraveLight.cg(0.85)), (0, engrave.cg)] {
            ctx.setStrokeColor(col)
            ctx.setLineWidth(w)
            ctx.move(to: CGPoint(x: a.x + dx, y: a.y + dx))
            ctx.addLine(to: CGPoint(x: b.x + dx, y: b.y + dx))
            ctx.strokePath()
        }
    }

    /* ---------- 입체 (원근) ---------- */

    /// 둥근 모서리 사각형 테두리 (판 중심 기준, y 아래로, 시계 방향)
    private static let outline: [CGPoint] = {
        let r = corner, hw = plateW / 2, hh = plateH / 2, seg = 6
        let corners: [(CGPoint, CGFloat)] = [
            (CGPoint(x: -hw + r, y: -hh + r), 180), (CGPoint(x: hw - r, y: -hh + r), 270),
            (CGPoint(x: hw - r, y: hh - r), 0), (CGPoint(x: -hw + r, y: hh - r), 90),
        ]
        var pts: [CGPoint] = []
        for (c, start) in corners {
            for k in 0...seg {
                let a = (start + 90 * CGFloat(k) / CGFloat(seg)) * .pi / 180
                pts.append(CGPoint(x: c.x + r * cos(a), y: c.y + r * sin(a)))
            }
        }
        return pts
    }()

    /// 세로축(yaw) → 가로축(pitch) 순으로 돌리고 원근 투영
    struct Camera {
        let cy: CGFloat, sy: CGFloat, cp: CGFloat, sp: CGFloat
        let unit: CGFloat
        let origin: CGPoint

        init(yaw: CGFloat, pitch: CGFloat, unit: CGFloat, origin: CGPoint) {
            cy = cos(yaw * .pi / 180); sy = sin(yaw * .pi / 180)
            cp = cos(pitch * .pi / 180); sp = sin(pitch * .pi / 180)
            self.unit = unit
            self.origin = origin
        }

        func rotate(_ x: CGFloat, _ y: CGFloat, _ z: CGFloat) -> (CGFloat, CGFloat, CGFloat) {
            let x1 = x * cy + z * sy
            let z1 = -x * sy + z * cy
            return (x1, y * cp - z1 * sp, y * sp + z1 * cp)
        }

        func project(_ x: CGFloat, _ y: CGFloat, _ z: CGFloat) -> CGPoint {
            let (x1, y2, z2) = rotate(x, y, z)
            let k = GoldBarDrawing.camDist / (GoldBarDrawing.camDist - z2)
            return CGPoint(x: origin.x + x1 * k * unit, y: origin.y + y2 * k * unit)
        }

        func faces(_ n: (CGFloat, CGFloat, CGFloat), at p: (CGFloat, CGFloat, CGFloat)) -> Bool {
            let rn = rotate(n.0, n.1, n.2)
            let rp = rotate(p.0, p.1, p.2)
            return rn.0 * (0 - rp.0) + rn.1 * (0 - rp.1) + rn.2 * (GoldBarDrawing.camDist - rp.2) > 0
        }
    }

    private static let light: (CGFloat, CGFloat, CGFloat) = norm(-0.45, -0.65, 0.62)
    private static let half: (CGFloat, CGFloat, CGFloat) = norm(light.0, light.1, light.2 + 1)

    private static func norm(_ x: CGFloat, _ y: CGFloat, _ z: CGFloat) -> (CGFloat, CGFloat, CGFloat) {
        let l = sqrt(x * x + y * y + z * z)
        return (x / l, y / l, z / l)
    }

    private static func edgeColor(_ cam: Camera, _ nx: CGFloat, _ ny: CGFloat, dim: Bool) -> RGB {
        let n = cam.rotate(nx, ny, 0)
        let diffuse = max(0, n.0 * light.0 + n.1 * light.1 + n.2 * light.2)
        let spec = pow(max(0, n.0 * half.0 + n.1 * half.1 + n.2 * half.2), 8)
        var col = edgeDark.lerp(edgeLight, min(1, 0.18 + 0.82 * diffuse))
        col = col.lerp(RGB(0xFFF4CF), min(1, spec * 0.4))
        if dim {
            let g = (col.r * 0.3 + col.g * 0.59 + col.b * 0.11) * 0.5
            col = RGB(r: g, g: g, b: g)
        }
        return col
    }

    /// 골드바 한 장을 [center]에 그린다. [unit]은 판 1단위당 포인트.
    /// relative: 마지막으로 멈춘 면 기준 돌린 각도 (반사광 위치), shine: 빛줄기 진행 0→1 (없으면 -1)
    static func drawSlab(_ ctx: CGContext, center: CGPoint, unit: CGFloat, front: CGImage, back: CGImage,
                         yaw: CGFloat, pitch: CGFloat, dim: Bool = false,
                         relative: CGFloat = 0, tiltX: CGFloat = 0, shine: CGFloat = -1) {
        let cam = Camera(yaw: yaw, pitch: pitch, unit: unit, origin: center)
        let h2 = thick / 2
        let n = outline.count

        // 1) 옆면: 카메라를 향한 것만, 방향에 따라 빛을 계산
        for i in 0..<n {
            let p0 = outline[i], p1 = outline[(i + 1) % n]
            let tx = p1.x - p0.x, ty = p1.y - p0.y
            let len = sqrt(tx * tx + ty * ty)
            if len < 1e-3 { continue }
            let nx = ty / len, ny = -tx / len
            if !cam.faces((nx, ny, 0), at: ((p0.x + p1.x) / 2, (p0.y + p1.y) / 2, 0)) { continue }
            let a = cam.project(p0.x, p0.y, h2), b = cam.project(p1.x, p1.y, h2)
            let c = cam.project(p1.x, p1.y, -h2), d = cam.project(p0.x, p0.y, -h2)
            let quad = CGMutablePath()
            quad.addLines(between: [a, b, c, d])
            quad.closeSubpath()
            let col = edgeColor(cam, nx, ny, dim: dim)
            let g = CGGradient(colorsSpace: CGColorSpace(name: CGColorSpace.sRGB),
                               colors: [col.lerp(RGB(0xFFF0C0), 0.12).cg, col.lerp(edgeDark, 0.22).cg] as CFArray,
                               locations: [0, 1])!
            let from = CGPoint(x: (a.x + d.x) / 2, y: (a.y + d.y) / 2)
            let to = CGPoint(x: (b.x + c.x) / 2, y: (b.y + c.y) / 2)
            fillGradient(ctx, path: quad, gradient: g, from: from, to: to)
            strokeGradient(ctx, path: quad, width: 1.2, gradient: g, from: from, to: to) // 면 사이 틈 메우기
        }

        // 2) 카메라를 향한 큰 면에 그림을 원근으로 입힌다 (세로 띠로 나눠 각각 아핀 변환)
        let showFront = cam.faces((0, 0, 1), at: (0, 0, h2))
        let z = showFront ? h2 : -h2
        let xl = showFront ? -plateW / 2 : plateW / 2
        let xr = -xl
        let img = showFront ? front : back
        let iw = CGFloat(img.width), ih = CGFloat(img.height)
        let strips = 24
        let plate = platePath()
        for i in 0..<strips {
            let f0 = CGFloat(i) / CGFloat(strips), f1 = CGFloat(i + 1) / CGFloat(strips)
            let mx0 = xl + (xr - xl) * f0, mx1 = xl + (xr - xl) * f1
            let p00 = cam.project(mx0, -plateH / 2, z), p10 = cam.project(mx1, -plateH / 2, z)
            let p01 = cam.project(mx0, plateH / 2, z), p11 = cam.project(mx1, plateH / 2, z)
            // 띠 사이 머리카락 같은 틈이 안 생기게 조금 겹친다
            let quad = CGMutablePath()
            let ex = (p10.x - p00.x) * 0.04, ey = (p10.y - p00.y) * 0.04
            quad.addLines(between: [
                CGPoint(x: p00.x - ex, y: p00.y - ey), CGPoint(x: p10.x + ex, y: p10.y + ey),
                CGPoint(x: p11.x + ex, y: p11.y + ey), CGPoint(x: p01.x - ex, y: p01.y - ey),
            ])
            quad.closeSubpath()
            // 판 단위 (u: 0..200, v: 0..300) → 화면
            let du = plateW * (f1 - f0)
            let u0 = plateW * f0
            let a = (p10.x - p00.x) / du, b = (p10.y - p00.y) / du
            let c = (p01.x - p00.x) / plateH, d = (p01.y - p00.y) / plateH
            let m = CGAffineTransform(a: a, b: b, c: c, d: d, tx: p00.x - a * u0, ty: p00.y - b * u0)

            ctx.saveGState()
            ctx.addPath(quad)
            ctx.clip()
            ctx.concatenate(m)
            // 그림 (CG는 아래가 원점이라 뒤집어 그린다)
            ctx.saveGState()
            ctx.translateBy(x: 0, y: plateH)
            ctx.scaleBy(x: 1, y: -1)
            ctx.draw(img, in: CGRect(x: 0, y: 0, width: plateW, height: plateH))
            ctx.restoreGState()
            ctx.addPath(plate)
            ctx.clip()
            if dim {
                ctx.setBlendMode(.saturation)
                ctx.setFillColor(gray: 0.5, alpha: 0.9)
                ctx.fill(CGRect(x: 0, y: 0, width: plateW, height: plateH))
                ctx.setBlendMode(.normal)
                ctx.setFillColor(gray: 0, alpha: 0.45)
                ctx.fill(CGRect(x: 0, y: 0, width: plateW, height: plateH))
            } else {
                drawReflections(ctx, relative: relative, tiltX: tiltX, shine: shine)
            }
            ctx.restoreGState()
        }
    }

    /// 비스듬한 빛 띠 (CSS skewX(-22deg)와 같은 기울임)
    private static func drawReflections(_ ctx: CGContext, relative: CGFloat, tiltX: CGFloat, shine: CGFloat) {
        let x = plateW * 0.4 + sin(relative * .pi / 180) * 260 - tiltX * 2.5
        ctx.saveGState()
        ctx.concatenate(CGAffineTransform(a: 1, b: 0, c: -0.404, d: 1, tx: 0, ty: 0))
        band(ctx, x - 40, 54, 0.55)
        band(ctx, x + 22, 16, 0.38)
        if shine >= 0 { band(ctx, -150 + shine * (plateW + 270) - 60, 80, 0.95) }
        ctx.restoreGState()
    }

    private static func band(_ ctx: CGContext, _ left: CGFloat, _ width: CGFloat, _ peak: CGFloat) {
        let g = CGGradient(colorsSpace: CGColorSpace(name: CGColorSpace.sRGB),
                           colors: [CGColor(gray: 1, alpha: 0), CGColor(gray: 1, alpha: peak),
                                    CGColor(gray: 1, alpha: peak), CGColor(gray: 1, alpha: 0)] as CFArray,
                           locations: [0, 0.45, 0.55, 1])!
        ctx.saveGState()
        ctx.clip(to: CGRect(x: left, y: -40, width: width, height: 380))
        ctx.drawLinearGradient(g, start: CGPoint(x: left, y: 0), end: CGPoint(x: left + width, y: 0), options: [])
        ctx.restoreGState()
    }

    /* ---------- 정지 그림 (위젯, 축하 화면) ---------- */

    /// 세로 골드바 한 장을 이미지로. 가로 longSide 픽셀 기준, 아래로 옅은 그림자를 넣을 수 있다.
    static func image(don: Int, height: CGFloat, pitch: CGFloat = -8, shadow: Bool = false, scale: CGFloat = 3) -> UIImage {
        let unit = height / plateH / 1.1
        let w = plateW * unit * 1.16, h = plateH * unit * 1.14 + (shadow ? height * 0.06 : 0)
        let format = UIGraphicsImageRendererFormat()
        format.scale = scale
        format.opaque = false
        return UIGraphicsImageRenderer(size: CGSize(width: w, height: h), format: format).image { rc in
            let ctx = rc.cgContext
            let center = CGPoint(x: w / 2, y: plateH * unit * 1.14 / 2)
            if shadow {
                ctx.saveGState()
                ctx.setShadow(offset: CGSize(width: 0, height: height * 0.03), blur: height * 0.05,
                              color: CGColor(gray: 0, alpha: 0.55))
                ctx.setFillColor(CGColor(gray: 0, alpha: 1))
                ctx.addPath(UIBezierPath(roundedRect: CGRect(x: center.x - plateW * unit / 2, y: center.y - plateH * unit / 2,
                                                             width: plateW * unit, height: plateH * unit),
                                         cornerRadius: corner * unit).cgPath)
                ctx.fillPath()
                ctx.restoreGState()
            }
            let faceImg = face(don: don, back: false, scale: unit * scale)
            drawSlab(ctx, center: center, unit: unit, front: faceImg, back: faceImg,
                     yaw: 0, pitch: pitch, dim: don == 0)
        }
    }
}
