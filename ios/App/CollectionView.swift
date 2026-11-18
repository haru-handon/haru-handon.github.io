import SwiftUI

/// 문구 도감. 왼쪽 카테고리 목록은 고정, 오른쪽 문구 목록만 스크롤된다. 못 모은 문구는 ??? 로만 보인다.
struct CollectionView: View {
    @ObservedObject var model: HomeModel

    var body: some View {
        let counts = model.counts
        let done = model.labels
        VStack(spacing: 0) {
            // 제목줄
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("문구 도감").font(.serif(21)).foregroundStyle(Palette.goldHi)
                    HStack(alignment: .lastTextBaseline, spacing: 0) {
                        Text("\(model.collected)").font(.system(size: 13, weight: .bold)).foregroundStyle(Palette.goldHi)
                        Text(" / \(Fortunes.all.count) 모음").font(.system(size: 12)).foregroundStyle(Palette.muted)
                    }
                    if !done.isEmpty {
                        VaultLabels(done: done, fresh: nil, vertical: false).padding(.top, 8)
                    }
                }
                Spacer()
                Button("닫기") { model.dogamOpen = false }
                    .font(.system(size: 14))
                    .foregroundStyle(Palette.text)
                    .padding(12)
            }
            .padding(.leading, 22).padding(.trailing, 10).padding(.top, 14).padding(.bottom, 14)
            Rectangle().fill(Palette.line).frame(height: 1)

            HStack(spacing: 0) {
                // 왼쪽: 카테고리 (고정)
                VStack(spacing: 0) {
                    ForEach(Category.allCases) { c in
                        let list = Fortunes.inCategory(c)
                        NavItem(category: c, got: list.filter { counts[$0.id] != nil }.count, size: list.count,
                                complete: done.contains(c), selected: c == model.dogamCategory) {
                            model.dogamCategory = c
                        }
                    }
                    Spacer()
                }
                .padding(.vertical, 8)
                .frame(width: 96)
                .background(Color.black.opacity(0.25))
                Rectangle().fill(Palette.line).frame(width: 1)

                // 오른쪽: 고른 카테고리의 문구만 스크롤
                let c = model.dogamCategory
                let list = Fortunes.inCategory(c)
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 0) {
                        CategoryHead(category: c, got: list.filter { counts[$0.id] != nil }.count, size: list.count,
                                     complete: done.contains(c))
                        ForEach(Array(list.enumerated()), id: \.element.id) { i, f in
                            EntryRow(no: i + 1, text: counts[f.id] != nil ? f.text : nil)
                        }
                    }
                    .padding(.bottom, 24)
                }
                .id(c)
            }
        }
        .background(Palette.ink.ignoresSafeArea())
    }
}

private struct NavItem: View {
    let category: Category
    let got: Int
    let size: Int
    let complete: Bool
    let selected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 0) {
                Rectangle().fill(selected ? Palette.gold : .clear).frame(width: 3, height: 56)
                VStack(alignment: .leading, spacing: 0) {
                    HStack(spacing: 5) {
                        Text(category.label).font(.serif(15)).foregroundStyle(selected ? Palette.goldHi : Palette.text)
                        if complete { Circle().fill(Palette.gold).frame(width: 6, height: 6) }
                    }
                    Text("\(got) / \(size)").font(.system(size: 11)).foregroundStyle(selected ? Palette.gold : Palette.faint)
                }
                .padding(.leading, 13)
                Spacer(minLength: 0)
            }
            .background(selected ? Palette.panel : .clear)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

