package com.studiotaraneh.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao interface SongDao {
    @Query("SELECT * FROM songs WHERE deleted = 0 ORDER BY updatedAt DESC") fun active(): Flow<List<SongEntity>>
    @Query("SELECT * FROM songs WHERE deleted = 0 AND favorite = 1 ORDER BY updatedAt DESC") fun favorites(): Flow<List<SongEntity>>
    @Query("SELECT * FROM songs WHERE deleted = 0 ORDER BY CASE WHEN lastOpenedAt > 0 THEN lastOpenedAt ELSE updatedAt END DESC LIMIT :limit") fun recent(limit: Int): Flow<List<SongEntity>>
    @Query("SELECT * FROM songs WHERE deleted = 1 ORDER BY updatedAt DESC") fun trash(): Flow<List<SongEntity>>
    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1") suspend fun get(id: Long): SongEntity?
    @Query("SELECT * FROM songs ORDER BY id ASC") suspend fun all(): List<SongEntity>
    @Insert suspend fun insert(song: SongEntity): Long
    @Update suspend fun update(song: SongEntity)
    @Query("UPDATE songs SET deleted=1, updatedAt=:now WHERE id=:id") suspend fun moveToTrash(id: Long, now: Long = System.currentTimeMillis())
    @Query("UPDATE songs SET deleted=0, updatedAt=:now WHERE id=:id") suspend fun restore(id: Long, now: Long = System.currentTimeMillis())
    @Query("DELETE FROM recordings WHERE songId=:id") suspend fun deleteRecordingsForSong(id: Long)
    @Query("DELETE FROM sections WHERE songId=:id") suspend fun deleteSectionsForSong(id: Long)
    @Query("DELETE FROM song_versions WHERE songId=:id") suspend fun deleteVersionsForSong(id: Long)
    @Query("DELETE FROM songs WHERE id=:id") suspend fun deleteForever(id: Long)
    @Query("UPDATE songs SET lastOpenedAt=:now WHERE id=:id") suspend fun markOpened(id: Long, now: Long = System.currentTimeMillis())
    @Query("UPDATE songs SET favorite=:value, updatedAt=:now WHERE id=:id") suspend fun setFavorite(id: Long, value: Boolean, now: Long = System.currentTimeMillis())
    @Query("UPDATE songs SET drumPatternId=:patternId, drumEnabled=:enabled, updatedAt=:now WHERE id=:id") suspend fun setDrumPattern(id: Long, patternId: Long?, enabled: Boolean, now: Long = System.currentTimeMillis())
    @Query("SELECT * FROM songs WHERE deleted=0 AND (title LIKE '%' || :q || '%' OR style LIKE '%' || :q || '%' OR subStyle LIKE '%' || :q || '%' OR artist LIKE '%' || :q || '%' OR notes LIKE '%' || :q || '%' OR EXISTS (SELECT 1 FROM sections WHERE sections.songId=songs.id AND (sections.title LIKE '%' || :q || '%' OR sections.content LIKE '%' || :q || '%'))) ORDER BY updatedAt DESC") fun search(q: String): Flow<List<SongEntity>>
}

@Dao interface SectionDao {
    @Query("SELECT * FROM sections WHERE songId=:songId ORDER BY position ASC, id ASC") fun forSong(songId: Long): Flow<List<SectionEntity>>
    @Query("SELECT * FROM sections WHERE songId=:songId ORDER BY position ASC, id ASC") suspend fun list(songId: Long): List<SectionEntity>
    @Insert suspend fun insert(section: SectionEntity): Long
    @Update suspend fun update(section: SectionEntity)
    @Query("DELETE FROM sections WHERE id=:id") suspend fun delete(id: Long)
    @Query("DELETE FROM sections WHERE songId=:songId") suspend fun deleteForSong(songId: Long)
    @Query("UPDATE sections SET color=:color, updatedAt=:now WHERE id=:id") suspend fun setColor(id: Long, color: Long, now: Long = System.currentTimeMillis())
    @Query("UPDATE sections SET position=:position, updatedAt=:now WHERE id=:id") suspend fun setPosition(id: Long, position: Int, now: Long = System.currentTimeMillis())
}

