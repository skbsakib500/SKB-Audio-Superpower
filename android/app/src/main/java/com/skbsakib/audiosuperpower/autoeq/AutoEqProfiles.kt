package com.skbsakib.audiosuperpower.autoeq

/**
 * Built-in headphone correction profiles.
 *
 * These are HAND-TUNED representative corrections approximating the general
 * spectral tendencies of common headphones. They are NOT byte-exact copies of
 * the AutoEQ project data — for accuracy, licensed/cited AutoEQ profiles can be
 * imported later. Value: immediately audible correction for popular models.
 *
 * Filter format matches AutoEqChain:
 *   type  0 = PK (peaking),  1 = LSH (low shelf), 2 = HSH (high shelf)
 *   freq  Hz
 *   q     Q factor (biquad)
 *   gain  dB
 */
data class AutoEqFilter(
    val type: Int,
    val freq: Float,
    val q: Float,
    val gainDb: Float
)

data class AutoEqProfile(
    val id: String,
    val brand: String,
    val model: String,
    val preampDb: Float,
    val filters: List<AutoEqFilter>
)

object AutoEqProfiles {

    val all: List<AutoEqProfile> = listOf(

        AutoEqProfile(
            id = "flat", brand = "Reference", model = "Flat (no correction)",
            preampDb = 0f, filters = emptyList()
        ),

        // ─────────────── Sony ───────────────
        AutoEqProfile(
            id = "sony_wh1000xm4", brand = "Sony", model = "WH-1000XM4",
            preampDb = -5.5f,
            filters = listOf(
                AutoEqFilter(1,  105f, 0.7f,  +3.5f),  // bass shelf
                AutoEqFilter(0,  200f, 1.2f,  -2.0f),
                AutoEqFilter(0,  900f, 1.5f,  +1.5f),
                AutoEqFilter(0, 2800f, 2.5f,  -3.5f),  // upper-mid dip
                AutoEqFilter(0, 5500f, 3.0f,  +2.5f),
                AutoEqFilter(0, 8000f, 2.5f,  -3.0f),  // treble spike cut
                AutoEqFilter(2, 10000f, 0.7f, +1.5f)
            )
        ),

        AutoEqProfile(
            id = "sony_wh1000xm5", brand = "Sony", model = "WH-1000XM5",
            preampDb = -5.0f,
            filters = listOf(
                AutoEqFilter(1,  110f, 0.7f,  +3.0f),
                AutoEqFilter(0,  250f, 1.2f,  -1.5f),
                AutoEqFilter(0,  950f, 1.5f,  +1.5f),
                AutoEqFilter(0, 3000f, 2.5f,  -3.0f),
                AutoEqFilter(0, 6000f, 3.0f,  +2.0f),
                AutoEqFilter(0, 8500f, 2.5f,  -2.5f),
                AutoEqFilter(2, 10500f, 0.7f, +1.5f)
            )
        ),

        AutoEqProfile(
            id = "sony_wf1000xm4", brand = "Sony", model = "WF-1000XM4",
            preampDb = -5.0f,
            filters = listOf(
                AutoEqFilter(1,  120f, 0.7f,  +4.5f),
                AutoEqFilter(0,  300f, 1.2f,  -2.5f),
                AutoEqFilter(0, 1000f, 1.5f,  +1.5f),
                AutoEqFilter(0, 3200f, 2.5f,  -3.0f),
                AutoEqFilter(0, 6000f, 3.0f,  +2.5f),
                AutoEqFilter(0, 9000f, 2.5f,  -2.0f)
            )
        ),

        // ─────────────── Apple ───────────────
        AutoEqProfile(
            id = "apple_airpods_pro_2", brand = "Apple", model = "AirPods Pro 2",
            preampDb = -4.5f,
            filters = listOf(
                AutoEqFilter(1,  100f, 0.7f,  +2.5f),
                AutoEqFilter(0,  250f, 1.2f,  -1.5f),
                AutoEqFilter(0, 1200f, 1.5f,  +1.0f),
                AutoEqFilter(0, 3000f, 2.5f,  -2.0f),
                AutoEqFilter(0, 6000f, 3.0f,  +2.0f),
                AutoEqFilter(0, 9500f, 2.5f,  -2.5f)
            )
        ),

        AutoEqProfile(
            id = "apple_airpods_max", brand = "Apple", model = "AirPods Max",
            preampDb = -4.0f,
            filters = listOf(
                AutoEqFilter(1,   90f, 0.7f,  +2.0f),
                AutoEqFilter(0,  200f, 1.2f,  -1.0f),
                AutoEqFilter(0, 2800f, 2.5f,  -2.0f),
                AutoEqFilter(0, 5500f, 3.0f,  +1.5f),
                AutoEqFilter(0, 8500f, 2.5f,  -2.0f)
            )
        ),

        // ─────────────── Sennheiser ───────────────
        AutoEqProfile(
            id = "sennheiser_hd600", brand = "Sennheiser", model = "HD 600",
            preampDb = -3.0f,
            filters = listOf(
                AutoEqFilter(1,  100f, 0.7f,  +4.0f),  // bass shelf (linear sub extension)
                AutoEqFilter(0,  3500f, 2.5f,  -1.5f),  // slight upper-mid dip
                AutoEqFilter(0,  8000f, 3.0f,  +1.5f)
            )
        ),

        AutoEqProfile(
            id = "sennheiser_hd650", brand = "Sennheiser", model = "HD 650 / HD 6XX",
            preampDb = -3.0f,
            filters = listOf(
                AutoEqFilter(1,   95f, 0.7f,  +4.5f),
                AutoEqFilter(0,  200f, 1.2f,  -1.5f),
                AutoEqFilter(0, 3500f, 2.5f,  -1.0f),
                AutoEqFilter(0, 8500f, 3.0f,  +1.5f)
            )
        ),

        AutoEqProfile(
            id = "sennheiser_hd800s", brand = "Sennheiser", model = "HD 800 S",
            preampDb = -3.0f,
            filters = listOf(
                AutoEqFilter(1,  100f, 0.7f,  +4.5f),
                AutoEqFilter(0,  250f, 1.2f,  -1.5f),
                AutoEqFilter(0, 6000f, 3.0f,  -4.5f),  // 6kHz peak cut (signature)
                AutoEqFilter(0, 10000f, 2.5f, +1.5f)
            )
        ),

        // ─────────────── Bose ───────────────
        AutoEqProfile(
            id = "bose_qc35", brand = "Bose", model = "QuietComfort 35 II",
            preampDb = -5.0f,
            filters = listOf(
                AutoEqFilter(1,  110f, 0.7f,  +4.0f),
                AutoEqFilter(0,  250f, 1.2f,  -2.5f),
                AutoEqFilter(0, 1000f, 1.5f,  +1.0f),
                AutoEqFilter(0, 3000f, 2.5f,  -2.0f),
                AutoEqFilter(0, 6500f, 3.0f,  +2.0f),
                AutoEqFilter(0, 9000f, 2.5f,  -2.0f)
            )
        ),

        AutoEqProfile(
            id = "bose_qc45", brand = "Bose", model = "QuietComfort 45",
            preampDb = -4.5f,
            filters = listOf(
                AutoEqFilter(1,  115f, 0.7f,  +3.5f),
                AutoEqFilter(0,  280f, 1.2f,  -2.0f),
                AutoEqFilter(0,  900f, 1.5f,  +1.0f),
                AutoEqFilter(0, 3200f, 2.5f,  -2.0f),
                AutoEqFilter(0, 7000f, 3.0f,  +1.5f),
                AutoEqFilter(0, 9500f, 2.5f,  -1.5f)
            )
        ),

        // ─────────────── Samsung ───────────────
        AutoEqProfile(
            id = "samsung_buds_pro2", brand = "Samsung", model = "Galaxy Buds2 Pro",
            preampDb = -4.0f,
            filters = listOf(
                AutoEqFilter(1,  100f, 0.7f,  +3.0f),
                AutoEqFilter(0,  300f, 1.2f,  -1.5f),
                AutoEqFilter(0, 1000f, 1.5f,  +1.0f),
                AutoEqFilter(0, 2800f, 2.5f,  -2.0f),
                AutoEqFilter(0, 6000f, 3.0f,  +2.0f),
                AutoEqFilter(0, 8500f, 2.5f,  -2.0f)
            )
        ),

        // ─────────────── JBL / Beats ───────────────
        AutoEqProfile(
            id = "jbl_tune_beam", brand = "JBL", model = "Tune Beam / Wave",
            preampDb = -5.0f,
            filters = listOf(
                AutoEqFilter(1,  110f, 0.7f,  +4.0f),
                AutoEqFilter(0,  300f, 1.2f,  -2.5f),
                AutoEqFilter(0, 1200f, 1.5f,  +1.5f),
                AutoEqFilter(0, 3500f, 2.5f,  -2.5f),
                AutoEqFilter(0, 6500f, 3.0f,  +2.5f),
                AutoEqFilter(0, 9500f, 2.5f,  -2.5f)
            )
        ),

        AutoEqProfile(
            id = "beats_studio3", brand = "Beats", model = "Studio3 Wireless",
            preampDb = -6.0f,
            filters = listOf(
                AutoEqFilter(1,  120f, 0.7f,  +4.5f),
                AutoEqFilter(0,  400f, 1.2f,  -3.5f),  // heavy bass bleed
                AutoEqFilter(0, 1500f, 1.5f,  +2.0f),
                AutoEqFilter(0, 4000f, 2.5f,  -3.0f),
                AutoEqFilter(0, 8000f, 3.0f,  +2.5f),
                AutoEqFilter(0, 11000f, 2.5f, -3.0f)
            )
        )
    )

    fun byId(id: String): AutoEqProfile? = all.firstOrNull { it.id == id }
    fun byBrand(): Map<String, List<AutoEqProfile>> =
        all.groupBy { it.brand }
}
