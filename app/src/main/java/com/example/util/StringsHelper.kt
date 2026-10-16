package com.example.util

object StringsHelper {

    fun get(key: String, lang: String): String {
        val isCs = lang.lowercase().startsWith("cs")
        return when (key) {
            "app_title" -> "HTMLPack"
            "app_subtitle" -> if (isCs) "Runtime a správce HTML aplikací" else "HTML Apps Runtime & Manager"
            "welcome_title" -> if (isCs) "Vítejte v HTMLPacku" else "Welcome to HTMLPack"
            "welcome_desc" -> if (isCs)
                "Osobní runtime pro vaše HTML, CSS a JS projekty. Žádné generování APK – aplikace běží v bezpečném sandboxu přímo zde, s vlastním nastavením, ikonou i zástupcem na ploše."
            else
                "Personal runtime for your HTML, CSS and JS projects. No APK build required – run in a secure sandbox with custom window settings, icons and home screen shortcuts."
            "create_first_app" -> if (isCs) "Vytvořit první aplikaci" else "Create First App"
            "import_package" -> if (isCs) "Importovat .htapk" else "Import .htapk"
            "load_sample_template" -> if (isCs) "Vyzkoušet vzorovou šablonu" else "Try Sample Template"
            "search_placeholder" -> if (isCs) "Hledat aplikace podle názvu nebo tagu…" else "Search apps by name or tag…"
            "new_app_btn" -> if (isCs) "Nová aplikace" else "New App"
            "no_apps_found" -> if (isCs) "Žádné aplikace nenalezeny" else "No apps found"
            "filter_all" -> if (isCs) "Všechny" else "All"
            "filter_fullscreen" -> if (isCs) "Fullscreen" else "Fullscreen"
            "filter_offline" -> if (isCs) "Offline" else "Offline"
            "filter_pin" -> if (isCs) "Se zámkem" else "PIN Locked"
            "menu_run" -> if (isCs) "Spustit aplikaci" else "Run App"
            "menu_edit" -> if (isCs) "Upravit nastavení" else "Edit Settings"
            "menu_shortcut" -> if (isCs) "Přidat zástupce na plochu" else "Add Home Shortcut"
            "menu_export" -> if (isCs) "Exportovat (.htapk)" else "Export (.htapk)"
            "menu_duplicate" -> if (isCs) "Duplikovat projekt" else "Duplicate Project"
            "menu_delete" -> if (isCs) "Smazat projekt" else "Delete Project"
            "confirm_delete" -> if (isCs) "Opravdu chcete smazat tuto aplikaci? Tuto akci nelze vzít zpět." else "Are you sure you want to delete this app? This cannot be undone."
            "cancel" -> if (isCs) "Zrušit" else "Cancel"
            "delete" -> if (isCs) "Smazat" else "Delete"
            "save" -> if (isCs) "Uložit" else "Save"
            "next" -> if (isCs) "Další" else "Next"
            "back" -> if (isCs) "Zpět" else "Back"
            "finish" -> if (isCs) "Dokončit" else "Finish"
            "run_now" -> if (isCs) "Uložit a spustit" else "Save & Run"
            "save_and_shortcut" -> if (isCs) "Uložit a přidat na plochu" else "Save & Pin Shortcut"
            "step_source" -> if (isCs) "Zdroj" else "Source"
            "step_identity" -> if (isCs) "Identita" else "Identity"
            "step_window" -> if (isCs) "Okno" else "Window"
            "step_behavior" -> if (isCs) "Chování" else "Behavior"
            "step_summary" -> if (isCs) "Souhrn" else "Summary"
            "tab_basic" -> if (isCs) "Základní" else "Basic"
            "tab_window" -> if (isCs) "Okno" else "Window"
            "tab_webview" -> if (isCs) "WebView" else "WebView"
            "tab_behavior" -> if (isCs) "Chování" else "Behavior"
            "tab_security" -> if (isCs) "Zabezpečení" else "Security"
            "tab_files" -> if (isCs) "Soubory" else "Files"
            "files_title" -> if (isCs) "Správce souborů" else "File Manager"
            "files_empty" -> if (isCs) "Žádné další soubory" else "No additional files"
            "view_file" -> if (isCs) "Prohlédnout obsah" else "View Content"
            "add_file" -> if (isCs) "Přidat soubor" else "Add File"
            "settings_title" -> if (isCs) "Nastavení" else "Settings"
            "settings_language" -> if (isCs) "Jazyk aplikace" else "App Language"
            "settings_theme" -> if (isCs) "Barevný režim" else "Theme Mode"
            "settings_storage" -> if (isCs) "Úložiště aplikací" else "App Storage"
            "settings_about" -> if (isCs) "O aplikaci" else "About HTMLPack"
            "about_version" -> if (isCs) "Verze 1.0 (Runtime & Manager)" else "Version 1.0 (Runtime & Manager)"
            "about_desc" -> if (isCs)
                "HTMLPack je samostatný runtime pro webové aplikace s podporou .htapk archivů a zástupců na domovské obrazovce."
            else
                "HTMLPack is a standalone runtime for web apps with .htapk archive support and home screen shortcuts."
            "pin_prompt" -> if (isCs) "Zadejte PIN pro odemčení aplikace" else "Enter PIN to unlock app"
            "pin_incorrect" -> if (isCs) "Nesprávný PIN kód" else "Incorrect PIN code"
            "unlock" -> if (isCs) "Odemknout" else "Unlock"
            "shortcut_added" -> if (isCs) "Zástupce byl přidán na plochu" else "Shortcut added to home screen"
            "export_success" -> if (isCs) "Balíček .htapk byl úspěšně exportován" else ".htapk package exported successfully"
            "import_success" -> if (isCs) "Aplikace byla úspěšně importována" else "App imported successfully"
            "error_occurred" -> if (isCs) "Došlo k chybě" else "An error occurred"
            else -> key
        }
    }
}
