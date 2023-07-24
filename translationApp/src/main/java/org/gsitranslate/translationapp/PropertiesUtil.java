package org.gsitranslate.translationapp;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

public class PropertiesUtil {

    HashMap<String, String> paths = new HashMap();
    HashMap<String, Properties> bundles = new HashMap();

    public void init(HashMap<String, String> bundlePaths) throws IOException {
        paths = bundlePaths;

        for (String i : paths.keySet()) {

            bundles.put(i, new Properties());
            String path = paths.get(i);
            Properties bundle = bundles.get(i);

            FileInputStream file = new FileInputStream(path);

            try {
                bundle.load(file);
            } finally {
                file.close();
            }
        }
    }

    public HashMap<String, String> findValueByKey(String key) throws IOException {
        System.out.println("PropertiesUtil.findValueByKey: Searching for " + key);

        HashMap<String, String> values = new HashMap<>();

        for (Map.Entry<String, Properties> entry : bundles.entrySet()) {
            String language = entry.getKey();
            Properties prop = entry.getValue();
            values.put(language, prop.getProperty(key));
        }

        System.out.println("PropertiesUtil.findValueByKey: Found " + values);

        return values;
    }

    public ArrayList<String> findKeysByValue(String value, String language) throws IOException {
        if (language.isEmpty()) {
            throw new IllegalArgumentException("PropertiesUtil.findKeysByValue: Language parameter cannot be empty.");
        }
        if (value.isEmpty()) {
            throw new IllegalArgumentException("PropertiesUtil.findKeysByValue: No value to search.");
        }
        Properties prop = bundles.get(language);
        ArrayList<String> keys = new ArrayList<>();

        System.out.println("PropertiesUtil.findKeysByValue: Searching for " + value + " in " + language);

        for (Map.Entry<Object, Object> entry : prop.entrySet()) {
            if (entry.getValue().toString().equalsIgnoreCase(value)) {
                keys.add(language + ": " + (String) entry.getKey());
            }
        }

        System.out.println("PropertiesUtil.findKeysByValue: Found " + keys);
        return keys;
    }

    public void write(String key, HashMap<String, String> values) throws IOException {
        for (Map.Entry<String, String> entry : paths.entrySet()) {
            String language = entry.getKey();
            String path = entry.getValue();

            FileOutputStream fileOutputStream = new FileOutputStream(path, false);
            Properties prop = bundles.get(language);
            try {
                prop.setProperty(key, values.get(language));
                prop.store(fileOutputStream, "");
            } finally {
                fileOutputStream.close();
                sort();
            }
        }
    }

    public void delete(String key) throws IOException {
        for (Map.Entry<String, String> entry : paths.entrySet()) {
            String language = entry.getKey();
            String path = entry.getValue();

            FileOutputStream fileOutputStream = new FileOutputStream(path, false);
            Properties prop = bundles.get(language);
            try {
                prop.remove(key);
                prop.store(fileOutputStream, null);
            } finally {
                fileOutputStream.close();
                sort();
            }
        }
    }

    public void sort() throws IOException {
        Map<String, String> sortedProps = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (Map.Entry<String, String> entry : paths.entrySet()) {
            String language = entry.getKey();
            String path = entry.getValue();

            Properties prop = bundles.get(language);

            // Create a sorted map to store the sorted entries
            sortedProps.putAll((Map) prop);
            final String encoding = getEncoding(path);

            // Write the sorted entries back to the properties file
            FileOutputStream fileOutputStream = new FileOutputStream(path);
            sortedProps.forEach(new BiConsumer<String, String>() {
                @Override
                public void accept(String key, String value) {
                    String line = key + "=" + value + System.lineSeparator();
                    try {
                        fileOutputStream.write(line.getBytes(encoding));
                    } catch (IOException e) {
                    }
                }
            });
            fileOutputStream.close();
        }
    }

    public String getEncoding(String filePath) throws IOException {
        byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
        CharsetDetector detector = new CharsetDetector();
        detector.setText(fileContent);
        CharsetMatch match = detector.detect();
        return match.getName();
    }
}
