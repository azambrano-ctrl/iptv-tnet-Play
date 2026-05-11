package com.streamvault.app.ui.screens.provider

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import com.streamvault.app.ui.interaction.mouseClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.*
import com.streamvault.app.R
import com.streamvault.app.device.rememberIsTelevisionDevice
import com.streamvault.app.ui.components.dialogs.PremiumDialog
import com.streamvault.app.ui.components.dialogs.PremiumDialogFooterButton
import com.streamvault.app.ui.components.shell.StatusPill
import com.streamvault.app.ui.theme.*
import com.streamvault.data.util.ProviderInputSanitizer
import com.streamvault.domain.model.ProviderEpgSyncMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ??? Source type ?????????????????????????????????????????????????????????????

private enum class SourceType { XTREAM, STALKER, M3U_URL, M3U_FILE }

// ??? Screen ??????????????????????????????????????????????????????????????????

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProviderSetupScreen(
    onProviderAdded: () -> Unit,
    onBack: () -> Unit,
    editProviderId: Long? = null,
    initialImportUri: String? = null,
    viewModel: ProviderSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val knownLocalM3uUrls by viewModel.knownLocalM3uUrls.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ?? Local form state ??????????????????????????????????????????????????????
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var name by rememberSaveable { mutableStateOf("TNET play") }
    var m3uUrl by rememberSaveable { mutableStateOf("") }
    var serverUrl by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var stalkerMacAddress by rememberSaveable { mutableStateOf("") }
    var stalkerDeviceProfile by rememberSaveable { mutableStateOf("") }
    var stalkerDeviceTimezone by rememberSaveable { mutableStateOf("") }
    var stalkerDeviceLocale by rememberSaveable { mutableStateOf("") }
    var fileImportError by rememberSaveable { mutableStateOf<String?>(null) }
    var handledInitialImportUri by rememberSaveable { mutableStateOf<String?>(null) }
    var showDiscardDraftDialog by rememberSaveable { mutableStateOf(false) }

    // ?? File import helper ????????????????????????????????????????????????????
    fun importM3uUri(uri: android.net.Uri) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    withContext(Dispatchers.Main) {
                        fileImportError = context.getString(R.string.setup_file_import_failed)
                    }
                    return@launch
                }
                inputStream.use {
                    var fileName = "Local_Playlist"
                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                    cursor?.use { c ->
                        if (c.moveToFirst()) {
                            val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (idx != -1) {
                                val displayName = c.getString(idx)
                                fileName = if (displayName.contains(".")) displayName.substringBeforeLast(".") else displayName
                            }
                        }
                    }
                    val ext = if (uri.toString().substringBefore('?').lowercase().endsWith(".m3u8")) "m3u8" else "m3u"
                    val outFile = java.io.File(context.filesDir, "m3u_${System.currentTimeMillis()}.$ext")
                    outFile.outputStream().use { out -> inputStream.copyTo(out) }
                    cleanupOldImportedM3uFiles(
                        filesDir = context.filesDir,
                        protectedFileUris = knownLocalM3uUrls + "file://${outFile.absolutePath}",
                        keepLatest = 20
                    )
                    withContext(Dispatchers.Main) {
                        viewModel.updateM3uTab(1)
                        m3uUrl = "file://${outFile.absolutePath}"
                        if (name.isEmpty()) name = ProviderInputSanitizer.sanitizeProviderNameForEditing(fileName)
                        fileImportError = null
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { fileImportError = resolveFileImportError(context, e) }
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? -> if (uri != null) importM3uUri(uri) }

    // ?? Effects ???????????????????????????????????????????????????????????????
    LaunchedEffect(knownLocalM3uUrls) {
        cleanupOldImportedM3uFilesAsync(context.filesDir, knownLocalM3uUrls, 20)
    }

    LaunchedEffect(initialImportUri) {
        val importUri = initialImportUri?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        if (handledInitialImportUri == importUri) return@LaunchedEffect
        handledInitialImportUri = importUri
        selectedTab = 2
        viewModel.updateM3uTab(1)
        runCatching { android.net.Uri.parse(importUri) }.getOrNull()?.let(::importM3uUri)
    }

    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            val previousLocal = uiState.m3uUrl.takeIf { it.startsWith("file://") }
            val selectedLocal = m3uUrl.takeIf { it.startsWith("file://") }
            val protectedUris = buildSet {
                knownLocalM3uUrls.forEach { knownUri ->
                    if (knownUri != previousLocal || previousLocal == selectedLocal) add(knownUri)
                }
                selectedLocal?.let(::add)
            }
            cleanupOldImportedM3uFilesAsync(context.filesDir, protectedUris, 20)
            onProviderAdded()
        }
    }

    if (uiState.pendingCombinedAttachProfileId != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.skipCreatedProviderCombinedAttach() },
            title = { Text("Add Playlist To Combined M3U?") },
            text = {
                Text(
                    buildString {
                        append("Add ")
                        append(uiState.createdProviderName ?: "this playlist")
                        append(" to ")
                        append(uiState.pendingCombinedAttachProfileName ?: "the active combined source")
                        append(" and keep that combined source active for Live TV?")
                    },
                    color = OnSurface
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.attachCreatedProviderToCombined() }) {
                    Text("Add To Combined", color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.skipCreatedProviderCombinedAttach() }) {
                    Text("Not Now", color = OnSurface)
                }
            }
        )
    }

    LaunchedEffect(editProviderId) {
        if (editProviderId != null) viewModel.loadProvider(editProviderId)
    }

    LaunchedEffect(uiState.isEditing, uiState.existingProviderId) {
        if (uiState.isEditing) {
            selectedTab = uiState.selectedTab
            serverUrl = uiState.serverUrl
            username = uiState.username
            password = uiState.password
            m3uUrl = uiState.m3uUrl
            stalkerMacAddress = uiState.stalkerMacAddress
            stalkerDeviceProfile = uiState.stalkerDeviceProfile
            stalkerDeviceTimezone = uiState.stalkerDeviceTimezone
            stalkerDeviceLocale = uiState.stalkerDeviceLocale
        }
    }

    // ?? Derived UI source type ????????????????????????????????????????????????
    val sourceType = when {
        selectedTab == 0 -> SourceType.XTREAM
        selectedTab == 1 -> SourceType.STALKER
        uiState.m3uTab == 1 -> SourceType.M3U_FILE
        else -> SourceType.M3U_URL
    }

    fun onSourceTypeSelected(type: SourceType) {
        if (uiState.isEditing) return
        when (type) {
            SourceType.XTREAM  -> {
                selectedTab = 0
                viewModel.applySourceDefaults(ProviderSetupViewModel.SetupSourceType.XTREAM)
            }
            SourceType.STALKER -> {
                selectedTab = 1
                viewModel.applySourceDefaults(ProviderSetupViewModel.SetupSourceType.STALKER)
            }
            SourceType.M3U_URL -> {
                selectedTab = 2
                viewModel.updateM3uTab(0)
                viewModel.applySourceDefaults(ProviderSetupViewModel.SetupSourceType.M3U)
            }
            SourceType.M3U_FILE-> {
                selectedTab = 2
                viewModel.updateM3uTab(1)
                viewModel.applySourceDefaults(ProviderSetupViewModel.SetupSourceType.M3U)
            }
        }
    }

    val hasUnsavedDraft = !uiState.isEditing && name.isBlank() && (
        serverUrl.isNotBlank() ||
            username.isNotBlank() ||
            password.isNotBlank() ||
            stalkerMacAddress.isNotBlank() ||
            stalkerDeviceProfile.isNotBlank() ||
            stalkerDeviceTimezone.isNotBlank() ||
            stalkerDeviceLocale.isNotBlank() ||
            m3uUrl.isNotBlank()
        )

    BackHandler {
        if (hasUnsavedDraft) {
            showDiscardDraftDialog = true
        } else {
            onBack()
        }
    }

    // ?? Layout ????????????????????????????????????????????????????????????????
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(BackgroundDeep, Background, Surface)))
    ) {
        val isWide = maxWidth >= 700.dp
        val hPad = if (isWide) 24.dp else 16.dp

        // D-pad focus chain: usernameFocus → passwordFocus → buttonFocus
        val usernameFocus = remember { FocusRequester() }
        val passwordFocus = remember { FocusRequester() }
        val buttonFocus   = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(200)
            runCatching { usernameFocus.requestFocus() }
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Panel izquierdo: logo + tagline
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.tnet_logo),
                    contentDescription = "TNET play",
                    modifier = Modifier
                        .height(80.dp)
                        .wrapContentWidth(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.Text(
                    text = "Tu entretenimiento sin límites",
                    style = androidx.tv.material3.MaterialTheme.typography.bodyMedium,
                    color = com.streamvault.app.ui.theme.OnSurfaceDim
                )
            }

            // Divisor vertical
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxHeight(0.6f)
                    .width(1.dp)
            ) {
                drawRect(color = androidx.compose.ui.graphics.Color(0x33FFFFFF))
            }

            // Panel derecho: formulario
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ProviderFormContent(
                    sourceType = SourceType.XTREAM,
                    uiState = uiState,
                    name = name, onNameChange = { },
                    serverUrl = serverUrl, onServerUrlChange = { serverUrl = ProviderInputSanitizer.sanitizeUrlForEditing(it) },
                    username = username, onUsernameChange = { username = ProviderInputSanitizer.sanitizeUsernameForEditing(it) },
                    password = password, onPasswordChange = { password = ProviderInputSanitizer.sanitizePasswordForEditing(it) },
                    m3uUrl = m3uUrl, onM3uUrlChange = { },
                    stalkerMacAddress = stalkerMacAddress, onStalkerMacAddressChange = { },
                    stalkerDeviceProfile = stalkerDeviceProfile, onStalkerDeviceProfileChange = { },
                    stalkerDeviceTimezone = stalkerDeviceTimezone, onStalkerDeviceTimezoneChange = { },
                    stalkerDeviceLocale = stalkerDeviceLocale, onStalkerDeviceLocaleChange = { },
                    fileImportError = fileImportError,
                    onFilePick = { },
                    onLoginXtream = { viewModel.loginXtream(serverUrl, username, password, name) },
                    onLoginStalker = { },
                    onAddM3u = { },
                    onToggleFastSync = { viewModel.updateXtreamFastSyncEnabled(!uiState.xtreamFastSyncEnabled) },
                    onToggleM3uVodClassification = { },
                    onSelectEpgSyncMode = viewModel::updateEpgSyncMode,
                    usernameFocusRequester = usernameFocus,
                    passwordFocusRequester = passwordFocus,
                    buttonFocusRequester = buttonFocus,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (uiState.syncProgress != null) {
        SyncProgressDialog(message = uiState.syncProgress!!)
    }

    if (showDiscardDraftDialog) {
        PremiumDialog(
            title = stringResource(R.string.setup_discard_draft_title),
            subtitle = stringResource(R.string.setup_discard_draft_body),
            onDismissRequest = { showDiscardDraftDialog = false },
            content = {},
            footer = {
                PremiumDialogFooterButton(
                    label = stringResource(R.string.setup_discard_draft_cancel),
                    onClick = { showDiscardDraftDialog = false }
                )
                PremiumDialogFooterButton(
                    label = stringResource(R.string.setup_discard_draft_confirm),
                    onClick = {
                        showDiscardDraftDialog = false
                        onBack()
                    },
                    emphasized = true
                )
            }
        )
    }
}

