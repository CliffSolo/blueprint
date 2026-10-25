package com.cliffsolo.blueprint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cliffsolo.blueprint.ui.course.CourseScreen
import com.cliffsolo.blueprint.ui.course.CourseViewModel
import com.cliffsolo.blueprint.ui.lesson.LessonScreen
import com.cliffsolo.blueprint.ui.lesson.LessonViewModel
import com.cliffsolo.blueprint.ui.library.LibraryScreen
import com.cliffsolo.blueprint.ui.library.LibraryViewModel
import com.cliffsolo.blueprint.ui.settings.SettingsScreen
import com.cliffsolo.blueprint.ui.theme.BlueprintTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as BlueprintApp
        setContent { BlueprintTheme { BlueprintNav(app) } }
    }
}

@Composable
private fun BlueprintNav(app: BlueprintApp) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "library") {
        composable("library") {
            LibraryScreen(
                vm = viewModel { LibraryViewModel(app) },
                onOpen = { nav.navigate("course/$it") },
                onSettings = { nav.navigate("settings") },
            )
        }
        composable("course/{courseId}", listOf(navArgument("courseId") { type = NavType.StringType })) { entry ->
            val courseId = entry.arguments?.getString("courseId").orEmpty()
            CourseScreen(
                vm = viewModel { CourseViewModel(app, courseId) },
                onBack = { nav.popBackStack() },
                onLesson = { nav.navigate("lesson/$courseId/$it") },
            )
        }
        composable(
            "lesson/{courseId}/{lessonId}",
            listOf(navArgument("courseId") { type = NavType.StringType }, navArgument("lessonId") { type = NavType.StringType }),
        ) { entry ->
            val courseId = entry.arguments?.getString("courseId").orEmpty()
            val lessonId = entry.arguments?.getString("lessonId").orEmpty()
            LessonScreen(
                app = app,
                vm = viewModel { LessonViewModel(app, courseId, lessonId) },
                onBack = { nav.popBackStack() },
                onNextLesson = { next ->
                    nav.navigate("lesson/$courseId/$next") { popUpTo("course/$courseId") }
                },
            )
        }
        composable("settings") { SettingsScreen(app, onBack = { nav.popBackStack() }) }
    }
}
