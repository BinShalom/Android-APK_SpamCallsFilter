package io.github.binshalom.prefixblocker

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.DateFormat
import java.util.Date

private val TOLL_FREE = listOf("800", "833", "844", "855", "866", "877", "888")

@Composable
fun MainScreen(
    repo: BlockRepository,
    roleHeld: Boolean,
    contactsGranted: Boolean,
    onRequestRole: () -> Unit,
    onToggleContacts: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicLightColorScheme(context)
    else lightColorScheme()

    MaterialTheme(colorScheme = colors) {
        Surface(Modifier.fillMaxSize()) {
            val enabled by repo.enabled.collectAsStateWithLifecycle()
            val allowContacts by repo.allowContacts.collectAsStateWithLifecycle()
            val prefixes by repo.prefixFlow.collectAsStateWithLifecycle()
            val exceptions by repo.exceptionFlow.collectAsStateWithLifecycle()
            val log by repo.log.collectAsStateWithLifecycle()

            LazyColumn(
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Text("Prefix Blocker", style = MaterialTheme.typography.headlineMedium) }

                // ---- Setup status ----
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (roleHeld) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (roleHeld) {
                                Text("Active: this app is your call screening app.")
                            } else {
                                Text("Not active yet. Android needs you to choose this app as your call screening app.")
                                Button(onClick = onRequestRole) { Text("Set as call screening app") }
                            }
                        }
                    }
                }

                item {
                    SwitchRow("Blocking enabled", enabled) { repo.setEnabled(it) }
                }

                // ---- Blocked prefixes ----
                item { SectionTitle("Blocked prefixes") }
                item {
                    Text(
                        "Any incoming number starting with these digits is blocked (US numbers, country code ignored).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                item { AddRow("Prefix, e.g. 800", maxLen = 10) { repo.addPrefix(it) } }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Quick add:", Modifier.align(Alignment.CenterVertically))
                    }
                }
                item {
                    FlowChips(TOLL_FREE, prefixes) { repo.addPrefix(it) }
                }
                if (prefixes.isEmpty()) {
                    item { Text("No prefixes yet.", style = MaterialTheme.typography.bodyMedium) }
                }
                items(prefixes, key = { "p$it" }) { p ->
                    ListRow(p) { repo.removePrefix(p) }
                }

                // ---- Exceptions ----
                item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
                item { SectionTitle("Exceptions (always allowed)") }
                item {
                    SwitchRow(
                        "Allow numbers saved in my contacts",
                        allowContacts && contactsGranted
                    ) { onToggleContacts(it) }
                }
                item {
                    Text(
                        "Specific numbers that should always ring through, even if they match a blocked prefix:",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                item { AddRow("Full number, e.g. 8005551234", maxLen = 15) { repo.addException(it) } }
                if (exceptions.isEmpty()) {
                    item { Text("No exceptions yet.", style = MaterialTheme.typography.bodyMedium) }
                }
                items(exceptions, key = { "e$it" }) { n ->
                    ListRow(n) { repo.removeException(n) }
                }

                // ---- Log ----
                item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionTitle("Recently blocked")
                        if (log.isNotEmpty()) TextButton(onClick = { repo.clearLog() }) { Text("Clear") }
                    }
                }
                if (log.isEmpty()) {
                    item { Text("Nothing blocked yet.", style = MaterialTheme.typography.bodyMedium) }
                }
                items(log, key = { "l${it.time}${it.number}" }) { c ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(c.number, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(c.time))}  ·  matched ${c.prefix}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) =
    Text(text, style = MaterialTheme.typography.titleMedium)

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun AddRow(hint: String, maxLen: Int, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { new -> text = new.filter { it.isDigit() }.take(maxLen) },
            modifier = Modifier.weight(1f),
            label = { Text(hint) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        FilledIconButton(
            onClick = { onAdd(text); text = "" },
            enabled = text.isNotEmpty()
        ) { Icon(Icons.Default.Add, contentDescription = "Add") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowChips(options: List<String>, already: List<String>, onClick: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { opt ->
            FilterChip(
                selected = opt in already,
                onClick = { onClick(opt) },
                label = { Text(opt) }
            )
        }
    }
}

@Composable
private fun ListRow(text: String, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Remove $text") }
    }
}
