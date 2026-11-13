package com.example.ui.i18n

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val isRtl: Boolean
) {
    ENGLISH("en", "English", "English", false),
    KURDISH_SORANI("ku", "Kurdish (Sorani)", "کوردیی سۆرانی", true)
}

object LocalizedStrings {

    fun get(key: String, lang: AppLanguage): String {
        val dict = when (lang) {
            AppLanguage.ENGLISH -> en
            AppLanguage.KURDISH_SORANI -> ku
        }
        return dict[key] ?: en[key] ?: key
    }

    private val en = mapOf(
        // Tabs
        "tab_today" to "Today",
        "tab_apps" to "Apps",
        "tab_downloads" to "Downloads",
        "tab_settings" to "Settings",

        // Actions
        "action_get" to "GET",
        "action_install" to "INSTALL",
        "action_open" to "OPEN",
        "action_read_story" to "READ STORY",
        "action_read_more" to "READ MORE →",
        "action_close" to "Close",
        "action_share" to "Share",
        "action_refresh" to "Refresh",
        "action_fx_on" to "FX ON",
        "action_fx_off" to "FX OFF",
        "action_sign_in" to "Sign In",
        "action_sign_out" to "Sign Out",

        // Today Screen
        "today_title" to "Today",
        "today_search_placeholder" to "Search stories, patch notes, updates...",
        "today_filter_all" to "All",
        "today_filter_banners" to "Banners",
        "today_filter_text" to "Text",
        "today_filter_patch" to "Patch Notes",
        "today_breaking" to "BREAKING",
        "today_no_stories" to "No stories found",
        "today_no_stories_sub" to "Try adjusting your search query or pull down to refresh.",
        "today_key_highlights" to "Key Highlights",
        "today_featured_mod" to "FEATURED MOD PACKAGE",

        // Apps Screen
        "apps_kicker" to "DISCOVER",
        "apps_title" to "Apps & Mods",
        "apps_search_prefix" to "Filter %s mods",
        "cat_all" to "All",
        "cat_apps" to "Apps",
        "cat_games" to "Games",
        "cat_tools" to "Tools",
        "cat_tweaks" to "Tweaks",
        "cat_emulators" to "Emulators",
        "cat_media" to "Media",

        // Downloads Screen
        "downloads_kicker" to "INSTALLATIONS",
        "downloads_title" to "Downloads",
        "downloads_active_queue" to "ACTIVE DOWNLOAD QUEUE",
        "downloads_completed" to "DOWNLOADED PACKAGES",
        "downloads_empty" to "No Downloaded Apps",
        "downloads_empty_sub" to "Downloaded APKs will appear here ready to install.",
        "downloads_downloading_banner" to "Downloading: %s (%d%%)",
        "downloads_active_section" to "Downloading",
        "downloads_action_view" to "View",
        "downloads_view" to "View",
        "action_update" to "UPDATE",
        "card_downloads_unit" to "downloads",
        "search_no_results" to "No matching apps found",

        // App Detail Sheet
        "detail_downloads" to "DOWNLOADS",
        "detail_version" to "VERSION",
        "detail_size_category" to "SIZE • CATEGORY",
        "detail_preview_screenshots" to "PREVIEW SCREENSHOTS",
        "detail_tab_features" to "Mod Features",
        "detail_tab_changelog" to "Changelog",
        "detail_whats_modded" to "What's Modded:",
        "detail_release_notes" to "Release Notes (v%s):",
        "detail_updated" to "Updated: %s",
        "detail_uninstall_note" to "Note: Uninstall the official app before installing this mod.",
        "detail_action_open" to "Open Installed App",
        "detail_action_update" to "Update APK (%s)",
        "detail_action_download" to "Download APK (%s)",
        "detail_lightbox_counter" to "%d of %d",
        "detail_lightbox_hint" to "Pinch to zoom • Double tap to reset",

        // Settings Screen
        "settings_kicker" to "PREFERENCES & ABOUT",
        "settings_title" to "Settings",
        "settings_language_section" to "LANGUAGE / زمان",
        "settings_language_title" to "Display Language",
        "settings_language_subtitle" to "Choose English or Kurdish Sorani (RTL)",
        "settings_appearance_section" to "APPEARANCE & PARTICLES",
        "settings_particles_title" to "Particle Animations",
        "settings_particles_on" to "Background dynamic particles active",
        "settings_particles_off" to "Particles disabled for battery saving",
        "settings_particles_density" to "Particle Density",
        "settings_dark_mode_title" to "Dark Appearance",
        "settings_dark_mode_on" to "Midnight OLED dark style",
        "settings_dark_mode_off" to "Clean iOS light style",
        "settings_accent_color" to "Accent Color Preset",
        "settings_support_section" to "SUPPORT & FEEDBACK",
        "settings_feedback_title" to "Send Feedback & Crash Report",
        "settings_feedback_subtitle" to "Directly file diagnostics & suggestions to cloud",
        "settings_account_section" to "ACCOUNT & CLOUD SYNC",
        "settings_account_title" to "Google Account Sync",
        "settings_account_signed_in" to "Syncing bookmarks & download history",
        "settings_account_signed_out" to "Sign in to backup bookmarks to cloud",
        "settings_about_section" to "ABOUT HSSTORE",
        "settings_version_title" to "HsStore Version",
        "settings_engine_title" to "Engine Architecture",
        "settings_dev_team" to "DEVELOPMENT TEAM",

        // Offline & Team Profile
        "status_offline" to "Offline",
        "status_offline_desc" to "Browsing cached apps",
        "hama_sade_team_title" to "HAMA SADE TEAM",
        "hama_sade_team_subtitle" to "Lead Architecture & Cyber Modding Collective",
        "hama_sade_team_bio" to "Official project creator & maintainer. Join for VIP mod drops, direct support, and update announcements.",
        "action_join_telegram" to "Join Telegram"
    )

