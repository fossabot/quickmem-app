package com.pwhs.quickmem.presentation.app.flashcard.create

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mr0xf00.easycrop.CropError
import com.mr0xf00.easycrop.CropResult
import com.mr0xf00.easycrop.crop
import com.mr0xf00.easycrop.rememberImageCropper
import com.mr0xf00.easycrop.rememberImagePicker
import com.mr0xf00.easycrop.ui.ImageCropperDialog
import com.pwhs.quickmem.R
import com.pwhs.quickmem.domain.model.flashcard.LanguageModel
import com.pwhs.quickmem.domain.model.flashcard.VoiceModel
import com.pwhs.quickmem.domain.model.pixabay.SearchImageResponseModel
import com.pwhs.quickmem.presentation.ads.BannerAds
import com.pwhs.quickmem.presentation.app.flashcard.component.ChipSelectImage
import com.pwhs.quickmem.presentation.app.flashcard.component.ExplanationCard
import com.pwhs.quickmem.presentation.app.flashcard.component.FlashCardTextField
import com.pwhs.quickmem.presentation.app.flashcard.component.FlashCardTextFieldContainer
import com.pwhs.quickmem.presentation.app.flashcard.component.FlashCardTopAppBar
import com.pwhs.quickmem.presentation.app.flashcard.component.FlashcardBottomSheet
import com.pwhs.quickmem.presentation.app.flashcard.component.FlashcardSelectImageBottomSheet
import com.pwhs.quickmem.presentation.app.flashcard.component.HintCard
import com.pwhs.quickmem.presentation.app.flashcard.component.LanguageBottomSheet
import com.pwhs.quickmem.presentation.app.flashcard.component.VoiceBottomSheet
import com.pwhs.quickmem.presentation.components.LoadingOverlay
import com.pwhs.quickmem.ui.theme.QuickMemTheme
import com.pwhs.quickmem.utils.ImageCompressor
import com.pwhs.quickmem.utils.bitmapToUri
import com.pwhs.quickmem.utils.toColor
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.result.ResultBackNavigator
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

