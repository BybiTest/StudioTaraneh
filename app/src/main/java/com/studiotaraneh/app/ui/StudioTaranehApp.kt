package com.studiotaraneh.app.ui

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.studiotaraneh.app.audio.AudioRecorder
import com.studiotaraneh.app.audio.DrumEngine
import com.studiotaraneh.app.data.*
import com.studiotaraneh.app.ui.theme.StudioTaranehTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable fun StudioTaranehApp(vm: StudioViewModel = viewModel()) {
    val settings by vm.settings.collectAsState()
    StudioTaranehTheme(theme=settings.theme, darkMode=settings.darkMode, fontScale=settings.fontScale) {
        LaunchedEffect(Unit) { vm.ensureDefaultStyles() }
        val nav = rememberNavController()
        NavHost(nav, startDestination = "splash") {
            composable("splash") { Splash { nav.navigate("home") { popUpTo("splash") { inclusive = true } } } }
            composable("home") { Home({nav.navigate("songs")},{nav.navigate("new")},{nav.navigate("rhythm")},{nav.navigate("trash")},{nav.navigate("settings")},{nav.navigate("backup")},{nav.navigate("favorites")},{nav.navigate("recent")},{nav.navigate("about")},{nav.navigate("privacy")},{nav.navigate("terms")}) }
            composable("songs") { SongList(vm,{nav.popBackStack()},{nav.navigate("edit/$it")},{nav.navigate("new")}) }
            composable("favorites") { SongCollection("علاقه‌مندی‌ها", vm.favorites, {nav.popBackStack()}) { nav.navigate("edit/$it") } }
            composable("recent") { SongCollection("اخیر", vm.recent, {nav.popBackStack()}) { nav.navigate("edit/$it") } }
            composable("new") { NewSong(vm,{nav.popBackStack()},{nav.navigate("edit/$it")}) }
            composable("edit/{id}", arguments=listOf(navArgument("id"){type=NavType.LongType})) { b -> Editor(vm,b.arguments!!.getLong("id"),{nav.popBackStack()},{nav.navigate("versions/${b.arguments!!.getLong("id")}")}) }
            composable("versions/{id}", arguments=listOf(navArgument("id"){type=NavType.LongType})) { b -> Versions(vm,b.arguments!!.getLong("id"),{nav.popBackStack()}) }
            composable("trash") { Trash(vm,{nav.popBackStack()}) }
            composable("settings") { Settings(vm,{nav.popBackStack()},{nav.navigate("about")},{nav.navigate("privacy")},{nav.navigate("terms")},{nav.navigate("styles")}) }
            composable("styles") { StyleManager(vm,{nav.popBackStack()}) }
            composable("about") { InfoPage("درباره استودیو ترانه", "استودیو ترانه یک دفتر محلی و حرفه‌ای برای نوشتن، ضبط، مدیریت و آرشیو شعر و ترانه.\n\nسازنده: سیدحمید موسوی زاده\nنسخه 1.5.0\nبدون قابلیت هوش مصنوعی") {nav.popBackStack()} }
            composable("privacy") { InfoPage("حریم خصوصی", "استودیو ترانه با معماری Local-First طراحی شده است. نوشته‌ها، پروژه‌ها و ضبط‌های صوتی تا حد امکان روی خود دستگاه نگهداری می‌شوند و هیچ متن یا فایل صوتی برای سرویس هوش مصنوعی ارسال نمی‌شود.\n\nسرویس‌های تبلیغاتی و خرید درون‌برنامه‌ای، در صورت فعال‌سازی، تابع سیاست‌های همان سرویس‌ها هستند.") {nav.popBackStack()} }
            composable("terms") { InfoPage("شرایط استفاده", "کاربر مسئول محتوایی است که ایجاد یا وارد می‌کند. پشتیبان‌گیری منظم توصیه می‌شود. حذف دائمی از سطل زباله برگشت‌پذیر نیست. استفاده از سرویس‌های خارجی تابع شرایط آن سرویس است.") {nav.popBackStack()} }
            composable("rhythm") { Rhythm({nav.popBackStack()}) }
            composable("backup") { BackupScreen(vm,{nav.popBackStack()}) }
        }
    }
}

@Composable private fun Splash(done:()->Unit) { LaunchedEffect(Unit){delay(3000);done()}; Box(Modifier.fillMaxSize(),Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Text("STUDIO TARANEH",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("استودیو ترانه");Spacer(Modifier.height(10.dp));Text("سیدحمید موسوی زاده");Text("نسخه 1.5.0")}} }

@Composable
private fun HomeButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text)
    }
}

@Composable private fun Home(onSongs:()->Unit,onNew:()->Unit,onRhythm:()->Unit,onTrash:()->Unit,onSettings:()->Unit,onBackup:()->Unit,onFavorites:()->Unit,onRecent:()->Unit,onAbout:()->Unit,onPrivacy:()->Unit,onTerms:()->Unit){
    Scaffold(topBar={TopAppBar(title={Text("استودیو ترانه")})}){p->LazyColumn(Modifier.padding(p).padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{Text("دفتر حرفه‌ای شعر و ترانه",style=MaterialTheme.typography.headlineSmall)}
        item{HomeButton("✍️ ترانه‌های من",onSongs)}; item{HomeButton("➕ ترانه جدید",onNew)}
        item{HomeButton("⭐ علاقه‌مندی‌ها",onFavorites)}; item{HomeButton("🕘 اخیر",onRecent)}
        item{HomeButton("🎙 ضبط صدا / ریتم‌ساز",onRhythm)}; item{HomeButton("🗑 سطل زباله",onTrash)}
        item{HomeButton("💾 پشتیبان‌گیری و بازیابی",onBackup)}; item{HomeButton("⚙️ تنظیمات",onSettings)}
        item{HorizontalDivider(Modifier.padding(vertical=6.dp))}
        item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){TextButton(onAbout){Text("درباره")};TextButton(onPrivacy){Text("حریم خصوصی")};TextButton(onTerms){Text("قوانین")}}}
        item{Text("نسخه 1.5.0 • بدون AI",style=MaterialTheme.typography.bodySmall)}
    }}
}

@Composable private fun SongCollection(title:String,songsFlow:Flow<List<SongEntity>>,onBack:()->Unit,onOpen:(Long)->Unit){
    val songs by songsFlow.collectAsState(initial=emptyList())
    Scaffold(topBar={TopAppBar(title={Text(title)},navigationIcon={IconButton(onBack){Icon(Icons.Default.ArrowBack,null)}})}){p->
        if(songs.isEmpty()) Box(Modifier.fillMaxSize().padding(p),Alignment.Center){Text("موردی برای نمایش وجود ندارد")}
        else LazyColumn(Modifier.padding(p).padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){items(songs,key={it.id}){song->ListItem(headlineContent={Text(song.title)},supportingContent={Text("${song.style} • ${song.bpm} BPM • ${song.timeSignature}")},leadingContent={Icon(if(song.favorite)Icons.Default.Star else Icons.Default.MusicNote,null)},modifier=Modifier.clickable{onOpen(song.id)})}}
    }
}

