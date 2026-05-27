/*
 * Copyright 2026 Nil MALHOMME (malhomme.nil+oss@icloud.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.ktestify.manager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Generic named/aliased object registry used by step definitions to store and retrieve objects (topics, schemas,
 * queues, …) by their name or alias.
 *
 * <p>Objects can be registered under their canonical name and/or an alias. Look-up checks the alias first, then falls
 * back to the canonical name, so both keys always resolve to the same object.
 *
 * @param <T> the type of object managed by this registry
 */
public class ObjectManager<T> {

    private final Map<String, T> registry = new LinkedHashMap<>();

    /**
     * Registers {@code object} under {@code name}.
     *
     * @param name the canonical name or alias
     * @param object the object to store
     */
    public void register(String name, T object) {
        registry.put(name, object);
    }

    /**
     * Registers {@code object} under both its canonical {@code name} and its {@code alias}. Either key can later be
     * used to retrieve it.
     *
     * @param name the canonical name
     * @param alias the alias (may be {@code null} or blank — ignored if so)
     * @param object the object to store
     */
    public void register(String name, String alias, T object) {
        registry.put(name, object);
        if (alias != null && !alias.isBlank()) {
            registry.put(alias, object);
        }
    }

    /**
     * Retrieves the object registered under {@code key}.
     *
     * @param key canonical name or alias
     * @return an {@link Optional} containing the object, or empty if not found
     */
    public Optional<T> get(String key) {
        return Optional.ofNullable(registry.get(key));
    }

    /**
     * Retrieves the object registered under {@code key}, throwing {@link IllegalStateException} if it is not found.
     *
     * @param key canonical name or alias
     * @return the registered object
     * @throws IllegalStateException if no object is registered under {@code key}
     */
    public T getOrThrow(String key) {
        return get(key).orElseThrow(() -> new IllegalStateException("No object registered under key: '" + key + "'"));
    }

    /** Returns {@code true} if an object is registered under {@code key}. */
    public boolean contains(String key) {
        return registry.containsKey(key);
    }

    /** Removes all registered objects. */
    public void clear() {
        registry.clear();
    }
}