@Destination<RootGraph>(
    navArgs = CreateFlashCardArgs::class
)
@Composable
fun CreateFlashCardScreen(
    modifier: Modifier = Modifier,
    viewModel: CreateFlashCardViewModel = hiltViewModel(),
    navigator: DestinationsNavigator,
    resultNavigator: ResultBackNavigator<Boolean>,
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val imageCompressor = remember { ImageCompressor(context) }
    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                CreateFlashCardUiEvent.FlashCardSaved -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.txt_flashcard_saved), Toast.LENGTH_SHORT
                    ).show()
                }

                CreateFlashCardUiEvent.FlashCardSaveError -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.txt_flashcard_save_error), Toast.LENGTH_SHORT
                    ).show()
                }

                CreateFlashCardUiEvent.LoadImage -> {
                    Timber.d("Load image")
                }
            }
        }
    }
    CreateFlashCard(
        modifier = modifier,
        term = uiState.term,
        termImageUri = uiState.termImageUri,
        termImageURL = uiState.termImageURL ?: "",
        onTermImageChanged = { viewModel.onEvent(CreateFlashCardUiAction.FlashCardTermImageChanged(it)) },
        onTermImageUrlChanged = { viewModel.onEvent(CreateFlashCardUiAction.OnTermImageChanged(it)) },
        definition = uiState.definition,
        definitionImageUri = uiState.definitionImageUri,
        definitionImageURL = uiState.definitionImageURL ?: "",
        hint = uiState.hint ?: "",
        showHint = uiState.showHint,
        explanation = uiState.explanation ?: "",
        showExplanation = uiState.showExplanation,
        isLoading = uiState.isLoading,
        onTermChanged = { viewModel.onEvent(CreateFlashCardUiAction.FlashCardTermChanged(it)) },
        onDefinitionChanged = {
            viewModel.onEvent(
                CreateFlashCardUiAction.FlashCardDefinitionChanged(
                    it
                )
            )
        },
        onDefinitionImageChanged = { uri ->
            if (uri == null) {
                viewModel.onEvent(CreateFlashCardUiAction.FlashCardDefinitionImageChanged(null))
                return@CreateFlashCard
            }
            scope.launch {
                val compressedImageBytes = imageCompressor.compressImage(uri, 200 * 1024L) // 200KB
                val compressedImageUri = compressedImageBytes?.let {
                    Uri.fromFile(
                        File(
                            context.cacheDir,
                            "compressed_image_${System.currentTimeMillis()}.jpg"
                        ).apply {
                            writeBytes(it)
                        })
                }
                viewModel.onEvent(
                    CreateFlashCardUiAction.FlashCardDefinitionImageChanged(
                        compressedImageUri
                    )
                )
            }
        },
        onHintChanged = { viewModel.onEvent(CreateFlashCardUiAction.FlashCardHintChanged(it)) },
        onShowHintClicked = { viewModel.onEvent(CreateFlashCardUiAction.ShowHintClicked(it)) },
        onExplanationChanged = {
            viewModel.onEvent(
                CreateFlashCardUiAction.FlashCardExplanationChanged(
                    it
                )
            )
        },
        onShowExplanationClicked = {
            viewModel.onEvent(
                CreateFlashCardUiAction.ShowExplanationClicked(
                    it
                )
            )
        },
        onUploadImage = { viewModel.onEvent(CreateFlashCardUiAction.UploadImage(it)) },
        onDeleteImage = {
            viewModel.onEvent(
                CreateFlashCardUiAction.RemoveImage(
                    uiState.definitionImageURL ?: ""
                )
            )
        },
        onNavigationBack = {
            resultNavigator.setResult(uiState.isCreated)
            navigator.navigateUp()
        },
        onSaveFlashCardClicked = {
            viewModel.onEvent(CreateFlashCardUiAction.SaveFlashCard)
        },
        queryImage = uiState.queryImage,
        searchImageResponseModel = uiState.searchImageResponseModel,
        onQueryImageChanged = { viewModel.onEvent(CreateFlashCardUiAction.OnQueryImageChanged(it)) },
        onDefinitionImageUrlChanged = {
            viewModel.onEvent(CreateFlashCardUiAction.OnDefinitionImageChanged(it))
        },
        isSearchImageLoading = uiState.isSearchImageLoading,
        studySetColor = uiState.studyColorModel?.hexValue?.toColor() ?: colorScheme.primary,
        termLanguageModel = uiState.termLanguageModel,
        termVoiceModel = uiState.termVoiceCode,
        termVoicesModel = uiState.termVoicesModel,
        definitionLanguageModel = uiState.definitionLanguageModel,
        definitionVoiceModel = uiState.definitionVoiceCode,
        definitionVoicesModel = uiState.definitionVoicesModel,
        languageModels = uiState.languageModels,
        onSelectLanguageClicked = { languageCode, isTerm ->
            viewModel.onEvent(CreateFlashCardUiAction.OnSelectLanguageClicked(languageCode, isTerm))
        },
        onVoiceSelected = { voiceCode, isTerm ->
            viewModel.onEvent(CreateFlashCardUiAction.OnSelectVoiceClicked(voiceCode, isTerm))
        },
    )
}

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
)
@Composable
fun CreateFlashCard(
    modifier: Modifier = Modifier,
    term: String = "",
    termImageUri: Uri? = null,
    termImageURL: String = "",
    definition: String = "",
    definitionImageUri: Uri? = null,
    definitionImageURL: String = "",
    onTermImageChanged: (Uri) -> Unit = {},
    isLoading: Boolean = false,
    hint: String = "",
    showHint: Boolean = false,
    explanation: String = "",
    showExplanation: Boolean = false,
    onTermChanged: (String) -> Unit = {},
    onDefinitionChanged: (String) -> Unit = {},
    onDefinitionImageChanged: (Uri?) -> Unit = {},
    onHintChanged: (String) -> Unit = {},
    onShowHintClicked: (Boolean) -> Unit = {},
    onExplanationChanged: (String) -> Unit = {},
    onShowExplanationClicked: (Boolean) -> Unit = {},
    onUploadImage: (Uri) -> Unit = {},
    onDeleteImage: () -> Unit = {},
    onNavigationBack: () -> Unit = {},
    onSaveFlashCardClicked: () -> Unit = {},
    queryImage: String = "",
    searchImageResponseModel: SearchImageResponseModel? = null,
    onQueryImageChanged: (String) -> Unit = {},
    onDefinitionImageUrlChanged: (String) -> Unit = {},
    onTermImageUrlChanged: (String) -> Unit = {},
    isSearchImageLoading: Boolean = false,
    studySetColor: Color = colorScheme.primary,
    termLanguageModel: LanguageModel? = null,
    termVoiceModel: VoiceModel? = null,
    termVoicesModel: List<VoiceModel> = emptyList(),
    definitionLanguageModel: LanguageModel? = null,
    definitionVoiceModel: VoiceModel? = null,
    languageModels: List<LanguageModel> = emptyList(),
    definitionVoicesModel: List<VoiceModel> = emptyList(),
    onSelectLanguageClicked: (LanguageModel, Boolean) -> Unit = { _, _ -> },
    onVoiceSelected: (VoiceModel, Boolean) -> Unit = { _, _ -> },
) {

    val bottomSheetSetting = rememberModalBottomSheetState()
    var showBottomSheetSetting by remember {
        mutableStateOf(false)
    }

    val imageCropper = rememberImageCropper()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val imagePicker = rememberImagePicker(onImage = { uri ->
        scope.launch {
            when (val result = imageCropper.crop(uri, context)) {
                CropResult.Cancelled -> { /* Handle cancellation */
                }

                is CropError -> { /* Handle error */
                }

                is CropResult.Success -> {
                    onDefinitionImageChanged(context.bitmapToUri(result.bitmap))
                }
            }
        }
    })

    val cropState = imageCropper.cropState

    var showSearchImageBottomSheet by remember {
        mutableStateOf(false)
    }

    val searchImageBottomSheet = rememberModalBottomSheetState()

    var showSelectLanguageBottomSheet by remember {
        mutableStateOf(false)
    }

    var showSelectVoiceBottomSheet by remember {
        mutableStateOf(false)
    }

    var isTermSelect by remember {
        mutableStateOf(true)
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                FlashCardTopAppBar(
                    onNavigationBack = onNavigationBack,
                    onSaveFlashCardClicked = onSaveFlashCardClicked,
                    enableSaveButton = term.isNotEmpty() && definition.isNotEmpty(),
                    onSettingsClicked = {
                        showBottomSheetSetting = true
                    },
                    title = stringResource(R.string.txt_create_flashcard),
                    color = studySetColor
                )
            },
            modifier = modifier
                .fillMaxSize()
        ) { innerPadding ->
            Box(contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    modifier = modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .imePadding()
                ) {
                    item {
                        Card(
                            modifier = modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            elevation = CardDefaults.elevatedCardElevation(
                                defaultElevation = 5.dp,
                                focusedElevation = 8.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = colorScheme.surface
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                FlashCardTextField(
                                    value = term,
                                    onValueChange = onTermChanged,
                                    hint = stringResource(R.string.txt_term),
                                    color = studySetColor
                                )
                                FlowRow(
                                    modifier = Modifier.padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    InputChip(
                                        selected = showSelectLanguageBottomSheet,
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = when {
                                                showSelectLanguageBottomSheet -> studySetColor
                                                else -> Color.Gray
                                            }
                                        ),
                                        onClick = {
                                            isTermSelect = true
                                            showSelectLanguageBottomSheet = true
                                        },
                                        label = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Language,
                                                    contentDescription = null,
                                                    tint = when {
                                                        showSelectLanguageBottomSheet -> studySetColor
                                                        else -> Color.Gray
                                                    }
                                                )
                                                Text(
                                                    text = termLanguageModel?.name
                                                        ?: stringResource(R.string.txt_english_us),
                                                    color = when {
                                                        showSelectLanguageBottomSheet -> studySetColor
                                                        else -> Color.Gray
                                                    }
                                                )
                                            }
                                        }
                                    )

                                    InputChip(
                                        selected = false,
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = when {
                                                false -> studySetColor
                                                else -> Color.Gray
                                            }
                                        ),
                                        onClick = {
                                            isTermSelect = true
                                            showSelectVoiceBottomSheet = true
                                        },
                                        label = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardVoice,
                                                    contentDescription = null,
                                                    tint = when {
                                                        showSelectVoiceBottomSheet -> studySetColor
                                                        else -> Color.Gray
                                                    }
                                                )
                                                Text(
                                                    text = termVoiceModel?.name
                                                        ?: stringResource(R.string.txt_voice),
                                                    color = when {
                                                        showSelectVoiceBottomSheet -> studySetColor
                                                        else -> Color.Gray
                                                    }
                                                )
                                            }
                                        }
                                    )

                                    InputChip(
                                        selected = showHint,
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = when {
                                                showHint -> studySetColor
                                                else -> Color.Gray
                                            }
                                        ),
                                        onClick = {
                                            onShowHintClicked(!showHint)
                                        },
                                        label = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lightbulb,
                                                    contentDescription = null,
                                                    tint = when {
                                                        showHint -> studySetColor
                                                        else -> Color.Gray
                                                    }
                                                )
                                                Text(
                                                    text = stringResource(R.string.txt_hint),
                                                    color = when {
                                                        showHint -> studySetColor
                                                        else -> Color.Gray
                                                    }
                                                )
                                            }
                                        }
                                    )

                                    ChipSelectImage(
                                        onUploadImage = onUploadImage,
                                        imageUri = termImageUri,
                                        imageUrl = termImageURL,
                                        onDeleteImage = onDeleteImage,
                                        color = when {
                                            termImageUri != null || termImageURL.isNotEmpty() -> studySetColor
                                            else -> Color.Gray
                                        },
                                        onChooseImage = {
                                            showSearchImageBottomSheet = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        if (showHint) {
                            HintCard(
                                hint = hint,
                                onHintChanged = onHintChanged,
                                onShowHintClicked = onShowHintClicked,
                                color = studySetColor
                            )
                        }
                    }
                    item {
                        FlashCardTextFieldContainer(
                            term = term,
                            onTermChanged = onTermChanged,
                            definition = definition,
                            onDefinitionChanged = onDefinitionChanged,
                            color = studySetColor
                        )
                    }

                    item {
                        if (showExplanation) {
                            ExplanationCard(
                                explanation = explanation,
                                onExplanationChanged = onExplanationChanged,
                                onShowExplanationClicked = onShowExplanationClicked,
                                color = studySetColor
                            )
                        }
                    }

                    item {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            color = studySetColor.copy(alpha = 0.5f)
                        )
                    }

                    item {
                        Text(
                            text = stringResource(R.string.txt_make_your_term_and_definition_as_clear_as_possible_you_can_add_hint_and_explanation_to_help_you_remember_better),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .padding(bottom = 32.dp),
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }

                }
                BannerAds(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
                LoadingOverlay(isLoading = isLoading)
            }

            if (showBottomSheetSetting) {
                FlashcardBottomSheet(
                    onDismissRequest = {
                        showBottomSheetSetting = false
                    },
                    sheetState = bottomSheetSetting,
                    onShowHintClicked = onShowHintClicked,
                    onShowExplanationClicked = onShowExplanationClicked
                )
            }

            if (showSearchImageBottomSheet) {
                FlashcardSelectImageBottomSheet(
                    modifier = Modifier,
                    searchImageBottomSheet = searchImageBottomSheet,
                    onDismissRequest = {
                        showSearchImageBottomSheet = false
                    },
                    queryImage = queryImage,
                    searchImageResponseModel = searchImageResponseModel,
                    onQueryImageChanged = onQueryImageChanged,
                    isSearchImageLoading = isSearchImageLoading,
                    onDefinitionImageUrlChanged = {
                        onDefinitionImageUrlChanged(it)
                        onDefinitionImageChanged(null)
                    },
                    imagePicker = imagePicker
                )
            }
        }
        if (cropState != null) {
            ImageCropperDialog(
                state = cropState,
            )
        }
    }
    if (showSelectLanguageBottomSheet) {
        LanguageBottomSheet(
            onDismissRequest = {
                showSelectLanguageBottomSheet = false
            },
            languageList = languageModels,
            onLanguageSelected = { language, isTerm ->
                onSelectLanguageClicked(language, isTerm)
                showSelectVoiceBottomSheet = true
            },
            languageModel = if (isTermSelect) termLanguageModel else definitionLanguageModel,
        )
    }

    if (showSelectVoiceBottomSheet) {
        VoiceBottomSheet(
            onDismissRequest = {
                showSelectVoiceBottomSheet = false
            },
            voiceList = if (isTermSelect) termVoicesModel else definitionVoicesModel,
            onVoiceSelected = { voice, isTerm ->
                onVoiceSelected(voice, isTerm)
            },
            voiceModel = if (isTermSelect) termVoiceModel else definitionVoiceModel,
        )
    }
}


@Preview(showSystemUi = true)
@Preview(showSystemUi = true, locale = "vi")
@Composable
fun CreateFlashCardPreview() {
    QuickMemTheme {
        CreateFlashCard()
    }
}