@Composable private fun InfoPage(title:String,body:String,onBack:()->Unit){Scaffold(topBar={TopAppBar(title={Text(title)},navigationIcon={IconButton(onBack){Icon(Icons.Default.ArrowBack,null)}})}){p->Column(Modifier.padding(p).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text(body,style=MaterialTheme.typography.bodyLarge)}}}

@Composable private fun SongList(vm:StudioViewModel,onBack:()->Unit,onOpen:(Long)->Unit,onNew:()->Unit){val songs by vm.songs.collectAsState();var q by remember{mutableStateOf("")};val results by remember(q){if(q.isBlank())vm.songs else vm.search(q)}.collectAsState(initial=songs);Scaffold(topBar={TopAppBar(title={Text("ترانه‌های من")},navigationIcon={IconButton(onBack){Icon(Icons.Default.ArrowBack,null)}},actions={IconButton(onNew){Icon(Icons.Default.Add,null)}})}){p->LazyColumn(Modifier.padding(p).padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){item{OutlinedTextField(q,{q=it},Modifier.fillMaxWidth(),singleLine=true,label={Text("جستجو در عنوان، هنرمند، سبک، یادداشت و متن بخش‌ها")})};items(results,key={it.id}){s->ListItem(headlineContent={Text(s.title)},supportingContent={Text("${s.style} • ${s.bpm} BPM • ${s.timeSignature}")},leadingContent={Icon(if(s.favorite)Icons.Default.Star else Icons.Default.MusicNote,null)},trailingContent={Row{IconButton({vm.setFavorite(s.id,!s.favorite)}){Icon(if(s.favorite)Icons.Default.Star else Icons.Default.StarBorder,null)};IconButton({onOpen(s.id)}){Icon(Icons.Default.Edit,null)}}})}}}}

@Composable private fun NewSong(vm:StudioViewModel,onBack:()->Unit,onCreated:(Long)->Unit){var title by remember{mutableStateOf("")};var style by remember{mutableStateOf("پاپ")};var bpm by remember{mutableIntStateOf(90)};var meter by remember{mutableStateOf("4/4")};Scaffold(topBar={TopAppBar(title={Text("ترانه جدید")},navigationIcon={IconButton(onBack){Icon(Icons.Default.ArrowBack,null)}})}){p->Column(Modifier.padding(p).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){OutlinedTextField(title,{title=it},Modifier.fillMaxWidth(),label={Text("عنوان ترانه")});OutlinedTextField(style,{style=it},Modifier.fillMaxWidth(),label={Text("سبک / زیرسبک")});OutlinedTextField(bpm.toString(),{it.toIntOrNull()?.coerceIn(40,240)?.let{v->bpm=v}},Modifier.fillMaxWidth(),label={Text("BPM")});Text("میزان");Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("2/4","3/4","4/4","6/8","12/8").forEach{FilterChip(meter==it,{meter=it},{Text(it)})}};Button({vm.createSong(title,style,bpm,meter,onCreated)},Modifier.fillMaxWidth()){Text("ساخت ترانه")}}}}