// ??? Form content ?????????????????????????????????????????????????????????????

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProviderFormContent(
    sourceType: SourceType,
    uiState: ProviderSetupState,
    name: String, onNameChange: (String) -> Unit,
    serverUrl: String, onServerUrlChange: (String) -> Unit,
    username: String, onUsernameChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    m3uUrl: String, onM3uUrlChange: (String) -> Unit,
    stalkerMacAddress: String, onStalkerMacAddressChange: (String) -> Unit,
    stalkerDeviceProfile: String, onStalkerDeviceProfileChange: (String) -> Unit,
    stalkerDeviceTimezone: String, onStalkerDeviceTimezoneChange: (String) -> Unit,
    stalkerDeviceLocale: String, onStalkerDeviceLocaleChange: (String) -> Unit,
    fileImportError: String?,
    onFilePick: () -> Unit,
    onLoginXtream: () -> Unit,
    onLoginStalker: () -> Unit,
    onAddM3u: () -> Unit,
    onToggleFastSync: () -> Unit,
    onToggleM3uVodClassification: () -> Unit,
    onSelectEpgSyncMode: (ProviderEpgSyncMode) -> Unit,
    usernameFocusRequester: FocusRequester = remember { FocusRequester() },
    passwordFocusRequester: FocusRequester = remember { FocusRequester() },
    buttonFocusRequester: FocusRequester = remember { FocusRequester() },
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isTelevisionDevice = rememberIsTelevisionDevice()
    val usernameFieldFocus = remember { FocusRequester() }

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .imePadding()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            when (sourceType) {
                SourceType.XTREAM -> {
                    ProviderTextField(
                        value = serverUrl, onValueChange = onServerUrlChange,
                        placeholder = "URL del servidor  (ej: http://mi-servidor.com:8080)",
                        focusRequester = usernameFocusRequester,
                        nextFocusRequester = usernameFieldFocus,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next
                        )
                    )
                    ProviderTextField(
                        value = username, onValueChange = onUsernameChange,
                        placeholder = androidx.compose.ui.res.stringResource(R.string.setup_user_hint),
                        focusRequester = usernameFieldFocus,
                        nextFocusRequester = passwordFocusRequester,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Next
                        )
                    )
                    ProviderTextField(
                        value = password, onValueChange = onPasswordChange,
                        placeholder = androidx.compose.ui.res.stringResource(R.string.setup_pass_hint),
                        isPassword = true,
                        focusRequester = passwordFocusRequester,
                        nextFocusRequester = buttonFocusRequester,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            keyboardType = if (isTelevisionDevice) KeyboardType.Ascii else KeyboardType.Password,
                            imeAction = ImeAction.Done
                        )
                    )
                    FormErrors(uiState.validationError, uiState.error)
                    ActionButton(
                        text = when {
                            uiState.isLoading -> androidx.compose.ui.res.stringResource(R.string.setup_connecting)
                            uiState.isEditing -> androidx.compose.ui.res.stringResource(R.string.setup_save)
                            else              -> androidx.compose.ui.res.stringResource(R.string.setup_login)
                        },
                        isLoading = uiState.isLoading,
                        focusRequester = buttonFocusRequester,
                        onClick = onLoginXtream
                    )
                }

                SourceType.STALKER -> {
                    ProviderTextField(
                        value = serverUrl, onValueChange = onServerUrlChange,
                        placeholder = "Portal URL",
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            keyboardType = if (isTelevisionDevice) KeyboardType.Ascii else KeyboardType.Uri,
                            imeAction = ImeAction.Next
                        )
                    )
                    ProviderTextField(
                        value = stalkerMacAddress, onValueChange = onStalkerMacAddressChange,
                        placeholder = "MAC address",
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Next
                        )
                    )
                    FormErrors(uiState.validationError, uiState.error)
                    ActionButton(
                        text = when {
                            uiState.isLoading -> androidx.compose.ui.res.stringResource(R.string.setup_connecting)
                            uiState.isEditing -> androidx.compose.ui.res.stringResource(R.string.setup_save)
                            else              -> androidx.compose.ui.res.stringResource(R.string.setup_login)
                        },
                        isLoading = uiState.isLoading,
                        onClick = onLoginStalker
                    )
                }

                SourceType.M3U_URL -> {
                    ProviderTextField(
                        value = m3uUrl, onValueChange = onM3uUrlChange,
                        placeholder = androidx.compose.ui.res.stringResource(R.string.setup_m3u_hint),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done)
                    )
                    FormErrors(uiState.validationError, uiState.error)
                    ActionButton(
                        text = when {
                            uiState.isLoading -> androidx.compose.ui.res.stringResource(R.string.setup_validating)
                            uiState.isEditing -> androidx.compose.ui.res.stringResource(R.string.setup_save)
                            else              -> androidx.compose.ui.res.stringResource(R.string.setup_add)
                        },
                        isLoading = uiState.isLoading,
                        onClick = onAddM3u
                    )
                }

                SourceType.M3U_FILE -> {
                    FileSelectorCard(
                        fileName = if (m3uUrl.startsWith("file://")) m3uUrl.substringAfterLast("/") else null,
                        fileSelectedHint = androidx.compose.ui.res.stringResource(R.string.setup_file_replace_hint),
                        emptySelectionTitle = androidx.compose.ui.res.stringResource(R.string.setup_file_select_title),
                        emptySelectionHint = androidx.compose.ui.res.stringResource(R.string.setup_file_browse_hint),
                        onClick = onFilePick
                    )
                    fileImportError?.let {
                        Text(text = it, style = MaterialTheme.typography.bodyMedium, color = ErrorColor)
                    }
                    FormErrors(uiState.validationError, uiState.error)
                    ActionButton(
                        text = when {
                            uiState.isLoading -> androidx.compose.ui.res.stringResource(R.string.setup_validating)
                            uiState.isEditing -> androidx.compose.ui.res.stringResource(R.string.setup_save)
                            else              -> androidx.compose.ui.res.stringResource(R.string.setup_add)
                        },
                        isLoading = uiState.isLoading,
                        onClick = onAddM3u
                    )
                }
            }
    }
}

