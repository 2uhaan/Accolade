package com.ruhaan.accolade.presentation.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ruhaan.accolade.domain.model.MediaType
import com.ruhaan.accolade.domain.model.SearchResult
import kotlin.math.cos
import kotlin.math.sin

// ─── Color Tokens ────────────────────────────────────────────────────────────
val OffWhite = Color(0xFFF2F2F7)
private val CardWhite = Color.White
private val DividerColor = Color(0xFFF0F0F0)
private val PlaceholderColor = Color(0xFFAAAAAA)
private val SubtextColor = Color(0xFF888888)

// ─── Screen ──────────────────────────────────────────────────────────────────
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SearchScreen(
    navController: NavController,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchClose: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
  val uiState by viewModel.uiState.collectAsState()
  val focusRequester = remember { FocusRequester() }

  // Auto-focus the search field when screen opens
  LaunchedEffect(Unit) { focusRequester.requestFocus() }

  BackHandler { onSearchClose() }

  Box(modifier = Modifier.fillMaxSize().background(OffWhite).statusBarsPadding()) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

      // ── Floating Search Bar ──────────────────────────────────────────
      FloatingSearchBarWithPulse(
          query = searchQuery,
          isLoading = uiState is SearchUiState.Loading,
          focusRequester = focusRequester,
          onQueryChange = { query ->
            onSearchQueryChange(query)
            viewModel.onSearchQueryChange(query)
          },
          onClearOrClose = {
            if (searchQuery.isNotEmpty()) {
              // First press clears text
              onSearchQueryChange("")
              viewModel.onSearchQueryChange("")
            } else {
              // Second press (empty query) exits search
              onSearchClose()
            }
          },
          sharedTransitionScope = sharedTransitionScope,
          animatedVisibilityScope = animatedVisibilityScope,
      )

      // ── Results Area ─────────────────────────────────────────────────
      when (val state = uiState) {
        is SearchUiState.Initial -> {
          InitialHint()
        }
        is SearchUiState.Loading -> {}
        is SearchUiState.Success -> {
          AnimatedVisibility(
              visible = state.results.isNotEmpty(),
              enter = fadeIn(tween(200)) + slideInVertically(tween(250)) { -20 },
              exit = fadeOut(tween(150)) + slideOutVertically(tween(200)) { -20 },
          ) {
            FloatingResultsCard(
                results = state.results,
                onResultClick = { result ->
                  when (result.mediaType) {
                    MediaType.MOVIE,
                    MediaType.TV_SHOW -> {
                      navController.navigate("detail/${result.id}/${result.mediaType.name}")
                    }
                    MediaType.PERSON -> {
                      navController.navigate("filmography/${result.id}")
                    }
                  }
                  onSearchClose()
                },
            )
          }

          if (state.results.isEmpty() && searchQuery.isNotEmpty()) {
            EmptyResultsCard(query = searchQuery)
          }
        }
        is SearchUiState.Error -> {
          ErrorCard(message = state.message)
        }
      }
    }
  }
}

// ─── Floating Search Bar ─────────────────────────────────────────────────────
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FloatingSearchBarWithPulse(
    query: String,
    isLoading: Boolean,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
    onClearOrClose: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
  val cornerRadius = 16.dp
  val shape = RoundedCornerShape(cornerRadius)
  val primaryColor = MaterialTheme.colorScheme.primary

  // ── Animation 1: rotating angle for the travelling bright spot ────────────
  val rotation by
      rememberInfiniteTransition(label = "borderRotation")
          .animateFloat(
              initialValue = 0f,
              targetValue = 360f,
              animationSpec =
                  infiniteRepeatable(
                      animation = tween(durationMillis = 1400, easing = LinearEasing),
                      repeatMode = RepeatMode.Restart,
                  ),
              label = "rotation",
          )

  // ── Animation 2: border fades in when loading starts, fades out when done ─
  val borderAlpha by
      animateFloatAsState(
          targetValue = if (isLoading) 1f else 0f,
          animationSpec = tween(durationMillis = 350),
          label = "borderAlpha",
      )

  // ── Animation 3: card shadow grows slightly while loading ─────────────────
  val shadowElevation by
      animateFloatAsState(
          targetValue = if (isLoading) 16f else 10f,
          animationSpec = tween(durationMillis = 500),
          label = "shadowElevation",
      )

  // ── Animation 4: search icon breathes (alpha) while loading ───────────────
  val iconAlpha by
      rememberInfiniteTransition(label = "iconBreath")
          .animateFloat(
              initialValue = 1f,
              targetValue = 0.3f,
              animationSpec =
                  infiniteRepeatable(
                      animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                      repeatMode = RepeatMode.Reverse,
                  ),
              label = "iconAlpha",
          )

  with(sharedTransitionScope) {
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .sharedElement(
                    rememberSharedContentState(key = "search_bar"),
                    animatedVisibilityScope = animatedVisibilityScope,
                )
                .shadow(
                    elevation = shadowElevation.dp,
                    shape = shape,
                    spotColor = Color.Black.copy(alpha = 0.10f),
                )
                .background(CardWhite, shape)
                // drawWithContent lets us paint the animated border on top of the card
                .drawWithContent {
                  drawContent() // TextField renders first

                  // Only paint if there's something visible to show
                  if (borderAlpha > 0f) {
                    val strokePx = 2.5.dp.toPx()
                    val inset = strokePx / 2f
                    val rad = cornerRadius.toPx()

                    // Orbit the gradient center around the card so the
                    // bright tip appears to travel along the border
                    val angleRad = Math.toRadians(rotation.toDouble())
                    val cx = size.width / 2f + (size.width / 2f) * cos(angleRad).toFloat()
                    val cy = size.height / 2f + (size.height / 2f) * sin(angleRad).toFloat()

                    val sweepBrush =
                        Brush.sweepGradient(
                            colorStops =
                                arrayOf(
                                    0.00f to Color.Transparent,
                                    0.30f to Color.Transparent,
                                    0.44f to primaryColor.copy(alpha = 0.25f * borderAlpha),
                                    0.50f to
                                        primaryColor.copy(
                                            alpha = 0.90f * borderAlpha
                                        ), // bright tip
                                    0.56f to primaryColor.copy(alpha = 0.25f * borderAlpha),
                                    0.70f to Color.Transparent,
                                    1.00f to Color.Transparent,
                                ),
                            center = Offset(cx, cy),
                        )

                    drawRoundRect(
                        brush = sweepBrush,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - strokePx, size.height - strokePx),
                        cornerRadius = CornerRadius(rad, rad),
                        style = Stroke(width = strokePx),
                    )
                  }
                },
    ) {
      TextField(
          value = query,
          onValueChange = onQueryChange,
          modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
          placeholder = {
            Text(
                text = "Search",
                color = PlaceholderColor,
                style = MaterialTheme.typography.bodyLarge,
            )
          },
          leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                // Breathes in primary color while loading, neutral when idle
                tint = if (isLoading) primaryColor.copy(alpha = iconAlpha) else PlaceholderColor,
                modifier = Modifier.size(22.dp),
            )
          },
          trailingIcon = {
            IconButton(onClick = onClearOrClose) {
              Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = if (query.isNotEmpty()) "Clear" else "Close",
                  tint = PlaceholderColor,
                  modifier = Modifier.size(20.dp),
              )
            }
          },
          colors =
              TextFieldDefaults.colors(
                  focusedContainerColor = Color.Transparent,
                  unfocusedContainerColor = Color.Transparent,
                  focusedIndicatorColor = Color.Transparent,
                  unfocusedIndicatorColor = Color.Transparent,
                  focusedTextColor = MaterialTheme.colorScheme.onSurface,
                  unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                  cursorColor = primaryColor,
              ),
          singleLine = true,
          textStyle = MaterialTheme.typography.bodyLarge,
      )
    }
  }
}

