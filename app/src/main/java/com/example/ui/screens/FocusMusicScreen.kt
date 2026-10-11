package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FocusTrack
import com.example.ui.TaskMateViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusMusicScreen(
    viewModel: TaskMateViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val playerState by viewModel.playerState.collectAsState()
    val isGenerating by viewModel.isGeneratingMusic.collectAsState()
    val tracks by viewModel.generatedTracks.collectAsState()

    var promptText by remember { mutableStateOf("Warm analog lo-fi chillhop with mellow piano for studying") }
    var selectedCategory by remember { mutableStateOf("Lo-Fi Study") }
    var useShortClip by remember { mutableStateOf(true) } // true: lyria-3-clip-preview, false: lyria-3-pro-preview

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Focus Music", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (useShortClip) "Lyria 3 Clip Preview (up to 30s)" else "Lyria 3 Pro Preview (Full Track)",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryBlue
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Hero Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PastelPurpleBg),
                    border = BorderStroke(1.dp, PastelPurpleBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "FOCUS SOUNDTRACKS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PastelPurpleText,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, PastelPurpleBorder)
                            ) {
                                Text(
                                    text = if (useShortClip) "Lyria 3 Clip" else "Lyria 3 Pro",
                                    color = PastelPurpleText,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Calming ambient study soundtracks and focus audio to keep you in the flow.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimaryLight,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Active Audio Player Card (when track is loaded or playing)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("audio_player_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (playerState.isPlaying) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (playerState.isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = if (playerState.currentTrackTitle.isNotBlank()) playerState.currentTrackTitle else "No Track Playing",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (playerState.isPlaying) "Playing Focus Audio • Seamless Loop" else "Paused • Ready to Play",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (playerState.isPlaying) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.toggleLoopAudio() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = "Looping",
                                    tint = if (playerState.isLooping) PrimaryBlue else Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Animated Visualizer Bars when playing
                        if (playerState.isPlaying) {
                            AudioWaveVisualizer()
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Progress Scrubber
                        val duration = playerState.durationMs.coerceAtLeast(1)
                        val progress = (playerState.currentPositionMs.toFloat() / duration).coerceIn(0f, 1f)

                        Slider(
                            value = progress,
                            onValueChange = { newProgress ->
                                val targetMs = (newProgress * duration).toInt()
                                viewModel.seekAudio(targetMs)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryBlue,
                                activeTrackColor = PrimaryBlue
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(playerState.currentPositionMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatTime(playerState.durationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.seekAudio(0) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Replay, contentDescription = "Restart")
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            IconButton(
                                onClick = { viewModel.togglePlayPause() },
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue)
                                    .testTag("play_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            IconButton(
                                onClick = {
                                    val forward = (playerState.currentPositionMs + 5000).coerceAtMost(playerState.durationMs)
                                    viewModel.seekAudio(forward)
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Forward5, contentDescription = "Forward 5s")
                            }
                        }
                    }
                }
            }

            // Generator Input Card
            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Generate New Track",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Preset Inspiration Chips
                        Text(
                            text = "Preset Styles",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val presets = listOf(
                                "Lo-Fi Study Beats" to "Warm vinyl lo-fi chillhop with Rhodes piano and soft rain",
                                "Deep Work Binaural" to "Binaural 432Hz ambient synth drone for deep analytical focus",
                                "High-Energy Sprint" to "Upbeat 128bpm melodic synthwave for high speed task execution",
                                "Gentle Piano Flow" to "Minimalist acoustic piano with slow peaceful tempo"
                            )
                            items(presets) { (title, prompt) ->
                                FilterChip(
                                    selected = promptText == prompt,
                                    onClick = {
                                        promptText = prompt
                                        selectedCategory = title
                                    },
                                    label = { Text(title) }
                                )
                            }
                        }

                        // Text Prompt Input
                        OutlinedTextField(
                            value = promptText,
                            onValueChange = { promptText = it },
                            label = { Text("Soundtrack Prompt") },
                            placeholder = { Text("e.g. Ambient electronic soundscape for coding...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("music_prompt_input"),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 2,
                            maxLines = 4
                        )

                        // Model / Length Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilterChip(
                                selected = useShortClip,
                                onClick = { useShortClip = true },
                                label = { Text("30s Clip (lyria-3-clip-preview)") },
                                modifier = Modifier.weight(1f)
                            )

                            FilterChip(
                                selected = !useShortClip,
                                onClick = { useShortClip = false },
                                label = { Text("Full Track (lyria-3-pro-preview)") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Generate Button
                        Button(
                            onClick = {
                                viewModel.generateFocusMusic(
                                    prompt = promptText,
                                    category = selectedCategory,
                                    useShortClip = useShortClip
                                )
                            },
                            enabled = promptText.isNotBlank() && !isGenerating,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("generate_music_button")
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Composing with Lyria AI...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Soundtrack", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            // Library of Tracks Header
            item {
                Text(
                    text = "Focus Music Library (${tracks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // List of Tracks
            items(tracks, key = { it.id }) { track ->
                TrackLibraryItem(
                    track = track,
                    isPlaying = playerState.isPlaying && playerState.currentTrackTitle == track.title,
                    onPlay = { viewModel.playFocusTrack(track) }
                )
            }
        }
    }
}

@Composable
fun AudioWaveVisualizer() {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val heights = listOf(
        infiniteTransition.animateFloat(initialValue = 6f, targetValue = 28f, animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse), label = "h1"),
        infiniteTransition.animateFloat(initialValue = 18f, targetValue = 10f, animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse), label = "h2"),
        infiniteTransition.animateFloat(initialValue = 10f, targetValue = 32f, animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse), label = "h3"),
        infiniteTransition.animateFloat(initialValue = 24f, targetValue = 8f, animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse), label = "h4"),
        infiniteTransition.animateFloat(initialValue = 8f, targetValue = 26f, animationSpec = infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Reverse), label = "h5"),
        infiniteTransition.animateFloat(initialValue = 20f, targetValue = 12f, animationSpec = infiniteRepeatable(tween(460, easing = LinearEasing), RepeatMode.Reverse), label = "h6")
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .width(5.dp)
                    .height(h.value.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(PrimaryBlue)
            )
        }
    }
}

@Composable
fun TrackLibraryItem(
    track: FocusTrack,
    isPlaying: Boolean,
    onPlay: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play track",
                    tint = if (isPlaying) Color.White else PrimaryBlue
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${track.category} • ${track.model}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${track.durationSeconds}s",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

private fun formatTime(millis: Int): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