@Composable
private fun FormErrors(validationError: String?, error: String?) {
    validationError?.let {
        Text(text = it, style = MaterialTheme.typography.bodyMedium, color = ErrorColor)
    }
    error?.let {
        Text(text = it, style = MaterialTheme.typography.bodyMedium, color = ErrorColor)
    }
}

// ??? Source type selector ן¿½ wide layout (left sidebar) ????????????????????????

@Composable
private fun SourceTypeSelectorPanel(
    sourceType: SourceType,
    isEditing: Boolean,
    isEditLabel: String,
    onSelect: (SourceType) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = SurfaceDefaults.colors(containerColor = Surface.copy(alpha = 0.92f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = isEditLabel,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.setup_shell_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceDim
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.setup_source_type_label),
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
            if (!isEditing || sourceType == SourceType.XTREAM) {
                SourceTypeCard(
                    title = androidx.compose.ui.res.stringResource(R.string.setup_xtream),
                    subtitle = androidx.compose.ui.res.stringResource(R.string.setup_info_xtream_body),
                    selected = sourceType == SourceType.XTREAM,
                    enabled = !isEditing,
                    onClick = { onSelect(SourceType.XTREAM) }
                )
            }
            if (!isEditing || sourceType == SourceType.STALKER) {
                SourceTypeCard(
                    title = androidx.compose.ui.res.stringResource(R.string.setup_stalker),
                    badge = androidx.compose.ui.res.stringResource(R.string.badge_beta),
                    subtitle = androidx.compose.ui.res.stringResource(R.string.setup_info_stalker_body),
                    selected = sourceType == SourceType.STALKER,
                    enabled = !isEditing,
                    onClick = { onSelect(SourceType.STALKER) }
                )
            }
            if (!isEditing || sourceType == SourceType.M3U_URL) {
                SourceTypeCard(
                    title = androidx.compose.ui.res.stringResource(R.string.setup_tab_url),
                    subtitle = androidx.compose.ui.res.stringResource(R.string.setup_info_m3u_body),
                    selected = sourceType == SourceType.M3U_URL,
                    enabled = !isEditing,
                    onClick = { onSelect(SourceType.M3U_URL) }
                )
            }
            if (!isEditing || sourceType == SourceType.M3U_FILE) {
                SourceTypeCard(
                    title = androidx.compose.ui.res.stringResource(R.string.setup_tab_file),
                    subtitle = androidx.compose.ui.res.stringResource(R.string.setup_file_browse_hint),
                    selected = sourceType == SourceType.M3U_FILE,
                    enabled = !isEditing,
                    onClick = { onSelect(SourceType.M3U_FILE) }
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.setup_info_manage_title),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceDim
            )
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.setup_info_manage_body),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceDim.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
private fun SourceTypeCard(
    title: String,
    badge: String? = null,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = { if (enabled) onClick() },
        modifier = Modifier.fillMaxWidth().mouseClickable(enabled = enabled, onClick = onClick),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor  = if (selected) Primary.copy(alpha = 0.18f) else SurfaceElevated,
            focusedContainerColor = if (selected) Primary.copy(alpha = 0.28f) else SurfaceHighlight
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, if (selected) Primary.copy(alpha = 0.5f) else SurfaceHighlight)),
            focusedBorder = Border(BorderStroke(2.dp, FocusBorder))
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) Primary else TextPrimary
                )
                badge?.let {
                    StatusPill(
                        label = it,
                        containerColor = AccentAmber,
                        contentColor = Color.Black,
                        horizontalPadding = 6.dp,
                        verticalPadding = 2.dp,
                        cornerRadius = 6.dp
                    )
                }
            }
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim, maxLines = 2)
        }
    }
}

