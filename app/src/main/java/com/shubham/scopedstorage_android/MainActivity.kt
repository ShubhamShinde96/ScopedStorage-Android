package com.shubham.scopedstorage_android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.shubham.scopedstorage_android.adapter.ViewPager2Adapter
import com.shubham.scopedstorage_android.databinding.ActivityMainBinding
import com.shubham.scopedstorage_android.fragments.InternalStorageFragment
import com.shubham.scopedstorage_android.fragments.SharedStorageFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var viewPagerAdapter: ViewPager2Adapter

    private val tabNameList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.viewPager2.orientation = ViewPager2.ORIENTATION_HORIZONTAL

        viewPagerAdapter = ViewPager2Adapter(supportFragmentManager, lifecycle)

        viewPagerAdapter.addFragment(SharedStorageFragment())
        viewPagerAdapter.addFragment(InternalStorageFragment())

        tabNameList.add("Shared Storage")
        tabNameList.add("Internal Storage")

        binding.viewPager2.adapter = viewPagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
            tab.text = tabNameList[position]
        }.attach()

    }

}



































