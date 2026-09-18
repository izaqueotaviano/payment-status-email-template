package com.iptvtv.player.ui.sources

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.iptvtv.player.domain.model.Source
import com.iptvtv.player.ui.components.AppBackground
import com.iptvtv.player.ui.components.PillButton
import com.iptvtv.player.ui.components.ProgressBar
import com.iptvtv.player.ui.components.SectionCard
import com.iptvtv.player.ui.sources.components.LabeledTextField
import com.iptvtv.player.ui.theme.BrandError
import com.iptvtv.player.ui.theme.BrandMuted
import com.iptvtv.player.ui.theme.BrandOnSurface
import kotlinx.coroutines.delay

private fun effectiveFormType(loaded: Source?, sourceType: String?): String = when (loaded) {
    is Source.M3uUrlSource -> "m3u"
    is Source.XtreamSource -> "xtream"
    is Source.LocalFileSource -> "local"
    null -> sourceType ?: "m3u"
}

private fun formTypeLabel(formType: String): String = when (formType) {
    "xtream" -> "Conta Xtream Codes"
    "local" -> "Arquivo local"
    else -> "Lista M3U por URL"
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
            // Hold the confirmation on screen long enough to be read before leaving.
            delay(1200)
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

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 56.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column {
                Text(
                    text = if (sourceId != null) "Editar fonte" else "Nova fonte",
                    color = BrandOnSurface,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(text = formTypeLabel(formType), color = BrandMuted, fontSize = 14.sp)
            }

            SectionCard(modifier = Modifier.width(680.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Conexão segura (HTTPS)",
                                    color = BrandMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    PillButton(
                                        text = "Sim",
                                        onClick = { useHttps = true },
                                        primary = useHttps,
                                    )
                                    PillButton(
                                        text = "Não",
                                        onClick = { useHttps = false },
                                        primary = !useHttps,
                                    )
                                }
                            }
                        }
                        "local" -> {
                            LabeledTextField(label = "Nome", value = name, onValueChange = { name = it })
                            PillButton(
                                text = "Selecionar arquivo",
                                onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                            )
                            Text(
                                text = if (pickedFileUri != null) "Arquivo selecionado" else "Nenhum arquivo selecionado",
                                color = BrandMuted,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }

            val savingStage = (saveState as? SaveState.Saving)?.stage
            if (savingStage != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.width(680.dp),
                ) {
                    Text(text = savingStage.label, color = BrandOnSurface, fontSize = 15.sp)
                    ProgressBar()
                    Text(
                        text = "Listas grandes podem levar um tempo. Não feche o app.",
                        color = BrandMuted,
                        fontSize = 12.sp,
                    )
                }
            }

            val savedCount = (saveState as? SaveState.Success)?.channelCount
            if (savedCount != null) {
                Text(
                    text = "Pronto! $savedCount canais importados.",
                    color = BrandOnSurface,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .width(680.dp)
                        .background(Color(0x332ECC71), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            val currentError = (saveState as? SaveState.Error)?.message
            if (currentError != null) {
                Text(
                    text = currentError,
                    color = BrandError,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .width(680.dp)
                        .background(Color(0x33FF6B81), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                val canSave = when (formType) {
                    "m3u" -> name.isNotBlank() && url.isNotBlank()
                    "xtream" -> name.isNotBlank() && host.isNotBlank() && username.isNotBlank() && password.isNotBlank()
                    "local" -> name.isNotBlank() && pickedFileUri != null
                    else -> false
                }
                PillButton(
                    text = when (saveState) {
                        is SaveState.Saving -> "Salvando..."
                        is SaveState.Success -> "Salvo"
                        else -> "Salvar"
                    },
                    primary = true,
                    enabled = canSave && saveState !is SaveState.Saving && saveState !is SaveState.Success,
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
                                fileUri = pickedFileUri ?: return@PillButton,
                            )
                            else -> return@PillButton
                        }
                        viewModel.save(sourceToSave)
                    },
                )
                PillButton(text = "Cancelar", onClick = onCancel)
            }
        }
    }
}