@Composable private fun Editor(vm:StudioViewModel,id:Long,onBack:()->Unit,onVersions:()->Unit){
    LaunchedEffect(id) { vm.markSongOpened(id) }
    val context=LocalContext.current
    val sections by vm.sections(id).collectAsState(initial=emptyList())
    val song by vm.song(id).collectAsState(initial=null)
    val recordings by vm.recordings(id).collectAsState(initial=emptyList())
    val deletedRecordings by vm.deletedRecordings(id).collectAsState(initial=emptyList())
    val patterns by vm.drumPatterns.collectAsState()
    var recorder by remember{mutableStateOf<AudioRecorder?>(null)}
    var recording by remember{mutableStateOf(false)}
    var recordingPaused by remember{mutableStateOf(false)}
    var recordingStartedAt by remember{mutableLongStateOf(0L)}
    var recordingElapsedMs by remember{mutableLongStateOf(0L)}
    var countInEnabled by remember{mutableStateOf(true)}
    var countInRemaining by remember{mutableIntStateOf(0)}
    var countInRunning by remember{mutableStateOf(false)}
    var countInCancelled by remember{mutableStateOf(false)}
    var renameRecordingId by remember{mutableStateOf<Long?>(null)}
    var renameText by remember{mutableStateOf("")}
    val scope = rememberCoroutineScope()
    var selectedSection by remember{mutableStateOf<Long?>(null)}
    var drumRunning by remember{mutableStateOf(false)}
    var drumStep by remember{mutableIntStateOf(-1)}
    var timelineRunning by remember{mutableStateOf(false)}
    var timelinePaused by remember{mutableStateOf(false)}
    var timelineLoop by remember{mutableStateOf(false)}
    var timelineSectionIndex by remember{mutableIntStateOf(-1)}
    var timelineStep by remember{mutableIntStateOf(-1)}
    var timelineStartIndex by remember{mutableIntStateOf(0)}
    val engine=remember{DrumEngine()}
    fun startRecordingWithCountIn() {
        if (countInRunning || recording) return
        scope.launch {
            countInCancelled = false
            if (countInEnabled) {
                countInRunning = true
                countInRemaining = 3
                repeat(3) {
                    delay(1000)
                    if (countInCancelled) { countInRunning = false; countInRemaining = 0; return@launch }
                    countInRemaining--
                }
                countInRunning = false
            }
            recorder = AudioRecorder(context)
            recorder!!.start()
            recording = true
            recordingPaused = false
            recordingStartedAt = System.currentTimeMillis()
            recordingElapsedMs = 0
            selectedSection = selectedSection ?: sections.firstOrNull()?.id
            drumRunning = song?.drumEnabled == true && patterns.firstOrNull { it.id == song?.drumPatternId } != null
        }
    }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){if(it) startRecordingWithCountIn()}

    DisposableEffect(Unit){onDispose{recorder?.stop();timelineRunning=false;drumRunning=false;engine.release()}}

    val selectedPattern=patterns.firstOrNull{it.id==song?.drumPatternId}
    val latestTimelinePaused by rememberUpdatedState(timelinePaused)
    LaunchedEffect(recording, recordingPaused) {
        while (recording) {
            if (!recordingPaused) recordingElapsedMs = System.currentTimeMillis() - recordingStartedAt
            delay(250)
        }
    }
    LaunchedEffect(drumRunning,selectedPattern?.id,selectedPattern?.bpm,selectedPattern?.beats,selectedPattern?.kick,selectedPattern?.snare,selectedPattern?.hiHat,song?.bpm){
        if(!drumRunning || selectedPattern==null){drumStep=-1;return@LaunchedEffect}
        val total=(selectedPattern.beats*selectedPattern.stepsPerBeat).coerceAtLeast(1)
        val masterBpm=(song?.bpm?:selectedPattern.bpm).coerceIn(40,220)
        val kick=selectedPattern.kick.padEnd(total,'0'); val snare=selectedPattern.snare.padEnd(total,'0'); val hat=selectedPattern.hiHat.padEnd(total,'0')
        var step=0
        while(drumRunning){
            drumStep=step
            if(kick.getOrNull(step)=='1') engine.triggerKick()
            if(snare.getOrNull(step)=='1') engine.triggerSnare()
            if(hat.getOrNull(step)=='1') engine.triggerHiHat()
            delay((60000L/(masterBpm*selectedPattern.stepsPerBeat.coerceAtLeast(1))).coerceAtLeast(20L))
            step=(step+1)%total
        }
    }

    LaunchedEffect(timelineRunning, sections, patterns, song?.bpm, timelineLoop, timelineStartIndex) {
        if (!timelineRunning || sections.isEmpty()) { if(!timelinePaused){ timelineSectionIndex=-1; timelineStep=-1 }; return@LaunchedEffect }
        val masterBpm=(song?.bpm?:90).coerceIn(40,220)
        var cycle=true
        while(timelineRunning && cycle){
            val ordered=sections.drop(timelineStartIndex.coerceIn(0,sections.lastIndex))
            ordered.forEachIndexed { localIndex, section ->
                if(!timelineRunning) return@LaunchedEffect
                val index=timelineStartIndex+localIndex
                timelineSectionIndex=index
                val pattern=section.drumPatternId?.let{pid->patterns.firstOrNull{it.id==pid}}
                    ?:song?.drumPatternId?.let{pid->patterns.firstOrNull{it.id==pid}}
                val stepsPerBeat=pattern?.stepsPerBeat?.coerceAtLeast(1)?:4
                val stepsPerBar=pattern?.let{(it.beats*it.stepsPerBeat).coerceAtLeast(1)}?:((song?.timeSignature?.substringBefore('/')?.toIntOrNull()?:4)*stepsPerBeat)
                val total=pattern?.let{(it.beats*it.stepsPerBeat).coerceAtLeast(1)}?:stepsPerBar
                val kick=pattern?.kick.orEmpty().padEnd(total,'0'); val snare=pattern?.snare.orEmpty().padEnd(total,'0'); val hat=pattern?.hiHat.orEmpty().padEnd(total,'0')
                val stepDelay=(60000L/(masterBpm*stepsPerBeat)).coerceAtLeast(20L)
                repeat(section.barCount.coerceIn(1,256)){
                    for(step in 0 until stepsPerBar){
                        while(latestTimelinePaused && timelineRunning) delay(100)
                        if(!timelineRunning) return@LaunchedEffect
                        timelineStep=step
                        if(kick.getOrNull(step%total)=='1') engine.triggerKick()
                        if(snare.getOrNull(step%total)=='1') engine.triggerSnare()
                        if(hat.getOrNull(step%total)=='1') engine.triggerHiHat()
                        delay(stepDelay)
                    }
                }
            }
            if(!timelineLoop) cycle=false
        }
        if (timelineRunning) {
            timelineRunning=false
            timelinePaused=false
            timelineSectionIndex=-1
            timelineStep=-1
        }
    }

    val createText=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")){uri->if(uri!=null)vm.exportSongText(id,uri)}
    val createJson=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->if(uri!=null)vm.exportSongJson(id,uri)}
    Scaffold(topBar={TopAppBar(title={Text(song?.title?:"ویرایش")},navigationIcon={IconButton(onBack){Icon(Icons.Default.ArrowBack,null)}},actions={IconButton({copyAll(context,song,sections)}){Icon(Icons.Default.ContentCopy,null)};IconButton({val send=Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,buildSongText(song,sections))};context.startActivity(Intent.createChooser(send,"اشتراک‌گذاری ترانه"))}){Icon(Icons.Default.Share,null)};IconButton({createText.launch((song?.title?.ifBlank{"StudioTaraneh"}?:"StudioTaraneh")+".txt")}){Icon(Icons.Default.Description,null)};IconButton({createJson.launch((song?.title?.ifBlank{"StudioTaraneh"}?:"StudioTaraneh")+".json")}){Icon(Icons.Default.DataObject,null)};IconButton(onVersions){Icon(Icons.Default.History,null)}})}){p->
        LazyColumn(Modifier.padding(p).padding(14.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            item{Text("${song?.style?:""} • ${song?.bpm?:90} BPM • ${song?.timeSignature?:"4/4"}")}
            item {
                SongTimelineCard(
                    sections = sections, patterns = patterns, song = song, activeIndex = timelineSectionIndex, activeStep = timelineStep,
                    running = timelineRunning, paused = timelinePaused, loop = timelineLoop, startIndex = timelineStartIndex,
                    onToggle = {
                        if (timelineRunning) { timelinePaused = !timelinePaused }
                        else { timelineStartIndex=timelineStartIndex.coerceIn(0,(sections.size-1).coerceAtLeast(0)); timelinePaused=false; timelineRunning=true }
                    },
                    onStop = { timelineRunning=false; timelinePaused=false; timelineSectionIndex=-1; timelineStep=-1 },
                    onLoopChange = { timelineLoop=it },
                    onSelectStart = { index -> if(!timelineRunning){ timelineStartIndex=index } },
                    onMove = { from,to -> val ids=sections.map{it.id}.toMutableList(); val moved=ids.removeAt(from); ids.add(to,moved); vm.reorderSections(id,ids); timelineStartIndex=timelineStartIndex.coerceAtMost((ids.size-1).coerceAtLeast(0)) },
                    onUpdate = { section, bars, patternId -> vm.setSectionTimeline(section, bars, patternId) }
                )
            }
            item{
                SongRhythmCard(
                    song=song,
                    patterns=patterns,
                    selectedPattern=selectedPattern,
                    running=drumRunning,
                    currentStep=drumStep,
                    onSelect={patternId->vm.setSongDrumPattern(id,patternId,patternId!=null)},
                    onToggle={enabled->vm.setSongDrumPattern(id,song?.drumPatternId,enabled)},
                    onPlay={drumRunning=true},
                    onStop={drumRunning=false}
                )
            }
            item{Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){Button({vm.addSection(id,"VERSE","Verse")}){Text("＋ ورس")};Button({vm.addSection(id,"CHORUS","Chorus / ترجیع‌بند")}){Text("＋ کورس")};Button({vm.addSection(id,"BRIDGE","Bridge")}){Text("＋ Bridge")};Button({vm.addSection(id,"OUTRO","Outro")}){Text("＋ Outro")}}}
            items(sections,key={it.id}){sec->SectionEditor(sec,vm,{selectedSection=sec.id})}
            item{
                RecordingControlCard(
                    sections=sections, selectedSection=selectedSection, recording=recording, paused=recordingPaused, elapsedMs=recordingElapsedMs,
                    selectedTitle=sections.firstOrNull{it.id==selectedSection}?.title ?: "کل ترانه",
                    onStart={
                        if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED) startRecordingWithCountIn()
                        else permission.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onPause={ if(recording){ recorder?.pause(); recordingPaused=true } },
                    onResume={ if(recording){ recorder?.resume(); recordingPaused=false; recordingStartedAt=System.currentTimeMillis()-recordingElapsedMs } },
                    onStop={
                        val file=recorder?.stop()
                        if(file!=null) vm.addRecording(id,file.absolutePath,file.nameWithoutExtension,selectedSection,recordingElapsedMs)
                        recording=false; recordingPaused=false; recordingElapsedMs=0; drumRunning=false
                    },
                    onSelectSection={selectedSection=it},
                    countInEnabled=countInEnabled,
                    countInRemaining=countInRemaining,
                    countInRunning=countInRunning,
                    onCountInChange={countInEnabled=it},
                    onCancelCountIn={countInCancelled=true;countInRunning=false;countInRemaining=0}
                )
            }
            item{
                if(countInRunning) Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("آماده ضبط...",style=MaterialTheme.typography.titleMedium);Text(countInRemaining.toString(),style=MaterialTheme.typography.displaySmall);Text("بعد از شمارش ضبط شروع می‌شود")}}
            }
            item{if(recordings.isNotEmpty())Text("ضبط‌های ذخیره‌شده",style=MaterialTheme.typography.titleMedium)}
            items(recordings,key={it.id}){r->RecordingRow(r,vm,{vm.deleteRecording(r)},{vm.setRecordingFavorite(r,!r.favorite)},{vm.setMainTake(r)},{renameRecordingId=r.id;renameText=r.title})}
            if(deletedRecordings.isNotEmpty()) item{Text("ضبط‌های حذف‌شده",style=MaterialTheme.typography.titleMedium)}
            items(deletedRecordings,key={it.id}){r->ListItem(headlineContent={Text(r.title)},supportingContent={Text("حذف‌شده • "+formatDuration(r.durationMs))},trailingContent={Row{IconButton({vm.restoreRecording(r)}){Icon(Icons.Default.Restore,"بازیابی")};IconButton({vm.deleteRecordingForever(r)}){Icon(Icons.Default.DeleteForever,"حذف دائمی")}}})}
            if(renameRecordingId!=null) item {
                AlertDialog(onDismissRequest={renameRecordingId=null},confirmButton={Button({renameRecordingId?.let{rid->recordings.firstOrNull{it.id==rid}?.let{r->vm.renameRecording(r,renameText)}};renameRecordingId=null}){Text("ذخیره")}},dismissButton={TextButton({renameRecordingId=null}){Text("انصراف")}},title={Text("تغییر نام ضبط")},text={OutlinedTextField(renameText,{renameText=it},singleLine=true,label={Text("نام")})})
            }
        }
    }
}