// ??? Source type row ן¿½ narrow layout (top tabs) ???????????????????????????????

@Composable
private fun SourceTypeTabRow(
    sourceType: SourceType,
    isEditing: Boolean,
    onSelect: (SourceType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!isEditing || sourceType == SourceType.XTREAM) {
            TabButton(
                text = androidx.compose.ui.res.stringResource(R.string.setup_xtream),
                isSelected = sourceType == SourceType.XTREAM,
                onClick = { if (!isEditing) onSelect(SourceType.XTREAM) }
            )
        }
        if (!isEditing || sourceType == SourceType.STALKER) {
            TabButton(
                text = androidx.compose.ui.res.stringResource(R.string.setup_stalker),
                badge = androidx.compose.ui.res.stringResource(R.string.badge_beta),
                isSelected = sourceType == SourceType.STALKER,
                onClick = { if (!isEditing) onSelect(SourceType.STALKER) }
            )
        }
        if (!isEditing || sourceType == SourceType.M3U_URL) {
            TabButton(
                text = androidx.compose.ui.res.stringResource(R.string.setup_tab_url),
                isSelected = sourceType == SourceType.M3U_URL,
                onClick = { if (!isEditing) onSelect(SourceType.M3U_URL) }
            )
        }
        if (!isEditing || sourceType == SourceType.M3U_FILE) {
            TabButton(
                text = androidx.compose.ui.res.stringResource(R.string.setup_tab_file),
                isSelected = sourceType == SourceType.M3U_FILE,
                onClick = { if (!isEditing) onSelect(SourceType.M3U_FILE) }
            )
        }
    }
}