@Dao interface RecordingDao {
    @Query("SELECT * FROM recordings WHERE songId=:songId ORDER BY createdAt DESC") fun forSong(songId: Long): Flow<List<RecordingEntity>>
    @Query("SELECT * FROM recordings WHERE songId=:songId ORDER BY createdAt ASC") suspend fun list(songId: Long): List<RecordingEntity>
    @Insert suspend fun insert(recording: RecordingEntity): Long
    @Query("DELETE FROM recordings WHERE id=:id") suspend fun delete(id: Long)
    @Query("SELECT * FROM recordings WHERE id=:id LIMIT 1") suspend fun get(id: Long): RecordingEntity?
    @Query("SELECT * FROM recordings WHERE songId=:songId AND deleted=1 ORDER BY createdAt DESC") fun trashForSong(songId: Long): Flow<List<RecordingEntity>>
    @Query("UPDATE recordings SET title=:title WHERE id=:id") suspend fun rename(id: Long,title: String)
    @Query("UPDATE recordings SET favorite=:value WHERE id=:id") suspend fun setFavorite(id: Long,value: Boolean)
    @Query("UPDATE recordings SET isMainTake=0 WHERE songId=:songId") suspend fun clearMainForSong(songId: Long)
    @Query("UPDATE recordings SET isMainTake=0 WHERE songId=:songId AND sectionId=:sectionId") suspend fun clearMainForSection(songId: Long,sectionId: Long)
    @Query("UPDATE recordings SET isMainTake=:value WHERE id=:id") suspend fun setMain(id: Long,value: Boolean)
    @Query("UPDATE recordings SET deleted=1 WHERE id=:id") suspend fun softDelete(id: Long)
    @Query("UPDATE recordings SET deleted=0 WHERE id=:id") suspend fun restore(id: Long)
}

@Dao interface VersionDao {
    @Query("SELECT * FROM song_versions WHERE songId=:songId ORDER BY createdAt DESC") fun forSong(songId: Long): Flow<List<SongVersionEntity>>
    @Query("SELECT * FROM song_versions WHERE songId=:songId ORDER BY createdAt ASC") suspend fun list(songId: Long): List<SongVersionEntity>
    @Insert suspend fun insert(version: SongVersionEntity): Long
    @Query("SELECT * FROM song_versions WHERE id=:id LIMIT 1") suspend fun get(id: Long): SongVersionEntity?
    @Query("DELETE FROM song_versions WHERE songId=:songId") suspend fun deleteForSong(songId: Long)
}


@Dao interface DrumPatternDao {
    @Query("SELECT * FROM drum_patterns ORDER BY updatedAt DESC")
    fun all(): Flow<List<DrumPatternEntity>>

    @Query("SELECT * FROM drum_patterns WHERE id=:id LIMIT 1")
    suspend fun get(id: Long): DrumPatternEntity?

    @Insert
    suspend fun insert(pattern: DrumPatternEntity): Long

    @Update
    suspend fun update(pattern: DrumPatternEntity)

    @Query("DELETE FROM drum_patterns WHERE id=:id")
    suspend fun delete(id: Long)
}


@Dao
interface StyleDao {
    @Query("SELECT * FROM styles ORDER BY isBuiltIn DESC, parent ASC, name ASC")
    fun all(): Flow<List<StyleEntity>>
    @Insert suspend fun insert(style: StyleEntity): Long
    @Query("DELETE FROM styles WHERE id=:id AND isBuiltIn=0") suspend fun delete(id: Long)
    @Query("SELECT COUNT(*) FROM styles") suspend fun count(): Int
}
