package com.ruhaan.accolade.presentation.detail

import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.ruhaan.accolade.R
import com.ruhaan.accolade.domain.model.Genre
import com.ruhaan.accolade.domain.model.MediaType
import com.ruhaan.accolade.domain.model.MovieDetail
import com.ruhaan.accolade.domain.model.Review
import com.ruhaan.accolade.presentation.common.ErrorPlaceholder
import com.ruhaan.accolade.presentation.search.OffWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    navController: NavController,
    movieId: Int,
    mediaType: MediaType,
    viewModel: MovieDetailViewModel = hiltViewModel(),
) {
  val uiState by viewModel.detailState.collectAsState()
  val reviewsState by viewModel.reviewsState.collectAsState()

  LaunchedEffect(movieId, mediaType) {
    viewModel.loadDetail(movieId, mediaType)
    viewModel.loadReviews(movieId, mediaType)
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = {},
            navigationIcon = {
              IconButton(onClick = { navController.navigateUp() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
              }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
        )
      }
  ) { paddingValues ->
    Box(modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding())) {
      when (val state = uiState) {
        is DetailUiState.Loading -> {
          ShimmerDetailScreen()
        }
        is DetailUiState.Error -> {
          ErrorView(
              message = state.message,
              onRetry = { viewModel.loadDetail(movieId, mediaType) },
              modifier = Modifier.align(Alignment.Center),
          )
        }
        is DetailUiState.Success -> {
          DetailContent(
              modifier = Modifier.padding(bottom = 80.dp),
              detail = state.detail,
              reviewsState = reviewsState,
              onCastClick = {
                navController.navigate(
                    "castcrew/${state.detail.id}/${state.detail.mediaType.name}/cast"
                )
              },
              onCrewClick = {
                navController.navigate(
                    "castcrew/${state.detail.id}/${state.detail.mediaType.name}/crew"
                )
              },
              onGenreClick = { genre ->
                navController.navigate("category/${genre.id}/${genre.name}")
              },
              onDirectorClick = { personId -> navController.navigate("filmography/$personId") },
          )
        }
      }
    }
  }
}

@Composable
private fun DetailContent(
    modifier: Modifier = Modifier,
    detail: MovieDetail,
    reviewsState: ReviewsUiState,
    onCastClick: () -> Unit,
    onCrewClick: () -> Unit,
    onGenreClick: (Genre) -> Unit,
    onDirectorClick: (Int) -> Unit,
) {
  val scrollState = rememberScrollState()

  Column(
      modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(bottom = 55.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // ── 1. Banner + Trailer overlay ──────────────────────────────
    BannerWithTrailer(
        backdropPath = detail.backdropPath,
        posterPath = detail.posterPath,
        trailer = detail.trailer,
        movieTitle = detail.title,
    )

    Spacer(modifier = Modifier.height(30.dp))

    // ── 2. Title ─────────────────────────────────────────────────
    Text(
        text = detail.title,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 24.dp),
    )

    Spacer(modifier = Modifier.height(6.dp))

    // ── 2. Meta row ───────────────────────────────────────────────
    Spacer(modifier = Modifier.height(16.dp))

    MetaSection(detail = detail, onDirectorClick = onDirectorClick)

    Spacer(modifier = Modifier.height(20.dp))

    // ── 3. Genre chips ────────────────────────────────────────────
    if (detail.genres.isNotEmpty()) {
      GenreChipsSection(genres = detail.genres, onGenreClick = onGenreClick)
    }

    Spacer(modifier = Modifier.height(20.dp))

    // ── 4. Overview / Description ─────────────────────────────────
    AboutSection(
        synopsis = detail.synopsis,
        modifier = Modifier.padding(horizontal = 20.dp),
    )

    Spacer(modifier = Modifier.height(28.dp))

    // ── 5. Cast & Crew cards ──────────────────────────────────────
    CastCrewCards(
        onCastClick = onCastClick,
        onCrewClick = onCrewClick,
        modifier = Modifier.padding(horizontal = 20.dp),
    )

    Spacer(modifier = Modifier.height(32.dp))

    // ── 6. Circular rating ────────────────────────────────────────
    CircularRatingSection(
        rating = detail.rating,
        modifier = Modifier.padding(horizontal = 20.dp),
    )

    Spacer(modifier = Modifier.height(32.dp))

    // ── 7. Reviews ────────────────────────────────────────────────
    ReviewsSection(
        reviewsState = reviewsState,
        modifier = Modifier.padding(horizontal = 20.dp),
    )
  }
}

