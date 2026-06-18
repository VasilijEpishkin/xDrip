package com.eveningoutpost.dexdrip;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Modern Material list row for the navigation drawer: leading icon + label,
 * with a rounded selected-state pill (handled via View.setActivated by the
 * hosting ListView's choice mode) — replaces the legacy plain-text
 * simple_list_item_activated_1 rows without altering navigation logic.
 */
public class NavDrawerAdapter extends BaseAdapter {

    private final Context context;
    private final List<String> labels;
    private final List<Integer> icons;

    public NavDrawerAdapter(final Context context, final List<String> labels, final List<Integer> icons) {
        this.context = context;
        this.labels = labels;
        this.icons = icons;
    }

    @Override
    public int getCount() {
        return labels.size();
    }

    @Override
    public String getItem(int position) {
        return labels.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final View row;
        if (convertView != null) {
            row = convertView;
        } else {
            row = LayoutInflater.from(context).inflate(R.layout.item_nav_drawer, parent, false);
        }

        final ImageView icon = row.findViewById(R.id.nav_item_icon);
        final TextView text = row.findViewById(R.id.nav_item_text);

        text.setText(labels.get(position));
        final int iconRes = (icons != null && position < icons.size()) ? icons.get(position) : R.drawable.ic_nav_home;
        icon.setImageResource(iconRes);

        return row;
    }
}
