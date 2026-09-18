package com.iptvtv.player.ui.sources

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.iptvtv.player.domain.model.Source
import com.iptvtv.player.ui.sources.components.LabeledTextField

private fun effectiveFormType(loaded: Source?, sourceType: String?): String = when (loaded) {
    is Source.M3uUrlSource -> "m3u"
    is Source.XtreamSource -> "xtream"
    is Source.LocalFileSource -> "local"
    null -> sourceType ?: "m3u"
}

@Composable
fun AddEditSourceScreen(
    viewModel: AddEditSourceViewModel,
    sourceId: Long?,
    sourceType: String?,
    onDone: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(sourceId) {
        if (sourceId != null) {
            viewModel.loadExisting(sourceId)
        }
    }

    val loadedSource by viewModel.loadedSource.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    LaunchedEffect(saveState) {
        if (saveState is SaveState.Success) {
            onDone()
        }
    }

    val formType = effectiveFormType(loadedSource, sourceType)

    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var useHttps by remember { mutableStateOf(false) }
    var pickedFileUri by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(loadedSource) {
        when (val source = loadedSource) {
            is Source.M3uUrlSource -> {
                name = source.name
                url = source.url
            }
            is Source.XtreamSource -> {
                name = source.name
                host = source.host
                port = source.port?.toString() ?: ""
                username = source.username
                password = source.password
                useHttps = source.useHttps
            }
            is Source.LocalFileSource -> {
                name = source.name
                pickedFileUri = source.fileUri
            }
            null -> Unit
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            pickedFileUri = uri.toString()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = if (sourceId != null) "Editar fonte" else "Nova fonte",
                style = MaterialTheme.typography.headlineMedium,
            )

            when (formType) {
                "m3u" -> {
                    LabeledTextField(label = "Nome", value = name, onValueChange = { name = it })
                    LabeledTextField(
                        label = "URL da lista M3U/M3U8",
                        value = url,
                        onValueChange = { url = it },
                        keyboardType = KeyboardType.Uri,
                    )
                }
                "xtream" -> {
                    LabeledTextField(label = "Nome", value = name, onValueChange = { name = it })
                    LabeledTextField(label = "Host", value = host, onValueChange = { host = it })
                    LabeledTextField(
                        label = "Porta (opcional)",
                        value = port,
                        onValueChange = { port = it },
                        keyboardType = KeyboardType.Number,
                    )
                    LabeledTextField(label = "Usuário", value = username, onValueChange = { username = it })
                    LabeledTextField(
                        label = "Senha",
                        value = password,
                        onValueChange = { password = it },
                        isPassword = true,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "Usar HTTPS", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { useHttps = true }) {
                                Text(text = if (useHttps) "✓ Sim" else "Sim")
                            }
                            Button(onClick = { useHttps = false }) {
                                Text(text = if (!useHttps) "✓ Não" else "Não")
                            }
                        }
                    }
                }
                "local" -> {
                    LabeledTextField(label = "Nome", value = name, onValueChange = { name = it })
                    Button(onClick = { filePickerLauncher.launch(arrayOf("*/*")) }) {
                        Text(text = "Selecionar arquivo")
                    }
                    Text(
                        text = pickedFileUri?.let { "Arquivo selecionado" } ?: "Nenhum arquivo selecionado",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            val currentError = (saveState as? SaveState.Error)?.message
            if (currentError != null) {
                Text(text = currentError, color = MaterialTheme.colorScheme.error)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                val canSave = when (formType) {
                    "m3u" -> name.isNotBlank() && url.isNotBlank()
                    "xtream" -> name.isNotBlank() && host.isNotBlank() && username.isNotBlank() && password.isNotBlank()
                    "local" -> name.isNotBlank() && pickedFileUri != null
                    else -> false
                }
                Button(
                    onClick = {
                        val sourceToSave = when (formType) {
                            "m3u" -> Source.M3uUrlSource(id = sourceId ?: 0, name = name, url = url)
                            "xtream" -> Source.XtreamSource(
                                id = sourceId ?: 0,
                                name = name,
                                host = host,
                                port = port.toIntOrNull(),
                                username = username,
                                password = password,
                                useHttps = useHttps,
                            )
                            "local" -> Source.LocalFileSource(
                                id = sourceId ?: 0,
                                name = name,
                                fileUri = pickedFileUri ?: return@Button,
                            )
                            else -> return@Button
                        }
                        viewModel.save(sourceToSave)
                    },
                    enabled = canSave && saveState !is SaveState.Saving,
                ) {
                    Text(text = if (saveState is SaveState.Saving) "Salvando..." else "Salvar")
                }
                Button(onClick = onCancel) {
                    Text(text = "Cancelar")
                }
            }
        }
    }
}
