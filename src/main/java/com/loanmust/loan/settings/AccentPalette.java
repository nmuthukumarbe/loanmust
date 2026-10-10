package com.loanmust.loan.settings;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The workspace accent colours offered in Settings. Each accent is a full 50-900 ramp
 * so both the Tailwind {@code brand-*} utilities and the CSS component variables can be
 * re-pointed at the chosen colour, re-theming the entire application at once.
 */
public enum AccentPalette {

    INDIGO("indigo", "Indigo", "#6366f1",
            "#eef2ff", "#e0e7ff", "#c7d2fe", "#a5b4fc", "#818cf8",
            "#6366f1", "#4f46e5", "#4338ca", "#3730a3", "#312e81"),
    OCEAN("ocean", "Ocean Blue", "#3b82f6",
            "#eff6ff", "#dbeafe", "#bfdbfe", "#93c5fd", "#60a5fa",
            "#3b82f6", "#2563eb", "#1d4ed8", "#1e40af", "#1e3a8a"),
    TEAL("teal", "Teal", "#14b8a6",
            "#f0fdfa", "#ccfbf1", "#99f6e4", "#5eead4", "#2dd4bf",
            "#14b8a6", "#0d9488", "#0f766e", "#115e59", "#134e4a"),
    EMERALD("emerald", "Emerald", "#10b981",
            "#ecfdf5", "#d1fae5", "#a7f3d0", "#6ee7b7", "#34d399",
            "#10b981", "#059669", "#047857", "#065f46", "#064e3b"),
    GOLD("gold", "Gold", "#f59e0b",
            "#fffbeb", "#fef3c7", "#fde68a", "#fcd34d", "#fbbf24",
            "#f59e0b", "#d97706", "#b45309", "#92400e", "#78350f"),
    ROSE("rose", "Rose", "#f43f5e",
            "#fff1f2", "#ffe4e6", "#fecdd3", "#fda4af", "#fb7185",
            "#f43f5e", "#e11d48", "#be123c", "#9f1239", "#881337"),
    VIOLET("violet", "Violet", "#8b5cf6",
            "#f5f3ff", "#ede9fe", "#ddd6fe", "#c4b5fd", "#a78bfa",
            "#8b5cf6", "#7c3aed", "#6d28d9", "#5b21b6", "#4c1d95"),
    GRAPHITE("graphite", "Graphite", "#64748b",
            "#f8fafc", "#f1f5f9", "#e2e8f0", "#cbd5e1", "#94a3b8",
            "#64748b", "#475569", "#334155", "#1e293b", "#0f172a");

    private static final int[] STOPS = {50, 100, 200, 300, 400, 500, 600, 700, 800, 900};

    private final String id;
    private final String label;
    private final String swatch;
    private final String[] ramp;

    AccentPalette(String id, String label, String swatch, String... ramp) {
        this.id = id;
        this.label = label;
        this.swatch = swatch;
        this.ramp = ramp;
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public String getSwatch() { return swatch; }

    /** Ordered {@code {"50": "#...", ... "900": "#..."}} map for template consumption. */
    public Map<String, String> shades() {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < STOPS.length; i++) {
            map.put(String.valueOf(STOPS[i]), ramp[i]);
        }
        return map;
    }

    public static AccentPalette fromId(String id) {
        if (id != null) {
            for (AccentPalette a : values()) {
                if (a.id.equalsIgnoreCase(id)) return a;
            }
        }
        return INDIGO;
    }

    public static List<AccentPalette> all() {
        return List.of(values());
    }
}
