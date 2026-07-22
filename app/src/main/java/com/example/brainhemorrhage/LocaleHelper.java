package com.example.brainhemorrhage;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

import java.util.Locale;

/**
 * Utility class for applying locale/language changes at runtime.
 * Call {@link #applyLocale(Context)} in Application.attachBaseContext()
 * and in Activity.attachBaseContext().
 */
public class LocaleHelper {

    private static final String PREFS_NAME = "NuerocheckPrefs";
    private static final String KEY_LANGUAGE = "language";

    /**
     * Map from the display name stored in SharedPreferences
     * to an IETF BCP-47 / ISO-639 language tag.
     */
    public static String getLanguageTag(String displayName) {
        switch (displayName) {
            case "Spanish (Español)": return "es";
            case "Hindi (हिन्दी)":    return "hi";
            case "Bengali (বাংলা)":   return "bn";
            case "Tamil (தமிழ்)":     return "ta";
            case "Telugu (తెలుగు)":   return "te";
            case "Marathi (मराठी)":   return "mr";
            case "Kannada (ಕನ್ನಡ)":   return "kn";
            case "Gujarati (ગુજરાતી)": return "gu";
            default:                   return "en";
        }
    }

    /**
     * Reads saved language from SharedPreferences and returns a
     * Context with that locale applied.
     */
    public static Context applyLocale(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String displayName = prefs.getString(KEY_LANGUAGE, "English");
        String tag = getLanguageTag(displayName);
        return setLocale(context, tag);
    }

    /** Apply a locale tag to a Context and return the wrapped context. */
    public static Context setLocale(Context context, String languageTag) {
        Locale locale = Locale.forLanguageTag(languageTag);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.createConfigurationContext(config);
        } else {
            context.getResources().updateConfiguration(config,
                    context.getResources().getDisplayMetrics());
            return context;
        }
    }
}
