package com.ruhaan.accolade.presentation.filmography

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.ruhaan.accolade.presentation.common.AppSpacing
import com.ruhaan.accolade.presentation.common.MovieCard
import com.ruhaan.accolade.presentation.common.ShimmerMovieCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilmographyScreen(
    navController: NavController,
    personId: Int,
    viewModel: FilmographyViewModel = hiltViewModel(),
) {
  val personState by viewModel.personState.collectAsState()
  val filmographyState by viewModel.filmographyState.collectAsState()

  LaunchedEffect(personId) { viewModel.load(personId) }

  Scaffold(
      topBar = {
        TopAppBar(
            title = {},
            navigationIcon = {
              IconButton(onClick = { navController.navigateUp() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
              }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
        )
      }
  ) { paddingValues ->
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding()),
        contentPadding =
            PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 0.dp,
                bottom = AppSpacing.contentPaddingBottom,
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Person header — spans full width
      item(span = { GridItemSpan(2) }) { PersonHeader(personState = personState) }

      // Filmography grid
      when (val state = filmographyState) {
        is FilmographyUiState.Loading -> {
          items(6) { ShimmerMovieCard() }
        }
        is FilmographyUiState.Error -> {
          item(span = { GridItemSpan(2) }) {
            Text(
                text = "Could not load filmography.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp),
            )
          }
        }
        is FilmographyUiState.Success -> {
          items(state.movies) { movie ->
            MovieCard(
                movie = movie,
                onMovieClick = {
                  navController.navigate("detail/${movie.id}/${movie.mediaType.name}")
                },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PersonHeader(personState: PersonUiState) {
  when (personState) {
    is PersonUiState.Loading -> {
      Box(
          modifier = Modifier.fillMaxWidth().height(200.dp),
          contentAlignment = Alignment.Center,
      ) {
        ShimmerPersonHeader()
      }
    }
    is PersonUiState.Error -> {
      Text(
          text = "Could not load person details.",
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(16.dp),
      )
    }
    is PersonUiState.Success -> {
      val person = personState.person
      var bioExpanded by remember { mutableStateOf(false) }
      val hasBio = !person.biography.isNullOrBlank()

      Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Profile image
        SubcomposeAsyncImage(
            model =
                ImageRequest.Builder(LocalContext.current)
                    .data(person.profilePath)
                    .crossfade(true)
                    .build(),
            contentDescription = person.name,
            modifier = Modifier.size(130.dp).shadow(8.dp, CircleShape).clip(CircleShape),
            contentScale = ContentScale.Crop,
            error = {
              Box(
                  modifier =
                      Modifier.fillMaxSize()
                          .background(
                              MaterialTheme.colorScheme.primaryContainer,
                              CircleShape,
                          ),
                  contentAlignment = Alignment.Center,
              ) {
                Text(
                    text = person.name.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
              }
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Name
        Text(
            text = person.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Left-aligned metadata + bio section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
        ) {
          // Born / Birthplace row
          if (person.birthday != null || person.placeOfBirth != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            ) {
              if (person.birthday != null) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                      text = "Born",
                      style = MaterialTheme.typography.titleMedium,
                      color = MaterialTheme.colorScheme.onBackground,
                      fontWeight = FontWeight.Bold,
                      letterSpacing = 0.5.sp,
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                      text =
                          person.birthday.let {
                            try {
                              java.time.LocalDate.parse(it)
                                  .format(
                                      java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")
                                  )
                            } catch (_: Exception) {
                              it
                            }
                          } ?: "",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onBackground,
                  )
                }
              }

              if (person.placeOfBirth != null) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                      text = "Birthplace",
                      style = MaterialTheme.typography.titleMedium,
                      color = MaterialTheme.colorScheme.onBackground,
                      fontWeight = FontWeight.Bold,
                      letterSpacing = 0.5.sp,
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                      text = person.placeOfBirth,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onBackground,
                  )
                }
              }
            }
          }

          // Biography
          var isOverflowing by remember { mutableStateOf(false) }

          if (hasBio) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Biography",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = person.biography,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 22.sp,
                maxLines = if (bioExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                onTextLayout = { result ->
                  // Only update when collapsed
                  if (!bioExpanded) {
                    isOverflowing = result.hasVisualOverflow
                  }
                },
            )

            if (isOverflowing) {
              Spacer(modifier = Modifier.height(4.dp))

              Text(
                  text = if (bioExpanded) "Show less" else "Read more",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.clickable { bioExpanded = !bioExpanded },
              )
            }
          }

          Spacer(modifier = Modifier.height(20.dp))

          // Divider
          HorizontalDivider(
              color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
              thickness = 1.dp,
          )

          Spacer(modifier = Modifier.height(20.dp))

          // "Filmography" section header
          Text(
              text = "Filmography",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.fillMaxWidth(),
              textAlign = TextAlign.Center,
          )
        }
      }
    }
  }
}

@Composable
private fun ShimmerPersonHeader() {
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
          Color(0xFFC0C0C0),
          Color(0xFFFFFFFF),
          Color(0xFFC0C0C0),
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
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Profile image circle
    Box(modifier = Modifier.size(130.dp).clip(CircleShape).background(brush))

    Spacer(modifier = Modifier.height(12.dp))

    // Name
    Box(
        modifier =
            Modifier.fillMaxWidth(0.5f)
                .height(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(brush)
    )

    Spacer(modifier = Modifier.height(20.dp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
      // Born / Birthplace row
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Born column
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Box(
              modifier =
                  Modifier.fillMaxWidth(0.4f)
                      .height(14.dp)
                      .clip(RoundedCornerShape(4.dp))
                      .background(brush)
          )
          Box(
              modifier =
                  Modifier.fillMaxWidth(0.7f)
                      .height(14.dp)
                      .clip(RoundedCornerShape(4.dp))
                      .background(brush)
          )
        }

        // Birthplace column
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Box(
              modifier =
                  Modifier.fillMaxWidth(0.55f)
                      .height(14.dp)
                      .clip(RoundedCornerShape(4.dp))
                      .background(brush)
          )
          Box(
              modifier =
                  Modifier.fillMaxWidth(0.9f)
                      .height(14.dp)
                      .clip(RoundedCornerShape(4.dp))
                      .background(brush)
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Biography label
      Box(
          modifier =
              Modifier.fillMaxWidth(0.3f)
                  .height(14.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(brush)
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Bio lines
      repeat(3) { index ->
        Box(
            modifier =
                Modifier.fillMaxWidth(if (index == 2) 0.6f else 1f)
                    .height(13.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
        )
        if (index < 2) Spacer(modifier = Modifier.height(6.dp))
      }

      Spacer(modifier = Modifier.height(20.dp))

      HorizontalDivider(
          color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
          thickness = 1.dp,
      )

      Spacer(modifier = Modifier.height(20.dp))

      // "Filmography" title placeholder
      Box(
          modifier =
              Modifier.fillMaxWidth(0.4f)
                  .height(20.dp)
                  .align(Alignment.CenterHorizontally)
                  .clip(RoundedCornerShape(6.dp))
                  .background(brush)
      )
    }
  }
}
