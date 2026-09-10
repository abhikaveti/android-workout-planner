package com.competitivephysique

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.competitivephysique.ui.CompetitivePhysiqueApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompetitivePhysiqueApp()
        }
    }
}
