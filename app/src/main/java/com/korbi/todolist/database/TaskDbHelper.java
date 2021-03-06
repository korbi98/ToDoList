/*
 * Copyright 2019 Korbinian Moser
 *
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.korbi.todolist.database;

/**
 * Created by korbi on 13.03.17.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.korbi.todolist.logic.Task;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TaskDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "TaskDataBase.db";
    private static final int DB_VERSION = 2;

    private static final String TASK_TABLE = "tasks";
    private static final String COL_ID = "_id";
    private static final String COL_TASK = "title";
    private static final String COL_DONE = "done";
    private static final String COL_DEADLINE = "deadline";
    private static final String COL_PRIORITY = "priority";
    private static final String COL_TIME_IS_SET = "time_is_set";

    private static final String CATEGORY_TABLE = "categories";
    private static final String COL_CATEGORY = "category";

    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public TaskDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTaskTable = "CREATE TABLE " + TASK_TABLE + " ( " + COL_ID + " INTEGER PRIMARY KEY, "
                + COL_TASK + " TEXT, " + COL_DEADLINE + " TEXT, " + COL_PRIORITY + " INTEGER, "
                + COL_DONE + " INTEGER NOT NULL, " + COL_TIME_IS_SET + " INTEGER NOT NULL)";

        db.execSQL(createTaskTable);

        db.execSQL("ALTER TABLE " + TASK_TABLE + " ADD COLUMN " + COL_CATEGORY + " INTEGER NOT NULL DEFAULT 1");

        String createCategoryTable = "CREATE TABLE " + CATEGORY_TABLE + " ( " + COL_ID + " INTEGER " +
                "PRIMARY KEY, " + COL_CATEGORY + " STRING)";

        db.execSQL(createCategoryTable);

        ContentValues values = new ContentValues();
        values.put(COL_CATEGORY, "MyTasks");
        db.insert(CATEGORY_TABLE, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) //This method ensures that there are no compatibility problems if you update from an older version of the app
    {
        switch (newVersion) {
            case 2:
                db.execSQL("ALTER TABLE " + TASK_TABLE + " ADD COLUMN " + COL_CATEGORY + " INTEGER NOT NULL DEFAULT 1");

                String createCategoryTable = "CREATE TABLE " + CATEGORY_TABLE + " ( " + COL_ID + " INTEGER " +
                        "PRIMARY KEY, " + COL_CATEGORY + " STRING)";

                db.execSQL(createCategoryTable);

                ContentValues values = new ContentValues();
                values.put(COL_CATEGORY, "MyTasks");
                db.insert(CATEGORY_TABLE, null, values);
                break;
        }
    }

    public void addTask(Task task) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_TASK, task.getTaskname());
        values.put(COL_DONE, task.getState());
        values.put(COL_DEADLINE, dateFormatter.format(task.getDeadline()));
        values.put(COL_PRIORITY, task.getPriority());
        values.put(COL_TIME_IS_SET, task.getTimeIsSet());
        values.put(COL_CATEGORY, getCategoryId(task.getCategory()));

        db.insert(TASK_TABLE, null, values);
        db.close();
    }

    public List<Task> getUncompletedTasksByCategory(String category) {
        List<Task> taskList = new ArrayList<>();

        String selectQuery = "SELECT  * FROM " + TASK_TABLE + " WHERE " + COL_DONE + " != 1 and "
                + COL_CATEGORY + " = " + getCategoryId(category);

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Task task = new Task(cursor.getInt(0), cursor.getString(1),
                        parseDate(cursor.getString(2)), cursor.getInt(3), cursor.getInt(4),
                        cursor.getInt(5), getTaskCategory(cursor.getInt(6)));

                taskList.add(task);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return taskList;
    }

    public List<Task> getTasksByCategory(String category) {
        List<Task> taskList = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + TASK_TABLE + " WHERE " + COL_CATEGORY + "=" + getCategoryId(category);

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Task task = new Task(cursor.getInt(0), cursor.getString(1),
                        parseDate(cursor.getString(2)), cursor.getInt(3), cursor.getInt(4),
                        cursor.getInt(5), getTaskCategory(cursor.getInt(6)));

                taskList.add(task);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return taskList;
    }

    public int updateTask(Task task) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_TASK, task.getTaskname());
        values.put(COL_DONE, task.getState());
        values.put(COL_PRIORITY, task.getPriority());
        values.put(COL_DEADLINE, dateFormatter.format(task.getDeadline()));
        values.put(COL_TIME_IS_SET, task.getTimeIsSet());
        values.put(COL_CATEGORY, getCategoryId(task.getCategory()));

        return db.update(TASK_TABLE, values, COL_ID + " =?",
                new String[]{String.valueOf(task.getId())});
    }

    public void deleteTask(Integer id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TASK_TABLE, COL_ID + " = ?",
                new String[]{String.valueOf(id)});

        db.close();
    }

    public void deleteAllTasks() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TASK_TABLE, "1", null);

        db.close();
    }

    public int getLatestID() {
        int latestID;
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.rawQuery("SELECT max(" + COL_ID + ") FROM tasks", null);
        cursor.moveToFirst();
        latestID = cursor.getInt(0);

        cursor.close();
        return latestID;
    }

    private Date parseDate(String datestring) //converts the string that stores the date in the database to a date object
    {
        Date date = null;

        try {
            date = dateFormatter.parse(datestring);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return date;
    }

    public List<String> getAllCategories() {
        List<String> catList = new ArrayList<>();

        String selectQuery = "SELECT  * FROM " + CATEGORY_TABLE;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                String category = cursor.getString(1);
                catList.add(category);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return catList;
    }

    public void addCategory(String newCategory) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_CATEGORY, newCategory);

        db.insert(CATEGORY_TABLE, null, values);
        db.close();
    }

    public void deleteCategory(String category) {
        SQLiteDatabase db = this.getWritableDatabase();

        db.delete(TASK_TABLE, COL_CATEGORY + " =? ",
                new String[]{String.valueOf(getCategoryId(category))});

        db.delete(CATEGORY_TABLE, COL_CATEGORY + " =? ",
                new String[]{category});

        db.close();
    }

    public void deleteAllCategories() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(CATEGORY_TABLE, "1", null);

        db.close();
    }

    public int updateCategory(String category, String newCatName) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_CATEGORY, newCatName);

        return db.update(CATEGORY_TABLE, values, COL_CATEGORY + " =?",
                new String[]{category});
    }

    private int getCategoryId(String category) {
        String selectQuery = "SELECT  * FROM " + CATEGORY_TABLE + " WHERE " + COL_CATEGORY +
                " = " + '"' + category + '"';

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor != null) cursor.moveToFirst();

        if (cursor.getCount() != 0) return cursor.getInt(0);
        else return 1;
    }

    public String getTaskCategory(int categoryID) {
        String selectQuery = "SELECT  * FROM " + CATEGORY_TABLE + " WHERE " + COL_ID +
                " = " + categoryID;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor != null) cursor.moveToFirst();

        return cursor.getString(1);
    }

    public String getFirstCategory() {
        String selectQuery = "SELECT  * FROM " + CATEGORY_TABLE + " ORDER BY ROWID ASC LIMIT 1";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor != null) cursor.moveToFirst();

        return cursor.getString(1);
    }

    //creates a String that separates columns with , and rows with \n so so that gets written to the export file
    public String createCSVstring() {

        String data = "";
        String selectQuery = "SELECT * FROM " + TASK_TABLE;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                data += String.valueOf(cursor.getInt(0)) + "," +
                        cursor.getString(1) + "," + cursor.getString(2) + ","
                        + String.valueOf(cursor.getInt(3)) + "," +
                        String.valueOf(cursor.getInt(4)) + "," +
                        String.valueOf(cursor.getInt(5)) + "," +
                        getTaskCategory(cursor.getInt(6)) + "\n";

            } while (cursor.moveToNext());
        }
        cursor.close();

        return data;
    }

    public void importFromCSV(List<String> importList) {

        for (String str : importList) {

            int id = -10; // If id doesn't become positive, the safefile is corrupted
            String task_name = "";
            String deadline = "";
            int priority = 10; // if priority doesn't get assigned the value 0,1,2 the safefile is corrupted
            int task_is_done = 10; // same as priority
            int time_is_set = 10; // same as priority
            String category = "";

            int currentVariable = 0;

            for (int i = 0; i < str.length(); i++) {

                if (String.valueOf(str.charAt(i)).equals(",")) {
                    currentVariable += 1;
                    i += 1; // to skip the ","
                }
                switch (currentVariable) {

                    case 0:
                        id = Character.getNumericValue(str.charAt(i));
                        break;
                    case 1:
                        task_name += str.charAt(i);
                        break;
                    case 2:
                        deadline += str.charAt(i);
                        break;
                    case 3:
                        priority = Character.getNumericValue(str.charAt(i));
                        break;
                    case 4:
                        task_is_done = Character.getNumericValue(str.charAt(i));
                        break;
                    case 5:
                        time_is_set = Character.getNumericValue(str.charAt(i));
                        break;
                    case 6:
                        category += String.valueOf(str.charAt(i));
                        break;
                }
            }

            if (id < 0 || priority < 0 || priority > 2 || time_is_set < 0 || time_is_set > 2 ||
                    task_is_done < 0 || task_is_done > 1) {
                Log.e("ImportError", "invalid file");
                break;
            }

            if (!getAllCategories().contains(category)) {
                addCategory(category);
            }


            Task importedTask = new Task(id, task_name, parseDate(deadline), priority,
                    task_is_done, time_is_set, category);

            Log.d("test", String.valueOf(getTasksByCategory(category).contains(importedTask)));

            if (!getTasksByCategory(category).contains(importedTask)) {
                importedTask.setId(getLatestID() + 1);
                addTask(importedTask);
            }
        }
    }
}