// ??? ProviderTextField ????????????????????????????????????????????????????????
//
// Key fix: uses BasicTextField with decorationBox and tracks focus via
// onFocusEvent { it.hasFocus } ן¿½ hasFocus is true when this node OR any
// descendant (the actual cursor/text composable) has focus. The old approach
// used onFocusChanged { isFocused } on an outer Box, which became false the
// moment the inner BasicTextField took focus, breaking keyboard scroll.

@Composable
private fun ProviderTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    focusRequester: FocusRequester = remember { FocusRequester() },
    nextFocusRequester: FocusRequester? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val isTelevisionDevice = rememberIsTelevisionDevice()
    var hasContainerFocus by remember { mutableStateOf(false) }
    var hasInputFocus by remember { mutableStateOf(false) }
    var acceptsInput by remember(isTelevisionDevice) { mutableStateOf(!isTelevisionDevice) }
    var pendingInputActivation by remember { mutableStateOf(false) }
    var editBaselineValue by remember { mutableStateOf(value) }
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }
    var isPasswordVisible by rememberSaveable(isPassword) { mutableStateOf(false) }
    var revealedPasswordIndex by remember { mutableStateOf<Int?>(null) }
    var previousValue by remember { mutableStateOf(value) }
    val containerFocusRequester = focusRequester
    val inputFocusRequester = remember { FocusRequester() }
    val visibilityToggleFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val isFocused = hasContainerFocus || hasInputFocus
    val passwordVisibilityDescription = if (isPassword) {
        androidx.compose.ui.res.stringResource(
            if (isPasswordVisible) R.string.setup_hide_password else R.string.setup_show_password
        )
    } else {
        null
    }

    fun activateInput() {
        if (!acceptsInput) {
            editBaselineValue = value
        }
        if (!isTelevisionDevice) {
            acceptsInput = true
            inputFocusRequester.requestFocus()
            keyboardController?.show()
            coroutineScope.launch {
                runCatching { bringIntoViewRequester.bringIntoView() }
                delay(180)
                runCatching { bringIntoViewRequester.bringIntoView() }
            }
            return
        }
        acceptsInput = true
        pendingInputActivation = true
        coroutineScope.launch {
            runCatching { bringIntoViewRequester.bringIntoView() }
        }
    }

    LaunchedEffect(value) {
        if (value != fieldValue.text) {
            val coercedSelectionStart = fieldValue.selection.start.coerceIn(0, value.length)
            val coercedSelectionEnd = fieldValue.selection.end.coerceIn(0, value.length)
            val coercedComposition = fieldValue.composition?.let { composition ->
                val compositionStart = composition.start.coerceIn(0, value.length)
                val compositionEnd = composition.end.coerceIn(0, value.length)
                if (compositionStart <= compositionEnd) {
                    TextRange(compositionStart, compositionEnd)
                } else {
                    null
                }
            }
            fieldValue = fieldValue.copy(
                text = value,
                selection = TextRange(coercedSelectionStart, coercedSelectionEnd),
                composition = coercedComposition
            )
        }
    }

    LaunchedEffect(acceptsInput, pendingInputActivation) {
        if (!isTelevisionDevice || !acceptsInput || !pendingInputActivation) {
            return@LaunchedEffect
        }
        inputFocusRequester.requestFocus()
        keyboardController?.show()
        coroutineScope.launch {
            delay(120)
            runCatching { bringIntoViewRequester.bringIntoView() }
        }
        pendingInputActivation = false
    }

    LaunchedEffect(isPassword) {
        if (!isPassword) {
            isPasswordVisible = false
        }
    }

    // Show most-recently typed character briefly before masking
    LaunchedEffect(value, isPassword) {
        if (!isPassword) { previousValue = value; revealedPasswordIndex = null; return@LaunchedEffect }
        revealedPasswordIndex = when {
            value.length > previousValue.length && value.isNotEmpty() -> value.lastIndex
            value.isEmpty() -> null
            else -> revealedPasswordIndex?.takeIf { it < value.length }
        }
        previousValue = value
    }
    LaunchedEffect(revealedPasswordIndex, value, isPassword) {
        val idx = revealedPasswordIndex ?: return@LaunchedEffect
        if (!isPassword || idx >= value.length) return@LaunchedEffect
        delay(1500)
        if (revealedPasswordIndex == idx) revealedPasswordIndex = null
    }

    val borderColor by animateColorAsState(if (isFocused) Primary else SurfaceHighlight, tween(150), label = "border")
    val bgColor     by animateColorAsState(if (isFocused) Surface  else SurfaceElevated, tween(150), label = "bg")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(containerFocusRequester)
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusEvent {
                hasContainerFocus = it.hasFocus
                if (!it.hasFocus && isTelevisionDevice) {
                    acceptsInput = false
                    keyboardController?.hide()
                }
            }
            .mouseClickable(focusRequester = containerFocusRequester, onClick = ::activateInput)
            .clickable(onClick = ::activateInput)
            .focusable()
            .padding(0.dp)
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = fieldValue,
            onValueChange = { updatedValue ->
                fieldValue = updatedValue
                if (updatedValue.text != value) {
                    onValueChange(updatedValue.text)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(inputFocusRequester)
                .focusProperties {
                    canFocus = !isTelevisionDevice || acceptsInput
                    if (isTelevisionDevice && acceptsInput) {
                        left = FocusRequester.Cancel
                        right = if (isPassword) visibilityToggleFocusRequester else FocusRequester.Cancel
                    }
                }
                .onPreviewKeyEvent { event ->
                    if (!isTelevisionDevice || !acceptsInput || event.nativeKeyEvent.action != android.view.KeyEvent.ACTION_DOWN) {
                        return@onPreviewKeyEvent false
                    }
                    val cursor = fieldValue.selection.end
                    when (event.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_BACK -> {
                            fieldValue = TextFieldValue(
                                text = editBaselineValue,
                                selection = TextRange(editBaselineValue.length)
                            )
                            if (editBaselineValue != value) {
                                onValueChange(editBaselineValue)
                            }
                            acceptsInput = false
                            pendingInputActivation = false
                            keyboardController?.hide()
                            containerFocusRequester.requestFocus()
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                            val nextCursor = (cursor - 1).coerceAtLeast(0)
                            fieldValue = fieldValue.copy(selection = TextRange(nextCursor))
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (isPassword && cursor >= fieldValue.text.length) {
                                visibilityToggleFocusRequester.requestFocus()
                                return@onPreviewKeyEvent true
                            }
                            val nextCursor = (cursor + 1).coerceAtMost(fieldValue.text.length)
                            fieldValue = fieldValue.copy(selection = TextRange(nextCursor))
                            true
                        }
                        else -> false
                    }
                }
                .onFocusEvent {
                    hasInputFocus = it.hasFocus
                    if (it.hasFocus) {
                        coroutineScope.launch {
                            delay(120)
                            runCatching { bringIntoViewRequester.bringIntoView() }
                        }
                    } else if (isTelevisionDevice) {
                        keyboardController?.hide()
                    }
                },
            singleLine = true,
            readOnly = isTelevisionDevice && !acceptsInput,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = OnBackground),
            visualTransformation = when {
                !isPassword || isPasswordVisible -> VisualTransformation.None
                else -> RevealingPasswordVisualTransformation(revealedPasswordIndex)
            },
            cursorBrush = SolidColor(Primary),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .background(bgColor, RoundedCornerShape(10.dp))
                        .border(if (isFocused) 2.dp else 1.dp, borderColor, RoundedCornerShape(10.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty()) {
                            Text(text = placeholder, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceDim)
                        }
                        innerTextField()
                    }

                    if (isPassword) {
                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(24.dp)
                                .focusRequester(visibilityToggleFocusRequester)
                                .focusProperties {
                                    canFocus = !isTelevisionDevice || acceptsInput
                                    left = inputFocusRequester
                                }
                                .onFocusEvent {
                                    if (it.hasFocus) {
                                        coroutineScope.launch {
                                            delay(120)
                                            runCatching { bringIntoViewRequester.bringIntoView() }
                                        }
                                    }
                                }
                                .semantics {
                                    contentDescription = passwordVisibilityDescription.orEmpty()
                                }
                                .clickable {
                                    isPasswordVisible = !isPasswordVisible
                                }
                                .mouseClickable(focusRequester = visibilityToggleFocusRequester) {
                                    isPasswordVisible = !isPasswordVisible
                                }
                                .focusable(enabled = !isTelevisionDevice || acceptsInput)
                        ) {
                            PasswordVisibilityGlyph(
                                isVisible = isPasswordVisible,
                                tint = if (isFocused) Primary else OnSurfaceDim,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun PasswordVisibilityGlyph(
    isVisible: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(18.dp)) {
        val stroke = Stroke(width = size.minDimension * 0.11f)
        drawOval(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(1.6f, size.height * 0.22f),
            size = androidx.compose.ui.geometry.Size(size.width - 3.2f, size.height * 0.56f),
            style = stroke
        )
        drawCircle(
            color = tint,
            radius = size.minDimension * 0.14f,
            center = center
        )
        if (!isVisible) {
            drawLine(
                color = tint,
                start = androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.82f),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.18f),
                strokeWidth = size.minDimension * 0.11f
            )
        }
    }
}