// ────────────────────────────────────────────────────────────────────────────
// 1. Banner + Trailer (play button overlaid on backdrop)
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun BannerWithTrailer(
    backdropPath: String?,
    posterPath: String,
    trailer: com.ruhaan.accolade.domain.model.Trailer?,
    movieTitle: String, // add this
) {
  val context = LocalContext.current
  val backgroundColor = MaterialTheme.colorScheme.background

  Box(
      modifier = Modifier.fillMaxWidth().height(380.dp),
  ) {
    // ── Backdrop ──────────────────────────────────────────────────
    if (backdropPath != null) {
      SubcomposeAsyncImage(
          model = ImageRequest.Builder(context).data(backdropPath).crossfade(true).build(),
          contentDescription = "Banner",
          modifier = Modifier.fillMaxWidth().height(240.dp),
          contentScale = ContentScale.Crop,
          loading = {
            Box(
                modifier =
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)
            )
          },
          error = {
            Box(
                modifier =
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)
            )
          },
      )
      // Gradient scrim
      Box(
          modifier =
              Modifier.fillMaxWidth()
                  .height(240.dp)
                  .background(
                      brush =
                          Brush.verticalGradient(
                              colors =
                                  listOf(
                                      Color.Transparent,
                                      backgroundColor.copy(alpha = 0.5f),
                                      backgroundColor,
                                  ),
                              startY = 80f,
                          )
                  )
      )
    } else {
      Box(
          modifier =
              Modifier.fillMaxWidth()
                  .height(240.dp)
                  .background(MaterialTheme.colorScheme.surfaceVariant)
      )
    }

    // ── Trailer play button centered on banner ────────────────────
    if (trailer != null) {
      Box(
          modifier =
              Modifier.align(Alignment.TopCenter)
                  .padding(top = 80.dp)
                  .size(48.dp)
                  .shadow(12.dp, CircleShape)
                  .clip(CircleShape)
                  .background(Color.Black.copy(alpha = 0.5f))
                  .clickable {
                    val intent =
                        Intent(
                            Intent.ACTION_VIEW,
                            "https://www.youtube.com/watch?v=${trailer.key}".toUri(),
                        )
                    context.startActivity(intent)
                  },
          contentAlignment = Alignment.Center,
      ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play Trailer",
            modifier = Modifier.size(38.dp),
            tint = Color.White,
        )
      }
    }

    // ── Poster card — centered, overlapping banner bottom ─────────
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context).data(posterPath).crossfade(true).build(),
        contentDescription = "Movie Poster",
        modifier =
            Modifier.width(160.dp) // Changed from 130.dp
                .height(240.dp) // Changed from 195.dp
                .align(Alignment.BottomCenter)
                .offset(y = 20.dp)
                .shadow(16.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp)),
        contentScale = ContentScale.Crop,
        error = { ErrorPlaceholder(movieTitle = movieTitle) },
    )
  }
}

// ────────────────────────────────────────────────────────────────────────────
// 2. Meta row
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun MetaSection(
    detail: MovieDetail,
    onDirectorClick: (Int) -> Unit,
) {
  val directorLabel = if (detail.mediaType == MediaType.MOVIE) "Directed By" else "Created By"
  val typeLabel = if (detail.mediaType == MediaType.MOVIE) "Movie" else "TV Show"

  Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    // Row 1: Director (1) & Year (2)
    MetaRow(
        label1 = directorLabel,
        value1 = detail.directors.firstOrNull()?.name ?: "N/A",
        clickableId1 = detail.directors.firstOrNull()?.id,
        onValueClick = onDirectorClick,
        label2 = "Year",
        value2 = detail.year,
    )

    // Row 2: Language (3) & Country (4)
    MetaRow(
        label1 = "Language",
        value1 = detail.language,
        label2 = "Country",
        value2 = detail.country,
    )

    // Row 3: Type (5) & Duration (6)
    MetaRow(label1 = "Type", value1 = typeLabel, label2 = "Duration", value2 = detail.runtime)
  }
}

