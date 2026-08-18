package chspif;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

public class ChspifTranslations
{
    public static Map<String, String> getTranslations(String lang)
    {
        InputStream stream = ChspifTranslations.class.getClassLoader()
                .getResourceAsStream("assets/carpet-chspif/lang/" + lang + ".json");
        if (stream == null)
        {
            return Collections.emptyMap();
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
        {
            Gson gson = new GsonBuilder().setLenient().create();
            Map<String, String> map = gson.fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
            return map != null ? map : Collections.emptyMap();
        }
        catch (Exception e)
        {
            return Collections.emptyMap();
        }
    }
}