private struct CategoryHead: View {
    let category: Category
    let got: Int
    let size: Int
    let complete: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text(category.label).font(.serif(18)).foregroundStyle(Palette.goldHi)
                Spacer()
                Text("\(got) / \(size)").font(.system(size: 12)).foregroundStyle(Palette.muted)
            }
            GeometryReader { g in
                ZStack(alignment: .leading) {
                    Capsule().fill(Palette.line)
                    Capsule().fill(LinearGradient(colors: [Palette.goldLo, Palette.goldHi], startPoint: .leading, endPoint: .trailing))
                        .frame(width: g.size.width * CGFloat(got) / CGFloat(max(size, 1)))
                }
            }
            .frame(height: 3)
            .padding(.top, 8)
            Text(complete ? "금고에 「\(category.label)」 라벨을 붙였어요" : "다 모으면 금고에 「\(category.label)」 라벨이 붙어요")
                .font(.system(size: 12.5, weight: .semibold))
                .foregroundStyle(complete ? Palette.goldHi : Palette.text)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 12).padding(.vertical, 10)
                .background(complete ? Palette.goldLo.opacity(0.18) : Color.white.opacity(0.03), in: RoundedRectangle(cornerRadius: 12))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(complete ? Palette.gold : Palette.line, lineWidth: 1))
                .padding(.top, 12)
        }
        .padding(.leading, 16).padding(.trailing, 18).padding(.top, 16).padding(.bottom, 12)
    }
}

private struct EntryRow: View {
    let no: Int
    let text: String?

    var body: some View {
        HStack(alignment: .top, spacing: 0) {
            Text(String(format: "%02d", no)).font(.system(size: 11)).foregroundStyle(Palette.faint)
                .frame(width: 24, alignment: .leading).padding(.top, 3)
            if let text {
                Text(text).font(.serif(14)).lineSpacing(4).foregroundStyle(Palette.text)
                    .fixedSize(horizontal: false, vertical: true)
            } else {
                Text("???").font(.system(size: 14)).kerning(2).foregroundStyle(Palette.faint)
            }
        }
        .padding(.leading, 16).padding(.trailing, 18).padding(.vertical, 10)
    }
}

/// 1냥(10돈)마다 나오는 축하 화면
struct NyangCelebration: View {
    let don: Int
    let onDismiss: () -> Void
    @State private var spin = false
    @State private var shown = false

    var body: some View {
        ZStack {
            Color.black.opacity(0.82).ignoresSafeArea()
            // 빛줄기
            Canvas { g, size in
                let c = CGPoint(x: size.width / 2, y: size.height * 0.4)
                for i in 0..<16 {
                    let a = Double(i) / 16 * .pi * 2
                    var p = Path()
                    p.move(to: c)
                    p.addLine(to: CGPoint(x: c.x + cos(a - 0.08) * 600, y: c.y + sin(a - 0.08) * 600))
                    p.addLine(to: CGPoint(x: c.x + cos(a + 0.08) * 600, y: c.y + sin(a + 0.08) * 600))
                    p.closeSubpath()
                    g.fill(p, with: .radialGradient(Gradient(colors: [Palette.goldHi.opacity(0.28), .clear]),
                                                    center: c, startRadius: 0, endRadius: 420))
                }
            }
            .rotationEffect(.degrees(spin ? 30 : 0))
            .ignoresSafeArea()

            VStack(spacing: 14) {
                Spacer()
                Image(uiImage: GoldBarDrawing.image(don: don, height: 230, pitch: -8, shadow: true))
                    .scaleEffect(shown ? 1 : 0.6)
                    .opacity(shown ? 1 : 0)
                Text("축하해요").font(.system(size: 12)).kerning(3).foregroundStyle(Palette.gold)
                Text("\(don / donPerNyang)냥 달성!").font(.serif(34)).foregroundStyle(Palette.goldHi)
                Text("금 \(don)돈 · \(gramLabel(don)) 모았어요").font(.system(size: 14)).foregroundStyle(Palette.text)
                Spacer()
                Button(action: onDismiss) {
                    Text("금고로 돌아가기").font(.system(size: 14, weight: .semibold)).foregroundStyle(Palette.goldHi)
                        .padding(.horizontal, 28).padding(.vertical, 12)
                        .overlay(Capsule().stroke(Palette.gold, lineWidth: 1))
                }
                .padding(.bottom, 40)
            }
        }
        .onAppear {
            withAnimation(.spring(response: 0.6, dampingFraction: 0.6)) { shown = true }
            withAnimation(.linear(duration: 12).repeatForever(autoreverses: false)) { spin = true }
        }
    }
}
