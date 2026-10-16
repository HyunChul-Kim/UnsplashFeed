package com.hyunchul.unsplashfeed.data.remote.mapper

import com.hyunchul.unsplashfeed.data.remote.dto.UnsplashPhotoDto
import com.hyunchul.unsplashfeed.data.remote.dto.UnsplashPhotoLinksDto
import com.hyunchul.unsplashfeed.data.remote.dto.UnsplashPhotoUrlsDto
import com.hyunchul.unsplashfeed.data.remote.dto.UnsplashUserDto
import com.hyunchul.unsplashfeed.data.remote.dto.UnsplashUserProfileImageDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnsplashPhotoMapperTest {

    @Test
    fun toPhoto_mapsDtoToPhoto() {
        val dto = UnsplashPhotoDto(
            id = "photo-1",
            description = null,
            altDescription = "man in black crew neck t-shirt drinking coffee",
            width = 1000,
            height = 720,
            urls = UnsplashPhotoUrlsDto(
                thumb = "thumb-url",
                small = "small-url",
                regular = "regular-url",
                full = "full-url",
                raw = "raw-url",
            ),
            user = UnsplashUserDto(
                name = "Test User",
                username = "tester",
                profileImage = UnsplashUserProfileImageDto(
                    small = "profile-small-url",
                    medium = "profile-medium-url",
                    large = "profile-large-url",
                ),
            ),
            links = UnsplashPhotoLinksDto(
                downloadLocation = "download-url",
            )
        )

        val photo = dto.toPhoto()

        assertEquals("photo-1", photo.id)
        assertNull(photo.description)
        assertEquals("man in black crew neck t-shirt drinking coffee", photo.altDescription)
        assertEquals("small-url", photo.thumbnailUrl)
        assertEquals("regular-url", photo.regularUrl)
        assertEquals("full-url", photo.fullUrl)
        assertEquals("Test User", photo.photographerName)
        assertEquals("profile-small-url", photo.photographerProfileUrl)
        assertEquals("download-url", photo.downloadLocation)
    }
}