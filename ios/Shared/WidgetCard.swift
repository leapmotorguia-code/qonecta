import SwiftUI

enum CardKind: String, CaseIterable { case vehicle, costs, regeneration }

struct WidgetCard: View {
    let data: VehicleData
    let date: Date
    let kind: CardKind
    private let mint = Color(red: 0.25, green: 0.95, blue: 0.72)
    private var hasData: Bool {
        data.updatedAt != nil && (kind == .vehicle || (kind == .costs && data.costsRecorded)
            || (kind == .regeneration && data.regenerationRecorded))
    }
    private func number(_ value: Double, digits: Int = 0) -> String {
        value.formatted(.number.locale(Locale(identifier: "es_ES")).precision(.fractionLength(digits)))
    }
    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            HStack {
                Image(systemName: kind == .regeneration ? "bolt.arrow.circlepath" : "car.side.fill")
                    .foregroundStyle(mint)
                Text(kind == .vehicle ? "C10 · Mi registro" : kind == .costs ? "C10 · Costes" : "C10 · Recuperación")
                    .font(.system(size: 13, weight: .bold)).lineLimit(1).minimumScaleFactor(0.75)
            }
            if !hasData {
                Spacer(minLength: 0)
                Text("Sin datos guardados").font(.headline)
                Text("Introduce los valores en el iPhone.").font(.caption)
                Spacer(minLength: 0)
            } else {
                switch kind {
                case .vehicle:
                    HStack(alignment: .firstTextBaseline, spacing: 2) {
                        Text(number(data.battery)).font(.system(size: 34, weight: .bold, design: .rounded))
                        Text("%").font(.headline).foregroundStyle(mint)
                        Spacer(minLength: 0)
                    }
                    ProgressView(value: data.battery, total: 100).tint(mint)
                    Text("Eléctrico: \(number(data.electricRange)) km").font(.system(size: 14, weight: .semibold))
                case .costs:
                    metric("Electricidad", "\(number(data.electricCost, digits: 2)) €")
                    metric("Gasolina", "\(number(data.petrolCost, digits: 2)) €")
                    Text("por 100 km · por separado").font(.system(size: 10))
                case .regeneration:
                    Text("\(number(data.recoveredKWh, digits: 3)) kWh")
                        .font(.system(size: 25, weight: .bold, design: .rounded)).minimumScaleFactor(0.7).lineLimit(1)
                    Text("\(number(data.regenPower, digits: 1)) kW × \(number(data.regenSeconds)) s")
                        .font(.caption)
                    Text("Estimación manual").font(.caption).foregroundStyle(mint)
                }
                Spacer(minLength: 0)
                if data.isStale(at: date) {
                    Label("Registro antiguo", systemImage: "clock.badge.exclamationmark")
                        .font(.system(size: 10, weight: .semibold)).foregroundStyle(.orange)
                }
                HStack(spacing: 3) {
                    Text("MANUAL ·")
                    if let updated = data.updatedAt {
                        Text(updated, style: .time)
                    }
                }.font(.system(size: 10, weight: .semibold)).foregroundStyle(.white.opacity(0.8))
            }
        }
        .foregroundStyle(.white)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .accessibilityElement(children: .combine)
    }
    private func metric(_ title: String, _ value: String) -> some View {
        HStack {
            Text(title).font(.system(size: 11))
            Spacer(minLength: 2)
            Text(value).font(.system(size: 16, weight: .bold, design: .rounded)).foregroundStyle(mint)
        }.lineLimit(1).minimumScaleFactor(0.65)
    }
}
