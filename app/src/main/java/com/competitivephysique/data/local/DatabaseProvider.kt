package com.competitivephysique.data.local

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var INSTANCE: CompetitivePhysiqueDatabase? = null

    fun get(context: Context): CompetitivePhysiqueDatabase =
        INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                CompetitivePhysiqueDatabase::class.java,
                "competitive_physique.db"
            ).build().also { INSTANCE = it }
        }
}
