package net.weavemc.weave.api.mapping;

import lombok.Setter;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MappingHelper {
    @Setter
    private IMapper mapper;

    public String mapClass(String className) {
        return mapper.mapClass(className);
    }

    public String mapMethod(String className, String methodName) {
        return mapper.mapMethod(className, methodName);
    }

    public String mapField(String className, String fieldName) {
        return mapper.mapField(className, fieldName);
    }

    public String reverseMapClass(String className) {
        return mapper.reverseMapClass(className);
    }

    public String reverseMapMethod(String className, String methodName) {
        return mapper.reverseMapMethod(className, methodName);
    }

    public String reverseMapField(String className, String fieldName) {
        return mapper.reverseMapField(className, fieldName);
    }
}