@Composable private fun SongRhythmCard(song:SongEntity?,patterns:List<DrumPatternEntity>,selectedPattern:DrumPatternEntity?,running:Boolean,currentStep:Int,onSelect:(Long?)->Unit,onToggle:(Boolean)->Unit,onPlay:()->Unit,onStop:()->Unit){
    var expanded by remember{mutableStateOf(false)}
    Card(Modifier.fillMaxWidth()){
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth()){
                Column(Modifier.weight(1f)){Text("ریتم اختصاصی ترانه",style=MaterialTheme.typography.titleMedium);Text(selectedPattern?.let{"${it.name} • BPM ترانه: ${song?.bpm?:it.bpm} • ${it.timeSignature}"}?:"هنوز Pattern انتخاب نشده",style=MaterialTheme.typography.bodySmall)}
                Switch(checked=song?.drumEnabled==true,onCheckedChange=onToggle,enabled=selectedPattern!=null)
            }
            OutlinedButton({expanded=!expanded},Modifier.fillMaxWidth()){Text(if(expanded)"بستن انتخاب Pattern" else "انتخاب Pattern")}
            if(expanded){
                if(patterns.isEmpty()) Text("ابتدا از صفحه ریتم‌ساز یک Pattern بساز و ذخیره کن.")
                patterns.forEach{pattern->
                    ListItem(headlineContent={Text(pattern.name)},supportingContent={Text("${pattern.bpm} BPM • ${pattern.timeSignature} • ${pattern.beats} ضرب")},trailingContent={RadioButton(selected=selectedPattern?.id==pattern.id,onClick={onSelect(pattern.id)})},modifier=Modifier.clickable{onSelect(pattern.id);expanded=false})
                }
                if(selectedPattern!=null) OutlinedButton({onSelect(null);expanded=false},Modifier.fillMaxWidth()){Text("حذف Pattern از ترانه")}
            }
            if(selectedPattern!=null){
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()){
                    Button(if(running)onStop else onPlay,Modifier.weight(1f)){Icon(if(running)Icons.Default.Stop else Icons.Default.PlayArrow,null);Spacer(Modifier.width(5.dp));Text(if(running)"توقف ریتم" else "پخش ریتم")}
                    Text("${selectedPattern.beats} ضرب • ${selectedPattern.stepsPerBeat} Step/Beat • BPM ترانه ${song?.bpm?:selectedPattern.bpm}",modifier=Modifier.align(Alignment.CenterVertically))
                }
                if(running){
                    val total=selectedPattern.beats*selectedPattern.stepsPerBeat
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(3.dp)){(0 until total).forEach{step->Box(Modifier.size(18.dp).background(if(step==currentStep)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,MaterialTheme.shapes.small))}}
                    Text("هنگام ضبط، این ریتم با ضبط وکال همزمان اجرا می‌شود.",style=MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun RecordingControlCard(
    sections:List<SectionEntity>, selectedSection:Long?, recording:Boolean, paused:Boolean, elapsedMs:Long, selectedTitle:String,
    onStart:()->Unit, onPause:()->Unit, onResume:()->Unit, onStop:()->Unit, onSelectSection:(Long?)->Unit,
    countInEnabled:Boolean, countInRemaining:Int, countInRunning:Boolean, onCountInChange:(Boolean)->Unit, onCancelCountIn:()->Unit
){
    var sectionMenu by remember{mutableStateOf(false)}
    Card(Modifier.fillMaxWidth()){
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("ضبط وکال",style=MaterialTheme.typography.titleMedium)
            Text(if(recording) "ضبط برای: $selectedTitle • ${formatDuration(elapsedMs)}" else "ضبط را می‌توانید به کل ترانه یا یک Section متصل کنید.",style=MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()){
                if(!recording && !countInRunning) Button(onStart,Modifier.weight(1f)){Icon(Icons.Default.Mic,null);Spacer(Modifier.width(4.dp));Text("شروع ضبط")}
                if(countInRunning) { Button(onCancelCountIn,Modifier.weight(1f)){Icon(Icons.Default.Close,null);Spacer(Modifier.width(4.dp));Text("لغو شمارش")}}
                else {
                    Button(if(paused) onResume else onPause,Modifier.weight(1f)){Icon(if(paused)Icons.Default.PlayArrow else Icons.Default.Pause,null);Spacer(Modifier.width(4.dp));Text(if(paused)"ادامه" else "مکث")}
                    Button(onStop,Modifier.weight(1f)){Icon(Icons.Default.Stop,null);Spacer(Modifier.width(4.dp));Text("توقف و ذخیره")}
                }
            }
            Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth()){
                Checkbox(checked=countInEnabled,onCheckedChange=onCountInChange,enabled=!recording&&!countInRunning)
                Text("شمارش معکوس ۳ ثانیه")
            }
            OutlinedButton({sectionMenu=true},Modifier.fillMaxWidth(),enabled=!recording&&!countInRunning){Text("Section ضبط: $selectedTitle")}
            DropdownMenu(expanded=sectionMenu,onDismissRequest={sectionMenu=false}){
                DropdownMenuItem(text={Text("کل ترانه")},onClick={sectionMenu=false;onSelectSection(null)})
                sections.forEach { section -> DropdownMenuItem(text={Text(section.title)},onClick={sectionMenu=false;onSelectSection(section.id)}) }
            }
        }
    }
}

