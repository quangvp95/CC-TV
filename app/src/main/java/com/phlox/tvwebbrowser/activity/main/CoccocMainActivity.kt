package com.phlox.tvwebbrowser.activity.main

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.example.coccocbrowsejavatest.R

/**
 * Loads [CoccocMainFragment].
 */
class CoccocMainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coccoc_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_browse_fragment, CoccocMainFragment())
                .commitNow()
        }
    }
}