@Composable
private fun MetaRow(
    label1: String,
    value1: String,
    label2: String,
    value2: String,
    clickableId1: Int? = null,
    onValueClick: ((Int) -> Unit)? = null,
) {
  Row(modifier = Modifier.fillMaxWidth()) {
    // Left Column (Items 1, 3, 5)
    Column(modifier = Modifier.weight(1f)) {
      Text(
          text = label1,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.5.sp,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
          text = value1,
          style = MaterialTheme.typography.bodyMedium,
          color =
              if (clickableId1 != null) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onBackground,
          modifier =
              if (clickableId1 != null && onValueClick != null) {
                Modifier.clickable { onValueClick(clickableId1) }
              } else Modifier,
      )
    }

    // Right Column (Items 2, 4, 6)
    Column(modifier = Modifier.weight(1f)) {
      Text(
          text = label2,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.5.sp,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
          text = value2,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onBackground,
      )
    }
  }
}

// ────────────────────────────────────────────────────────────────────────────
// 3. About / Synopsis
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun AboutSection(synopsis: String, modifier: Modifier = Modifier) {
  SectionHeader(title = "Overview", modifier = modifier)

  Spacer(modifier = Modifier.height(10.dp))

  Text(
      text = synopsis,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
      lineHeight = 22.sp,
      modifier = modifier,
  )
}

// ────────────────────────────────────────────────────────────────────────────
// 4. Genre chips
// ────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenreChipsSection(genres: List<Genre>, onGenreClick: (Genre) -> Unit) {
  FlowRow(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    genres.forEach { genre ->
      SuggestionChip(
          onClick = { onGenreClick(genre) },
          label = {
            Text(
                text = genre.name.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            )
          },
          colors =
              SuggestionChipDefaults.suggestionChipColors(
                  containerColor = MaterialTheme.colorScheme.primaryContainer,
                  labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
              ),
          border =
              SuggestionChipDefaults.suggestionChipBorder(
                  enabled = true,
                  borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
              ),
      )
    }
  }
}

