package com.korbi.todolist.ui;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.korbi.todolist.logic.Task;
import com.korbi.todolist.database.TaskDbHelper;
import com.korbi.todolist.logic.ToDoListRecyclerAdapter;
import com.korbi.todolist.widget.ToDoListWidget;
import com.korbi.todolist.todolist.R;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener
//TODO create own Icon
// TODO maybe change widget to just textviews in LinearLayout instead of listview
{
    public static TaskDbHelper db;
    public static List<Task> taskItems;
    public static List<Task> undoTaskItems;
    public  ToDoListRecyclerAdapter adapter;
    public RecyclerView rv;
    private TextView emptylist;
    private DrawerLayout drawer;

    public static SharedPreferences settings;
    public Snackbar undoDeleteSnack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        settings = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        db = new TaskDbHelper(this);

        taskItems = db.getAllTasks();

        emptylist = (TextView) findViewById(R.id.emptylistMessage);

        checkIfToShowEmptyListView();

        undoTaskItems = new ArrayList<>();
        adapter = new ToDoListRecyclerAdapter(taskItems, getApplicationContext());
        adapter.sort();

        rv = (RecyclerView) findViewById(R.id.TaskRecyclerView);
        rv.setAdapter(adapter);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setHasFixedSize(true);
        rv.setItemAnimator(new ToDoListAnimator());

        setUpItemSwipe();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent OpenAddTask = new Intent(MainActivity.this, AddEditTask.class);
                startActivity(OpenAddTask);
            }
        });


        undoDeleteSnack = Snackbar
                .make(findViewById(R.id.coordinatorlayout), "", Snackbar.LENGTH_LONG)
                .setAction(R.string.undo_delete, new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        Toast.makeText(getApplicationContext(), getString(R.string.undo_pressed), Toast.LENGTH_LONG).show();
                        for (Task t : undoTaskItems)
                        {
                            taskItems.add(t);
                        }
                        adapter.sort();
                        adapter.notifyItemInserted(0);//fixes strange
                        adapter.notifyItemRangeChanged(0, taskItems.size());
                        undoTaskItems.clear();
                    }
                });
        undoDeleteSnack.setActionTextColor(Color.YELLOW);
        undoDeleteSnack.addCallback(new Snackbar.Callback()
        {

            @Override
            public void onDismissed(Snackbar snackbar, int event)
            {
                for (Task t : undoTaskItems)
                {
                    db.deleteTask(t.getId());
                    updateWidget();
                    checkIfToShowEmptyListView();
                }
                undoTaskItems.clear();
            }
        });

        if (settings.getBoolean(Settings.INCLUDE_TIME_SETTING, false))
        {
            for (Task t : taskItems)
            {
                if (t.getTimeIsSet() == Task.DATE_AND_TIME) t.setTimeIsSet(Task.JUST_DATE);
            }
        }

        updateWidget();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if(taskItems.size() == 0) emptylist.setVisibility(View.VISIBLE);
        else emptylist.setVisibility(View.GONE);

        if (settings.getBoolean(Settings.INCLUDE_TIME_SETTING, false))
        {
            for (Task t : taskItems)
            {
                if (t.getTimeIsSet() == Task.DATE_AND_TIME) t.setTimeIsSet(Task.JUST_DATE);
            }
        }

        /* Normally it should be enough to call sort() and notifydatasetchanged() but then tasks with no deadline get sometimes shown above ones with deadline
         * I don't understand why, but reloading the tasks from the db and resetting the adapter solves this*/

        taskItems = db.getAllTasks();
        adapter = new ToDoListRecyclerAdapter(taskItems, getApplicationContext());
        adapter.sort();

        rv.setAdapter(adapter);
        updateWidget();
    }

    @Override
    public void onStop()
    {
        updateWidget();
        super.onStop();
        if(taskItems.size() == 0) emptylist.setVisibility(View.VISIBLE);
        else emptylist.setVisibility(View.GONE);
    }

    @Override
    public void onPause()
    {
        updateWidget();
        super.onPause();
        if(taskItems.size() == 0) emptylist.setVisibility(View.VISIBLE);
        else emptylist.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_completed_tasks:

                createConfirmDialogue();

                return true;

            case R.id.about:

                Intent openAboutApp = new Intent(MainActivity.this, AboutTheApp.class);
                startActivity(openAboutApp);

                return true;

            case R.id.settings:

                Intent openSettings = new Intent(MainActivity.this, Settings.class);
                startActivity(openSettings);

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void createConfirmDialogue() //creates confirm Dialog for clearing completed tasks
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder
                .setMessage(getString(R.string.confirm_message))
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        int size = taskItems.size();
                        adapter.clear();
                        adapter.notifyItemRangeRemoved(0, size);
                        checkIfToShowEmptyListView();
                    }
                })
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                }).show();
    }

    private void setUpItemSwipe()
    {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.
                SimpleCallback(0, ItemTouchHelper.LEFT) {

            Drawable background;
            Drawable deleteIcon;
            int deleteIconMargin;
            boolean initiated;

            private void init()
            {
                background = new ColorDrawable(Color.RED);
                deleteIcon = ContextCompat.getDrawable(getApplicationContext(),
                        R.drawable.ic_delete_sweep_white_24px);
                deleteIconMargin = (int) getResources().getDimension(R.dimen.ic_delete_margin);
                initiated = true;
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) //Only for Drag and Drop
            {
                return false;
            }

            @Override
            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder)
            {
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir)
            {
                Task task = taskItems.get(viewHolder.getAdapterPosition());
                ToDoListRecyclerAdapter adapter = (ToDoListRecyclerAdapter) rv.getAdapter();
                taskItems.remove(task);
                undoTaskItems.add(task);
                adapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                undoDeleteSnack.show();
                setDeleteSnackbarText();



            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView,
                                    RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive)
            {
                View itemView = viewHolder.itemView;

                if (viewHolder.getAdapterPosition() == -1) return;

                if (!initiated) init();

                background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(),
                        itemView.getRight(), itemView.getBottom());
                background.draw(c);

                int itemHeight = itemView.getBottom() - itemView.getTop();
                int intrinsicWidth = deleteIcon.getIntrinsicWidth();
                int intrinsicHeight = deleteIcon.getIntrinsicHeight();

                int deleteIconLeft = itemView.getRight() - deleteIconMargin - intrinsicWidth;
                int deleteIconRight = itemView.getRight() - deleteIconMargin;
                int deleteIconTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
                int deleteIconBottom = deleteIconTop + intrinsicHeight;
                deleteIcon.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom);

                deleteIcon.draw(c);

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        mItemTouchHelper.attachToRecyclerView(rv);
    }

    public void updateWidget()
    {
        Intent intent = new Intent(this, ToDoListWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int ids[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), ToDoListWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,ids);
        sendBroadcast(intent);
    }

    private void setDeleteSnackbarText()
    {
        if (undoTaskItems.size() == 1)
        {
            undoDeleteSnack.setText(R.string.one_task_deleted);
        }
        else
        {
            undoDeleteSnack.setText(String.format(getString(R.string.multiple_tasks_deleted),
                                                            undoTaskItems.size()));
        }
    }

    private void checkIfToShowEmptyListView()
    {
        if(taskItems.size() == 0) emptylist.setVisibility(View.VISIBLE);
        else emptylist.setVisibility(View.GONE);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();


        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}
