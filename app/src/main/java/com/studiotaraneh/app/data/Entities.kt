package com.studiotaraneh.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String = "",
    val style: String = "پاپ",
    val subStyle: String = "",
    val notes: String = "",
    val bpm: Int = 90,
    val timeSignature: String = "4/4",
    val favorite: Boolean = false,
    val deleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long = 0L,
    val drumPatternId: Long? = null,
    val drumEnabled: Boolean = false
)

@Entity(tableName = "sections")
data class SectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val position: Int,
    val type: String,
    val title: String,
    val content: String = "",
    val color: Long = 0xFFFFFFFF,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val textSize: Int = 18,
    val alignment: String = "start",
    val updatedAt: Long = System.currentTimeMillis(),
    val barCount: Int = 4,
    val drumPatternId: Long? = null
)

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val sectionId: Long? = null,
    val path: String,
    val title: String,
    val durationMs: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isMainTake: Boolean = false,
    val favorite: Boolean = false,
    val deleted: Boolean = false
)

@Entity(tableName = "song_versions")
data class SongVersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val snapshot: String,
    val createdAt: Long = System.currentTimeMillis()
)


@Entity(tableName = "drum_patterns")
data class DrumPatternEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val bpm: Int = 90,
    val timeSignature: String = "4/4",
    val beats: Int = 4,
    val stepsPerBeat: Int = 4,
    val kick: String = "",
    val snare: String = "",
    val hiHat: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)


@Entity(tableName = "styles")
data class StyleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parent: String = "",
    val isBuiltIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