@Composable private fun RecordingRow(r:RecordingEntity,vm:StudioViewModel,onDelete:()->Unit,onFavorite:()->Unit,onMain:()->Unit,onRename:()->Unit){
    val context=LocalContext.current
    var player by remember(r.id){mutableStateOf<MediaPlayer?>(null)}
    var playing by remember(r.id){mutableStateOf(false)}
    var position by remember(r.id){mutableIntStateOf(0)}
    var duration by remember(r.id){mutableIntStateOf(r.durationMs.coerceAtLeast(0).toInt())}
    DisposableEffect(r.id){onDispose{player?.release()}}
    LaunchedEffect(playing,player){
        while(playing && player!=null){
            position=runCatching{player!!.currentPosition}.getOrDefault(position)
            duration=runCatching{player!!.duration}.getOrDefault(duration)
            delay(200)
        }
    }
    fun startOrResume(){
        runCatching{
            if(player==null){
                player=MediaPlayer().apply{setDataSource(context,Uri.parse(r.path));prepare();duration=this.duration;setOnCompletionListener{playing=false;position=0};seekTo(position);start()}
            } else player!!.start()
            playing=true
        }
    }
    Card(Modifier.fillMaxWidth()){
        Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){
                    Row(verticalAlignment=Alignment.CenterVertically){Text(r.title,fontWeight=FontWeight.Bold);if(r.isMainTake){Spacer(Modifier.width(6.dp));AssistChip(onClick=onMain,label={Text("Main Take")})}}
                    Text((if(r.sectionId==null)"کل ترانه" else "Section #${r.sectionId}")+" • "+formatDuration(r.durationMs),style=MaterialTheme.typography.bodySmall)
                }
                IconButton(onFavorite){Icon(if(r.favorite)Icons.Default.Star else Icons.Default.StarBorder,"علاقه‌مندی")}
                IconButton(onRename){Icon(Icons.Default.Edit,"تغییر نام")}
                IconButton(onDelete){Icon(Icons.Default.Delete,"حذف")}
            }
            if(r.durationMs>0 || duration>0){
                Slider(value=position.toFloat().coerceIn(0f,duration.coerceAtLeast(1).toFloat()),onValueChange={position=it.toInt()},onValueChangeFinished={player?.seekTo(position)},valueRange=0f..duration.coerceAtLeast(1).toFloat(),modifier=Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(formatDuration(position.toLong()));Text(formatDuration(duration.toLong()))}
            }
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                Button({if(playing){player?.pause();playing=false}else startOrResume()}){Icon(if(playing)Icons.Default.Pause else Icons.Default.PlayArrow,null);Spacer(Modifier.width(4.dp));Text(if(playing)"مکث" else "پخش")}
                OutlinedButton({player?.pause();playing=false;player?.seekTo(0);position=0}){Icon(Icons.Default.Stop,null);Text("توقف")}
                if(!r.isMainTake) OutlinedButton(onMain){Icon(Icons.Default.Star,null);Text("Main Take")}
            }
        }
    }
}

private fun buildSongText(song:SongEntity?,sections:List<SectionEntity>):String=buildString{append(song?.title.orEmpty()).append("\n\n");sections.forEach{append("[").append(it.title).append("]\n").append(it.content).append("\n\n")}}

