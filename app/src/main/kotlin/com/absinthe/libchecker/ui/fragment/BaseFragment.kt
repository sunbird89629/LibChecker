package com.absinthe.libchecker.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.viewbinding.ViewBinding
import timber.log.Timber

abstract class BaseFragment<T : ViewBinding> : Fragment() {

    private var _binding: T? = null
    val binding get() = _binding!!

    private var parentActivityVisible = false
    private var visible = false
    private var localParentFragment: BaseFragment<T>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.d("${javaClass.simpleName} ==> onCreateView")
        _binding = initBinding(inflater)
        init()
        return binding.root
    }

    abstract fun initBinding(inflater: LayoutInflater): T
    abstract fun init()

    open fun onVisibilityChanged(visible: Boolean) {
        Timber.d("${javaClass.simpleName} ==> onVisibilityChanged = $visible")
    }

    override fun onDestroyView() {
        Timber.d("${javaClass.simpleName} ==> onDestroyView")
        _binding = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        Timber.d("${javaClass.simpleName} ==> onResume")
        onVisibilityChanged(true)
    }

    override fun onPause() {
        super.onPause()
        Timber.d("${javaClass.simpleName} ==> onPause")
        onVisibilityChanged(false)
    }

    fun getNavController(): NavController {
        return NavHostFragment.findNavController(this)
    }
}