    private val ku = mapOf(
        // Tabs (Kurdish Sorani)
        "tab_today" to "ئەمڕۆ",
        "tab_apps" to "بەرنامەکان",
        "tab_downloads" to "داگیراوەکان",
        "tab_settings" to "ڕێکخستن",

        // Actions
        "action_get" to "داگرتن",
        "action_install" to "دامەزراندن",
        "action_open" to "کردنەوە",
        "action_read_story" to "خوێندنەوەی هەواڵ",
        "action_read_more" to "زیاتر بخوێنەوە ←",
        "action_close" to "داخستن",
        "action_share" to "هاوبەشکردن",
        "action_refresh" to "نوێکردنەوە",
        "action_fx_on" to "کاریگەری چالاکە",
        "action_fx_off" to "کاریگەری ناچالاکە",
        "action_sign_in" to "چوونەژوورەوە",
        "action_sign_out" to "دەرچوون",

        // Today Screen
        "today_title" to "ئەمڕۆ",
        "today_search_placeholder" to "گەڕان لە هەواڵەکان و نوێکارییەکان...",
        "today_filter_all" to "هەمووی",
        "today_filter_banners" to "سەرەکییەکان",
        "today_filter_text" to "دەقی",
        "today_filter_patch" to "نوێکارییەکان",
        "today_breaking" to "بەپەلە",
        "today_no_stories" to "هیچ هەواڵێک نەدۆزرایەوە",
        "today_no_stories_sub" to "وشەی گەڕانەکەت بگۆڕە یان پەڕەکە بەرەو خوار ڕابکێشە بۆ نوێکردنەوە.",
        "today_key_highlights" to "خاڵە سەرەکییەکان",
        "today_featured_mod" to "مۆدی هەڵبژێردراو",

        // Apps Screen
        "apps_kicker" to "دۆزینەوە",
        "apps_title" to "بەرنامە و مۆدەکان",
        "apps_search_prefix" to "گەڕان لە مۆدەکانی %s",
        "cat_all" to "هەمووی",
        "cat_apps" to "بەرنامەکان",
        "cat_games" to "یارییەکان",
        "cat_tools" to "ئامرازەکان",
        "cat_tweaks" to "دەستکاریکراو",
        "cat_emulators" to "لاساییکەرەوەکان",
        "cat_media" to "میدیا",

        // Downloads Screen
        "downloads_kicker" to "دامەزراوەکان",
        "downloads_title" to "داگیراوەکان",
        "downloads_active_queue" to "ڕیزی داگرتنە چالاکەکان",
        "downloads_completed" to "پەڕگە داگیراوەکان",
        "downloads_empty" to "هیچ بەرنامەیەکی داگیراو نییە",
        "downloads_empty_sub" to "فایلە داگیراوەکان لێرە دەردەکەون بۆ دامەزراندن.",
        "downloads_downloading_banner" to "داگرتن: %s (%d%%)",
        "downloads_active_section" to "دادەبەزێت",
        "downloads_action_view" to "پیشاندان",
        "downloads_view" to "پیشاندان",
        "action_update" to "نوێکردنەوە",
        "card_downloads_unit" to "داونلۆد",
        "search_no_results" to "هیچ بەرنامەیەک نەدۆزرایەوە",

        // App Detail Sheet
        "detail_downloads" to "داونلۆدەکان",
        "detail_version" to "وەشان",
        "detail_size_category" to "قەبارە • جۆر",
        "detail_preview_screenshots" to "وێنەکانی پێشبینین",
        "detail_tab_features" to "تایبەتمەندییەکان",
        "detail_tab_changelog" to "گۆڕانکارییەکان",
        "detail_whats_modded" to "دەستکارییەکان:",
        "detail_release_notes" to "تێبینییەکانی وەشان (v%s):",
        "detail_updated" to "نوێکراوەتەوە: %s",
        "detail_uninstall_note" to "تێبینی: پێش دامەزراندن، پێویستە وەشانی فەرمیی ئەپەکە بسڕیتەوە.",
        "detail_action_open" to "کردنەوەی بەرنامە",
        "detail_action_update" to "نوێکردنەوەی APK (%s)",
        "detail_action_download" to "داگرتنی APK (%s)",
        "detail_lightbox_counter" to "%d لە %d",
        "detail_lightbox_hint" to "بە دوو پەنجە گەورەی بکە • دوو جار لێبدە بۆ گەڕاندنەوە",

        // Settings Screen
        "settings_kicker" to "هەڵبژاردە و زانیاری",
        "settings_title" to "ڕێکخستنەکان",
        "settings_language_section" to "زمان / LANGUAGE",
        "settings_language_title" to "زمانی بەرنامە",
        "settings_language_subtitle" to "ئینگلیزی یان کوردیی سۆرانی هەڵبژێرە",
        "settings_appearance_section" to "ڕووکار و تەنۆلکەکان",
        "settings_particles_title" to "جووڵەی تەنۆلکەکان",
        "settings_particles_on" to "تەنۆلکە جووڵاوەکانی پاشبنەما چالاکن",
        "settings_particles_off" to "تەنۆلکەکان ناچالاک کراون بۆ پاشەکەوتی پاتری",
        "settings_particles_density" to "چڕیی تەنۆلکەکان",
        "settings_dark_mode_title" to "ڕووکاری تاریک",
        "settings_dark_mode_on" to "شێوازی تاریکی تەواو (OLED)",
        "settings_dark_mode_off" to "شێوازی ڕووناکی ڕوون",
        "settings_accent_color" to "ڕەنگی سەرەکی",
        "settings_support_section" to "پشتیوانی و ڕاووبۆچوون",
        "settings_feedback_title" to "ناردنی ڕاووبۆچوون و ڕاپۆرتی کێشە",
        "settings_feedback_subtitle" to "ڕاستەوخۆ سەرنجەکانت بنێرە بۆ گەشەپێدەران",
        "settings_account_section" to "هەژمار و هاوکاتکردن",
        "settings_account_title" to "هاوکاتکردنی هەژماری گۆگڵ",
        "settings_account_signed_in" to "نیشانەکان و مێژووی داگرتن هاوکات کراون",
        "settings_account_signed_out" to "بچۆ ژوورەوە بۆ پاراستنی نیشانەکانت لە هەور",
        "settings_about_section" to "دەربارەی HSSTORE",
        "settings_version_title" to "وەشانی HSSTORE",
        "settings_engine_title" to "تەلارسازیی ماتۆڕ",
        "settings_dev_team" to "تیمی گەشەپێدان",

        // Offline & Team Profile
        "status_offline" to "ئۆفلاین",
        "status_offline_desc" to "بەرنامە هەڵگیراوەکان پیشان دەدرێن",
        "hama_sade_team_title" to "HAMA SADE TEAM",
        "hama_sade_team_subtitle" to "تەلارسازی و مۆدسازی پێشکەوتوو",
        "hama_sade_team_bio" to "دروستکەر و سەرپەرشتیاری فەرمی پرۆژەکە. بەشداربە لە کەناڵی تەلەگرام بۆ نوێترین مۆدەکان و وەشانی نوێ.",
        "action_join_telegram" to "چوون بۆ تەلەگرام"
    )
}
