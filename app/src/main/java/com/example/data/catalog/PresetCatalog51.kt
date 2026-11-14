package com.example.data.catalog

import com.example.data.model.ModApp

object PresetCatalog51 {

    private data class SeedSpec(
        val id: String,
        val name: String,
        val version: String,
        val sizeDisplay: String,
        val packageName: String,
        val glowColorHex: String,
        val description: String,
        val category: String
    )

    private fun parseSizeBytes(sizeDisplay: String): Long {
        val parts = sizeDisplay.trim().split(Regex("\\s+"))
        val num = parts.firstOrNull()?.toDoubleOrNull() ?: return 0L
        val unit = parts.getOrNull(1)?.uppercase() ?: "MB"
        return when (unit) {
            "GB" -> (num * 1024.0 * 1024.0 * 1024.0).toLong()
            "KB" -> (num * 1024.0).toLong()
            else -> (num * 1024.0 * 1024.0).toLong()
        }
    }

    private fun parseVersionCode(version: String): Int {
        val digits = version.trim().removePrefix("v").removePrefix("V")
            .split(".")
            .firstOrNull()
            ?.toIntOrNull()
        return digits ?: 1
    }

    private val specs = listOf(
        // Apps — Social (1–9)
        SeedSpec(
            id = "preset_01_snapchat_prime_v6",
            name = "Snapchat Prime V6",
            version = "6.0",
            sizeDisplay = "177.6 MB",
            packageName = "com.snapchat.android",
            glowColorHex = "#FFFC00",
            description = "سەیرکردنی ستۆری و ستریک بە نهێنی بێ دیاربوون، سکرینشۆت بێ ئاگادارکردنەوە، ئیمۆجی ئایفۆن و چارەسەری SS06/SS07.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_02_snapchat_elite_plus_v4",
            name = "Snapchat ELITE Plus V4",
            version = "4.0",
            sizeDisplay = "165 MB",
            packageName = "com.snapchat.android",
            glowColorHex = "#8429FF",
            description = "هێشتنەوەی نامە سڕاوەکان تەنانەت ئەگەر لای بەرامبەر بسڕێتەوە، لەگەڵ تایبەتمەندییەکانی VIP و دۆخی تارمایی.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_03_instagram_prime",
            name = "Instagram Prime",
            version = "6.0",
            sizeDisplay = "88 MB",
            packageName = "com.instagram.android",
            glowColorHex = "#F00075",
            description = "داگرتنی هەموو ڤیدیۆ و ڕیڵس و ستۆرییەکان، سەیرکردنی نامە و ستۆری بێ دەرکەوتنی Seen، و فۆنت و ئیمۆجی ئایفۆن.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_04_tiktok_prime",
            name = "TikTok Prime",
            version = "8.0",
            sizeDisplay = "135 MB",
            packageName = "com.zhiliaoapp.musically",
            glowColorHex = "#E61938",
            description = "داگرتنی ڤیدیۆ بە کوالێتی بنەڕەتی و بێ لۆگۆ (Watermark)، لابردنی هەموو ڕیکلامەکان و کردنەوەی فلتەرەکان.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_05_telegram_prime",
            name = "Telegram Prime",
            version = "6.0",
            sizeDisplay = "74 MB",
            packageName = "org.telegram.messenger",
            glowColorHex = "#007AFF",
            description = "تایبەتمەندییەکانی Telegram Premium بەخۆڕایی، خێرایی بەرزی داگرتن، و بینینی نامە سڕاوەکان.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_06_messenger_prime",
            name = "Messenger Prime",
            version = "1.0",
            sizeDisplay = "98.8 MB",
            packageName = "com.facebook.orca",
            glowColorHex = "#8429FF",
            description = "پاراستنی نامە سڕاوەکان، بینینی نامە و ستۆری بە نهێنی، و ناردنی وێنە و ڤیدیۆ بە کوالێتی تەواو.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_07_facebook_prime",
            name = "FaceBook Prime",
            version = "1.0",
            sizeDisplay = "112 MB",
            packageName = "com.facebook.katana",
            glowColorHex = "#007AFF",
            description = "بەبێ ڕیکلام و بە توانای داگرتنی ڕاستەوخۆی ڤیدیۆ و ستۆرییەکان.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_08_x_prime",
            name = "X Prime",
            version = "1.0",
            sizeDisplay = "82 MB",
            packageName = "com.twitter.android",
            glowColorHex = "#0D747C",
            description = "لابردنی هەموو ڕیکلامەکان و داگرتنی ڤیدیۆ و GIF لە X بە یەک کرتە.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_09_sendit_prime",
            name = "Sendit Prime",
            version = "3.0",
            sizeDisplay = "54 MB",
            packageName = "com.getsendit.sendit",
            glowColorHex = "#F00075",
            description = "کردنەوەی تایبەتمەندییەکانی VIP لە بەرنامەی Sendit بۆ بینینی ئاماژە و زانیاری لەسەر نێردەری نامەکان.",
            category = "Apps"
        ),

        // Apps — Photo, Video & Design (10–22)
        SeedSpec(
            id = "preset_10_capcut_prime",
            name = "CapCut Prime",
            version = "13.0",
            sizeDisplay = "145 MB",
            packageName = "com.lemon.lvoverseas",
            glowColorHex = "#0D747C",
            description = "هەموو تایبەتمەندییەکانی Pro کراوەن بێ واتەرمارک، پشتگیری زمانی کوردی و هەناردەکردنی 4K 60fps.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_11_alight_motion_prime",
            name = "Alight Motion Prime",
            version = "4.0",
            sizeDisplay = "96 MB",
            packageName = "com.alightcreative.motion",
            glowColorHex = "#1CA64C",
            description = "مۆشن گرافیکی پیشەیی بە کراوەیی هەموو ئیفێکتەکانی VIP، پشتگیری XML و لابردنی واتەرمارک.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_12_tracket_motion_prime",
            name = "Tracket Motion Prime",
            version = "1.0",
            sizeDisplay = "68 MB",
            packageName = "",
            glowColorHex = "#8429FF",
            description = "تراککردن و جووڵەی سینەمایی بۆ مۆنتاژی خێرای ڤیدیۆ.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_13_remini_prime",
            name = "Remini Prime",
            version = "8.0",
            sizeDisplay = "72 MB",
            packageName = "com.bigwinepot.nwdn.international",
            glowColorHex = "#E61938",
            description = "سافکردن و ڕوونکردنەوەی وێنەی تەڵخ و کۆن بە AI بەبێ ڕیکلام و سنووری ڕۆژانە.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_14_photoroom_prime",
            name = "PhotoRoom Prime",
            version = "3.0",
            sizeDisplay = "64 MB",
            packageName = "com.photoroom.app",
            glowColorHex = "#8429FF",
            description = "لابردنی پاشبنەمای وێنە بە کوالێتی ستۆدیۆیی HD و هەناردەکردنی بێ لۆگۆ.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_15_picsart_pro_prime",
            name = "Picsart Pro Prime",
            version = "3.0",
            sizeDisplay = "85 MB",
            packageName = "com.picsart.studio",
            glowColorHex = "#F00075",
            description = "هەموو ئامراز و فلتەرەکانی Picsart Gold کراوەن لەگەڵ فۆنتی کوردی.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_16_lightroom_prime",
            name = "LightRoom Prime",
            version = "1.0",
            sizeDisplay = "110 MB",
            packageName = "com.adobe.lrmobile",
            glowColorHex = "#007AFF",
            description = "ڕەنگکردنی پیشەیی وێنە و کردنەوەی بەشی Masking و هەموو فلتەرەکان.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_17_inshot_prime",
            name = "InShot Prime",
            version = "7.0",
            sizeDisplay = "69 MB",
            packageName = "com.camerasideas.instashot",
            glowColorHex = "#E61938",
            description = "مۆنتاژی خێرای ڤیدیۆ و وێنە بێ واتەرمارک بە کراوەیی هەموو ستیکەر و ئیفێکتەکان.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_18_kinemaster_prime",
            name = "KineMaster Prime",
            version = "5.0",
            sizeDisplay = "108 MB",
            packageName = "com.nexstreaming.app.kinemasterfree",
            glowColorHex = "#E59400",
            description = "مۆنتاژی فرە-چین بێ لۆگۆ، فرۆشگای ئەسێتی کراوە و کوالێتی 4K.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_19_videoleap_prime",
            name = "VideoLeap Prime",
            version = "2.0",
            sizeDisplay = "92 MB",
            packageName = "com.lightricks.videoleap",
            glowColorHex = "#8429FF",
            description = "ئیفێکتی سینەمایی و ئامرازەکانی AI کراوەن.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_20_cute_cut_pro_prime",
            name = "Cute Cut Pro Prime",
            version = "2.0",
            sizeDisplay = "52 MB",
            packageName = "com.yuBo.CuteCutPro",
            glowColorHex = "#0D747C",
            description = "وەشانی Pro بەبێ واتەرمارک بۆ تایپۆگرافی و وێنەکێشان لەسەر ڤیدیۆ.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_21_polarr_prime",
            name = "Polarr Prime",
            version = "1.0",
            sizeDisplay = "66 MB",
            packageName = "photo.editor.polarr",
            glowColorHex = "#007AFF",
            description = "کردنەوەی هەموو فلتەر و کۆدەکانی Polarr QR و ئامرازەکانی ڕووخسار.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_22_lovi_prime",
            name = "Lovi Prime",
            version = "1.0",
            sizeDisplay = "58 MB",
            packageName = "",
            glowColorHex = "#F00075",
            description = "تێمپلەیتی ستۆری و ڤیدیۆی میوزیکاڵی پڕۆ بە کوالێتی بەرز.",
            category = "Apps"
        ),

        // Apps — Lifestyle, iOS & Tools (23–40)
        SeedSpec(
            id = "preset_23_hs_font_emoji_iphone",
            name = "HS Font Emoji iPhone",
            version = "3.0",
            sizeDisplay = "42 MB",
            packageName = "",
            glowColorHex = "#1CA64C",
            description = "گۆڕینی فۆنتی کوردی و ئیمۆجی مۆبایل بۆ شێوازی ئایفۆن (iOS).",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_24_hs_keyboard_emoji_iphone",
            name = "HS Keyboard Emoji iPhone",
            version = "1.0",
            sizeDisplay = "38 MB",
            packageName = "",
            glowColorHex = "#0D747C",
            description = "کیبۆردی شێوە ئایفۆن بە ئیمۆجی iOS و هەموو پیتە کوردییەکان.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_25_emoji_ios_16",
            name = "Emoji iOS 16",
            version = "16.0",
            sizeDisplay = "28 MB",
            packageName = "",
            glowColorHex = "#E59400",
            description = "فایلی فۆنتی ئیمۆجی ئایفۆن (TTF) بۆ بەرنامەکانی مۆنتاژ و سیستەم.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_26_gallery_iphone_prime",
            name = "Gallery iPhone Prime",
            version = "1.0",
            sizeDisplay = "32 MB",
            packageName = "",
            glowColorHex = "#F00075",
            description = "گەلەری وێنە و ڤیدیۆ بە دیزاینی ئایفۆن (iOS Photos) بێ ڕیکلام.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_27_files_iphone_prime",
            name = "Files iPhone Prime",
            version = "1.0",
            sizeDisplay = "29 MB",
            packageName = "",
            glowColorHex = "#007AFF",
            description = "بەڕێوەبەری فایلەکانی مۆبایل بە دیزاینی iOS Files.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_28_calendar_iphone_prime",
            name = "Calendar iPhone Prime",
            version = "1.0",
            sizeDisplay = "24 MB",
            packageName = "",
            glowColorHex = "#E61938",
            description = "ڕۆژژمێری ئەپڵ (iOS Calendar) بە دیزاینێکی سادە بێ ڕیکلام.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_29_dialer_iphone_prime",
            name = "Dialer iPhone Prime",
            version = "1.0",
            sizeDisplay = "35 MB",
            packageName = "",
            glowColorHex = "#1CA64C",
            description = "شاشەی پەیوەندی و لێدانی ژمارە بە شێوازی ڕەسەنی iOS.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_30_youtube_prime",
            name = "YouTube Prime",
            version = "5.0",
            sizeDisplay = "84 MB",
            packageName = "com.google.android.youtube",
            glowColorHex = "#E61938",
            description = "سەیرکردنی یوتوب بێ ڕیکلام و کارکردن لە پاشبنەما کاتێک شاشە کوژاوەتەوە.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_31_mytv_plus_prime",
            name = "MYTV+ Prime",
            version = "4.0",
            sizeDisplay = "45 MB",
            packageName = "",
            glowColorHex = "#8429FF",
            description = "پەخشی ڕاستەوخۆی یارییەکان، کەناڵە وەرزشی و جیهانییەکان بەخۆڕایی.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_32_netflix_prime",
            name = "Netflix Prime",
            version = "3.0",
            sizeDisplay = "62 MB",
            packageName = "com.netflix.mediaclient",
            glowColorHex = "#E61938",
            description = "سەیرکردنی فیلم و زنجیرە جیهانییەکان بە کوالێتی 4K و ژێرنووس بێ هەژمار.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_33_black_hole",
            name = "Black Hole",
            version = "1.0",
            sizeDisplay = "34 MB",
            packageName = "",
            glowColorHex = "#0D747C",
            description = "گوێگرتن و داگرتنی گۆرانی بە کوالێتی 320kbps و کارکردن بێ ئینتەرنێت.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_34_djay_pro_prime",
            name = "djay Pro Prime",
            version = "4.0",
            sizeDisplay = "78 MB",
            packageName = "com.algoriddim.djay_free",
            glowColorHex = "#8429FF",
            description = "ستۆدیۆی تێکەڵکردنی دەنگ و هەموو ئامرازەکانی DJ بە کراوەیی.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_35_hs_lab_kurd_prime",
            name = "HS Lab Kurd Prime",
            version = "1.0",
            sizeDisplay = "44 MB",
            packageName = "",
            glowColorHex = "#0D747C",
            description = "تاقیگەی تایبەتی دیزاین و نووسینی پڕۆفیشناڵ بە فۆنتە کوردییەکان.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_36_hs_notes_prime",
            name = "HS Notes Prime",
            version = "1.0",
            sizeDisplay = "26 MB",
            packageName = "",
            glowColorHex = "#E59400",
            description = "تۆمارکردنی تێبینی بە دیزاینی ئایفۆن و فۆنتی کوردی.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_37_truecaller_prime",
            name = "Truecaller Prime",
            version = "3.0",
            sizeDisplay = "76 MB",
            packageName = "com.truecaller",
            glowColorHex = "#007AFF",
            description = "ئاشکراکردنی ناوی ژمارە نەناسراوەکان بە تایبەتمەندی Gold و بلۆککردنی سپام.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_38_z_translate_prime",
            name = "Z Translate Prime",
            version = "2.0",
            sizeDisplay = "39 MB",
            packageName = "",
            glowColorHex = "#1CA64C",
            description = "وەرگێڕانی ڕاستەوخۆی شاشە و ژێرنووسی ڤیدیۆ و یارییەکان.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_39_baxa_app",
            name = "BaXa App",
            version = "1.0",
            sizeDisplay = "31 MB",
            packageName = "",
            glowColorHex = "#8429FF",
            description = "وەشانی پڕایمی ئەپی BaXa بە هەموو تایبەتمەندییە کراوەکانەوە.",
            category = "Apps"
        ),
        SeedSpec(
            id = "preset_40_hama_cloner_arm",
            name = "Hama Cloner Arm",
            version = "1.0",
            sizeDisplay = "48 MB",
            packageName = "",
            glowColorHex = "#E59400",
            description = "دروستکردنی کۆپی دووەم لە هەر ئەپ و یارییەک بێ کێشە لەسەر یەک مۆبایل.",
            category = "Apps"
        ),

        // Games (41–51)
        SeedSpec(
            id = "preset_41_god_of_war_iii",
            name = "God Of War III",
            version = "1.0",
            sizeDisplay = "18.5 GB",
            packageName = "",
            glowColorHex = "#E61938",
            description = "یاری ئەفسانەیی کۆنسۆڵ لەسەر مۆبایلەکەت لەگەڵ فایلە پێویستەکان.",
            category = "Games"
        ),
        SeedSpec(
            id = "preset_42_gta_v",
            name = "GTA V",
            version = "1.0",
            sizeDisplay = "45 GB",
            packageName = "",
            glowColorHex = "#1CA64C",
            description = "جیهانی کراوەی GTA V لەسەر شاشەی ئەندرۆید.",
            category = "Games"
        ),
        SeedSpec(
            id = "preset_43_gta_san_andreas_kurdish",
            name = "GTA San Andreas Kurdish",
            version = "1.0",
            sizeDisplay = "2.9 GB",
            packageName = "com.rockstargames.gtasa",
            glowColorHex = "#E59400",
            description = "بە ژێرنووسی کوردی، گۆرانی کوردی، جلوبەرگی کوردی و مێنوی فێڵی کوردی.",
            category = "Games"
        ),
        SeedSpec(
            id = "preset_44_gta_vice_city_kurdish",
            name = "GTA Vice City Kurdish",
            version = "1.0",
            sizeDisplay = "1.6 GB",
            packageName = "com.rockstargames.gtavc",
            glowColorHex = "#F00075",
            description = "چیتەی ڤایس سیتی بە ژێرنووس و مێنوی زمانی کوردی.",
            category = "Games"
        ),
        SeedSpec(
            id = "preset_45_bully_kurdish",
            name = "BULLY Kurdish",
            version = "1.0",
            sizeDisplay = "2.8 GB",
            packageName = "com.rockstargames.bully",
            glowColorHex = "#007AFF",
            description = "یاری بەناوبانگی بۆڵی بە ژێرنووس و مۆد و مێنوی کوردی.",
            category = "Games"
        ),
        SeedSpec(
            id = "preset_46_choo_choo_charles",
            name = "Choo Choo Charles",
            version = "1.0",
            sizeDisplay = "420 MB",
            packageName = "",
            glowColorHex = "#E61938",
            description = "یاری ترسناک و سەرکێشی شەمەندەفەرەکە بۆ ئەندرۆید.",
            category = "Games"
        ),
        SeedSpec(
            id = "preset_47_car_parking_multiplayer",
            name = "Car Parking Multiplayer",
            version = "1.0",
            sizeDisplay = "950 MB",
            packageName = "com.olzhas.carparking.multyplayer",
            glowColorHex = "#8429FF",
            description = "پارەی بێسنوور و کردنەوەی هەموو سەیارەکان.",
            category = "Games"
        ),
        SeedSpec(
            id = "preset_48_dragon_8ball_pool",
            name = "Dragon 8Ball Pool",
            version = "1.0",
            sizeDisplay = "115 MB",
            packageName = "com.miniclip.eightballpool",
            glowColorHex = "#0D747C",
            description = "مۆدی بلیارد بە هێڵی ئامانجی درێژ بەبێ باند.",
            category = "Games"
        ),
        SeedSpec(
            id = "preset_49_subway_surfers",
            name = "Subway Surfers",
            version = "1.0",
            sizeDisplay = "160 MB",
            packageName = "com.kiloo.subwaysurf",
            glowColorHex = "#E59400",
            description = "پارە و کلیلی بێسنوور و هەموو کارەکتەرەکان کراوەن.",
            category = "Games"
        ),
        SeedSpec(
            id = "preset_50_minecraft",
            name = "Minecraft",
            version = "1.0",
            sizeDisplay = "240 MB",
            packageName = "com.mojang.minecraftpe",
            glowColorHex = "#1CA64C",
            description = "وەشانی تەواوی ماینکرافت بەخۆڕایی و سکینی کراوە.",
            category = "Games"
        ),
        SeedSpec(
            id = "preset_51_truckers_of_europe_3",
            name = "Truckers Of Europe 3",
            version = "1.0",
            sizeDisplay = "490 MB",
            packageName = "com.WandaSoftware.TruckersofEurope3",
            glowColorHex = "#007AFF",
            description = "لێخوڕینی بارهەڵگر بە پارەی بێسنوور و کڕینی هەموو بارهەڵگرەکان.",
            category = "Games"
        )
    )

    fun buildDraftModApps(): List<ModApp> {
        val now = System.currentTimeMillis().toString()
        return specs.mapIndexed { index, spec ->
            ModApp(
                id = spec.id,
                name = spec.name,
                packageName = spec.packageName,
                category = spec.category,
                version = spec.version,
                versionCode = parseVersionCode(spec.version),
                sizeBytes = parseSizeBytes(spec.sizeDisplay),
                sizeDisplay = spec.sizeDisplay,
                iconUrl = "",
                downloadUrl = "",
                description = spec.description,
                author = "HsStore",
                updatedDate = now,
                sortOrder = index,
                downloadCount = 0L,
                isVerified = true,
                glowColorHex = spec.glowColorHex,
                isPublished = false
            )
        }
    }
}