private fun copyAll(context:Context,song:SongEntity?,sections:List<SectionEntity>){val text=buildString{append(song?.title.orEmpty()).append("\n\n");sections.forEach{append("[${it.title}]\n${it.content}\n\n")}};(context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(ClipData.newPlainText("Studio Taraneh",text))}

@Composable private fun SongTimelineCard(
    sections: List<SectionEntity>, patterns: List<DrumPatternEntity>, song: SongEntity?, activeIndex: Int, activeStep: Int,
    running: Boolean, paused: Boolean, loop: Boolean, startIndex: Int, onToggle: () -> Unit, onStop: () -> Unit, onLoopChange: (Boolean) -> Unit,
    onSelectStart: (Int) -> Unit, onMove: (Int, Int) -> Unit, onUpdate: (SectionEntity, Int, Long?) -> Unit
) {
    val totalBars=sections.sumOf{it.barCount.coerceAtLeast(1)}
    val bpm=(song?.bpm?:90).coerceIn(40,220)
    val totalSeconds=totalBars*4*60.0/bpm
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Timeline ترانه",style=MaterialTheme.typography.titleMedium)
                    Text("$totalBars میزان • حدود ${formatTimelineTime(totalSeconds)} • ${bpm} BPM",style=MaterialTheme.typography.bodySmall)
                }
                Button(onToggle,enabled=sections.isNotEmpty()) { Icon(if(running && !paused)Icons.Default.Pause else Icons.Default.PlayArrow,null); Spacer(Modifier.width(4.dp)); Text(if(running && !paused)"مکث" else if(running)"ادامه" else "پخش") }
                OutlinedButton(onStop,enabled=running) { Icon(Icons.Default.Stop,null); Spacer(Modifier.width(4.dp)); Text("توقف") }
            }
            Row(verticalAlignment=Alignment.CenterVertically) {
                Checkbox(checked=loop,onCheckedChange=onLoopChange,enabled=!running)
                Text("تکرار Timeline")
                Spacer(Modifier.width(12.dp))
                Text(if(running && activeIndex>=0) "${if(paused)"مکث" else "در حال پخش"}: ${sections[activeIndex].title}" else "شروع از: ${sections.getOrNull(startIndex)?.title ?: "ابتدا"}",style=MaterialTheme.typography.bodySmall)
            }
            if(running && activeIndex>=0){
                val section=sections[activeIndex]
                val pattern=section.drumPatternId?.let{pid->patterns.firstOrNull{it.id==pid}}?:song?.drumPatternId?.let{pid->patterns.firstOrNull{it.id==pid}}
                val steps=(pattern?.beats?:4)*(pattern?.stepsPerBeat?:4)
                LinearProgressIndicator(progress={if(steps>0)(activeStep.coerceAtLeast(0)%steps+1).toFloat()/steps else 0f},Modifier.fillMaxWidth())
            }
            if(sections.isEmpty()) Text("ابتدا یک بخش مثل Verse یا Chorus اضافه کنید.")
            sections.forEachIndexed { index, section ->
                val fallback=song?.drumPatternId
                val selected=section.drumPatternId?:fallback
                val name=patterns.firstOrNull{it.id==selected}?.name?:"بدون Pattern"
                Card(
                    modifier=Modifier.fillMaxWidth().border(if(activeIndex==index)2.dp else 0.dp,if(activeIndex==index)MaterialTheme.colorScheme.primary else Color.Transparent,MaterialTheme.shapes.medium).clickable(enabled=!running){onSelectStart(index)}
                ) {
                    Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment=Alignment.CenterVertically){
                            Text("${index+1}. ${section.title}",Modifier.weight(1f),fontWeight=FontWeight.Bold)
                            Text("${section.barCount} میزان",style=MaterialTheme.typography.labelMedium)
                            IconButton(enabled=!running && index>0,onClick={onMove(index,index-1)}){Icon(Icons.Default.KeyboardArrowUp,"بالا")}
                            IconButton(enabled=!running && index<sections.lastIndex,onClick={onMove(index,index+1)}){Icon(Icons.Default.KeyboardArrowDown,"پایین")}
                        }
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            OutlinedTextField(value=section.barCount.toString(),onValueChange={it.toIntOrNull()?.let{v->onUpdate(section,v.coerceIn(1,256),section.drumPatternId)}},modifier=Modifier.width(105.dp),singleLine=true,label={Text("میزان")})
                            var menu by remember(section.id){mutableStateOf(false)}
                            Box{OutlinedButton(onClick={menu=true}){Text(name)};DropdownMenu(expanded=menu,onDismissRequest={menu=false}){
                                DropdownMenuItem(text={Text("Default ترانه")},onClick={menu=false;onUpdate(section,section.barCount,null)})
                                patterns.forEach{pattern->DropdownMenuItem(text={Text(pattern.name)},onClick={menu=false;onUpdate(section,section.barCount,pattern.id)})}
                            }}
                        }
                        if(!running && startIndex==index) Text("▶ شروع پخش از این بخش",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

private fun formatTimelineTime(seconds:Double):String { val total=seconds.toLong().coerceAtLeast(0); return "%02d:%02d".format(total/60,total%60) }

@Composable private fun SectionEditor(s:SectionEntity,vm:StudioViewModel,onSelected:()->Unit){
    val context=LocalContext.current
    var text by remember(s.id){mutableStateOf(s.content)}
    val colors=listOf(0xFFFFFFFF,0xFFFF8CF5,0xFF7DD3FC,0xFFFFD166,0xFFA7F3D0,0xFFC4B5FD)
    val clipboard=context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val align=when(s.alignment){"center"->TextAlign.Center;"end"->TextAlign.End;else->TextAlign.Start}
    Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            Box(Modifier.size(18.dp).background(Color(s.color)))
            Text(s.title,fontWeight=FontWeight.Bold,Modifier.weight(1f))
            IconButton(onSelected){Icon(Icons.Default.Mic,"اتصال ضبط به این بخش")}
            IconButton({vm.undoSection(s)}){Icon(Icons.Default.Undo,"Undo")}
            IconButton({vm.redoSection(s)}){Icon(Icons.Default.Redo,"Redo")}
            IconButton({vm.deleteSection(s)}){Icon(Icons.Default.Delete,"حذف بخش")}
        }
        Row(horizontalArrangement=Arrangement.spacedBy(5.dp),verticalAlignment=Alignment.CenterVertically){
            FilterChip(s.bold,{vm.formatSection(s,bold=!s.bold)},{Text("B",fontWeight=FontWeight.Bold)})
            FilterChip(s.italic,{vm.formatSection(s,italic=!s.italic)},{Text("I",fontStyle=androidx.compose.ui.text.font.FontStyle.Italic)})
            Text("اندازه:",style=MaterialTheme.typography.bodySmall)
            listOf(14,18,22,28).forEach{size->FilterChip(s.textSize==size,{vm.formatSection(s,textSize=size)},{Text(size.toString())})}
        }
        Row(horizontalArrangement=Arrangement.spacedBy(5.dp),verticalAlignment=Alignment.CenterVertically){
            Text("تراز:",style=MaterialTheme.typography.bodySmall)
            listOf("start" to "راست","center" to "وسط","end" to "چپ").forEach{(value,label)->FilterChip(s.alignment==value,{vm.formatSection(s,alignment=value)},{Text(label)})}
            Spacer(Modifier.weight(1f))
            TextButton({clipboard.setPrimaryClip(ClipData.newPlainText("Studio Taraneh Section",text))}){Text("کپی")}
            TextButton({clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.let{pasted->text+=pasted;vm.editSection(s,text)}}){Text("Paste")}
        }
        OutlinedTextField(
            value=text,
            onValueChange={text=it;vm.editSection(s,it)},
            modifier=Modifier.fillMaxWidth(),
            minLines=5,
            textStyle=MaterialTheme.typography.bodyLarge.copy(fontSize=s.textSize.sp,fontWeight=if(s.bold)FontWeight.Bold else FontWeight.Normal,fontStyle=if(s.italic)androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,textAlign=align),
            label={Text("متن ${s.title}")}
        )
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically){
            Text("رنگ بخش:",style=MaterialTheme.typography.bodySmall)
            colors.forEach{c->FilterChip(s.color==c,{vm.setSectionColor(s,c)},{Box(Modifier.size(16.dp).background(Color(c),MaterialTheme.shapes.small))})}
        }
    }
}

@Composable private fun Versions(vm:StudioViewModel,id:Long,onBack:()->Unit){
    val versions by vm.versions(id).collectAsState(initial=emptyList())
    Scaffold(topBar={TopAppBar(title={Text("تاریخچه نسخه‌ها")},navigationIcon={IconButton(onBack){Icon(Icons.Default.ArrowBack,null)}})}) { p ->
        LazyColumn(Modifier.padding(p)) {
            items(versions,key={it.id}) { v ->
                ListItem(
                    headlineContent={Text("نسخه ${v.id} • ${java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(v.createdAt))}")},
                    supportingContent={Text("Snapshot ذخیره‌شده از متن، قالب‌بندی و Timeline" )},
                    trailingContent={Button({vm.restoreVersion(v.id)}){Text("بازیابی")}}
                )
            }
        }
    }
}

@Composable private fun Trash(vm:StudioViewModel,onBack:()->Unit){val items by vm.trash.collectAsState();Scaffold(topBar={TopAppBar(title={Text("سطل زباله")},navigationIcon={IconButton(onBack){Icon(Icons.Default.ArrowBack,null)}})}){p->LazyColumn(Modifier.padding(p)){items(items){s->ListItem(headlineContent={Text(s.title)},trailingContent={Row{IconButton({vm.restore(s.id)}){Icon(Icons.Default.Restore,null)};IconButton({vm.deleteForever(s.id)}){Icon(Icons.Default.DeleteForever,null)}}})}}}}

