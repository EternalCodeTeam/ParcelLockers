package com.eternalcode.parcellockers.database.persister;

import com.eternalcode.parcellockers.shared.Position;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.BaseDataType;
import com.j256.ormlite.support.DatabaseResults;
import java.sql.SQLException;

public class PositionPersister extends BaseDataType {

    private static final PositionPersister INSTANCE = new PositionPersister();

    private PositionPersister() {
        super(SqlType.LONG_STRING, new Class<?>[] { Position.class });
    }

    public static PositionPersister getSingleton() {
        return INSTANCE;
    }

    @Override
    public Object javaToSqlArg(FieldType fieldType, Object javaObject) {
        Position pos = (Position) javaObject;
        return pos.toString();
    }


    @Override
    public Object resultToSqlArg(FieldType fieldType, DatabaseResults results, int columnPos) throws SQLException {
        return results.getString(columnPos);
    }

    @Override
    public Object parseDefaultString(FieldType fieldType, String defaultStr) {
        return defaultStr;
    }

    @Override
    public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {
        String string = (String) sqlArg;
        return Position.parse(string);
    }

}
