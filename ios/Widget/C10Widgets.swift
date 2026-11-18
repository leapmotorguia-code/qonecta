import WidgetKit
import SwiftUI

struct C10Entry: TimelineEntry {
    let date: Date
    let data: VehicleData
}
struct C10Provider: TimelineProvider {
    func placeholder(in context: Context) -> C10Entry { C10Entry(date: .now, data: VehicleData()) }
    func getSnapshot(in context: Context, completion: @escaping (C10Entry) -> Void) {
        completion(C10Entry(date: .now, data: VehicleStore.read(from: VehicleStore.defaults())))
    }
    func getTimeline(in context: Context, completion: @escaping (Timeline<C10Entry>) -> Void) {
        let now = Date()
        let data = VehicleStore.read(from: VehicleStore.defaults())
        var entries = [C10Entry(date: now, data: data)]
        if let updated = data.updatedAt, updated.addingTimeInterval(3601) > now {
            entries.append(C10Entry(date: updated.addingTimeInterval(3601), data: data))
        }
        completion(Timeline(entries: entries, policy: .after(now.addingTimeInterval(1800))))
    }
}
struct C10Widget: Widget {
    let type: CardKind
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "C10Widget.\(type.rawValue)", provider: C10Provider()) { entry in
            WidgetCard(data: entry.data, date: entry.date, kind: type)
                .containerBackground(for: .widget) { Color(red: 0.035, green: 0.08, blue: 0.13) }
        }
        .configurationDisplayName(type == .vehicle ? "Mi C10 · Manual" : type == .costs ? "Costes por 100 km" : "Energía recuperada")
        .description("Datos introducidos en el iPhone. No conecta con la telemetría ni controla el coche.")
        .supportedFamilies([.systemSmall])
    }
}
@main struct C10WidgetBundle: WidgetBundle {
    var body: some Widget {
        C10Widget(type: .vehicle)
        C10Widget(type: .costs)
        C10Widget(type: .regeneration)
    }
}
