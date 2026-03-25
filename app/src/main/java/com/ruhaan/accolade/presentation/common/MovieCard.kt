package com.ruhaan.accolade.presentation.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.ruhaan.accolade.domain.model.Movie

@Composable
fun MovieCard(movie: Movie, onMovieClick: (Movie) -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val haptic = LocalHapticFeedback.current

  // Subtle background highlight on click
  val backgroundColor by
      animateColorAsState(
          targetValue = if (isPressed) Color(0xFFF5F5F5) else Color.Transparent,
          animationSpec = tween(durationMillis = 100),
          label = "background_color",
      )

  // Scale animation on press
  val scale by
      animateFloatAsState(
          targetValue = if (isPressed) 0.96f else 1f,
          animationSpec =
              spring(
                  dampingRatio = Spring.DampingRatioMediumBouncy,
                  stiffness = Spring.StiffnessLow,
              ),
          label = "scale",
      )

  // Build complete image URL — replace w500 with w342
  val imageUrl = remember(movie.posterPath) { "https://image.tmdb.org/t/p/w342${movie.posterPath}" }

  Column(
      modifier =
          modifier
              .fillMaxWidth()
              .graphicsLayer {
                scaleX = scale
                scaleY = scale
              }
              .background(color = backgroundColor, shape = RoundedCornerShape(12.dp))
              .clickable(interactionSource = interactionSource, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                onMovieClick(movie)
              }
              .padding(8.dp)
              .semantics {
                contentDescription = "${movie.title}, ${movie.year}"
                role = Role.Button
              }
  ) {
    // Poster Image
    SubcomposeAsyncImage(
        model =
            ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .memoryCacheKey(imageUrl) // Cache optimization
                .diskCacheKey(imageUrl)
                .build(),
        contentDescription = "Poster for ${movie.title}",
        modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(12.dp)),
        contentScale = ContentScale.Crop,
        loading = { ShimmerMovieCard() },
        error = { ErrorPlaceholder(movieTitle = movie.title) },
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Movie Info
    MovieInfo(movie = movie)
  }
}

@Composable
fun ErrorPlaceholder(movieTitle: String) {
  Box(
      modifier = Modifier.fillMaxSize().background(Color(0xFF14181C)),
      contentAlignment = Alignment.Center,
  ) {
    Text(
        text = movieTitle.ifEmpty { "Error" },
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = Color.White,
        textAlign = TextAlign.Center,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(16.dp),
    )
  }
}

@Composable
private fun MovieInfo(movie: Movie) {
  Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Text(
        text = movie.title,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onBackground,
    )

    Text(
        text = movie.year,
        style = MaterialTheme.typography.bodySmall,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        fontWeight = FontWeight.Normal,
    )
  }
}

@Composable
fun ShimmerMovieCard() {
  val infiniteTransition = rememberInfiniteTransition(label = "shimmer")

  val translateAnim by
      infiniteTransition.animateFloat(
          initialValue = 0f,
          targetValue = 1000f,
          animationSpec =
              infiniteRepeatable(
                  animation = tween(durationMillis = 1200, easing = LinearEasing),
                  repeatMode = RepeatMode.Restart,
              ),
          label = "translate",
      )

  val shimmerColors =
      listOf(
          Color(0xFFC0C0C0), // Silver
          Color(0xFFFFFFFF), // White shine
          Color(0xFFC0C0C0), // Silver
      )

  val brush =
      remember(translateAnim) {
        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnim - 1000f, translateAnim - 1000f),
            end = Offset(translateAnim, translateAnim),
        )
      }

  Column(
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(brush)
    )

    Box(
        modifier =
            Modifier.fillMaxWidth(0.8f)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
    )

    Box(
        modifier =
            Modifier.fillMaxWidth(0.4f)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
    )
  }
}


