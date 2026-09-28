package com.studiotaraneh.app.backup

import android.content.Context
import android.net.Uri
import com.studiotaraneh.app.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(private val context: Context, private val db: AppDatabase) {
    suspend fun exportTo(uri: Uri) = withContext(Dispatchers.IO) {
        val songs = db.songs().all()
        val sections = songs.flatMap { db.sections().list(it.id) }
        val recordings = songs.flatMap { db.recordings().list(it.id) }
        val versions = songs.flatMap { db.versions().list(it.id) }
        val drumPatterns = db.drumPatterns().all().first()
        val styles = db.styles().all().first()
        val root = JSONObject().apply {
            put("format", "studio-taraneh-backup")
            put("version", 3)
            put("createdAt", System.currentTimeMillis())
            put("songs", JSONArray().also { a -> songs.forEach { a.put(it.toJson()) } })
            put("sections", JSONArray().also { a -> sections.forEach { a.put(it.toJson()) } })
            put("recordings", JSONArray().also { a -> recordings.forEach { a.put(it.toJson()) } })
            put("versions", JSONArray().also { a -> versions.forEach { a.put(it.toJson()) } })
            put("drumPatterns", JSONArray().also { a -> drumPatterns.forEach { a.put(it.toJson()) } })
            put("styles", JSONArray().also { a -> styles.forEach { a.put(it.toJson()) } })
        }
        context.contentResolver.openOutputStream(uri)?.use { output ->
            ZipOutputStream(BufferedOutputStream(output)).use { zip ->
                zip.putNextEntry(ZipEntry("backup.json")); zip.write(root.toString().toByteArray()); zip.closeEntry()
                recordings.forEach { r ->
                    val file = File(r.path)
                    if (file.exists()) {
                        zip.putNextEntry(ZipEntry("audio/${file.name}"))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
        } ?: error("Unable to open backup destination")
    }

    suspend fun importFrom(uri: Uri) = withContext(Dispatchers.IO) {
        val temp = File(context.cacheDir, "studio_import_${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            var json: JSONObject? = null
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(BufferedInputStream(input)).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val normalized = entry.name.replace('\\', '/')
                        if (normalized.startsWith("/") || normalized.split('/').any { it == ".." }) {
                            throw IllegalArgumentException("Unsafe backup entry")
                        }
                        val out = File(temp, normalized)
                        val base = temp.canonicalFile
                        val target = out.canonicalFile
                        if (!target.path.startsWith(base.path + File.separator) && target != base) {
                            throw IllegalArgumentException("Unsafe backup path")
                        }
                        if (entry.isDirectory) out.mkdirs() else {
                            out.parentFile?.mkdirs()
                            out.outputStream().use { zip.copyTo(it) }
                            if (normalized == "backup.json") json = JSONObject(out.readText())
                        }
                        zip.closeEntry(); entry = zip.nextEntry
                    }
                }
            } ?: error("Unable to open backup")
            val root = json ?: error("Invalid backup: backup.json missing")
            if (root.optString("format") != "studio-taraneh-backup") error("Unsupported backup format")
            val songMap = mutableMapOf<Long, Long>()
            val sectionMap = mutableMapOf<Long, Long>()
            val patternMap = mutableMapOf<Long, Long>()
            val songs = root.getJSONArray("songs")
            for (i in 0 until songs.length()) {
                val s = songs.getJSONObject(i)
                val newId = db.songs().insert(s.toSong().copy(id = 0, drumPatternId = null, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
                songMap[s.getLong("id")] = newId
            }
            val sections = root.getJSONArray("sections")
            for (i in 0 until sections.length()) {
                val s = sections.getJSONObject(i); val newSong = songMap[s.getLong("songId")] ?: continue
                val newId = db.sections().insert(s.toSection().copy(id = 0, songId = newSong, drumPatternId = null))
                sectionMap[s.getLong("id")] = newId
            }
            val recordings = root.getJSONArray("recordings")
            for (i in 0 until recordings.length()) {
                val r = recordings.getJSONObject(i); val newSong = songMap[r.getLong("songId")] ?: continue
                val oldPath = File(r.getString("path")); val audioName = oldPath.name
                val extracted = File(temp, "audio/$audioName")
                val target = File(context.filesDir, "recordings").apply { mkdirs() }.resolve("${System.currentTimeMillis()}_$audioName")
                if (extracted.exists()) extracted.copyTo(target, overwrite = true)
                db.recordings().insert(r.toRecording().copy(id = 0, songId = newSong, sectionId = r.optLong("sectionId").takeIf { !r.isNull("sectionId") }?.let { sectionMap[it] }, path = if (target.exists()) target.absolutePath else r.getString("path"), isMainTake=r.optBoolean("isMainTake",false), favorite=r.optBoolean("favorite",false), deleted=r.optBoolean("deleted",false)))
            }
            val versions = root.getJSONArray("versions")
            for (i in 0 until versions.length()) {
                val v = versions.getJSONObject(i); val newSong = songMap[v.getLong("songId")] ?: continue
                db.versions().insert(v.toVersion().copy(id = 0, songId = newSong))
            }

            val styleArray = root.optJSONArray("styles")
            if (styleArray != null) {
                for (i in 0 until styleArray.length()) {
                    val style = styleArray.getJSONObject(i)
                    val name = style.optString("name").trim()
                    if (name.isNotBlank()) db.styles().insert(style.toStyle().copy(id=0))
                }
            }

            val patterns = root.optJSONArray("drumPatterns")
            if (patterns != null) {
                for (i in 0 until patterns.length()) {
                    val pattern = patterns.getJSONObject(i)
                    val newId = db.drumPatterns().insert(pattern.toDrumPattern().copy(id = 0))
                    patternMap[pattern.getLong("id")] = newId
                }
            }
            for (i in 0 until sections.length()) {
                val s = sections.getJSONObject(i)
                val newSection = sectionMap[s.getLong("id")] ?: continue
                val oldPatternId = if (s.isNull("drumPatternId")) null else s.optLong("drumPatternId")
                val newPatternId = oldPatternId?.let { patternMap[it] }
                if (newPatternId != null) {
                    val currentSections = db.sections().list(songMap[s.getLong("songId")] ?: continue)
                    val current = currentSections.firstOrNull { it.id == newSection } ?: continue
                    db.sections().update(current.copy(drumPatternId = newPatternId))
                }
            }
            for (i in 0 until songs.length()) {
                val s = songs.getJSONObject(i)
                val newSong = songMap[s.getLong("id")] ?: continue
                val oldPatternId = if (s.isNull("drumPatternId")) null else s.optLong("drumPatternId")
                val newPatternId = oldPatternId?.let { patternMap[it] }
                val current = db.songs().get(newSong) ?: continue
                db.songs().update(current.copy(drumPatternId = newPatternId, drumEnabled = s.optBoolean("drumEnabled", false)))
            }
        } finally { temp.deleteRecursively() }
    }
}

private fun SongEntity.toJson() = JSONObject().apply { put("id",id);put("title",title);put("artist",artist);put("style",style);put("subStyle",subStyle);put("notes",notes);put("bpm",bpm);put("timeSignature",timeSignature);put("favorite",favorite);put("deleted",deleted);put("createdAt",createdAt);put("updatedAt",updatedAt);put("lastOpenedAt",lastOpenedAt);put("drumPatternId",drumPatternId ?: JSONObject.NULL);put("drumEnabled",drumEnabled) }
private fun SectionEntity.toJson() = JSONObject().apply { put("id",id);put("songId",songId);put("position",position);put("type",type);put("title",title);put("content",content);put("color",color);put("bold",bold);put("italic",italic);put("textSize",textSize);put("alignment",alignment);put("updatedAt",updatedAt);put("barCount",barCount);put("drumPatternId",drumPatternId ?: JSONObject.NULL) }
private fun RecordingEntity.toJson() = JSONObject().apply { put("id",id);put("songId",songId);put("sectionId",sectionId ?: JSONObject.NULL);put("path",path);put("title",title);put("durationMs",durationMs);put("createdAt",createdAt);put("isMainTake",isMainTake);put("favorite",favorite);put("deleted",deleted) }
private fun SongVersionEntity.toJson() = JSONObject().apply { put("id",id);put("songId",songId);put("snapshot",snapshot);put("createdAt",createdAt) }
private fun DrumPatternEntity.toJson() = JSONObject().apply {
    put("id",id); put("name",name); put("bpm",bpm); put("timeSignature",timeSignature)
    put("beats",beats); put("stepsPerBeat",stepsPerBeat); put("kick",kick)
    put("snare",snare); put("hiHat",hiHat); put("createdAt",createdAt); put("updatedAt",updatedAt)
}
private fun JSONObject.toSong() = SongEntity(id=getLong("id"),title=getString("title"),artist=optString("artist"),style=optString("style","پاپ"),subStyle=optString("subStyle"),notes=optString("notes"),bpm=optInt("bpm",90),timeSignature=optString("timeSignature","4/4"),favorite=optBoolean("favorite"),deleted=optBoolean("deleted"),createdAt=optLong("createdAt"),updatedAt=optLong("updatedAt"),lastOpenedAt=optLong("lastOpenedAt",0),drumPatternId=if(isNull("drumPatternId")) null else optLong("drumPatternId"),drumEnabled=optBoolean("drumEnabled"))
private fun JSONObject.toSection() = SectionEntity(id=getLong("id"),songId=getLong("songId"),position=getInt("position"),type=getString("type"),title=getString("title"),content=optString("content"),color=optLong("color",0xFFFFFFFF),bold=optBoolean("bold",false),italic=optBoolean("italic",false),textSize=optInt("textSize",18).coerceIn(12,32),alignment=optString("alignment","start"),updatedAt=optLong("updatedAt"),barCount=optInt("barCount",4).coerceIn(1,256),drumPatternId=if(isNull("drumPatternId")) null else optLong("drumPatternId"))
private fun JSONObject.toRecording() = RecordingEntity(id=getLong("id"),songId=getLong("songId"),sectionId=if (isNull("sectionId")) null else getLong("sectionId"),path=getString("path"),title=getString("title"),durationMs=optLong("durationMs"),createdAt=optLong("createdAt"),isMainTake=optBoolean("isMainTake",false),favorite=optBoolean("favorite",false),deleted=optBoolean("deleted",false))
private fun JSONObject.toVersion() = SongVersionEntity(id=getLong("id"),songId=getLong("songId"),snapshot=getString("snapshot"),createdAt=optLong("createdAt"))
private fun JSONObject.toDrumPattern() = DrumPatternEntity(
    id=getLong("id"),
    name=getString("name"),
    bpm=optInt("bpm",90),
    timeSignature=optString("timeSignature","4/4"),
    beats=optInt("beats",4),
    stepsPerBeat=optInt("stepsPerBeat",4),
    kick=optString("kick"),
    snare=optString("snare"),
    hiHat=optString("hiHat"),
    createdAt=optLong("createdAt"),
    updatedAt=optLong("updatedAt")
)

private fun StyleEntity.toJson() = JSONObject().apply { put("id",id); put("name",name); put("parent",parent); put("isBuiltIn",isBuiltIn); put("createdAt",createdAt) }
private fun JSONObject.toStyle() = StyleEntity(id=getLong("id"),name=getString("name"),parent=optString("parent"),isBuiltIn=optBoolean("isBuiltIn",false),createdAt=optLong("createdAt",System.currentTimeMillis()))