// ??? Sync progress dialog ?????????????????????????????????????????????????????

@Composable
fun SyncProgressDialog(message: String) {
    PremiumDialog(
        title = androidx.compose.ui.res.stringResource(R.string.settings_syncing_title),
        subtitle = androidx.compose.ui.res.stringResource(R.string.settings_syncing_subtitle),
        onDismissRequest = {},
        widthFraction = 0.32f,
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = Primary)
                StatusPill(
                    label = androidx.compose.ui.res.stringResource(R.string.settings_syncing_btn),
                    containerColor = PrimaryGlow
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnBackground,
                    textAlign = TextAlign.Center
                )
            }
        }
    )
}

// ??? ActionButton ?????????????????????????????????????????????????????????????

@Composable
private fun ActionButton(
    text: String,
    isLoading: Boolean = false,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.04f else 1f, tween(150), label = "scale")

    Surface(
        onClick = { if (!isLoading) onClick() },
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .scale(scale)
            .focusRequester(focusRequester)
            .onFocusEvent { isFocused = it.hasFocus }
            .mouseClickable(enabled = !isLoading, onClick = onClick),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (!isLoading) Primary else SurfaceHighlight,
            focusedContainerColor = if (!isLoading) PrimaryLight else SurfaceHighlight
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, if (!isLoading) PrimaryLight else SurfaceHighlight)),
            focusedBorder = Border(BorderStroke(3.dp, FocusBorder))
        )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp,
                    color = OnBackground.copy(alpha = 0.6f)
                )
            } else {
                Text(text = text, style = MaterialTheme.typography.bodyLarge, color = Color.White)
            }
        }
    }
}

