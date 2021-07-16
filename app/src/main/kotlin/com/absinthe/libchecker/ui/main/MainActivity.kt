package com.absinthe.libchecker.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.app.Global
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.databinding.ActivityMainBinding
import com.absinthe.libchecker.hook.HookNavigationViewItemSelectListener
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libchecker.viewmodel.HomeViewModel
import com.google.android.material.animation.AnimationUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import jonathanfinerty.once.Once
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private val appViewModel by viewModels<HomeViewModel>()
    private val navController by unsafeLazy {
        (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment)?.navController
    }
    private val navOptions by unsafeLazy {
        NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .setPopUpTo(
                navController!!.graph.startDestinationId,
                inclusive = false,
                saveState = true
            )
            .build()
    }
    private val doubleClickArray = arrayOf(0L, 0L)
    private var currentFragmentId = R.id.app_list_fragment

    override fun setViewBinding(): ViewGroup {
        binding = ActivityMainBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initView()
        handleIntentFromShortcuts(intent)
        initObserver()
        initAllApplicationInfoItems()
        clearApkCache()
        appViewModel.initRegexRules()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntentFromShortcuts(intent)
    }

    override fun onStart() {
        super.onStart()
        registerPackageBroadcast()
    }

    override fun onResume() {
        super.onResume()
        if (GlobalValues.shouldRequestChange.value == true) {
            appViewModel.requestChange(true)
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterPackageBroadcast()
    }

    fun showNavigationView() {
        binding.navView
            .animate()
            .translationY(0F)
            .setDuration(225)
            .interpolator = AnimationUtils.LINEAR_OUT_SLOW_IN_INTERPOLATOR
    }

    private fun initView() {
        setAppBar(binding.appbar, binding.toolbar)
        (binding.root as ViewGroup).bringChildToFront(binding.appbar)
        supportActionBar?.title = LCAppUtils.setTitle(this)

        navController?.let { controller ->
            binding.navView.apply {
                setupWithNavController(controller)
                hookNavigationBarView(this)
                setOnClickListener { /*Do nothing*/ }
            }
        }
    }

    private val requestPackageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            appViewModel.packageChangedLiveData.postValue(intent.data?.encodedSchemeSpecificPart)
        }
    }

    private fun registerPackageBroadcast() {
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }

        registerReceiver(requestPackageReceiver, intentFilter)
    }

    private fun unregisterPackageBroadcast() {
        unregisterReceiver(requestPackageReceiver)
    }

    private fun handleIntentFromShortcuts(intent: Intent) {
        if (navController == null) {
            return
        }
        when (intent.action) {
            Constants.ACTION_APP_LIST -> navController?.navigate(
                R.id.app_list_fragment,
                null,
                navOptions
            )
            Constants.ACTION_STATISTICS -> navController?.navigate(
                R.id.lib_reference_fragment,
                null,
                navOptions
            )
            Constants.ACTION_SNAPSHOT -> navController?.navigate(
                R.id.snapshot_fragment,
                null,
                navOptions
            )
        }
        Analytics.trackEvent(
            Constants.Event.LAUNCH_ACTION,
            EventProperties().set("Action", intent.action)
        )
    }

    private fun initAllApplicationInfoItems() {
        Global.applicationListJob = lifecycleScope.launch(Dispatchers.IO) {
            AppItemRepository.allApplicationInfoItems = appViewModel.getAppsList()
            Global.applicationListJob = null
        }.also {
            it.start()
        }
    }

    private fun initObserver() {
        appViewModel.apply {
            if (!Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.FIRST_LAUNCH)) {
                initItems()
            }

            reloadAppsFlag.observe(this@MainActivity) {
                if (it) {
                    navController?.navigate(R.id.app_list_fragment, null, navOptions)
                }
            }
        }
    }

    private fun clearApkCache() {
        FileUtils.delete(File(externalCacheDir, "temp.apk"))
    }

    private fun hookNavigationBarView(navView: BottomNavigationView) {
        NavigationBarView::class.java.getDeclaredField("selectedListener").apply {
            isAccessible = true
            val origin = get(navView) as NavigationBarView.OnItemSelectedListener
            set(navView, HookNavigationViewItemSelectListener(origin, {
                performClickNavigationItem(it)
            }))
        }
    }

    private fun performClickNavigationItem(fragmentId: Int) {
        if (currentFragmentId == fragmentId) {
            System.arraycopy(doubleClickArray, 1, doubleClickArray, 0, doubleClickArray.size - 1)
            doubleClickArray[doubleClickArray.size - 1] = System.currentTimeMillis()

            if (System.currentTimeMillis() - doubleClickArray[0] < 250L) {
                if (appViewModel.controller?.isAllowRefreshing() == true) {
                    appViewModel.controller?.onReturnTop()
                }
            }
        }
        currentFragmentId = fragmentId
    }
}
