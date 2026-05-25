package com.vunv.n5nihongo.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

private const val STACK_MS = 320
private const val DETAIL_MS = 140
private const val TAB_MS = 240
private const val ROOT_MS = 380

private val bottomTabRoutes = listOf("home", "kanji", "leaderboard", "profile")

private val detailRoutes = setOf(
    "lessonDetail",
    "alphabet",
    "numbersTime",
    "kanjiDetail",
    "lessonQuiz",
    "lessonFlashcard"
)

private fun NavBackStackEntry.baseRoute(): String? =
    destination.route?.substringBefore("?")?.substringBefore("/{")?.trim()

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isBottomTabSwitch(): Boolean {
    val from = initialState.baseRoute()
    val to = targetState.baseRoute()
    return from != null && to != null && from in bottomTabRoutes && to in bottomTabRoutes
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isDetailNavigation(): Boolean {
    val to = targetState.baseRoute() ?: return false
    return detailRoutes.contains(to)
}

private fun tabSlideOffset(
    fromRoute: String?,
    toRoute: String?,
    fullWidth: Int
): Int {
    val from = bottomTabRoutes.indexOf(fromRoute)
    val to = bottomTabRoutes.indexOf(toRoute)
    if (from < 0 || to < 0 || from == to) return 0
    val quarter = fullWidth / 5
    return if (to > from) quarter else -quarter
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.mainEnterTransition(): EnterTransition {
    if (isBottomTabSwitch()) {
        return fadeIn(tween(TAB_MS)) + slideInHorizontally(tween(TAB_MS)) { fullWidth ->
            tabSlideOffset(initialState.baseRoute(), targetState.baseRoute(), fullWidth)
        }
    }
    if (isDetailNavigation()) {
        return fadeIn(tween(DETAIL_MS))
    }
    return slideInHorizontally(tween(STACK_MS)) { it } + fadeIn(tween(STACK_MS))
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.mainExitTransition(): ExitTransition {
    if (isBottomTabSwitch()) {
        return fadeOut(tween(TAB_MS)) + slideOutHorizontally(tween(TAB_MS)) { fullWidth ->
            -tabSlideOffset(initialState.baseRoute(), targetState.baseRoute(), fullWidth)
        }
    }
    if (isDetailNavigation()) {
        return fadeOut(tween(DETAIL_MS))
    }
    return slideOutHorizontally(tween(STACK_MS)) { -it / 4 } + fadeOut(tween(STACK_MS))
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.mainPopEnterTransition(): EnterTransition {
    if (isBottomTabSwitch()) {
        return fadeIn(tween(TAB_MS)) + slideInHorizontally(tween(TAB_MS)) { fullWidth ->
            -tabSlideOffset(initialState.baseRoute(), targetState.baseRoute(), fullWidth)
        }
    }
    return slideInHorizontally(tween(STACK_MS)) { -it / 4 } + fadeIn(tween(STACK_MS))
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.mainPopExitTransition(): ExitTransition {
    if (isBottomTabSwitch()) {
        return fadeOut(tween(TAB_MS)) + slideOutHorizontally(tween(TAB_MS)) { fullWidth ->
            tabSlideOffset(initialState.baseRoute(), targetState.baseRoute(), fullWidth)
        }
    }
    val from = initialState.baseRoute()
    if (from != null && detailRoutes.contains(from)) {
        return fadeOut(tween(DETAIL_MS))
    }
    return slideOutHorizontally(tween(STACK_MS)) { it } + fadeOut(tween(STACK_MS))
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.rootEnterTransition(): EnterTransition =
    fadeIn(tween(ROOT_MS)) + slideInHorizontally(tween(ROOT_MS)) { it / 3 }

fun AnimatedContentTransitionScope<NavBackStackEntry>.rootExitTransition(): ExitTransition =
    fadeOut(tween(ROOT_MS)) + slideOutHorizontally(tween(ROOT_MS)) { -it / 3 }

fun AnimatedContentTransitionScope<NavBackStackEntry>.rootPopEnterTransition(): EnterTransition =
    fadeIn(tween(ROOT_MS)) + slideInHorizontally(tween(ROOT_MS)) { -it / 3 }

fun AnimatedContentTransitionScope<NavBackStackEntry>.rootPopExitTransition(): ExitTransition =
    fadeOut(tween(ROOT_MS)) + slideOutHorizontally(tween(ROOT_MS)) { it / 3 }