// ??? FileSelectorCard ?????????????????????????????????????????????????????????

@Composable
private fun FileSelectorCard(
    fileName: String?,
    fileSelectedHint: String,
    emptySelectionTitle: String,
    emptySelectionHint: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) Primary else SurfaceHighlight
    val bgColor     = if (isFocused) Surface  else SurfaceElevated

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(80.dp).onFocusEvent { isFocused = it.hasFocus }.mouseClickable(onClick = onClick),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, borderColor)),
            focusedBorder = Border(BorderStroke(2.dp, FocusBorder))
        ),
        colors = ClickableSurfaceDefaults.colors(containerColor = bgColor, focusedContainerColor = bgColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (fileName != null) {
                Text(text = fileName, style = MaterialTheme.typography.bodyLarge, color = OnBackground, textAlign = TextAlign.Center)
                Text(text = fileSelectedHint, style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim)
            } else {
                Text(text = emptySelectionTitle, style = MaterialTheme.typography.bodyLarge, color = OnBackground)
                Text(text = emptySelectionHint, style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim)
            }
        }
    }
}

// ??? TabButton (used by SourceTypeTabRow) ?????????????????????????????????????

@Composable
private fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit, badge: String? = null) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = Modifier.onFocusEvent { isFocused = it.hasFocus }.mouseClickable(onClick = onClick),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Primary.copy(alpha = 0.2f) else Surface,
            focusedContainerColor = SurfaceHighlight
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, if (isSelected) Primary.copy(alpha = 0.4f) else SurfaceHighlight)),
            focusedBorder = Border(BorderStroke(2.dp, FocusBorder))
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) Primary else if (isFocused) TextPrimary else OnSurface
            )
            badge?.let {
                StatusPill(
                    label = it,
                    containerColor = AccentAmber,
                    contentColor = Color.Black,
                    horizontalPadding = 6.dp,
                    verticalPadding = 2.dp,
                    cornerRadius = 6.dp
                )
            }
        }
    }
}