@Composable private fun Settings(vm:StudioViewModel,onBack:()->Unit,onAbout:()->Unit,onPrivacy:()->Unit,onTerms:()->Unit,onStyles:()->Unit){val s by vm.settings.collectAsState();Scaffold(topBar={TopAppBar(title={Text("تنظیمات")},navigationIcon={IconButton(onBack){Icon(Icons.Default.ArrowBack,null)}})}){p->LazyColumn(Modifier.padding(p).padding(horizontal=12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
item{Text("تم کامل برنامه",style=MaterialTheme.typography.titleMedium)}
items(listOf("Neon Studio","Midnight","Graphite","Purple Night","AMOLED")){theme->ListItem(headlineContent={Text(theme)},trailingContent={RadioButton(s.theme==theme,{vm.setTheme(theme)})})}
item{Text("حالت نمایش",style=MaterialTheme.typography.titleMedium)}
item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){listOf("روشن" to "light","تاریک" to "dark","سیستم" to "system").forEach{(label,value)->FilterChip(s.darkMode==value,{vm.setDarkMode(value)},{Text(label)})}}}
item{Text("اندازه فونت",style=MaterialTheme.typography.titleMedium)}
item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){listOf("کوچک" to .85f,"متوسط" to 1f,"بزرگ" to 1.15f,"خیلی بزرگ" to 1.3f).forEach{(name,v)->FilterChip(kotlin.math.abs(s.fontScale-v)<0.001f,{vm.setFontScale(v)},{Text(name)})}}}
item{Text("زبان",style=MaterialTheme.typography.titleMedium)}
item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(s.language=="fa",{vm.setLanguage("fa")},{Text("فارسی")});FilterChip(s.language=="en",{vm.setLanguage("en")},{Text("English")})}}
item{HorizontalDivider()}; item{TextButton(onStyles){Text("مدیریت سبک‌ها و زیرسبک‌ها")}}; item{TextButton(onAbout){Text("درباره برنامه")}}; item{TextButton(onPrivacy){Text("حریم خصوصی")}}; item{TextButton(onTerms){Text("شرایط استفاده و قوانین")}}
item{Text("سازنده: سیدحمید موسوی زاده\nنسخه 1.5.0\nAI: غیرفعال",style=MaterialTheme.typography.bodySmall)}
}}}

@Composable private fun StyleManager(vm: StudioViewModel, onBack: () -> Unit) {
    val styles by vm.styles.collectAsState()
    var name by remember { mutableStateOf("") }
    var parent by remember { mutableStateOf("") }
    Scaffold(topBar={TopAppBar(title={Text("مدیریت سبک‌ها")},navigationIcon={IconButton(onBack){Icon(Icons.Default.ArrowBack,null)}})}) { p ->
        LazyColumn(Modifier.padding(p).padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            item { Text("سبک‌های اصلی و زیرسبک‌های قابل توسعه", style=MaterialTheme.typography.titleMedium) }
            item {
                OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),singleLine=true,label={Text("نام سبک یا زیرسبک جدید")})
            }
            item {
                OutlinedTextField(parent,{parent=it},Modifier.fillMaxWidth(),singleLine=true,label={Text("والد اختیاری؛ مثلاً رپ یا پاپ")})
            }
            item { Button({ vm.addCustomStyle(name,parent); name=""; parent="" },Modifier.fillMaxWidth()){Text("افزودن سبک سفارشی") } }
            items(styles,key={it.id}) { style ->
                ListItem(headlineContent={Text(style.name)},supportingContent={if(style.parent.isNotBlank()) Text("زیرسبک: ${style.parent}") else Text(if(style.isBuiltIn) "سبک پیش‌فرض" else "سبک سفارشی")},trailingContent={if(!style.isBuiltIn) IconButton({vm.deleteCustomStyle(style.id)}){Icon(Icons.Default.Delete,"حذف")}})
            }
        }
    }
}