// ────────────────────────────────────────────────────────────────────────────
// 5. Cast & Crew cards — square, icon centered, label below
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun CastCrewCards(
    onCastClick: () -> Unit,
    onCrewClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Row(
      modifier = modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    CastCrewSquareCard(
        label = "Crew",
        iconResId = R.drawable.ic_crew,
        onClick = onCrewClick,
        modifier = Modifier.weight(1f),
    )
    CastCrewSquareCard(
        label = "Cast",
        iconResId = R.drawable.ic_cast,
        onClick = onCastClick,
        modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun CastCrewSquareCard(
    label: String,
    iconResId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Card(
      onClick = onClick,
      modifier = modifier.aspectRatio(1f),
      shape = RoundedCornerShape(16.dp),
      colors =
          CardDefaults.cardColors(
              containerColor = OffWhite,
          ),
      elevation =
          CardDefaults.cardElevation(
              defaultElevation = 6.dp,
              pressedElevation = 2.dp,
          ),
  ) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
      Icon(
          painter = painterResource(id = iconResId),
          contentDescription = label,
          modifier = Modifier.size(32.dp),
          tint = MaterialTheme.colorScheme.primary,
      )
      Spacer(modifier = Modifier.height(10.dp))
      Text(
          text = label,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp,
          color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

// ────────────────────────────────────────────────────────────────────────────
// 6. Circular rating (no label text, larger size)
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun CircularRatingSection(rating: Int, modifier: Modifier = Modifier) {
  val progress by
      animateFloatAsState(
          targetValue = rating / 100f,
          animationSpec = tween(durationMillis = 1200),
          label = "ratingAnim",
      )

  val ringColor =
      when {
        rating >= 70 -> Color(0xFF4CAF50)
        rating >= 50 -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
      }

  Column(
      modifier = modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    SectionHeader(
        title = "Rating",
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(20.dp))
    Box(contentAlignment = Alignment.Center) {
      // Background track
      CircularProgressIndicator(
          progress = { 1f },
          modifier = Modifier.size(160.dp),
          color = ringColor.copy(alpha = 0.12f),
          strokeWidth = 10.dp,
          strokeCap = StrokeCap.Round,
      )
      // Foreground arc
      CircularProgressIndicator(
          progress = { progress },
          modifier = Modifier.size(160.dp),
          color = ringColor,
          strokeWidth = 10.dp,
          strokeCap = StrokeCap.Round,
      )
      // Center percentage
      Text(
          text = "$rating%",
          style = MaterialTheme.typography.headlineLarge,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.onBackground,
      )
    }
  }
}

// ────────────────────────────────────────────────────────────────────────────
// 7. Reviews
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun ReviewsSection(
    reviewsState: ReviewsUiState,
    modifier: Modifier = Modifier,
) {
  SectionHeader(title = "Audience Reviews", modifier = modifier)

  Spacer(modifier = Modifier.height(12.dp))

  when (reviewsState) {
    is ReviewsUiState.Loading -> {
      Box(
          modifier = modifier.fillMaxWidth().height(80.dp),
          contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator()
      }
    }
    is ReviewsUiState.Empty -> {
      Text(
          text = "No reviews yet.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
          modifier = modifier,
      )
    }
    is ReviewsUiState.Error -> {
      Text(
          text = "Could not load reviews.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
          modifier = modifier,
      )
    }
    is ReviewsUiState.Success -> {
      ReviewsList(reviews = reviewsState.reviews, modifier = modifier)
    }
  }
}

@Composable
private fun ReviewsList(reviews: List<Review>, modifier: Modifier = Modifier) {
  var expanded by remember { mutableStateOf(false) }
  val visibleReviews = if (expanded) reviews else reviews.take(2)

  Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    visibleReviews.forEach { review -> ReviewCard(review = review) }

    if (reviews.size > 2) {
      Text(
          text = if (expanded) "Show less ▲" else "Read more (${reviews.size - 2} more) ▼",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
          letterSpacing = 0.5.sp,
          modifier =
              Modifier.align(Alignment.CenterHorizontally)
                  .clickable { expanded = !expanded }
                  .padding(vertical = 4.dp),
      )
    }
  }
}

@Composable
private fun ReviewCard(review: Review) {
  var contentExpanded by remember { mutableStateOf(false) }
  val isLong = review.content.length > 300

  OutlinedCard(
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        SubcomposeAsyncImage(
            model = review.avatarPath,
            contentDescription = "Avatar",
            modifier =
                Modifier.size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
            error = {
              Box(
                  modifier =
                      Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer),
                  contentAlignment = Alignment.Center,
              ) {
                Text(
                    text = review.author.take(1).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
              }
            },
        )

        Column(modifier = Modifier.weight(1f)) {
          Text(
              text = review.author,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
          )
          Text(
              text = review.createdAt,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
          )
        }

        review.rating?.let { rating ->
          val badgeColor =
              when {
                rating >= 70 -> Color(0xFF4CAF50)
                rating >= 50 -> Color(0xFFFFC107)
                else -> Color(0xFFF44336)
              }
          Box(
              modifier =
                  Modifier.background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                      .padding(horizontal = 8.dp, vertical = 4.dp),
              contentAlignment = Alignment.Center,
          ) {
            Text(
                text = "$rating%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = badgeColor,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
          text = parseMarkdown(review.content),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
          lineHeight = 20.sp,
          maxLines = if (contentExpanded) Int.MAX_VALUE else 4,
          overflow = TextOverflow.Ellipsis,
      )

      if (isLong) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (contentExpanded) "Show less" else "Show full review",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { contentExpanded = !contentExpanded },
        )
      }
    }
  }
}

fun parseMarkdown(input: String): AnnotatedString {
  val builder = AnnotatedString.Builder()
  var i = 0

  while (i < input.length) {

    // **_bold + italic_**
    if (i + 3 < input.length && input.startsWith("**_", i)) {
      val end = input.indexOf("_**", i + 3)
      if (end != -1) {
        val text = input.substring(i + 3, end)
        builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
          append(text)
        }
        i = end + 3
        continue
      }
    }

    // **bold**
    if (i + 1 < input.length && input.startsWith("**", i)) {
      val end = input.indexOf("**", i + 2)
      if (end != -1) {
        val text = input.substring(i + 2, end)
        builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text) }
        i = end + 2
        continue
      }
    }

    // _italic_
    if (input[i] == '_') {
      val end = input.indexOf("_", i + 1)
      if (end != -1) {
        val text = input.substring(i + 1, end)
        builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text) }
        i = end + 1
        continue
      }
    }

    builder.append(input[i])
    i++
  }

  return builder.toAnnotatedString()
}

