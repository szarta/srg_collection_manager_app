package com.srg.inventory.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    viewModel: CollectionViewModel,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    val selectedCardType by viewModel.selectedCardType.collectAsState()
    val selectedDeckCardNumbers by viewModel.selectedDeckCardNumbers.collectAsState()
    val selectedDivision by viewModel.selectedDivision.collectAsState()
    val searchScopes by viewModel.searchScopes.collectAsState()
    val minPower by viewModel.minPower.collectAsState()
    val minTechnique by viewModel.minTechnique.collectAsState()
    val minAgility by viewModel.minAgility.collectAsState()
    val minStrike by viewModel.minStrike.collectAsState()
    val minSubmission by viewModel.minSubmission.collectAsState()
    val minGrapple by viewModel.minGrapple.collectAsState()
    val cardTypes by viewModel.cardTypes.collectAsState()
    val divisions by viewModel.divisions.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Title
                Text(
                    text = "Filter Cards",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Search Scope Toggles
                    Text("Search in:", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = searchScopes.contains("name"),
                            onClick = {
                                val newScopes = if (searchScopes.contains("name")) {
                                    searchScopes - "name"
                                } else {
                                    searchScopes + "name"
                                }
                                viewModel.setSearchScopes(newScopes)
                            },
                            label = { Text("Name") }
                        )
                        FilterChip(
                            selected = searchScopes.contains("tags"),
                            onClick = {
                                val newScopes = if (searchScopes.contains("tags")) {
                                    searchScopes - "tags"
                                } else {
                                    searchScopes + "tags"
                                }
                                viewModel.setSearchScopes(newScopes)
                            },
                            label = { Text("Tags") }
                        )
                        FilterChip(
                            selected = searchScopes.contains("rules_text"),
                            onClick = {
                                val newScopes = if (searchScopes.contains("rules_text")) {
                                    searchScopes - "rules_text"
                                } else {
                                    searchScopes + "rules_text"
                                }
                                viewModel.setSearchScopes(newScopes)
                            },
                            label = { Text("Card Text") }
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 16.dp))

                    // Card Type
                    Text("Card Type:", style = MaterialTheme.typography.titleMedium)
                    LazyRow(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCardType == null,
                                onClick = { viewModel.setCardTypeFilter(null) },
                                label = { Text("All") }
                            )
                        }
                        items(cardTypes) { type ->
                            FilterChip(
                                selected = selectedCardType == type,
                                onClick = { viewModel.setCardTypeFilter(type) },
                                label = { Text(type.replace("Card", "")) }
                            )
                        }
                    }

                    // MainDeckCard: Deck Numbers (multi-select)
                    if (selectedCardType == "MainDeckCard") {
                        Divider(modifier = Modifier.padding(vertical = 16.dp))
                        Text("Deck Card Numbers:", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (selectedDeckCardNumbers.isEmpty()) "All" else selectedDeckCardNumbers.sorted().joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Grid of numbers 1-30 (6 rows of 5)
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            for (row in 0..5) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    for (col in 1..5) {
                                        val number = row * 5 + col
                                        if (number <= 30) {
                                            FilterChip(
                                                selected = selectedDeckCardNumbers.contains(number),
                                                onClick = {
                                                    val newNumbers = if (selectedDeckCardNumbers.contains(number)) {
                                                        selectedDeckCardNumbers - number
                                                    } else {
                                                        selectedDeckCardNumbers + number
                                                    }
                                                    viewModel.setDeckCardNumbers(newNumbers)
                                                },
                                                label = { Text(number.toString()) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                                if (row < 5) Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }

                    // Competitor: Stats filters
                    val isCompetitor = selectedCardType?.contains("Competitor") == true
                    if (isCompetitor) {
                        Divider(modifier = Modifier.padding(vertical = 16.dp))
                        Text("Minimum Stats:", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        StatSlider(
                            label = "Power (PWR)",
                            value = minPower,
                            onValueChange = { viewModel.setMinPower(it) }
                        )
                        StatSlider(
                            label = "Technique (TEC)",
                            value = minTechnique,
                            onValueChange = { viewModel.setMinTechnique(it) }
                        )
                        StatSlider(
                            label = "Agility (AGI)",
                            value = minAgility,
                            onValueChange = { viewModel.setMinAgility(it) }
                        )
                        StatSlider(
                            label = "Strike (STR)",
                            value = minStrike,
                            onValueChange = { viewModel.setMinStrike(it) }
                        )
                        StatSlider(
                            label = "Submission (SUB)",
                            value = minSubmission,
                            onValueChange = { viewModel.setMinSubmission(it) }
                        )
                        StatSlider(
                            label = "Grapple (GRP)",
                            value = minGrapple,
                            onValueChange = { viewModel.setMinGrapple(it) }
                        )
                    }

                    // SingleCompetitor: Division
                    if (selectedCardType == "SingleCompetitorCard") {
                        Divider(modifier = Modifier.padding(vertical = 16.dp))
                        Text("Division:", style = MaterialTheme.typography.titleMedium)
                        LazyRow(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedDivision == null,
                                    onClick = { viewModel.setDivisionFilter(null) },
                                    label = { Text("All") }
                                )
                            }
                            items(divisions) { division ->
                                FilterChip(
                                    selected = selectedDivision == division,
                                    onClick = { viewModel.setDivisionFilter(division) },
                                    label = { Text(division) }
                                )
                            }
                        }
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.clearFilters()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear All")
                    }

                    Button(
                        onClick = onApply,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

@Composable
fun StatSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value.toString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 5f..30f,
            steps = 24 // 25 discrete values (5-30 inclusive)
        )
    }
}
