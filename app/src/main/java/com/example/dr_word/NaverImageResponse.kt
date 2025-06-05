package com.example.dr_word

data class NaverImageResponse(
    val items: List<ImageItem>
)

data class ImageItem(
    val title: String,
    val link: String,
    val thumbnail: String
)