// ────────────────────────────────────────────────────────────────────────────
// Shared section header  –  accent bar + label
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
  Row(modifier = modifier.fillMaxWidth()) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Start,
    )
  }
}

// ────────────────────────────────────────────────────────────────────────────
// Error view
// ────────────────────────────────────────────────────────────────────────────

@Composable
fun ErrorView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text(
        text = "Error",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.error,
    )
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
    )
    Button(onClick = onRetry) { Text("Retry") }
  }
}

// ────────────────────────────────────────────────────────────────────────────
// loading
// ────────────────────────────────────────────────────────────────────────────

@Composable
fun ShimmerDetailScreen() {
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
      modifier =
          Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 108.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // ── 1. Banner + Poster ───────────────────────────────────
    Box(
        modifier = Modifier.fillMaxWidth().height(380.dp),
    ) {
      // Backdrop
      Box(modifier = Modifier.fillMaxWidth().height(240.dp).background(brush))
      // Poster
      Box(
          modifier =
              Modifier.width(160.dp)
                  .height(240.dp)
                  .align(Alignment.BottomCenter)
                  .offset(y = 20.dp)
                  .shadow(16.dp, RoundedCornerShape(14.dp))
                  .clip(RoundedCornerShape(14.dp))
                  .background(brush)
      )
    }

    Spacer(modifier = Modifier.height(30.dp))

    // ── 2. Title ─────────────────────────────────────────────
    Box(
        modifier =
            Modifier.fillMaxWidth(0.6f)
                .height(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(brush)
    )

    Spacer(modifier = Modifier.height(8.dp))

    Box(
        modifier =
            Modifier.fillMaxWidth(0.35f)
                .height(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(brush)
    )

    Spacer(modifier = Modifier.height(32.dp))

    // ── 3. Meta section — 3 rows × 2 columns ─────────────────
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      repeat(3) {
        Row(modifier = Modifier.fillMaxWidth()) {
          repeat(2) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              // Label
              Box(
                  modifier =
                      Modifier.fillMaxWidth(0.5f)
                          .height(14.dp)
                          .clip(RoundedCornerShape(4.dp))
                          .background(brush)
              )
              // Value
              Box(
                  modifier =
                      Modifier.fillMaxWidth(0.7f)
                          .height(16.dp)
                          .clip(RoundedCornerShape(4.dp))
                          .background(brush)
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // ── 4. Genre chips ────────────────────────────────────────
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      listOf(0.22f, 0.28f, 0.25f).forEach { fraction ->
        Box(
            modifier =
                Modifier.fillMaxWidth(fraction)
                    .height(32.dp)
                    .clip(RoundedCornerShape(50))
                    .background(brush)
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // ── 5. Overview ───────────────────────────────────────────
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // Section header
      Box(
          modifier =
              Modifier.fillMaxWidth(0.3f)
                  .height(22.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(brush)
      )
      Spacer(modifier = Modifier.height(2.dp))
      // Text lines
      listOf(1f, 1f, 1f, 0.7f).forEach { fraction ->
        Box(
            modifier =
                Modifier.fillMaxWidth(fraction)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
        )
      }
    }

    Spacer(modifier = Modifier.height(28.dp))

    // ── 6. Cast & Crew cards ──────────────────────────────────
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      repeat(2) {
        Box(
            modifier =
                Modifier.weight(1f)
                    .aspectRatio(1f)
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(brush)
        )
      }
    }

    Spacer(modifier = Modifier.height(32.dp))

    // ── 7. Rating circle ──────────────────────────────────────
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
          modifier =
              Modifier.fillMaxWidth(0.2f)
                  .height(22.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(brush)
                  .align(Alignment.Start)
      )
      Spacer(modifier = Modifier.height(20.dp))
      Box(modifier = Modifier.size(160.dp).clip(CircleShape).background(brush))
    }

    Spacer(modifier = Modifier.height(32.dp))

    // ── 8. Reviews ────────────────────────────────────────────
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
          modifier =
              Modifier.fillMaxWidth(0.4f)
                  .height(22.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(brush)
      )
      Spacer(modifier = Modifier.height(2.dp))
      repeat(2) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush)
        )
      }
    }
  }
}
