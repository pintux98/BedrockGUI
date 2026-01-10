package it.pintux.life.common.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface FormConfig {

    String getString(String path, String defaultValue);

    String getString(String path);

    List<String> getStringList(String path);

    Set<String> getKeys(String path);

    Map<String, Object> getValues(String path);

    FormConfig loadFormFile(String relativePath);
}
