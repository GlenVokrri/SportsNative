package com.example.sportsnative

import java.io.Serializable

data class Provider(
    val name: String,
    val channels: List<Channel>
) : Serializable
data class StreamSource(
    val title: String,
    val streamUrl: String,
    val referer: String = "https://daddylive.pk/"
) : Serializable

data class Channel(
    val name: String,
    val sources: List<StreamSource>
) : Serializable