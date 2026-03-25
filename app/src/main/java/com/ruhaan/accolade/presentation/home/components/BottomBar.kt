package com.ruhaan.accolade.presentation.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ruhaan.accolade.R
import kotlinx.coroutines.android.awaitFrame

sealed class BottomNavItem(val route: String, val icon: Int, val iconFilled: Int) {
  object Movies : BottomNavItem("home", R.drawable.ic_home_outline, R.drawable.ic_home_filled)

  object Schedule :
      BottomNavItem("schedule", R.drawable.ic_schedule_outline, R.drawable.ic_schedule_filled)

  object Category :
      BottomNavItem("category", R.drawable.ic_category_outline, R.drawable.ic_category_filled)
}

@Composable
fun FloatingBottomBar(navController: NavHostController, modifier: Modifier = Modifier) {
  val items = listOf(BottomNavItem.Movies, BottomNavItem.Schedule, BottomNavItem.Category)
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route

  var isVisible by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    awaitFrame()
    isVisible = true
  }

  AnimatedVisibility(
      visible = isVisible,
      enter =
          slideInVertically(
              initialOffsetY = { it },
              animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
          ) + fadeIn(animationSpec = tween(300)),
      modifier = modifier,
  ) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
      BottomBarContent(
          items = items,
          currentRoute = currentRoute,
          onItemClick = { item ->
            val isOnExactRoute = currentRoute == item.route

            val isOnNestedRoute = currentRoute?.startsWith("${item.route}/") == true

            when {
              isOnExactRoute -> {}
              isOnNestedRoute -> {

                navController.popBackStack(item.route, inclusive = false)
              }
              else -> {

                navController.navigate(item.route) {
                  popUpTo(navController.graph.startDestinationId) { saveState = true }
                  launchSingleTop = true
                  restoreState = false
                }
              }
            }
          },
      )
    }
  }
}

@Composable
private fun BottomBarContent(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit,
) {

  val selectedIndex =
      items.indexOfFirst { item ->
        currentRoute == item.route || currentRoute?.startsWith("${item.route}/") == true
      }

  val previousIndex = remember { mutableIntStateOf(selectedIndex) }

  val indicatorOffset by
      animateFloatAsState(
          targetValue = selectedIndex.toFloat(),
          animationSpec =
              if (previousIndex.intValue < 0) snap()
              else
                  spring(
                      dampingRatio = Spring.DampingRatioLowBouncy,
                      stiffness = Spring.StiffnessMediumLow,
                  ),
          label = "indicator_offset",
      )

  LaunchedEffect(selectedIndex) { previousIndex.intValue = selectedIndex }

  Box(
      modifier =
          Modifier.shadow(
                  elevation = 8.dp,
                  shape = RoundedCornerShape(30.dp),
                  ambientColor = Color.Black.copy(alpha = 0.1f),
                  spotColor = Color.Black.copy(alpha = 0.1f),
              )
              .clip(RoundedCornerShape(30.dp))
              .background(Color.White)
              .pointerInput(Unit) {
                awaitPointerEventScope {
                  while (true) {
                    awaitPointerEvent()
                  }
                }
              }
  ) {
    if (selectedIndex >= 0) {
      Box(
          modifier =
              Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                  .offset(x = indicatorOffset * (56.dp + 16.dp))
                  .size(56.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary)
      )
    }

    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      items.forEachIndexed { index, item ->
        val isSelected = index == selectedIndex

        BottomNavIcon(
            item = item,
            isSelected = isSelected,
            currentRoute = currentRoute,
            onClick = { onItemClick(item) },
        )
      }
    }
  }
}

@Composable
private fun BottomNavIcon(
    item: BottomNavItem,
    isSelected: Boolean,
    currentRoute: String?,
    onClick: () -> Unit,
) {

  val scale by
      animateFloatAsState(
          targetValue = if (isSelected) 1.1f else 1f,
          animationSpec =
              spring(
                  dampingRatio = Spring.DampingRatioMediumBouncy,
                  stiffness = Spring.StiffnessMedium,
              ),
          label = "icon_scale",
      )

  val iconTint by
      animateColorAsState(
          targetValue = if (isSelected) Color.White else Color.Black,
          animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
          label = "icon_tint",
      )

  Box(
      modifier =
          Modifier.size(56.dp)
              .clip(CircleShape)
              .clickable(
                  onClick = { onClick() },
                  indication =
                      ripple(
                          bounded = false,
                          radius = 24.dp,
                          color = MaterialTheme.colorScheme.primary,
                          // color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                      ),
                  interactionSource = remember { MutableInteractionSource() },
              ),
      contentAlignment = Alignment.Center,
  ) {
    Crossfade(
        targetState = isSelected,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "icon_crossfade",
    ) { selected ->
      Icon(
          painter = painterResource(id = if (selected) item.iconFilled else item.icon),
          contentDescription = item.route,
          tint = iconTint,
          modifier =
              Modifier.size(24.dp).graphicsLayer {
                scaleX = scale
                scaleY = scale
              },
      )
    }
  }
}