// ─── Floating Results Card ───────────────────────────────────────────────────
@Composable
private fun FloatingResultsCard(
    results: List<SearchResult>,
    onResultClick: (SearchResult) -> Unit,
) {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .heightIn(max = 520.dp) // dynamic height, bounded
              .shadow(
                  elevation = 16.dp,
                  shape = RoundedCornerShape(20.dp),
                  spotColor = Color.Black.copy(alpha = 0.12f),
              )
              .background(CardWhite, RoundedCornerShape(20.dp)),
  ) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
      itemsIndexed(results) { index, result ->
        SearchResultRow(
            result = result,
            onClick = { onResultClick(result) },
        )
        // Divider between items, not after last
        if (index < results.lastIndex) {
          HorizontalDivider(
              modifier = Modifier.padding(horizontal = 16.dp),
              thickness = 1.dp,
              color = DividerColor,
          )
        }
      }
    }
  }
}

// ─── Result Row (inside card — no elevation, flat) ───────────────────────────
@Composable
private fun SearchResultRow(result: SearchResult, onClick: () -> Unit) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .clickable(onClick = onClick)
              .padding(horizontal = 16.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      Text(
          text = result.title,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
      )
      // Only show year if it's not empty (persons won't have a year)
      if (result.year.isNotEmpty()) {
        Text(
            text = result.year,
            style = MaterialTheme.typography.bodySmall,
            color = SubtextColor,
        )
      }
    }

    // Media type badge
    val baseColor =
        when (result.mediaType) {
          MediaType.MOVIE -> Color(0xFF1976D2)
          MediaType.TV_SHOW -> Color(0xFF388E3C)
          MediaType.PERSON -> Color(0xFFF57C00)
        }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = baseColor.copy(alpha = 0.1f),
    ) {
      Text(
          text =
              when (result.mediaType) {
                MediaType.MOVIE -> "Movie"
                MediaType.TV_SHOW -> "TV"
                MediaType.PERSON -> "PERSON"
              },
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Medium,
          color = baseColor,
      )
    }
  }
}

// ─── Supporting States ───────────────────────────────────────────────────────
@Composable
private fun EmptyResultsCard(query: String) {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .shadow(elevation = 12.dp, shape = RoundedCornerShape(20.dp))
              .background(CardWhite, RoundedCornerShape(20.dp))
              .padding(32.dp),
      contentAlignment = Alignment.Center,
  ) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text(
          text = "No Results",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
          text = "Nothing found for \"$query\"",
          style = MaterialTheme.typography.bodySmall,
          color = SubtextColor,
      )
    }
  }
}

@Composable
private fun ErrorCard(message: String) {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .shadow(elevation = 12.dp, shape = RoundedCornerShape(20.dp))
              .background(CardWhite, RoundedCornerShape(20.dp))
              .padding(32.dp),
      contentAlignment = Alignment.Center,
  ) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text(
          text = "Something went wrong",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.error,
      )
      Text(
          text = message,
          style = MaterialTheme.typography.bodySmall,
          color = SubtextColor,
      )
    }
  }
}

@Composable
private fun InitialHint() {
  // Subtle hint — no card, just floats on the off-white bg
  Box(
      modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
      contentAlignment = Alignment.Center,
  ) {
    Text(
        text = "Search movies, shows and artist",
        style = MaterialTheme.typography.bodySmall,
        color = PlaceholderColor,
    )
  }
}
