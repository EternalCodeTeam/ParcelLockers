package com.eternalcode.parcellockers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.eternalcode.parcellockers.locker.LockerService;
import com.eternalcode.parcellockers.parcel.Parcel;
import com.eternalcode.parcellockers.parcel.service.ParcelService;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;

class ApiSurfaceTest {

    @Test
    void exposesOnlySupportedParcelOperations() {
        Set<String> methods = methodNames(ParcelService.class);

        assertEquals(Set.of(
            "send", "update", "updateIfStatus", "collect", "get",
            "getBySender", "getByReceiver", "getCollectible", "getReturnable",
            "getAll", "delete"
        ), methods);
        assertFalse(methods.contains("rollbackSend"));
        assertFalse(methods.contains("invalidate"));
        assertFalse(methods.contains("deleteAll"));
    }

    @Test
    void exposesOnlySupportedLockerOperations() {
        assertEquals(Set.of("get", "create", "delete", "rename", "isLockerFull"),
            methodNames(LockerService.class));
    }

    @Test
    void parcelDescriptionIsExplicitlyNullable() {
        RecordComponent description = Arrays.stream(Parcel.class.getRecordComponents())
            .filter(component -> component.getName().equals("description"))
            .findFirst()
            .orElseThrow();

        assertNotNull(description.getAnnotatedType().getAnnotation(Nullable.class));
        assertNotNull(description.getAccessor().getAnnotatedReturnType().getAnnotation(Nullable.class));
    }

    private static Set<String> methodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toSet());
    }
}
