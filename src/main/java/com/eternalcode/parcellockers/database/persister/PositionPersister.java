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
        super(SqlType.LONG_STRING, new Class<?>[] { PositionPersister.class });
    }

    public static PositionPersister getSingleton() {
        return instance;
    }

    @Override
    public Object javaToSqlArg(FieldType fieldType, Object javaObject) {
        Position pos = (Position) javaObject;
        String worldName = "world";

        if (!pos.isNoneWorld()) {
            worldName = pos.world();
        }

        return worldName + "/" + pos.x() + "/" + pos.y() + "/" + pos.z();
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
        String s = (String) sqlArg;
        String[] params = s.split("/");
        if (params.length != 4) {
            throw new IllegalArgumentException("Invalid position format: " + s);
        }
        try {
            return new Position(
                Integer.parseInt(params[1]),
                Integer.parseInt(params[2]),
                Integer.parseInt(params[3]),
                params[0]
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid coordinate format: " + s, e);
        }
    }

}
