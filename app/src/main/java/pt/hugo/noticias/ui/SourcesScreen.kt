package pt.hugo.noticias.ui

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.ui.platform.LocalContext
import pt.hugo.noticias.notify.Notifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.hugo.noticias.data.Region

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(vm: NewsViewModel, onBack: () -> Unit) {
    val sources by vm.sources.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var region by remember { mutableStateOf(Region.PT) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fontes") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            item(key = "notify_hint") {
                Text(
                    "Toca no sino para receberes notificações das notícias novas dessa fonte. " +
                        "O Android verifica cerca de 15 em 15 minutos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                )
            }
            listOf(
                Region.PT to "Portugal",
                Region.SPORT to "Desporto",
                Region.EN to "Mundo (inglês)",
            ).forEach { (r, label) ->
                item(key = "header_$r") { SectionHeader(label) }
                items(sources.filter { it.source.region == r }, key = { it.source.id }) { item ->
                    ListItem(
                        headlineContent = { Text(item.source.name) },
                        supportingContent = { Text(Uri.parse(item.source.url).host ?: item.source.url) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    enabled = item.enabled,
                                    onClick = {
                                        val turnOn = !item.notify
                                        if (turnOn && !Notifier.canNotify(context)) {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        vm.toggleNotify(item.source.id, turnOn)
                                    },
                                ) {
                                    if (item.notify && item.enabled) {
                                        Icon(Icons.Filled.Notifications, "Desligar notificações", tint = MaterialTheme.colorScheme.primary)
                                    } else {
                                        Icon(Icons.Outlined.Notifications, "Ligar notificações")
                                    }
                                }
                                if (item.source.custom) {
                                    IconButton(onClick = { vm.removeCustomSource(item.source.id) }) {
                                        Icon(Icons.Filled.Delete, "Remover")
                                    }
                                }
                                Switch(
                                    checked = item.enabled,
                                    onCheckedChange = { vm.toggleSource(item.source.id, it) },
                                )
                            }
                        },
                    )
                }
            }

            item(key = "add") {
                Column(Modifier.padding(16.dp)) {
                    Text("Adicionar fonte (feed RSS)", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Endereço do feed (https://…)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = region == Region.PT, onClick = { region = Region.PT }, label = { Text("Portugal") })
                        FilterChip(selected = region == Region.SPORT, onClick = { region = Region.SPORT }, label = { Text("Desporto") })
                        FilterChip(selected = region == Region.EN, onClick = { region = Region.EN }, label = { Text("Mundo") })
                    }
                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        error = vm.addCustomSource(name, url, region)
                        if (error == null) {
                            name = ""
                            url = ""
                        }
                    }) { Text("Adicionar") }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}
