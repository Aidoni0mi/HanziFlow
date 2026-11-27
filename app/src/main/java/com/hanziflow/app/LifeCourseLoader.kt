package com.hanziflow.app

import android.content.Context
import com.hanziflow.core.LifeSituation
import com.hanziflow.core.parseLifeCourse

/** Loads the bundled original life-course content. Call from a background dispatcher. */
fun readLifeCourse(context: Context): List<LifeSituation> =
    context.assets.open("life_course.tsv").bufferedReader(Charsets.UTF_8).use { reader ->
        parseLifeCourse(reader.readText())
    }
