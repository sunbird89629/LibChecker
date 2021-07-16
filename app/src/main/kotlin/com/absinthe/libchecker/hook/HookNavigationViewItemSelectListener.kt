package com.absinthe.libchecker.hook

import android.view.MenuItem
import com.google.android.material.navigation.NavigationBarView

open class HookNavigationViewItemSelectListener(
    private val origin: NavigationBarView.OnItemSelectedListener,
    private val action: (id: Int) -> Unit
) : NavigationBarView.OnItemSelectedListener {
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        action(item.itemId)
        return origin.onNavigationItemSelected(item)
    }
}
