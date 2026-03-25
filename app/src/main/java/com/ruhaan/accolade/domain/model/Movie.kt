package com.ruhaan.accolade.domain.model
data class Movie(
    val id: Int,
    val title: String,
    val year: String,
    val posterPath: String,
    val backdropPath: String? = null,
    val releaseDate: String = "",
    val mediaType: MediaType = MediaType.MOVIE,
)

enum class MediaType {
  MOVIE,
  TV_SHOW,
  PERSON,
}
