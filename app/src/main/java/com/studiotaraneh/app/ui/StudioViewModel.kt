package com.studiotaraneh.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studiotaraneh.app.StudioTaranehApplication
import com.studiotaraneh.app.data.*
import com.studiotaraneh.app.backup.BackupManager
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import com.studiotaraneh.app.settings.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StudioViewModel(app: Application) : AndroidViewModel(app) {
    private val undoStacks = mutableMapOf<Long, ArrayDeque<SectionEntity>>()
    private val redoStacks = mutableMapOf<Long, ArrayDeque<SectionEntity>>()
    private val db = (app as StudioTaranehApplication).database
    private val backup = BackupManager(app, db)
    val settingsRepo = SettingsRepository(app)
    val settings = settingsRepo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.studiotaraneh.app.settings.AppSettings())
    val songs = db.songs().active().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favorites = db.songs().favorites().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recent = db.songs().recent(20).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val trash = db.songs().trash().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun createSong(title:String, style:String, bpm:Int=90, meter:String="4/4", onCreated:(Long)->Unit) = viewModelScope.launch { val id=db.songs().insert(SongEntity(title=title.ifBlank{"ترانه بدون عنوان"}, style=style, bpm=bpm, timeSignature=meter)); db.sections().insert(SectionEntity(songId=id,position=0,type="INTRO",title="Intro")); saveVersion(id); onCreated(id) }
    fun song(id:Long)=flow { emit(db.songs().get(id)) }
    fun markSongOpened(id: Long) = viewModelScope.launch { db.songs().markOpened(id) }
    fun updateSong(song:SongEntity, snapshot:Boolean=true)=viewModelScope.launch { db.songs().update(song.copy(updatedAt=System.currentTimeMillis())); if(snapshot) saveVersion(song.id) }
    fun addSection(songId:Long,type:String,title:String=type)=viewModelScope.launch { val current=db.sections().list(songId); db.sections().insert(SectionEntity(songId=songId,position=current.size,type=type,title=title,barCount=4)); saveVersion(songId) }
    fun updateSection(section:SectionEntity)=viewModelScope.launch { db.sections().update(section.copy(updatedAt=System.currentTimeMillis())); db.songs().get(section.songId)?.let { db.songs().update(it.copy(updatedAt=System.currentTimeMillis())) } }
    fun editSection(section:SectionEntity, newText:String)=viewModelScope.launch {
        if (newText == section.content) return@launch
        val stack=undoStacks.getOrPut(section.id){ArrayDeque()}; stack.addLast(section); while(stack.size>100) stack.removeFirst()
        redoStacks.getOrPut(section.id){ArrayDeque()}.clear()
        db.sections().update(section.copy(content=newText,updatedAt=System.currentTimeMillis()))
        db.songs().get(section.songId)?.let { db.songs().update(it.copy(updatedAt=System.currentTimeMillis())) }
    }
    fun undoSection(section:SectionEntity)=viewModelScope.launch {
        val stack=undoStacks[section.id] ?: return@launch; val previous=stack.removeLastOrNull() ?: return@launch
        redoStacks.getOrPut(section.id){ArrayDeque()}.addLast(section); db.sections().update(previous.copy(updatedAt=System.currentTimeMillis())); db.songs().get(section.songId)?.let { db.songs().update(it.copy(updatedAt=System.currentTimeMillis())) }; saveVersion(section.songId)
    }
    fun redoSection(section:SectionEntity)=viewModelScope.launch {
        val stack=redoStacks[section.id] ?: return@launch; val next=stack.removeLastOrNull() ?: return@launch
        undoStacks.getOrPut(section.id){ArrayDeque()}.addLast(section); db.sections().update(next.copy(updatedAt=System.currentTimeMillis())); db.songs().get(section.songId)?.let { db.songs().update(it.copy(updatedAt=System.currentTimeMillis())) }; saveVersion(section.songId)
    }
    fun formatSection(section:SectionEntity,bold:Boolean=section.bold,italic:Boolean=section.italic,textSize:Int=section.textSize,alignment:String=section.alignment)=viewModelScope.launch {
        db.sections().update(section.copy(bold=bold,italic=italic,textSize=textSize.coerceIn(12,32),alignment=alignment,updatedAt=System.currentTimeMillis())); db.songs().get(section.songId)?.let { db.songs().update(it.copy(updatedAt=System.currentTimeMillis())) }; saveVersion(section.songId)
    }
    fun setSectionColor(section:SectionEntity,color:Long)=viewModelScope.launch { db.sections().setColor(section.id,color); db.songs().get(section.songId)?.let { db.songs().update(it.copy(updatedAt=System.currentTimeMillis())) } }
    fun reorderSections(songId:Long, orderedIds:List<Long>)=viewModelScope.launch {
        orderedIds.forEachIndexed { index, sectionId -> db.sections().setPosition(sectionId, index) }
        db.songs().get(songId)?.let { db.songs().update(it.copy(updatedAt=System.currentTimeMillis())) }
        saveVersion(songId)
    }
    fun setSectionTimeline(section:SectionEntity, barCount:Int, patternId:Long?)=viewModelScope.launch {
        db.sections().update(section.copy(barCount=barCount.coerceIn(1,256), drumPatternId=patternId, updatedAt=System.currentTimeMillis()))
        db.songs().get(section.songId)?.let { db.songs().update(it.copy(updatedAt=System.currentTimeMillis())) }
        saveVersion(section.songId)
    }
    fun deleteSection(section:SectionEntity)=viewModelScope.launch { db.sections().delete(section.id); saveVersion(section.songId) }
    fun sections(songId:Long)=db.sections().forSong(songId)
    fun recordings(songId:Long)=db.recordings().forSong(songId)
    fun deletedRecordings(songId:Long)=db.recordings().trashForSong(songId)
    val styles = db.styles().all().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    fun ensureDefaultStyles() = viewModelScope.launch {
        if (db.styles().count() == 0) {
            listOf(
                StyleEntity(name="پاپ", isBuiltIn=true), StyleEntity(name="پاپ عاشقانه", parent="پاپ", isBuiltIn=true),
                StyleEntity(name="پاپ احساسی", parent="پاپ", isBuiltIn=true), StyleEntity(name="پاپ غمگین", parent="پاپ", isBuiltIn=true),
                StyleEntity(name="پاپ شاد", parent="پاپ", isBuiltIn=true), StyleEntity(name="رپ", isBuiltIn=true),
                StyleEntity(name="Trap", parent="رپ", isBuiltIn=true), StyleEntity(name="Drill", parent="رپ", isBuiltIn=true),
                StyleEntity(name="Old School", parent="رپ", isBuiltIn=true), StyleEntity(name="Underground", parent="رپ", isBuiltIn=true),
                StyleEntity(name="Conscious", parent="رپ", isBuiltIn=true), StyleEntity(name="Melodic Rap", parent="رپ", isBuiltIn=true)
            ).forEach { db.styles().insert(it) }
        }
    }
    fun addCustomStyle(name: String, parent: String = "") = viewModelScope.launch {
        val value = name.trim()
        if (value.isNotBlank()) db.styles().insert(StyleEntity(name=value, parent=parent.trim(), isBuiltIn=false))
    }
    fun deleteCustomStyle(id: Long) = viewModelScope.launch { db.styles().delete(id) }

    val drumPatterns = db.drumPatterns().all().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    fun saveDrumPattern(
        name:String,
        bpm:Int,
        meter:String,
        beats:Int,
        kick:String,
        snare:String,
        hiHat:String
    ) = viewModelScope.launch {
        val safeName = name.trim().ifBlank { "الگوی ریتم ${System.currentTimeMillis()}" }
        db.drumPatterns().insert(
            DrumPatternEntity(
                name = safeName,
                bpm = bpm.coerceIn(40, 220),
                timeSignature = meter,
                beats = beats.coerceIn(1, 16),
                stepsPerBeat = 4,
                kick = kick,
                snare = snare,
                hiHat = hiHat
            )
        )
    }

    fun updateDrumPattern(pattern:DrumPatternEntity) = viewModelScope.launch {
        db.drumPatterns().update(pattern.copy(updatedAt = System.currentTimeMillis()))
    }

    fun deleteDrumPattern(id:Long) = viewModelScope.launch {
        db.drumPatterns().delete(id)
    }

    fun versions(songId:Long)=db.versions().forSong(songId)
    fun addRecording(songId:Long,path:String,title:String,sectionId:Long?=null,durationMs:Long=0)=viewModelScope.launch {
        db.recordings().insert(RecordingEntity(songId=songId,sectionId=sectionId,path=path,title=title,durationMs=durationMs))
        saveVersion(songId)
    }
    fun renameRecording(recording:RecordingEntity,title:String)=viewModelScope.launch {
        db.recordings().rename(recording.id,title.trim().ifBlank { recording.title }); saveVersion(recording.songId)
    }
    fun setRecordingFavorite(recording:RecordingEntity,value:Boolean)=viewModelScope.launch { db.recordings().setFavorite(recording.id,value) }
    fun setMainTake(recording:RecordingEntity)=viewModelScope.launch {
        if (recording.sectionId == null) db.recordings().clearMainForSong(recording.songId) else db.recordings().clearMainForSection(recording.songId, recording.sectionId)
        db.recordings().setMain(recording.id,true)
        saveVersion(recording.songId)
    }
    fun deleteRecording(recording:RecordingEntity)=viewModelScope.launch {
        db.recordings().softDelete(recording.id); saveVersion(recording.songId)
    }
    fun restoreRecording(recording:RecordingEntity)=viewModelScope.launch { db.recordings().restore(recording.id); saveVersion(recording.songId) }
    fun deleteRecordingForever(recording:RecordingEntity)=viewModelScope.launch {
        db.recordings().delete(recording.id); runCatching { java.io.File(recording.path).delete() }; saveVersion(recording.songId)
    }
    fun moveToTrash(id:Long)=viewModelScope.launch { db.songs().moveToTrash(id) }
    fun restore(id:Long)=viewModelScope.launch { db.songs().restore(id) }
    fun deleteForever(id:Long)=viewModelScope.launch { db.songs().deleteRecordingsForSong(id); db.songs().deleteSectionsForSong(id); db.songs().deleteVersionsForSong(id); db.songs().deleteForever(id) }
    fun setFavorite(id:Long,value:Boolean)=viewModelScope.launch { db.songs().setFavorite(id,value) }
    fun search(q:String)=db.songs().search(q)
    fun setSongDrumPattern(songId:Long, patternId:Long?, enabled:Boolean)=viewModelScope.launch { db.songs().setDrumPattern(songId, patternId, enabled); saveVersion(songId) }
    fun saveVersion(id:Long)=viewModelScope.launch {
        val s=db.songs().get(id) ?: return@launch
        val sections=db.sections().list(id)
        val snapshot=JSONObject().apply {
            put("song", JSONObject().apply { put("title",s.title);put("artist",s.artist);put("style",s.style);put("subStyle",s.subStyle);put("notes",s.notes);put("bpm",s.bpm);put("timeSignature",s.timeSignature);put("drumPatternId",s.drumPatternId ?: JSONObject.NULL);put("drumEnabled",s.drumEnabled) })
            put("sections", JSONArray().also { a -> sections.forEach { x -> a.put(JSONObject().apply { put("position",x.position);put("type",x.type);put("title",x.title);put("content",x.content);put("color",x.color);put("bold",x.bold);put("italic",x.italic);put("textSize",x.textSize);put("alignment",x.alignment);put("barCount",x.barCount);put("drumPatternId",x.drumPatternId ?: JSONObject.NULL) }) } })
            put("recordings", JSONArray().also { a -> db.recordings().list(id).forEach { r -> a.put(JSONObject().apply { put("id",r.id);put("sectionId",r.sectionId ?: JSONObject.NULL);put("title",r.title);put("durationMs",r.durationMs);put("isMainTake",r.isMainTake);put("favorite",r.favorite);put("deleted",r.deleted) }) } })
        }.toString()
        db.versions().insert(SongVersionEntity(songId=id,snapshot=snapshot))
    }
    fun restoreVersion(versionId:Long)=viewModelScope.launch {
        val v=db.versions().get(versionId) ?: return@launch
        val root=runCatching { JSONObject(v.snapshot) }.getOrNull() ?: return@launch
        val old=db.songs().get(v.songId) ?: return@launch
        val so=root.getJSONObject("song")
        db.songs().update(old.copy(title=so.optString("title",old.title),artist=so.optString("artist",old.artist),style=so.optString("style",old.style),subStyle=so.optString("subStyle",old.subStyle),notes=so.optString("notes",old.notes),bpm=so.optInt("bpm",old.bpm),timeSignature=so.optString("timeSignature",old.timeSignature),drumPatternId=if(so.isNull("drumPatternId")) null else so.optLong("drumPatternId"),drumEnabled=so.optBoolean("drumEnabled",old.drumEnabled),updatedAt=System.currentTimeMillis()))
        db.sections().deleteForSong(v.songId)
        val arr=root.getJSONArray("sections")
        for(i in 0 until arr.length()) { val x=arr.getJSONObject(i); db.sections().insert(SectionEntity(songId=v.songId,position=x.optInt("position",i),type=x.optString("type","VERSE"),title=x.optString("title","Verse"),content=x.optString("content"),color=x.optLong("color",0xFFFFFFFF),bold=x.optBoolean("bold",false),italic=x.optBoolean("italic",false),textSize=x.optInt("textSize",18).coerceIn(12,32),alignment=x.optString("alignment","start"),updatedAt=System.currentTimeMillis(),barCount=x.optInt("barCount",4).coerceIn(1,256),drumPatternId=if(x.isNull("drumPatternId")) null else x.optLong("drumPatternId"))) }
        saveVersion(v.songId)
    }
    fun exportBackup(uri:Uri)=viewModelScope.launch { backup.exportTo(uri) }
    fun exportSongText(songId:Long, uri:Uri)=viewModelScope.launch {
        val s=db.songs().get(songId) ?: return@launch
        val sections=db.sections().list(songId)
        val text=buildString {
            append(s.title).append("\n")
            if(s.artist.isNotBlank()) append("هنرمند: ").append(s.artist).append("\n")
            append("سبک: ").append(s.style).append(" • ").append(s.timeSignature).append(" • ").append(s.bpm).append(" BPM\n\n")
            sections.forEach { append("[ ").append(it.title).append(" ]\n").append(it.content).append("\n\n") }
            if(s.notes.isNotBlank()) append("یادداشت‌ها\n").append(s.notes).append("\n")
        }
        getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }
    fun exportSongJson(songId:Long, uri:Uri)=viewModelScope.launch {
        val s=db.songs().get(songId) ?: return@launch
        val sections=db.sections().list(songId)
        val root=JSONObject().apply {
            put("format","studio-taraneh-song"); put("version",1); put("song",JSONObject().apply {
                put("title",s.title); put("artist",s.artist); put("style",s.style); put("subStyle",s.subStyle); put("notes",s.notes); put("bpm",s.bpm); put("timeSignature",s.timeSignature)
            })
            put("sections",JSONArray().also { a -> sections.forEach { x -> a.put(JSONObject().apply { put("position",x.position);put("type",x.type);put("title",x.title);put("content",x.content);put("color",x.color);put("bold",x.bold);put("italic",x.italic);put("textSize",x.textSize);put("alignment",x.alignment);put("barCount",x.barCount);put("drumPatternId",x.drumPatternId ?: JSONObject.NULL) }) } })
        }
        getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(root.toString(2).toByteArray(Charsets.UTF_8)) }
    }
    fun importBackup(uri:Uri)=viewModelScope.launch { backup.importFrom(uri) }
    fun setTheme(v:String)=viewModelScope.launch { settingsRepo.setTheme(v) }
    fun setDarkMode(v:String)=viewModelScope.launch { settingsRepo.setDarkMode(v) }
    fun setFontScale(v:Float)=viewModelScope.launch { settingsRepo.setFontScale(v) }
    fun setLanguage(v:String)=viewModelScope.launch { settingsRepo.setLanguage(v) }
}