@Composable private fun Rhythm(onBack:()->Unit, vm:StudioViewModel = viewModel()) {
    var bpm by remember { mutableIntStateOf(90) }
    var meter by remember { mutableStateOf("4/4") }
    var beats by remember { mutableIntStateOf(4) }
    var running by remember { mutableStateOf(false) }
    var currentStep by remember { mutableIntStateOf(-1) }
    var patternName by remember { mutableStateOf("") }
    var lastTapAt by remember { mutableLongStateOf(0L) }
    var tapBpm by remember { mutableIntStateOf(90) }

    var kick by remember { mutableStateOf(List(16) { false }) }
    var snare by remember { mutableStateOf(List(16) { false }) }
    var hiHat by remember { mutableStateOf(List(16) { false }) }

    val patterns by vm.drumPatterns.collectAsState()
    val engine = remember { DrumEngine() }
    val scroll = rememberScrollState()

    fun resizePattern(newBeats:Int) {
        beats = newBeats.coerceIn(1, 16)
        val size = beats * 4
        kick = kick.take(size).let { it + List(size - it.size) { false } }
        snare = snare.take(size).let { it + List(size - it.size) { false } }
        hiHat = hiHat.take(size).let { it + List(size - it.size) { false } }
        currentStep = -1
    }

    fun loadPattern(p:DrumPatternEntity) {
        bpm = p.bpm.coerceIn(40, 220)
        meter = p.timeSignature
        resizePattern(p.beats)
        fun decode(value:String):List<Boolean> {
            val size = p.beats.coerceIn(1,16) * 4
            return List(size) { i -> value.getOrNull(i) == '1' }
        }
        kick = decode(p.kick)
        snare = decode(p.snare)
        hiHat = decode(p.hiHat)
        patternName = p.name
        running = false
    }

    DisposableEffect(Unit) {
        onDispose {
            running = false
            engine.release()
        }
    }

    LaunchedEffect(running, bpm, beats, kick, snare, hiHat) {
        if (running) {
            val totalSteps = beats * 4
            while (running) {
                for (step in 0 until totalSteps) {
                    if (!running) break
                    currentStep = step
                    if (kick.getOrNull(step) == true) engine.triggerKick()
                    if (snare.getOrNull(step) == true) engine.triggerSnare()
                    if (hiHat.getOrNull(step) == true) engine.triggerHiHat()
                    delay((60000L / bpm / 4L).coerceAtLeast(20L))
                }
            }
        } else {
            currentStep = -1
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Drum Pattern • ریتم‌ساز واقعی") },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { p ->
        LazyColumn(
            Modifier.padding(p).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Pattern Sequencer", style = MaterialTheme.typography.titleLarge)
                        Text("هر خانه یک Step است؛ Kick، Snare و Hi-Hat با صدای تولیدشده روی خود دستگاه پخش می‌شوند.")
                        Text("${bpm} BPM • ${beats} ضرب • تقسیم هر ضرب: ۴ Step")
                        Slider(value=bpm.toFloat(), onValueChange={ bpm = it.toInt() }, valueRange=40f..220f)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton({
                                val now = System.currentTimeMillis()
                                if (lastTapAt > 0L) {
                                    val delta = now - lastTapAt
                                    if (delta in 250..2000) tapBpm = (60000L / delta).toInt().coerceIn(40,220)
                                }
                                lastTapAt = now
                                bpm = tapBpm
                            }, Modifier.weight(1f)) { Text("Tap Tempo • $bpm") }
                            OutlinedButton({ running = false; currentStep = -1 }, Modifier.weight(1f)) { Text("Stop") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("2/4","3/4","4/4","6/8","12/8").forEach {
                                FilterChip(meter == it, { meter = it }, { Text(it) })
                            }
                        }
                        Text("تعداد ضرب", style = MaterialTheme.typography.titleMedium)
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            (1..16).forEach { value ->
                                FilterChip(beats == value, { resizePattern(value) }, { Text(value.toString()) })
                            }
                        }
                    }
                }
            }

            item {
                MetronomeCard(bpm=bpm, onBpmChange={bpm=it}, engine=engine)
            }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("الگوی ضرب", style = MaterialTheme.typography.titleMedium)
                        Text("خانه روشن = فعال • Step فعلی با حاشیه مشخص می‌شود.", style = MaterialTheme.typography.bodySmall)
                        Column(Modifier.horizontalScroll(scroll)) {
                            val totalSteps = beats * 4
                            val rows = listOf(
                                "Kick" to kick,
                                "Snare" to snare,
                                "Hi-Hat" to hiHat
                            )
                            Row {
                                Spacer(Modifier.width(62.dp))
                                (0 until totalSteps).forEach { i ->
                                    Box(
                                        Modifier.width(30.dp).padding(2.dp),
                                        contentAlignment = Alignment.Center
                                    ) { Text("${i + 1}", style = MaterialTheme.typography.labelSmall) }
                                }
                            }
                            rows.forEach { (label, values) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(label, Modifier.width(62.dp), fontWeight = FontWeight.Bold)
                                    (0 until totalSteps).forEach { i ->
                                        val active = values.getOrNull(i) == true
                                        val isCurrent = currentStep == i
                                        Box(
                                            Modifier
                                                .padding(2.dp)
                                                .size(26.dp)
                                                .background(
                                                    if (active) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant,
                                                    MaterialTheme.shapes.small
                                                )
                                                .border(
                                                    width = if (isCurrent) 2.dp else 0.dp,
                                                    color = if (isCurrent) MaterialTheme.colorScheme.tertiary
                                                    else Color.Transparent,
                                                    shape = MaterialTheme.shapes.small
                                                )
                                                .clickable {
                                                    when (label) {
                                                        "Kick" -> kick = kick.toMutableList().also { it[i] = !it[i] }
                                                        "Snare" -> snare = snare.toMutableList().also { it[i] = !it[i] }
                                                        "Hi-Hat" -> hiHat = hiHat.toMutableList().also { it[i] = !it[i] }
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), Modifier.fillMaxWidth()) {
                    Button(
                        { running = !running },
                        Modifier.weight(1f)
                    ) {
                        Icon(if (running) Icons.Default.Stop else Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (running) "Stop" else "Play")
                    }
                    OutlinedButton(
                        {
                            kick = List(beats * 4) { false }
                            snare = List(beats * 4) { false }
                            hiHat = List(beats * 4) { false }
                            currentStep = -1
                        },
                        Modifier.weight(1f)
                    ) { Text("پاک کردن Pattern") }
                }
            }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("ذخیره Pattern", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            patternName,
                            { patternName = it },
                            Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("نام Pattern") }
                        )
                        Button(
                            {
                                vm.saveDrumPattern(
                                    patternName,
                                    bpm,
                                    meter,
                                    beats,
                                    kick.joinToString("") { if (it) "1" else "0" },
                                    snare.joinToString("") { if (it) "1" else "0" },
                                    hiHat.joinToString("") { if (it) "1" else "0" }
                                )
                                patternName = ""
                            },
                            Modifier.fillMaxWidth()
                        ) { Text("ذخیره در برنامه") }
                    }
                }
            }

            if (patterns.isNotEmpty()) {
                item { Text("Patternهای ذخیره‌شده", style = MaterialTheme.typography.titleMedium) }
                items(patterns, key = { it.id }) { item ->
                    ListItem(
                        headlineContent = { Text(item.name) },
                        supportingContent = { Text("${item.bpm} BPM • ${item.timeSignature} • ${item.beats} ضرب") },
                        trailingContent = {
                            Row {
                                IconButton({ loadPattern(item) }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "بارگذاری")
                                }
                                IconButton({ vm.deleteDrumPattern(item.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable private fun MetronomeCard(bpm:Int,onBpmChange:(Int)->Unit,engine:DrumEngine){
    var running by remember { mutableStateOf(false) }
    var beat by remember { mutableIntStateOf(0) }
    LaunchedEffect(running,bpm){
        if(running){ while(running){ engine.triggerKick(); beat=(beat+1)%4; delay((60000L/bpm.coerceIn(40,220)).coerceAtLeast(20L)) } }
    }
    Card(Modifier.fillMaxWidth()){ Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text("مترونوم مستقل",style=MaterialTheme.typography.titleMedium)
        Text("${bpm} BPM • ضرب ${beat+1}",style=MaterialTheme.typography.bodySmall)
        Slider(value=bpm.toFloat(), onValueChange={ onBpmChange(it.toInt()) }, valueRange=40f..220f)
        Button({running=!running},Modifier.fillMaxWidth()){Icon(if(running)Icons.Default.Stop else Icons.Default.PlayArrow,null);Spacer(Modifier.width(6.dp));Text(if(running)"توقف مترونوم" else "شروع مترونوم")}
    }}
}

@Composable private fun BackupScreen(vm:StudioViewModel,onBack:()->Unit){
    val context=LocalContext.current
    var status by remember{mutableStateOf("")}
    val create=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")){uri->if(uri!=null){vm.exportBackup(uri);status="پشتیبان‌گیری آغاز شد"}}
    val open=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){vm.importBackup(uri);status="بازیابی آغاز شد"}}
    Scaffold(topBar={TopAppBar(title={Text("پشتیبان‌گیری و بازیابی")},navigationIcon={IconButton(onBack){Icon(Icons.Default.ArrowBack,null)}})}){p->
        Column(Modifier.padding(p).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
            Text("پشتیبان کامل شامل ترانه‌ها، بخش‌ها، نسخه‌ها و فایل‌های صوتی ذخیره‌شده است.")
            Button({create.launch("StudioTaraneh-${System.currentTimeMillis()}.taraneh")},Modifier.fillMaxWidth()){Text("ساخت فایل پشتیبان") }
            OutlinedButton({open.launch(arrayOf("application/octet-stream","application/zip"))},Modifier.fillMaxWidth()){Text("بازیابی از فایل پشتیبان") }
            if(status.isNotBlank())Text(status,style=MaterialTheme.typography.bodyMedium)
            Text("توجه: در زمان بازیابی، یک نسخه جدید از داده‌های پشتیبان وارد برنامه می‌شود و داده‌های فعلی حذف نمی‌شوند.",style=MaterialTheme.typography.bodySmall)
        }
    }
}

private fun measureDuration(path:String):Long=runCatching{val m=android.media.MediaMetadataRetriever();try{m.setDataSource(path);m.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()?:0L}finally{m.release()}}.getOrDefault(0L)
private fun formatDuration(ms:Long):String{val total=ms/1000;return "%02d:%02d".format(total/60,total%60)}
