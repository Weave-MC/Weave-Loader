package net.weavemc.weave.api.mapping;

import org.jetbrains.annotations.Nullable;

/**
 * A mapper is used to map classes, methods and fields to their obfuscated counterparts.
 * The owner and name should be in MCP format.
 */
public interface IMapper {
    @Nullable
    default String mapUniversal(@Nullable String name) {
        String mapped = null;

        mapped = mapClass(name);
        if (mapped != null) {
            return mapped;
        }

        mapped = mapMethod(null, name);
        if (mapped != null) {
            return mapped;
        }

        mapped = mapField(null, name);

        return mapped;
    }

    @Nullable
    String mapClass(@Nullable String name);

    @Nullable
    String mapMethod(@Nullable String owner, @Nullable String name);

    @Nullable
    String mapField(@Nullable String owner, @Nullable String name);

    @Nullable
    default String reverseMapUniversal(@Nullable String name) {
        String mapped = null;

        mapped = reverseMapClass(name);
        if (mapped != null) {
            return mapped;
        }

        mapped = reverseMapMethod(null, name);
        if (mapped != null) {
            return mapped;
        }

        mapped = reverseMapField(null, name);

        return mapped;
    }

    @Nullable
    String reverseMapClass(@Nullable String name);

    @Nullable
    String reverseMapMethod(@Nullable String owner, @Nullable String name);

    @Nullable
    String reverseMapField(@Nullable String owner, @Nullable String name);
}
