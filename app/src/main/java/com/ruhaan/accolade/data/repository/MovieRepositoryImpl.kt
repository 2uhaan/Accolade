package com.ruhaan.accolade.data.repository

import com.ruhaan.accolade.data.local.CacheConfig
import com.ruhaan.accolade.data.local.cachedOr
import com.ruhaan.accolade.data.local.dao.CreditsCacheDao
import com.ruhaan.accolade.data.local.dao.MovieDetailCacheDao
import com.ruhaan.accolade.data.local.dao.MovieListCacheDao
import com.ruhaan.accolade.data.local.dao.PersonCacheDao
import com.ruhaan.accolade.data.local.dao.ReviewsCacheDao
import com.ruhaan.accolade.data.local.entity.CreditsCacheEntity
import com.ruhaan.accolade.data.local.entity.MovieDetailCacheEntity
import com.ruhaan.accolade.data.local.entity.MovieListCacheEntity
import com.ruhaan.accolade.data.local.entity.PersonCacheEntity
import com.ruhaan.accolade.data.local.entity.ReviewsCacheEntity
import com.ruhaan.accolade.data.local.fromJson
import com.ruhaan.accolade.data.local.isExpired
import com.ruhaan.accolade.data.local.toJson
import com.ruhaan.accolade.data.remote.api.TmdbApiService
import com.ruhaan.accolade.domain.mapper.MovieDetailMapper
import com.ruhaan.accolade.domain.mapper.MovieMapper
import com.ruhaan.accolade.domain.mapper.SearchMapper
import com.ruhaan.accolade.domain.model.CastMember
import com.ruhaan.accolade.domain.model.CrewMember
import com.ruhaan.accolade.domain.model.MediaType
import com.ruhaan.accolade.domain.model.Movie
import com.ruhaan.accolade.domain.model.MovieDetail
import com.ruhaan.accolade.domain.model.PaginatedResult
import com.ruhaan.accolade.domain.model.Person
import com.ruhaan.accolade.domain.model.Review
import com.ruhaan.accolade.domain.model.SearchResult
import com.ruhaan.accolade.domain.repository.MovieRepository
import com.ruhaan.accolade.presentation.home.components.EditorsPicks
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@Singleton
class MovieRepositoryImpl
@Inject
constructor(
    private val apiService: TmdbApiService,
    private val movieListCacheDao: MovieListCacheDao,
    private val movieDetailCacheDao: MovieDetailCacheDao,
    private val creditsCacheDao: CreditsCacheDao,
    private val personCacheDao: PersonCacheDao,
    private val reviewsCacheDao: ReviewsCacheDao,
) : MovieRepository {

  // ── Date helpers ─────────────────────────────────────────────────────────

  private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

  private fun daysFromToday(days: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, days)
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
  }

  private val targetLanguages = "en|hi|ta|te|ml|kn"

  // ── Trending ─────────────────────────────────────────────────────────────

  override suspend fun getTrendingMovies(): List<Movie> =
      cachedOr(
          getCached = { movieListCacheDao.get("trending")?.json },
          isFresh = { cachedAt -> !isExpired(cachedAt, CacheConfig.TTL_TRENDING) },
          getCachedAt = { movieListCacheDao.get("trending")?.cachedAt },
          deserialize = { it.fromJson<List<Movie>>() },
          fetchRemote = {
            MovieMapper.mapFromTrendingList(apiService.getTrendingAll().results).take(6)
          },
          saveToCache = { movies ->
            movieListCacheDao.upsert(
                MovieListCacheEntity(
                    cacheKey = "trending",
                    json = movies.toJson(),
                    cachedAt = System.currentTimeMillis(),
                )
            )
          },
      )

  // ── Editor's Picks ───────────────────────────────────────────────────────

  override suspend fun getEditorsPicks(): List<Movie> =
      cachedOr(
          getCached = { movieListCacheDao.get("editors_picks")?.json },
          isFresh = { cachedAt -> !isExpired(cachedAt, CacheConfig.TTL_EDITORS_PICKS) },
          getCachedAt = { movieListCacheDao.get("editors_picks")?.cachedAt },
          deserialize = { it.fromJson<List<Movie>>() },
          fetchRemote = { fetchEditorsPicsFromNetwork() },
          saveToCache = { movies ->
            movieListCacheDao.upsert(
                MovieListCacheEntity(
                    cacheKey = "editors_picks",
                    json = movies.toJson(),
                    cachedAt = System.currentTimeMillis(),
                )
            )
          },
      )

  private suspend fun fetchEditorsPicsFromNetwork(): List<Movie> = coroutineScope {
    EditorsPicks.items
        .map { curated ->
          async {
            runCatching {
                  when (curated.mediaType) {
                    MediaType.MOVIE -> {
                      val dto = apiService.getMovieDetail(curated.tmdbId)
                      Movie(
                          id = dto.id,
                          title = dto.title,
                          year = dto.releaseDate?.take(4) ?: "N/A",
                          posterPath = "https://image.tmdb.org/t/p/w500${dto.posterPath ?: ""}",
                          backdropPath =
                              dto.backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" },
                          releaseDate = dto.releaseDate ?: "",
                          mediaType = MediaType.MOVIE,
                      )
                    }
                    MediaType.TV_SHOW -> {
                      val dto = apiService.getTvShowDetail(curated.tmdbId)
                      Movie(
                          id = dto.id,
                          title = dto.name,
                          year = dto.firstAirDate?.take(4) ?: "N/A",
                          posterPath = "https://image.tmdb.org/t/p/w500${dto.posterPath ?: ""}",
                          backdropPath =
                              dto.backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" },
                          releaseDate = dto.firstAirDate ?: "",
                          mediaType = MediaType.TV_SHOW,
                      )
                    }
                    MediaType.PERSON -> null
                  }
                }
                .getOrNull()
          }
        }
        .awaitAll()
        .filterNotNull()
  }

  // ── Paginated lists — cache page 1 only ──────────────────────────────────

  override suspend fun getPreviousMovies(page: Int): PaginatedResult<Movie> =
      cachedPaginatedList(
          page = page,
          cacheKey = "prev_movies",
          ttl = CacheConfig.TTL_PREVIOUS,
          fetchRemote = {
            val r =
                apiService.discoverMovies(
                    page = page,
                    minDate = daysFromToday(-30),
                    maxDate = today(),
                    minVoteCount = 10,
                    withOriginalLanguage = targetLanguages,
                )
            PaginatedResult(MovieMapper.mapFromDtoList(r.results), r.page, r.totalPages)
          },
      )

  override suspend fun getPreviousTvShows(page: Int): PaginatedResult<Movie> =
      cachedPaginatedList(
          page = page,
          cacheKey = "prev_tv",
          ttl = CacheConfig.TTL_PREVIOUS,
          fetchRemote = {
            val r =
                apiService.discoverTv(
                    page = page,
                    minDate = daysFromToday(-30),
                    maxDate = today(),
                    minVoteCount = 10,
                    withOriginalLanguage = targetLanguages,
                )
            PaginatedResult(MovieMapper.mapFromTvShowDtoList(r.results), r.page, r.totalPages)
          },
      )

  override suspend fun getThisWeekMovies(page: Int): PaginatedResult<Movie> =
      cachedPaginatedList(
          page = page,
          cacheKey = "this_week_movies",
          ttl = CacheConfig.TTL_THIS_WEEK,
          fetchRemote = {
            val r =
                apiService.discoverMovies(
                    page = page,
                    minDate = today(),
                    maxDate = daysFromToday(7),
                    minPopularity = 10f,
                    withOriginalLanguage = targetLanguages,
                )
            PaginatedResult(MovieMapper.mapFromDtoList(r.results), r.page, r.totalPages)
          },
      )

  override suspend fun getThisWeekTvShows(page: Int): PaginatedResult<Movie> =
      cachedPaginatedList(
          page = page,
          cacheKey = "this_week_tv",
          ttl = CacheConfig.TTL_THIS_WEEK,
          fetchRemote = {
            val r =
                apiService.discoverTv(
                    page = page,
                    minDate = today(),
                    maxDate = daysFromToday(7),
                    minPopularity = 10f,
                    withOriginalLanguage = targetLanguages,
                )
            PaginatedResult(MovieMapper.mapFromTvShowDtoList(r.results), r.page, r.totalPages)
          },
      )

  override suspend fun getUpcomingMovies(page: Int): PaginatedResult<Movie> =
      cachedPaginatedList(
          page = page,
          cacheKey = "upcoming_movies",
          ttl = CacheConfig.TTL_UPCOMING,
          fetchRemote = {
            val r =
                apiService.discoverMovies(
                    page = page,
                    minDate = daysFromToday(8),
                    withOriginalLanguage = targetLanguages,
                )
            PaginatedResult(MovieMapper.mapFromDtoList(r.results), r.page, r.totalPages)
          },
      )

  override suspend fun getUpcomingTvShows(page: Int): PaginatedResult<Movie> =
      cachedPaginatedList(
          page = page,
          cacheKey = "upcoming_tv",
          ttl = CacheConfig.TTL_UPCOMING,
          fetchRemote = {
            val r =
                apiService.discoverTv(
                    page = page,
                    minDate = daysFromToday(8),
                    withOriginalLanguage = targetLanguages,
                )
            PaginatedResult(MovieMapper.mapFromTvShowDtoList(r.results), r.page, r.totalPages)
          },
      )

  /**
   * Only page 1 is cached. Pages 2+ always hit the network (user is actively scrolling = online).
   */
  private suspend fun cachedPaginatedList(
      page: Int,
      cacheKey: String,
      ttl: Long,
      fetchRemote: suspend () -> PaginatedResult<Movie>,
  ): PaginatedResult<Movie> {
    if (page != 1) return fetchRemote()

    val key = "${cacheKey}_1"
    // Read once here (suspend context) so deserialize lambda doesn't need to
    val cachedEntity = movieListCacheDao.get(key)

    return cachedOr(
        getCached = { cachedEntity?.json },
        isFresh = { cachedAt -> !isExpired(cachedAt, ttl) },
        getCachedAt = { cachedEntity?.cachedAt },
        deserialize = {
          PaginatedResult(
              items = it.fromJson<List<Movie>>(),
              currentPage = cachedEntity?.currentPage ?: 1,
              totalPages = cachedEntity?.totalPages ?: 1,
          )
        },
        fetchRemote = fetchRemote,
        saveToCache = { result ->
          movieListCacheDao.upsert(
              MovieListCacheEntity(
                  cacheKey = key,
                  json = result.items.toJson(),
                  cachedAt = System.currentTimeMillis(),
                  currentPage = result.currentPage,
                  totalPages = result.totalPages,
              )
          )
        },
    )
  }

  // ── Movie Detail ─────────────────────────────────────────────────────────

  override suspend fun getMovieDetail(id: Int, mediaType: MediaType): MovieDetail {
    val key = "${mediaType.name}_$id"
    return cachedOr(
        getCached = { movieDetailCacheDao.get(key)?.json },
        isFresh = { cachedAt -> !isExpired(cachedAt, CacheConfig.TTL_DETAIL) },
        getCachedAt = { movieDetailCacheDao.get(key)?.cachedAt },
        deserialize = { it.fromJson<MovieDetail>() },
        fetchRemote = { fetchMovieDetailFromNetwork(id, mediaType) },
        saveToCache = { detail ->
          movieDetailCacheDao.upsert(
              MovieDetailCacheEntity(
                  cacheKey = key,
                  json = detail.toJson(),
                  cachedAt = System.currentTimeMillis(),
              )
          )
        },
    )
  }

  private suspend fun fetchMovieDetailFromNetwork(id: Int, mediaType: MediaType): MovieDetail =
      when (mediaType) {
        MediaType.MOVIE -> {
          val detail = apiService.getMovieDetail(id)
          val credits = apiService.getMovieCredits(id)
          val videos = apiService.getMovieVideos(id)
          MovieDetailMapper.mapMovieDetail(detail, credits, videos)
        }
        MediaType.TV_SHOW -> {
          val detail = apiService.getTvShowDetail(id)
          val credits = apiService.getTvShowCredits(id)
          val videos = apiService.getTvShowVideos(id)
          MovieDetailMapper.mapTvShowDetail(detail, credits, videos)
        }
        MediaType.PERSON -> error("Use getPersonDetail() for PERSON")
      }

  // ── Cast ─────────────────────────────────────────────────────────────────

  override suspend fun getCast(id: Int, mediaType: MediaType): List<CastMember> {
    val key = "cast_${mediaType.name}_$id"
    return cachedOr(
        getCached = { creditsCacheDao.get(key)?.json },
        isFresh = { cachedAt -> !isExpired(cachedAt, CacheConfig.TTL_CREDITS) },
        getCachedAt = { creditsCacheDao.get(key)?.cachedAt },
        deserialize = { it.fromJson<List<CastMember>>() },
        fetchRemote = {
          val credits =
              when (mediaType) {
                MediaType.MOVIE -> apiService.getMovieCredits(id)
                MediaType.TV_SHOW -> apiService.getTvShowCredits(id)
                MediaType.PERSON -> error("Person does not have cast")
              }
          MovieDetailMapper.mapCast(credits)
        },
        saveToCache = { cast ->
          creditsCacheDao.upsert(CreditsCacheEntity(key, cast.toJson(), System.currentTimeMillis()))
        },
    )
  }

  // ── Crew ─────────────────────────────────────────────────────────────────

  override suspend fun getCrew(id: Int, mediaType: MediaType): List<CrewMember> {
    val key = "crew_${mediaType.name}_$id"
    return cachedOr(
        getCached = { creditsCacheDao.get(key)?.json },
        isFresh = { cachedAt -> !isExpired(cachedAt, CacheConfig.TTL_CREDITS) },
        getCachedAt = { creditsCacheDao.get(key)?.cachedAt },
        deserialize = { it.fromJson<List<CrewMember>>() },
        fetchRemote = {
          val credits =
              when (mediaType) {
                MediaType.MOVIE -> apiService.getMovieCredits(id)
                MediaType.TV_SHOW -> apiService.getTvShowCredits(id)
                MediaType.PERSON -> error("Person does not have cast")
              }
          MovieDetailMapper.mapCrew(credits)
        },
        saveToCache = { crew ->
          creditsCacheDao.upsert(CreditsCacheEntity(key, crew.toJson(), System.currentTimeMillis()))
        },
    )
  }

  // ── Genre lists ──────────────────────────────────────────────────────────

  override suspend fun getMoviesByGenre(genreId: Int, page: Int): List<Movie> {
    if (page != 1) {
      return MovieMapper.mapFromDtoList(
          apiService
              .discoverMoviesByGenre(genreId = genreId, sortBy = "popularity.desc", page = page)
              .results
      )
    }
    val key = "genre_movie_${genreId}_1"
    return cachedOr(
        getCached = { movieListCacheDao.get(key)?.json },
        isFresh = { cachedAt -> !isExpired(cachedAt, CacheConfig.TTL_GENRE) },
        getCachedAt = { movieListCacheDao.get(key)?.cachedAt },
        deserialize = { it.fromJson<List<Movie>>() },
        fetchRemote = {
          MovieMapper.mapFromDtoList(
              apiService
                  .discoverMoviesByGenre(genreId = genreId, sortBy = "popularity.desc", page = 1)
                  .results
          )
        },
        saveToCache = { movies ->
          movieListCacheDao.upsert(
              MovieListCacheEntity(key, movies.toJson(), System.currentTimeMillis())
          )
        },
    )
  }

  override suspend fun getTvShowsByGenre(genreId: Int, page: Int): List<Movie> {
    if (page != 1) {
      return MovieMapper.mapFromTvShowDtoList(
          apiService
              .discoverTvShowsByGenre(genreId = genreId, sortBy = "popularity.desc", page = page)
              .results
      )
    }
    val key = "genre_tv_${genreId}_1"
    return cachedOr(
        getCached = { movieListCacheDao.get(key)?.json },
        isFresh = { cachedAt -> !isExpired(cachedAt, CacheConfig.TTL_GENRE) },
        getCachedAt = { movieListCacheDao.get(key)?.cachedAt },
        deserialize = { it.fromJson<List<Movie>>() },
        fetchRemote = {
          MovieMapper.mapFromTvShowDtoList(
              apiService
                  .discoverTvShowsByGenre(genreId = genreId, sortBy = "popularity.desc", page = 1)
                  .results
          )
        },
        saveToCache = { movies ->
          movieListCacheDao.upsert(
              MovieListCacheEntity(key, movies.toJson(), System.currentTimeMillis())
          )
        },
    )
  }

  // ── Search — intentionally NOT cached ────────────────────────────────────

  override suspend fun searchMulti(query: String): List<SearchResult> {
    val response = apiService.searchMulti(query = query, page = 1)
    return SearchMapper.mapSearchResults(response.results).take(20)
  }

  // ── Reviews ──────────────────────────────────────────────────────────────

  override suspend fun getReviews(id: Int, mediaType: MediaType): List<Review> {
    val key = "reviews_${mediaType.name}_$id"
    return cachedOr(
        getCached = { reviewsCacheDao.get(key)?.json },
        isFresh = { cachedAt -> !isExpired(cachedAt, CacheConfig.TTL_REVIEWS) },
        getCachedAt = { reviewsCacheDao.get(key)?.cachedAt },
        deserialize = { it.fromJson<List<Review>>() },
        fetchRemote = {
          val response =
              when (mediaType) {
                MediaType.MOVIE -> apiService.getMovieReviews(id)
                MediaType.TV_SHOW -> apiService.getTvShowReviews(id)
                MediaType.PERSON -> error("Person does not have reviews")
              }
          MovieDetailMapper.mapReviews(response)
        },
        saveToCache = { reviews ->
          reviewsCacheDao.upsert(
              ReviewsCacheEntity(key, reviews.toJson(), System.currentTimeMillis())
          )
        },
    )
  }

  // ── Person ───────────────────────────────────────────────────────────────

  override suspend fun getPersonDetail(personId: Int): Person {
    val key = "person_$personId"
    return cachedOr(
        getCached = { personCacheDao.get(key)?.json },
        isFresh = { cachedAt -> !isExpired(cachedAt, CacheConfig.TTL_PERSON) },
        getCachedAt = { personCacheDao.get(key)?.cachedAt },
        deserialize = { it.fromJson<Person>() },
        fetchRemote = {
          val dto = apiService.getPersonDetail(personId)
          Person(
              id = dto.id,
              name = dto.name,
              biography = dto.biography?.takeIf { it.isNotBlank() },
              profilePath = dto.profilePath?.let { "https://image.tmdb.org/t/p/w500$it" },
              birthday = dto.birthday,
              placeOfBirth = dto.placeOfBirth,
          )
        },
        saveToCache = { person ->
          personCacheDao.upsert(PersonCacheEntity(key, person.toJson(), System.currentTimeMillis()))
        },
    )
  }

  override suspend fun getPersonFilmography(personId: Int): List<Movie> {
    val key = "filmography_$personId"
    return cachedOr(
        getCached = { personCacheDao.get(key)?.json },
        isFresh = { cachedAt -> !isExpired(cachedAt, CacheConfig.TTL_FILMOGRAPHY) },
        getCachedAt = { personCacheDao.get(key)?.cachedAt },
        deserialize = { it.fromJson<List<Movie>>() },
        fetchRemote = {
          val response = apiService.getPersonCredits(personId)
          response.cast
              .filter { it.posterPath != null }
              .distinctBy { it.id }
              .sortedByDescending { it.releaseDate ?: it.firstAirDate ?: "" }
              .map { credit ->
                Movie(
                    id = credit.id,
                    title = credit.title ?: credit.name ?: "Unknown",
                    posterPath = "https://image.tmdb.org/t/p/w500${credit.posterPath}",
                    mediaType =
                        if (credit.mediaType == "tv") MediaType.TV_SHOW else MediaType.MOVIE,
                    year = (credit.releaseDate ?: credit.firstAirDate ?: "").take(4),
                )
              }
        },
        saveToCache = { filmography ->
          personCacheDao.upsert(
              PersonCacheEntity(key, filmography.toJson(), System.currentTimeMillis())
          )
        },
    )
  }
}
