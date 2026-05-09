package com.mini.location.views.sponsor

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.mini.location.views.base.BaseActivity
import com.mini.location.views.theme.locationTheme
import com.mini.location.viewmodels.SponsorViewModel

class SponsorActivity : BaseActivity() {
    private val viewModel: SponsorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            locationTheme {
                SponsorScreen(viewModel = viewModel, onBackClick = { finish() })
            }
        }
    }
}
