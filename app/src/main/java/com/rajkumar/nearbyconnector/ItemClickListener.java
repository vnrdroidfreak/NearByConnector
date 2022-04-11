package com.rajkumar.nearbyconnector;

import android.view.View;
import androidx.annotation.NonNull;

public interface ItemClickListener<Item> {
    public void onItemClick(final View view, @NonNull Item item, int position, Object...extras);
}
