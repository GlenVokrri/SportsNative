package com.example.sportsnative

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.json.JSONArray

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val providerList = loadProvidersFromAssets()

        setContent {
            // State to track if we are looking at providers or a specific provider's channels
            var selectedProvider by remember { mutableStateOf<Provider?>(null) }

            if (selectedProvider == null) {
                ProviderGridScreen(
                    providers = providerList,
                    onProviderSelected = { selectedProvider = it }
                )
            } else {
                // If a provider is selected, show its channels.
                // BackHandler catches the TV remote back button to return to the provider list!
                BackHandler { selectedProvider = null }

                ChannelGridScreen(
                    provider = selectedProvider!!,
                    onSourceSelected = { source: StreamSource ->
                        val intent = Intent(this@MainActivity, PlayerActivity::class.java).apply {
                            putExtra("EXTRA_SOURCE", source)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }

    private fun loadProvidersFromAssets(): List<Provider> {
        val providers = mutableListOf<Provider>()
        try {
            val jsonString = assets.open("channels.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)

            for (p in 0 until jsonArray.length()) {
                val providerObj = jsonArray.getJSONObject(p)
                val providerName = providerObj.getString("provider")

                val channelsArray = providerObj.getJSONArray("channels")
                val channelsList = mutableListOf<Channel>()

                for (i in 0 until channelsArray.length()) {
                    val channelObj = channelsArray.getJSONObject(i)
                    val channelName = channelObj.getString("name")

                    val sourcesArray = channelObj.getJSONArray("sources")
                    val sourcesList = mutableListOf<StreamSource>()

                    for (j in 0 until sourcesArray.length()) {
                        val sourceObj = sourcesArray.getJSONObject(j)
                        val title = sourceObj.getString("title")
                        val streamUrl = sourceObj.getString("streamUrl")
                        val referer = sourceObj.optString("referer", "https://daddylive.pk/")

                        sourcesList.add(StreamSource(title, streamUrl, referer))
                    }
                    channelsList.add(Channel(channelName, sourcesList))
                }
                providers.add(Provider(providerName, channelsList))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return providers
    }
}

@Composable
fun ProviderGridScreen(providers: List<Provider>, onProviderSelected: (Provider) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF121212)).padding(32.dp)
    ) {
        Text(text = "Select Provider", fontSize = 28.sp, color = Color.White, modifier = Modifier.padding(bottom = 24.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(providers) { provider ->
                GenericCard(title = provider.name, onClick = { onProviderSelected(provider) })
            }
        }
    }
}

@Composable
fun ChannelGridScreen(provider: Provider, onSourceSelected: (StreamSource) -> Unit) {
    var selectedChannelForPopup by remember { mutableStateOf<Channel?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF121212)).padding(32.dp)
    ) {
        Text(text = "${provider.name} Channels", fontSize = 28.sp, color = Color.White, modifier = Modifier.padding(bottom = 24.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(provider.channels) { channel ->
                GenericCard(title = channel.name, onClick = { selectedChannelForPopup = channel })
            }
        }
    }

    selectedChannelForPopup?.let { channel ->
        SourceSelectionDialog(
            channel = channel,
            onDismiss = { selectedChannelForPopup = null },
            onSelectSource = { source ->
                selectedChannelForPopup = null
                onSourceSelected(source)
            }
        )
    }
}

// Renamed from ChannelCard to GenericCard since it builds both Provider and Channel boxes now
@Composable
fun GenericCard(title: String, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width = if (isFocused) 3.dp else 1.dp,
                color = if (isFocused) Color(0xFF00E5FF) else Color(0xFF333333),
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                color = if (isFocused) Color(0xFF2A2A2A) else Color(0xFF1E1E1E),
                shape = RoundedCornerShape(12.dp)
            )
            .focusable()
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Text(text = title, color = Color.White, fontSize = 16.sp)
    }
}

@Composable
fun SourceSelectionDialog(
    channel: Channel,
    onDismiss: () -> Unit,
    onSelectSource: (StreamSource) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp), color = Color(0xFF1E1E1E), modifier = Modifier.width(400.dp).padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = channel.name, color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))
                Text(text = "Select Stream Server", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(channel.sources) { source ->
                        SourceButton(source = source, onClick = { onSelectSource(source) })
                    }
                }
            }
        }
    }
}

@Composable
fun SourceButton(source: StreamSource, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .fillMaxWidth().height(50.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .border(width = if (isFocused) 2.dp else 1.dp, color = if (isFocused) Color(0xFF00E5FF) else Color(0xFF383838), shape = RoundedCornerShape(8.dp))
            .background(color = if (isFocused) Color(0xFF333333) else Color(0xFF252525), shape = RoundedCornerShape(8.dp))
            .focusable().clickable { onClick() }.padding(horizontal = 16.dp)
    ) {
        Text(text = source.title, color = Color.White, fontSize = 16.sp)
    }
}