package app.aialertpay.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class TeamActivity extends Activity {
    private SharedPreferences store;
    private String owner;
    private ArrayAdapter<String> adapter;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        owner = Receipts.owner();
        if (owner == null) { finish(); return; }
        setContentView(R.layout.team);
        store = getSharedPreferences("team_members", MODE_PRIVATE);
        ListView list = findViewById(R.id.members);
        list.setEmptyView(findViewById(R.id.team_empty));
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        list.setAdapter(adapter);
        refresh();
        findViewById(R.id.add_member).setOnClickListener(v -> addMember());
        findViewById(R.id.open_collection).setOnClickListener(v -> finish());
        findViewById(R.id.open_qr).setOnClickListener(v -> {
            startActivity(new Intent(this, PaymentQrActivity.class));
            finish();
        });
    }

    private void refresh() {
        ArrayList<String> names = new ArrayList<>(store.getStringSet(owner, Collections.emptySet()));
        names.sort(String.CASE_INSENSITIVE_ORDER);
        adapter.clear();
        adapter.addAll(names);
        ((android.widget.TextView)findViewById(R.id.member_count)).setText(String.valueOf(names.size()));
    }

    private void addMember() {
        EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setHint("Team member name");
        name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)});
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Add team member")
                .setMessage("Save a name in this phone’s shop team list.")
                .setView(name).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create();
        dialog.setOnShowListener(shown -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String member = name.getText().toString().trim();
            if (member.isEmpty()) { name.setError("Enter a name"); return; }
            Set<String> names = new HashSet<>(store.getStringSet(owner, Collections.emptySet()));
            for (String existing : names) {
                if (existing.equalsIgnoreCase(member)) { name.setError("Use a distinct name for this member"); return; }
            }
            names.add(member);
            store.edit().putStringSet(owner, names).apply();
            refresh();
            dialog.dismiss();
        }));
        dialog.show();
    }
}