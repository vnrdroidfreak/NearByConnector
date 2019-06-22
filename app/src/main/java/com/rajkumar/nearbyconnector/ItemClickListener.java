package com.rajkumar.nearbyconnector;

import android.support.annotation.NonNull;
import android.view.View;

public interface ItemClickListener<Item> {
    public void onItemClick(final View view,@NonNull Item item, int position, Object...extras);
}
