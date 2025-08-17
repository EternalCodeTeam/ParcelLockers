package com.eternalcode.parcellockers.database.persister;

import com.eternalcode.parcellockers.shared.ParcelLockersException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.BaseDataType;
import com.j256.ormlite.support.DatabaseResults;
import de.eldoria.jacksonbukkit.JacksonPaper;
import io.sentry.Sentry;
import java.sql.SQLException;
import java.util.List;
import org.bukkit.inventory.ItemStack;

@SuppressWarnings("unchecked")
public class ItemStackPersister extends BaseDataType {

    private static final ItemStackPersister instance = new ItemStackPersister();
    private static final ObjectMapper JSON = JsonMapper.builder()
        .addModule(JacksonPaper.builder()
            .useLegacyItemStackSerialization()
            .build()
        )
        .build();

    private ItemStackPersister() {
        super(SqlType.LONG_STRING, new Class<?>[] { ItemStackPersister.class });
    }

    public static ItemStackPersister getSingleton() {
        return instance;
    }

    @Override
    public Object javaToSqlArg(FieldType fieldType, Object javaObject) {
        List<ItemStack> stacks = (List<ItemStack>) javaObject;

        try {
            return JSON.writeValueAsString(stacks);
        } catch (JsonProcessingException e) {
            Sentry.captureException(e);
            throw new ParcelLockersException("Failed to serialize itemstacks", e);
        }
    }

    @Override
    public Object parseDefaultString(FieldType fieldType, String defaultString) {
        return defaultString;
    }

    @Override
    public Object resultToSqlArg(FieldType fieldType, DatabaseResults results, int columnPos) throws SQLException {
        return results.getString(columnPos);
    }

    @Override
    public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {
        String string = (String) sqlArg;

        try {
            return JSON.readValue(string, JSON.getTypeFactory().constructCollectionType(List.class, ItemStack.class));
        } catch (JsonProcessingException exception) {
            Sentry.captureException(exception);
            throw new ParcelLockersException("Failed to deserialize itemstacks", exception);
        }
    }
}
