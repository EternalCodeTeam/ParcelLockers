package com.eternalcode.parcellockers.database.persister;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.BaseDataType;
import com.j256.ormlite.support.DatabaseResults;

import java.sql.SQLException;
import java.time.Instant;
import java.time.format.DateTimeParseException;

public class InstantPersister extends BaseDataType {

    private static final InstantPersister INSTANCE = new InstantPersister();

    private InstantPersister() {
        super(SqlType.LONG_STRING, new Class<?>[] { Instant.class });
    }

    public static InstantPersister getSingleton() {
        return INSTANCE;
    }

    @Override
    public Object javaToSqlArg(FieldType fieldType, Object javaObject) {
        Instant instant = (Instant) javaObject;
        return instant.toString();
    }

    @Override
    public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {
        String value = (String) sqlArg;
        try {
            return Instant.parse(value);
        }
        catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid Instant format: " + value, e);
        }
    }

    @Override
    public Object parseDefaultString(FieldType fieldType, String defaultStr) {
        return defaultStr;
    }

    @Override
    public Object resultToSqlArg(FieldType fieldType, DatabaseResults results, int columnPos) throws SQLException {
        return results.getString(columnPos);
    }
}
