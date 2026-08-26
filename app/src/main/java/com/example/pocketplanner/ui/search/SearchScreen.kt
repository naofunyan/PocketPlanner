package com.example.pocketplanner.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.pocketplanner.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (List<String>) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }

    // Start empty, users will add destinations via the search bar
    val selectedDestinations = remember { mutableStateListOf<MapboxPlace>() }

    if (isSearchActive) {
        BackHandler {
            isSearchActive = false
            viewModel.clearSearch()
        }
        FullScreenAutocomplete(
            viewModel = viewModel,
            onClose = {
                isSearchActive = false
                viewModel.clearSearch()
            },
            onDestinationSelected = { dest ->
                if (!selectedDestinations.any { it.mainText == dest.mainText }) {
                    selectedDestinations.add(dest)
                }
            }
        )
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, // DYNAMIC BACKGROUND
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.search_new_trip),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground // DYNAMIC TEXT
                    )
                },
                navigationIcon = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface, // DYNAMIC BACKGROUND
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(40.dp)
                            .clickable(onClick = onNavigateBack)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.search_back_cd),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 16.dp)
            ) {
                Button(
                    onClick = { onNavigateToDetails(selectedDestinations.map { it.mainText }) },
                    enabled = selectedDestinations.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(stringResource(R.string.search_continue), fontSize = 18.sp, fontWeight = FontWeight.Bold) // DYNAMIC TEXT
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.search_where_going),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground, // DYNAMIC TEXT
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Search Input
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface, // DYNAMIC BACKGROUND
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), // DYNAMIC BORDER
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { isSearchActive = true }
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(R.string.search_search_cd),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = stringResource(R.string.search_for_destinations),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, // DYNAMIC TEXT
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Selected Destinations
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (selectedDestinations.isEmpty()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = stringResource(R.string.search_add_dest_desc),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, // DYNAMIC TEXT
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else {
                    selectedDestinations.forEach { dest ->
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant, // DYNAMIC BACKGROUND
                            shadowElevation = 2.dp,
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape), // DYNAMIC BACKGROUND
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (dest.emojiFlag != null) {
                                        Text(text = dest.emojiFlag, style = MaterialTheme.typography.bodyMedium)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer, // DYNAMIC ICON
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = dest.mainText,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant // DYNAMIC TEXT
                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.search_remove_cd),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant, // DYNAMIC ICON
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { selectedDestinations.remove(dest) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenAutocomplete(
    viewModel: SearchViewModel,
    onClose: () -> Unit,
    onDestinationSelected: (MapboxPlace) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, // DYNAMIC BACKGROUND
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface, // DYNAMIC BACKGROUND
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), // DYNAMIC BORDER
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.search_back_cd), tint = MaterialTheme.colorScheme.onSurface) // DYNAMIC ICON
                        }

                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onQueryChange(it) },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            placeholder = { Text(stringResource(R.string.search_for_destinations), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) }, // DYNAMIC TEXT
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface) // DYNAMIC TEXT
                        )

                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearSearch() }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.search_clear_cd), tint = MaterialTheme.colorScheme.onSurface) // DYNAMIC ICON
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (searchResults.isNotEmpty()) {
                val groupedResults = searchResults.groupBy { it.placeType }

                LazyColumn {
                    groupedResults.forEach { (type, items) ->
                        item {
                            Text(
                                text = type.uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary // DYNAMIC TEXT
                                ),
                                modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
                            )
                        }

                        items(items.size) { index ->
                            val result = items[index]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDestinationSelected(result)
                                        onClose()
                                    }
                                    .padding(horizontal = 24.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), // DYNAMIC BACKGROUND
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (result.emojiFlag != null) {
                                        Text(text = result.emojiFlag, style = MaterialTheme.typography.bodyLarge)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant, // DYNAMIC ICON
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = result.mainText,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface // DYNAMIC TEXT
                                        )
                                    )
                                    if (result.secondaryText.isNotEmpty()) {
                                        Text(
                                            text = result.secondaryText,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant // DYNAMIC TEXT
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}