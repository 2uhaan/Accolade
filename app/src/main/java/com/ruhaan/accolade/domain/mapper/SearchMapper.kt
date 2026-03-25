package com.ruhaan.accolade.domain.mapper

import com.ruhaan.accolade.data.remote.dto.SearchResultDto
import com.ruhaan.accolade.domain.model.MediaType
import com.ruhaan.accolade.domain.model.SearchResult

object SearchMapper {

  fun mapSearchResults(dtos: List<SearchResultDto>): List<SearchResult> {
    return dtos
        .filter {
          it.mediaType == "movie" || it.mediaType == "tv" || it.mediaType == "person"
        } // Include people
        .map { mapSearchResult(it) }
        .sortedByDescending { it.popularity }
  }

  private fun mapSearchResult(dto: SearchResultDto): SearchResult {
    val title = dto.title ?: dto.name ?: "Unknown"
    val year =
        when (dto.mediaType) {
          "movie" -> dto.releaseDate?.take(4) ?: "N/A"
          "tv" -> dto.firstAirDate?.take(4) ?: "N/A"
          "person" -> "" // No year for people
          else -> "N/A"
        }
    val mediaType =
        when (dto.mediaType) {
          "movie" -> MediaType.MOVIE
          "tv" -> MediaType.TV_SHOW
          "person" -> MediaType.PERSON
          else -> MediaType.MOVIE
        }

    return SearchResult(
        id = dto.id,
        title = title,
        year = year,
        mediaType = mediaType,
        popularity = dto.popularity,
    )
  }
}
