package com.ulan.timetable.Activities;

import android.content.Context;
import android.content.Intent;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.ulan.timetable.Adapters.NotesListAdapter;
import com.ulan.timetable.Model.Note;
import com.ulan.timetable.R;
import com.ulan.timetable.Utils.DbHelper;

import java.util.ArrayList;

public class NotesActivity extends AppCompatActivity {

    private Context context = this;
    private ListView listView;
    private DbHelper db;
    private NotesListAdapter adapter;
    private int listposition = 0;
    CoordinatorLayout coordinatorLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);
        coordinatorLayout = findViewById(R.id.coordinatorNotes);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        db = new DbHelper(context);
        adapter = new NotesListAdapter(NotesActivity.this, R.layout.listview_notes_adapter, db.getNote());
        listView = findViewById(R.id.notelist);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(context, NoteInfoActivity.class);
                intent.putExtra("key", adapter.getNotelist().get(position));
                startActivity(intent);
            }
        });

        initCustomDialog();

        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                final int checkedCount = listView.getCheckedItemCount();
                mode.setTitle(checkedCount + " Selected");
                listposition = position;
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater menuInflater = mode.getMenuInflater();
                menuInflater.inflate(R.menu.toolbar_action_mode, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_delete:
                        ArrayList<Note> removelist = new ArrayList<>();
                        SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
                        for (int i = 0; i < checkedItems.size(); i++) {
                            int key = checkedItems.keyAt(i);
                            if (checkedItems.get(key)) {
                                db.deleteNoteById(adapter.getItem(key));
                                removelist.add(adapter.getNotelist().get(key));
                            }
                        }
                        adapter.getNotelist().removeAll(removelist);
                        db.updateNote(adapter.getNote());
                        adapter.notifyDataSetChanged();
                        mode.finish();
                        return true;

                    case R.id.action_edit:
                        if(listView.getCheckedItemCount() == 1) {
                            LayoutInflater inflater = getLayoutInflater();
                            final View alertLayout = inflater.inflate(R.layout.dialog_add_note, null);
                            final EditText title = alertLayout.findViewById(R.id.titlenote);
                            final Note note = adapter.getNotelist().get(listposition);
                            title.setText(note.getTitle());
                            AlertDialog.Builder alert = new AlertDialog.Builder(NotesActivity.this);
                            alert.setTitle("Edit note");
                            final Button cancel = alertLayout.findViewById(R.id.cancel);
                            final Button save = alertLayout.findViewById(R.id.save);
                            alert.setView(alertLayout);
                            alert.setCancelable(false);
                            final AlertDialog dialog = alert.create();
                            dialog.show();

                            cancel.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    dialog.dismiss();
                                }
                            });

                            save.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if(TextUtils.isEmpty(title.getText())) {
                                        title.setError("Title field can not be empty!");
                                        title.requestFocus();
                                    } else {
                                        DbHelper dbHelper = new DbHelper(NotesActivity.this);
                                        note.setTitle(title.getText().toString());
                                        dbHelper.updateNote(note);
                                        adapter.notifyDataSetChanged();
                                        mode.finish();
                                        dialog.dismiss();
                                    }
                                }
                            });
                            return true;
                        } else {
                            Snackbar.make(coordinatorLayout, "Please, select one item.", Snackbar.LENGTH_LONG).show();
                            mode.finish();
                        }
                    default:
                        return false;
                }
            }
            @Override
            public void onDestroyActionMode(ActionMode mode) { }
        });
    }

    private void initCustomDialog() {
        LayoutInflater inflater = getLayoutInflater();
        final View alertLayout = inflater.inflate(R.layout.dialog_add_note, null);
        final EditText title = alertLayout.findViewById(R.id.titlenote);
        final AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle("Add note");
        final Button cancel = alertLayout.findViewById(R.id.cancel);
        final Button save = alertLayout.findViewById(R.id.save);
        alert.setView(alertLayout);
        alert.setCancelable(false);
        final AlertDialog dialog = alert.create();
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.show();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(TextUtils.isEmpty(title.getText())) {
                    title.setError("Title field can not be empty!");
                    title.requestFocus();
                } else {
                    DbHelper dbHelper = new DbHelper(context);
                    final Note note = new Note();
                    note.setTitle(title.getText().toString());
                    dbHelper.insertNote(note);
                    adapter.clear();
                    adapter.addAll(dbHelper.getNote());
                    adapter.notifyDataSetChanged();

                    title.setText("");
                    dialog.dismiss();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.clear();
        adapter.addAll(db.getNote());
        adapter.notifyDataSetChanged();
    }
}
