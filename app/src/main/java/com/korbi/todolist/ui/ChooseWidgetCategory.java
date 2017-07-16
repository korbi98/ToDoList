package com.korbi.todolist.ui;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.korbi.todolist.database.TaskDbHelper;
import com.korbi.todolist.todolist.R;
import com.korbi.todolist.widget.ToDoListWidget;

import java.util.List;

public class ChooseWidgetCategory extends AppCompatActivity {

    private SharedPreferences settings;
    private int widgetToChange;
    private String currentWidgetCategory;
    private String getWidgetIdKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_widget_category);
        this.setFinishOnTouchOutside(false);

        settings = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        Bundle bundle = getIntent().getExtras();

        widgetToChange = bundle.getInt(ToDoListWidget.APP_ID);
        getWidgetIdKey = Settings.WIDGET_CATEGORY + String.valueOf(widgetToChange);

        ListView lv = (ListView) findViewById(R.id.choose_widget_category_list);

        TaskDbHelper db = new TaskDbHelper(this);
        final List<String> categories = db.getAllCategories();
        currentWidgetCategory = settings.getString(getWidgetIdKey, categories.get(0));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, categories);
        lv.setAdapter(adapter);

        lv.setItemChecked(categories.indexOf(currentWidgetCategory), true);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                currentWidgetCategory = categories.get(position);
                settings.edit().putString(getWidgetIdKey, currentWidgetCategory).apply();
                updateWidget();
                finish();
            }
        });
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        updateWidget();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        updateWidget();
    }

    public void updateWidget()
    {
        Intent intent = new Intent(this, ToDoListWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int ids[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), ToDoListWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,ids);
        sendBroadcast(intent);
    }
}