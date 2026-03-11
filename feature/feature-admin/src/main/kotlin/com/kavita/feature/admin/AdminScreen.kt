package com.kavita.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.kavita.core.model.Library
import com.kavita.core.model.repository.UserInfo
import com.kavita.core.core.ui.R
import com.kavita.core.ui.components.EmptyState
import com.kavita.core.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.administration)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingIndicator(modifier = Modifier.padding(innerPadding))
            !uiState.isAdmin -> {
                EmptyState(
                    message = stringResource(R.string.no_admin_permission),
                    modifier = Modifier.padding(innerPadding),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Info del servidor
                    uiState.serverInfo?.let { info ->
                        item {
                            ServerInfoCard(info)
                        }
                    }

                    // Bibliotecas
                    item {
                        Text(
                            text = stringResource(R.string.libraries),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }

                    items(uiState.libraries) { library ->
                        LibraryAdminItem(
                            library = library,
                            isScanning = uiState.isScanningLibrary == library.id,
                            onScan = { viewModel.scanLibrary(library.id) },
                            onRefreshMetadata = { viewModel.refreshMetadata(library.id) },
                        )
                    }

                    // Seccion de usuarios
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.users),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            IconButton(onClick = viewModel::showInviteDialog) {
                                Icon(Icons.Default.PersonAdd, contentDescription = stringResource(R.string.invite_user))
                            }
                        }
                    }

                    if (uiState.isLoadingUsers) {
                        item { LoadingIndicator() }
                    } else {
                        items(uiState.users) { user ->
                            UserItem(
                                user = user,
                                isDeleting = uiState.isDeletingUser == user.id,
                                onDelete = { viewModel.deleteUser(user.id) },
                            )
                        }
                    }
                }

                if (uiState.showInviteDialog) {
                    var email by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = viewModel::dismissInviteDialog,
                        title = { Text(stringResource(R.string.invite_user)) },
                        text = {
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text(stringResource(R.string.email_label)) },
                                singleLine = true,
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = { viewModel.inviteUser(email) },
                                enabled = email.isNotBlank(),
                            ) { Text(stringResource(R.string.invite)) }
                        },
                        dismissButton = {
                            TextButton(onClick = viewModel::dismissInviteDialog) { Text(stringResource(R.string.cancel)) }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerInfoCard(info: com.kavita.core.model.repository.ServerAdminInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.server_info),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.version_label, info.version),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(R.string.os_label, info.os),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(R.string.cores_label, info.numOfCores),
                style = MaterialTheme.typography.bodySmall,
            )
            if (info.isDocker) {
                Text(
                    text = stringResource(R.string.running_in_docker),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun LibraryAdminItem(
    library: Library,
    isScanning: Boolean,
    onScan: () -> Unit,
    onRefreshMetadata: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = library.name,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = library.type.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            } else {
                Row {
                    IconButton(onClick = onScan) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.scan))
                    }
                    IconButton(onClick = onRefreshMetadata) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh_metadata))
                    }
                }
            }
        }
    }
}

@Composable
private fun UserItem(
    user: UserInfo,
    isDeleting: Boolean,
    onDelete: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(user.username) },
        supportingContent = {
            Column {
                user.email?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Text(
                    text = user.roles.joinToString(", "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        trailingContent = {
            if (isDeleting) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else if (!user.roles.contains("Admin")) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_user))
                }
            }
        },
    )
}
