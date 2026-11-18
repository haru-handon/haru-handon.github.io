import SwiftUI
import WidgetKit

struct HandonEntry: TimelineEntry {
    let date: Date
    let snap: HandonSnapshot
}

struct HandonProvider: TimelineProvider {
    func placeholder(in context: Context) -> HandonEntry { HandonEntry(date: Date(), snap: .sample) }

    func getSnapshot(in context: Context, completion: @escaping (HandonEntry) -> Void) {
        completion(HandonEntry(date: Date(), snap: context.isPreview ? .sample : .load()))
    }

    /// 지금 한 장 + 자정에 한 장 (날짜가 바뀌면 "오늘의 금이 도착했어요"로)
    func getTimeline(in context: Context, completion: @escaping (Timeline<HandonEntry>) -> Void) {
        let now = Date()
        let midnight = Calendar.current.nextDate(after: now, matching: DateComponents(hour: 0, minute: 0, second: 5),
                                                 matchingPolicy: .nextTime) ?? now.addingTimeInterval(3600)
        let entries = [HandonEntry(date: now, snap: .load()), HandonEntry(date: midnight, snap: .load())]
        completion(Timeline(entries: entries, policy: .after(midnight)))
    }
}

struct HandonWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "HandonWidget", provider: HandonProvider()) { entry in
            HandonEntryView(entry: entry)
        }
        .configurationDisplayName("하루 한돈")
        .description("모은 금을 골드바로 보여줘요. 중간 크기에는 오늘의 문구도 나와요.")
        .supportedFamilies([.systemSmall, .systemMedium, .accessoryCircular, .accessoryRectangular])
        .contentMarginsDisabled()
    }
}

private struct HandonEntryView: View {
    let entry: HandonEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        HandonWidgetView(snap: entry.snap, family: family)
            .padding(isAccessory ? 2 : 0)
            .containerBackground(for: .widget) {
                if isAccessory { Color.clear } else { WidgetSteelBackground() }
            }
    }

    private var isAccessory: Bool {
        family == .accessoryCircular || family == .accessoryRectangular || family == .accessoryInline
    }
}

@main
struct HandonWidgetBundle: WidgetBundle {
    var body: some Widget { HandonWidget() }
}
