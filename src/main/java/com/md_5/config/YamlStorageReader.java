package com.md_5.config;

import java.io.Reader;
import org.bukkit.plugin.Plugin;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

public class YamlStorageReader<T extends AnnotatedConfig> {

    private Reader reader;

    public YamlStorageReader(Reader reader, Plugin plugin) {
        this.reader = reader;
    }

    public T load(Class<T> clazz) throws InstantiationException, IllegalAccessException {
        Yaml yaml = new Yaml(new CustomClassLoaderConstructor(clazz.getClassLoader()));
        T object = yaml.loadAs(reader, clazz);
        if (object == null) {
            object = clazz.newInstance();
        }
        return object;
    }
}
