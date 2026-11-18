import SwiftUI
import WidgetKit

@main struct C10WidgetApp: App {
    var body: some Scene { WindowGroup { ContentView() } }
}

struct ContentView: View {
    @State private var saved = VehicleStore.read(from: VehicleStore.defaults())
    @State private var values = Array(repeating: "", count: 8)
    @State private var message = ""
    private let labels = ["Batería (%)", "Autonomía eléctrica (km)", "Consumo eléctrico (kWh/100 km)",
        "Electricidad (€/kWh)", "Consumo gasolina (L/100 km)", "Gasolina (€/L)",
        "Potencia regenerativa media (kW)", "Duración de frenada (segundos)"]
    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text("Tus datos, visibles en CarPlay")
                        .font(.title3.bold())
                    Text("Registro manual y cálculos orientativos. No lee sensores, cámaras ni la cuenta Leapmotor.")
                    Text("Primera prueba: guarda un registro y añade el widget desde los ajustes de CarPlay del iPhone.")
                        .font(.footnote).foregroundStyle(.secondary)
                }
                Section("Batería y autonomía · obligatorios") { fields(0..<2) }
                Section {
                    fields(2..<6)
                } header: { Text("Coste por 100 km · opcional") }
                footer: { Text("Rellena los cuatro valores o déjalos vacíos. Compara electricidad y gasolina por separado; no calcula un viaje mixto.") }
                Section {
                    fields(6..<8)
                } header: { Text("Energía recuperada · opcional") }
                footer: { Text("Potencia media × segundos ÷ 3.600. Es una estimación manual, no una lectura del coche.") }
                Section {
                    Button("Guardar y actualizar widgets", action: save)
                        .accessibilityIdentifier("saveRecord")
                    if !message.isEmpty { Text(message).accessibilityIdentifier("saveMessage") }
                }
                Section("Vista de las tarjetas") {
                    ScrollView(.horizontal) {
                        HStack(spacing: 12) {
                            ForEach(CardKind.allCases, id: \.self) { kind in
                                WidgetCard(data: saved, date: .now, kind: kind)
                                    .padding(14).frame(width: 170, height: 170)
                                    .background(Color(red: 0.035, green: 0.08, blue: 0.13))
                                    .clipShape(RoundedRectangle(cornerRadius: 22))
                            }
                        }
                    }.listRowBackground(Color.clear)
                }
                Section("Cómo verlo en el coche") {
                    Text("1. Guarda los datos con el coche aparcado.\n2. Conecta el iPhone a tu AutoKit/Carlink como siempre.\n3. En el iPhone: Ajustes → General → CarPlay → tu conexión → Widgets. Añade una tarjeta de C10 Widget.\n4. En la pantalla CarPlay, desliza a la zona de widgets.")
                    Text("La aparición en CarPlay y el intervalo de actualización los gestiona iOS. El widget no abre una app completa en CarPlay ni se actualiza como un velocímetro.")
                        .font(.footnote).foregroundStyle(.secondary)
                }
            }
            .navigationTitle("C10 Widget")
            .onAppear(perform: load)
        }
    }
    @ViewBuilder private func fields(_ indices: Range<Int>) -> some View {
        ForEach(Array(indices), id: \.self) { i in
            VStack(alignment: .leading, spacing: 4) {
                Text(labels[i]).font(.caption).foregroundStyle(.secondary)
                TextField("Sin introducir", text: $values[i])
                    .keyboardType(.decimalPad).accessibilityLabel(labels[i])
                    .accessibilityIdentifier("field\(i)")
            }
        }
    }
    private func load() {
        guard saved.updatedAt != nil else { return }
        let all = [saved.battery, saved.electricRange, saved.electricConsumption, saved.electricityPrice,
                   saved.petrolConsumption, saved.petrolPrice, saved.regenPower, saved.regenSeconds]
        values = all.map { String($0) }
        if !saved.costsRecorded { for i in 2..<6 { values[i] = "" } }
        if !saved.regenerationRecorded { for i in 6..<8 { values[i] = "" } }
    }
    private func save() {
        let costs = values[2..<6].contains { !$0.trimmingCharacters(in: .whitespaces).isEmpty }
        let regen = values[6..<8].contains { !$0.trimmingCharacters(in: .whitespaces).isEmpty }
        var parsed = Array(repeating: 0.0, count: 8)
        for i in 0..<8 where i < 2 || (i < 6 && costs) || (i >= 6 && regen) {
            guard let value = VehicleData.number(values[i]) else {
                message = "Revisa: \(labels[i]). Introduce un número válido."
                return
            }
            parsed[i] = value
        }
        var data = VehicleData()
        data.battery = parsed[0]; data.electricRange = parsed[1]
        data.electricConsumption = parsed[2]; data.electricityPrice = parsed[3]
        data.petrolConsumption = parsed[4]; data.petrolPrice = parsed[5]
        data.regenPower = parsed[6]; data.regenSeconds = parsed[7]
        data.costsRecorded = costs; data.regenerationRecorded = regen; data.updatedAt = .now
        do {
            try VehicleStore.save(data, to: VehicleStore.defaults())
            saved = data
            WidgetCenter.shared.reloadAllTimelines()
            message = "Guardado manualmente. iOS decide cuándo refrescar el widget."
        } catch { message = error.localizedDescription }
        // Numeric keyboard must not cover the save result.
        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
    }
}
