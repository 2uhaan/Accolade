package com.ruhaan.accolade.presentation.category

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ruhaan.accolade.domain.model.Genre
import com.ruhaan.accolade.presentation.common.AppSpacing
import com.ruhaan.accolade.presentation.common.MainNavigationScreen
import kotlinx.coroutines.delay

@Composable
fun AllCategoriesScreen(navController: NavController) {
  val hapticFeedback = LocalHapticFeedback.current

  MainNavigationScreen(navController = navController) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = AppSpacing.contentPaddingTop,
                bottom = AppSpacing.contentPaddingBottom + 32.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Header
      item(span = { GridItemSpan(2) }) {
        Text(
            text = "Categories",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
      }

      // Category Cards
      items(getAllGenres()) { genre ->
        CategoryCard(
            genre = genre,
            onClick = {
              hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
              navController.navigate("category/${genre.id}/${genre.name}")
            },
        )
      }
    }
  }
}

@Composable
private fun CategoryCard(genre: Genre, onClick: () -> Unit) {
  var isExploding by remember { mutableStateOf(false) }
  val scale by
      animateFloatAsState(
          targetValue = if (isExploding) 1.3f else 1f,
          animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
          label = "scale",
      )
  val alpha by
      animateFloatAsState(
          targetValue = if (isExploding) 0f else 1f,
          animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
          label = "alpha",
      )

  LaunchedEffect(isExploding) {
    if (isExploding) {
      delay(300)
      onClick()
      isExploding = false
    }
  }

  Card(
      onClick = { isExploding = true },
      modifier =
          Modifier.fillMaxWidth().aspectRatio(1f).graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
          },
      colors = CardDefaults.cardColors(containerColor = getGenreColor(genre.id)),
      elevation =
          CardDefaults.cardElevation(
              defaultElevation = 6.dp,
              pressedElevation = 8.dp,
              hoveredElevation = 8.dp,
          ),
      shape = RoundedCornerShape(16.dp),
  ) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
      Text(
          text = genre.name,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.ExtraBold,
          color = Color.White,
          textAlign = androidx.compose.ui.text.style.TextAlign.Center,
          letterSpacing = 0.5.sp,
          lineHeight = 28.sp,
      )
    }
  }
}

// All available genres (merged from movies and TV)
private fun getAllGenres(): List<Genre> {
  return listOf(
      Genre(28, "Action"),
      Genre(12, "Adventure"),
      Genre(16, "Animation"),
      Genre(35, "Comedy"),
      Genre(80, "Crime"),
      Genre(99, "Docu"),
      Genre(18, "Drama"),
      Genre(10751, "Family"),
      Genre(14, "Fantasy"),
      Genre(36, "History"),
      Genre(27, "Horror"),
      Genre(10402, "Music"),
      Genre(9648, "Mystery"),
      Genre(10749, "Romance"),
      Genre(878, "Science Fiction"),
      Genre(53, "Thriller"),
      Genre(10752, "War"),
      Genre(37, "Western"),
      Genre(10762, "Kids"),
      Genre(10763, "News"),
      Genre(10764, "Reality"),
      Genre(10766, "Soap"),
      Genre(10767, "Talk"),
      Genre(10768, "Politics"),
  )
}

// Assign unique colors to each genre
private fun getGenreColor(genreId: Int): Color {
  return when (genreId) {
    28 -> Color(0xFFE53935) // Action - Red
    12 -> Color(0xFFFB8C00) // Adventure - Orange
    16 -> Color(0xFFAB47BC) // Animation - Purple
    35 -> Color(0xFFFFEB3B) // Comedy - Yellow
    80 -> Color(0xFF424242) // Crime - Dark Gray
    99 -> Color(0xFF8D6E63) // Documentary - Brown
    18 -> Color(0xFF5C6BC0) // Drama - Indigo
    10751 -> Color(0xFF26A69A) // Family - Teal
    14 -> Color(0xFF9C27B0) // Fantasy - Deep Purple
    36 -> Color(0xFF795548) // History - Brown
    27 -> Color(0xFF212121) // Horror - Black
    10402 -> Color(0xFFEC407A) // Music - Pink
    9648 -> Color(0xFF7E57C2) // Mystery - Deep Purple
    10749 -> Color(0xFFF06292) // Romance - Light Pink
    878 -> Color(0xFF00ACC1) // Science Fiction - Cyan
    53 -> Color(0xFF616161) // Thriller - Gray
    10752 -> Color(0xFF6D4C41) // War - Dark Brown
    37 -> Color(0xFFD4A574) // Western - Tan
    10762 -> Color(0xFF42A5F5) // Kids - Light Blue
    10763 -> Color(0xFF78909C) // News - Blue Gray
    10764 -> Color(0xFFFF7043) // Reality - Deep Orange
    10766 -> Color(0xFFBA68C8) // Soap - Light Purple
    10767 -> Color(0xFF66BB6A) // Talk - Green
    10768 -> Color(0xFF8E24AA) // Politics - Purple
    else -> Color(0xFF607D8B) // Default - Blue Gray
  }
}
