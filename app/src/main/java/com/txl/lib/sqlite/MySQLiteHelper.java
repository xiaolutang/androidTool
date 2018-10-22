package com.txl.lib.sqlite;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Copyright (c) 2018, 唐小陆 All rights reserved.
 * author：txl
 * date：2018/10/17
 * description：
 */
public class MySQLiteHelper extends SQLiteOpenHelper {
    public static final String DATABESE_NAME = "todo.db";
    public static int DB_VERSION = 1;
    public static String table_todo = "table_todo";

    public MySQLiteHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super( context, name, factory, version );
    }

    public MySQLiteHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version, DatabaseErrorHandler errorHandler) {
        super( context, name, factory, version, errorHandler );
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "create table if not exists " + table_todo + " (id integer primary key autoincrement, completeDate INTEGER, completeDateStr TEXT, content TEXT," +
                "date INTEGER, dateStr TEXT, status INTEGER, title TEXT, type INTEGER, userId INTEGER)";
        db.execSQL( sql );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(newVersion == 2 && oldVersion == 1){
            String sql = "alter table  " + table_todo + " add column test_update varchar";
            db.execSQL( sql );
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        super.onDowngrade( db, oldVersion, newVersion );
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure( db );
    }
}