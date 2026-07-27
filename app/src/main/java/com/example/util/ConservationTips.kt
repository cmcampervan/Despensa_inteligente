package com.example.util

import java.util.Locale

object ConservationTips {
    fun getTipForProduct(productName: String, category: String, location: String): String {
        val nameLower = productName.lowercase(Locale.getDefault())

        return when {
            nameLower.contains("leche") -> "Refrigerar entre 2°C y 4°C. Consumir dentro de los 3-4 días tras abrir."
            nameLower.contains("queso") -> "Guardar en nevera envuelto en papel vegetal o recipiente hermético para evitar humedad."
            nameLower.contains("yogur") -> "Mantener siempre refrigerado a <4°C. No congelar."
            nameLower.contains("mantequilla") -> "Refrigerar. Se puede mantener a temperatura ambiente brevemente para untar."
            
            nameLower.contains("carne") || nameLower.contains("pollo") || nameLower.contains("ternera") ->
                "Refrigerar a <2°C en el estante inferior. Si no se consume en 48h, congelar a -18°C."
            nameLower.contains("pescado") || nameLower.contains("marisco") ->
                "Conservar en la parte más fría de la nevera sobre hielo o consumir en 24h/congelar."
            
            nameLower.contains("manzana") || nameLower.contains("pera") ->
                "Se conservan semanas en nevera o en alacena fresca lejos de la luz directa solar."
            nameLower.contains("plátano") || nameLower.contains("platano") || nameLower.contains("banana") ->
                "Mantener a temperatura ambiente en la alacena. Evitar refrigerar para no oscurecer la piel."
            nameLower.contains("tomate") ->
                "Conservar a temperatura ambiente para preservar sabor y textura. Solo refrigerar si está muy maduro."
            nameLower.contains("patata") || nameLower.contains("papa") || nameLower.contains("cebolla") || nameLower.contains("ajo") ->
                "Guardar en alacena oscura, fresca y bien ventilada. Mantener patatas y cebollas separadas."
            nameLower.contains("lechuga") || nameLower.contains("espinaca") || nameLower.contains("verdura") ->
                "Conservar en el cajón de verduras de la nevera en bolsa transpirable con papel absorbente."

            nameLower.contains("arroz") || nameLower.contains("pasta") || nameLower.contains("harina") ->
                "Almacenar en envase hermético en alacena seca y protegida de plagas y humedad."
            nameLower.contains("pan") ->
                "Guardar en panera de tela o madera a temperatura ambiente. Congelar en rebanadas si no se consume pronto."
            nameLower.contains("legumbre") || nameLower.contains("lenteja") || nameLower.contains("garbanzo") ->
                "Almacenar en recipientes herméticos en lugar fresco, seco y oscuro."

            nameLower.contains("atún") || nameLower.contains("lata") || nameLower.contains("conserva") ->
                "Almacenar en alacena. Una vez abierta la lata, trasvasar a recipiente no metálico y refrigerar max 2 días."
            nameLower.contains("aceite") ->
                "Guardar en alacena oscura lejos de fuentes de calor (horno/fuego) para evitar oxidación."

            category.contains("Lácteos", ignoreCase = true) -> "Mantener en refrigeración constante entre 2°C y 5°C."
            category.contains("Carnes", ignoreCase = true) -> "Refrigerar en zona más fría de nevera (<3°C) o congelar."
            category.contains("Frutas", ignoreCase = true) || category.contains("Verduras", ignoreCase = true) ->
                if (location.uppercase() == "NEVERA") "Guardar en el cajón de vegetales de la nevera."
                else "Mantener en alacena o frutero ventilado y protegido del sol."
            category.contains("Congelados", ignoreCase = true) -> "Conservar en congelador a -18°C sin romper cadena de frío."
            location.uppercase() == "NEVERA" -> "Mantener refrigerado entre 2°C y 5°C en envase cerrado."
            else -> "Guardar en alacena fresca, limpia, seca y alejada de luz solar directa."
        }
    }
}