// ─── Revealing password visual transformation ─────────────────────────────────

private class RevealingPasswordVisualTransformation(
    private val revealedIndex: Int?
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val transformed = buildString(text.length) {
            text.text.forEachIndexed { index, ch ->
                append(if (revealedIndex != null && index == revealedIndex) ch else '*')
            }
        }
        return TransformedText(AnnotatedString(transformed), OffsetMapping.Identity)
    }
}

// ─── File cleanup helpers ─────────────────────────────────────────────────────

private fun cleanupOldImportedM3uFiles(
    filesDir: java.io.File,
    protectedFileUris: Set<String>,
    keepLatest: Int
) {
    val protectedPaths = protectedFileUris
        .mapNotNull { it.removePrefix("file://").takeIf { p -> p.isNotBlank() } }
        .toSet()

    val importedFiles = filesDir
        .listFiles { file -> file.isFile && file.name.startsWith("m3u_") && file.name.endsWith(".m3u") }
        ?.sortedByDescending { it.lastModified() }
        ?: return

    importedFiles.drop(keepLatest).forEach { stale ->
        if (stale.absolutePath !in protectedPaths) runCatching { stale.delete() }
    }
}

private suspend fun cleanupOldImportedM3uFilesAsync(
    filesDir: java.io.File,
    protectedFileUris: Set<String>,
    keepLatest: Int
) = withContext(Dispatchers.IO) {
    cleanupOldImportedM3uFiles(filesDir, protectedFileUris, keepLatest)
}

private fun resolveFileImportError(context: android.content.Context, error: Throwable): String {
    val msg = error.message.orEmpty()
    val isStorageFull = msg.contains("ENOSPC", ignoreCase = true) ||
        msg.contains("no space", ignoreCase = true) ||
        msg.contains("space left", ignoreCase = true)
    return if (isStorageFull) context.getString(R.string.setup_file_import_storage_full)
           else context.getString(R.string.setup_file_import_failed)
}
