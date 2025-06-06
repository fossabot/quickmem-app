package com.pwhs.quickmem.presentation.app.study_set.add_to_folder.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pwhs.quickmem.R
import com.pwhs.quickmem.domain.model.folder.GetFolderResponseModel
import com.pwhs.quickmem.presentation.ads.BannerAds
import com.pwhs.quickmem.presentation.app.library.component.SearchTextField
import com.pwhs.quickmem.ui.theme.QuickMemTheme

@Composable
fun AddStudySetToFoldersList(
    modifier: Modifier = Modifier,
    folders: List<GetFolderResponseModel> = emptyList(),
    onAddStudySetToFolders: (String) -> Unit = {},
    folderImportedIds: List<String> = emptyList(),
    avatarUrl: String = "",
    username: String = "",
) {
    var searchQuery by remember { mutableStateOf("") }

    val filterFolders = folders.filter {
        searchQuery.trim().takeIf { query -> query.isNotEmpty() }?.let { query ->
            it.title.contains(query, ignoreCase = true)
        } ?: true
    }

    Box(modifier = modifier) {
        when {
            folders.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp)
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = stringResource(R.string.txt_user_avatar),
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = stringResource(R.string.txt_hello, username),
                        style = typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = colorScheme.onSurface.copy(alpha = 0.1f),
                    )
                    Text(
                        text = stringResource(R.string.txt_empty_folder_prompt),
                        textAlign = TextAlign.Center,
                        style = typography.bodyMedium.copy(
                            color = colorScheme.onSurface.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            else -> {
                LazyColumn {
                    item {
                        if (folders.isNotEmpty()) {
                            SearchTextField(
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                placeholder = stringResource(R.string.txt_search_folders),
                            )
                        }
                    }
                    items(items = filterFolders, key = { it.id }) { folder ->
                        AddStudySetToFoldersItem(
                            folder = folder,
                            onAddStudySetToFolders = {
                                onAddStudySetToFolders(it)
                            },
                            isAdded = folderImportedIds.contains(folder.id)
                        )
                    }
                    item {
                        if (filterFolders.isEmpty() && searchQuery.trim().isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.txt_no_folders_found),
                                    style = typography.bodyLarge,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    item {
                        BannerAds(
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.padding(60.dp))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AddStudySetToFoldersListPreview() {
    QuickMemTheme {
        AddStudySetToFoldersList()
    }

}