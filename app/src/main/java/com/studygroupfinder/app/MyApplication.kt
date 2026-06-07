package com.studygroupfinder.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Study Group Finder.
 *
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation,
 * including a base class for the application that serves as the
 * application-level dependency container.
 */
@HiltAndroidApp
class MyApplication : Application()
