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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (List<String>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // Start empty, users will add destinations via the search bar
    val selectedDestinations = remember { mutableStateListOf<String>() }

    Scaffold(
        containerColor = Color(0xFFFAFAFA), // Off-white matching the mockup
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "NEW TRIP", 
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFF001F3F) // Deep navy color
                    )
                },
                navigationIcon = {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .clickable(onClick = onNavigateBack)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = "Back", 
                                tint = MaterialTheme.colorScheme.primary, 
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Back", 
                                color = MaterialTheme.colorScheme.primary, 
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
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
                    onClick = { 
                        onNavigateToDetails(selectedDestinations)
                    },
                    enabled = selectedDestinations.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("Continue", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                text = "Where are you going?",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF001F3F)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Search Input
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF001F3F),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search for destinations", color = Color.Gray, fontSize = 16.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color(0xFF001F3F)
                        ),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        val dest = searchQuery.trim()
                                        if (dest.isNotEmpty() && !selectedDestinations.contains(dest)) {
                                            selectedDestinations.add(dest)
                                            searchQuery = ""
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.Add, 
                                        contentDescription = "Add Destination",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (searchQuery.isNotBlank() && !selectedDestinations.contains(searchQuery.trim())) {
                                    selectedDestinations.add(searchQuery.trim())
                                    searchQuery = "" // Clear after adding
                                }
                            }
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
            
            // Selected Destinations
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                selectedDestinations.forEach { dest ->
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Vietnam Flag mock
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0xFFDA251D), CircleShape), // Vietnam Red
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFCD00), // Vietnam Yellow
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Text(
                                text = dest,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF001F3F)
                                )
                            )
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove",
                                tint = Color.LightGray,
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
