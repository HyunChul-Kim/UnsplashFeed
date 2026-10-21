package com.hyunchul.unsplashfeed.data.remote.mapper

import com.hyunchul.unsplashfeed.data.remote.dto.UnsplashPhotoDto
import com.hyunchul.unsplashfeed.domain.model.Photo

fun UnsplashPhotoDto.toPhoto(): Photo {
    return Photo(
        id = id,
        description = description,
        altDescription = altDescription,
        width = width,
        height = height,
        thumbnailUrl = urls.small,
        regularUrl = urls.regular,
        fullUrl = urls.full,
        rawUrl = urls.raw,
        photographerName = user.name,
        photographerProfileUrl = user.profileImage.small,
        downloadLocation = links.downloadLocation,
    )
}