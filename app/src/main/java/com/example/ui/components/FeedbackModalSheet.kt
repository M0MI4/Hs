package com.example.ui.components

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalizedStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackModalSheet(
    onDismiss: () -> Unit,
    onSubmitFeedback: suspend (type: String, subject: String, details: String, deviceInfo: String) -> Result<String>,
    modifier: Modifier = Modifier,
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    initialCategoryKey: String = "SUGGESTION",
    initialSubject: String = ""
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val brokenLinkLabel = LocalizedStrings.get("feedback_broken_link_chip", currentLanguage)
    val categories = listOf("Suggestion", brokenLinkLabel, "Bug Report", "Crash Trace", "General")
    val categoryKeys = listOf("SUGGESTION", "BROKEN_LINK", "BUG", "CRASH", "GENERAL")
    val initialIndex = remember(initialCategoryKey) {
        categoryKeys.indexOf(initialCategoryKey).takeIf { it >= 0 } ?: 0
    }
    var selectedCategoryIndex by remember(initialIndex) { mutableIntStateOf(initialIndex) }

    var subject by remember(initialSubject) { mutableStateOf(initialSubject) }
    var details by remember { mutableStateOf("") }
    var includeTelemetry by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    val deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} | Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}) | HsStore v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        },
        modifier = modifier.testTag("feedback_modal_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = LocalizedStrings.get("feedback_sheet_title", currentLanguage),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                )

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Text(
                text = "Reports are logged and reviewed directly by the HsStore team.",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Horizontal category chips so all 5 categories (including Broken Link) fit cleanly
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEachIndexed { idx, label ->
                    val selected = idx == selectedCategoryIndex
                    FilterChip(
                        selected = selected,
                        onClick = { selectedCategoryIndex = idx },
                        label = {
                            Text(
                                text = label,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("feedback_cat_${categoryKeys[idx].lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject / Target App") },
                placeholder = { Text("e.g. Mod download crash or New mod request") },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feedback_subject_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                label = { Text("Detailed Description") },
                placeholder = { Text("Describe the broken link, issue, or suggestion...") },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                minLines = 4,
                maxLines = 7,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feedback_details_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Include Diagnostic Telemetry",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = deviceInfo,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    )
                }
                Switch(
                    checked = includeTelemetry,
                    onCheckedChange = { includeTelemetry = it }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (subject.isBlank() || details.isBlank()) {
                        Toast.makeText(context, "Please enter both subject and details", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        isSubmitting = true
                        val telemetry = if (includeTelemetry) deviceInfo else "Telemetry disabled"
                        val key = categoryKeys[selectedCategoryIndex]
                        val res = onSubmitFeedback(key, subject.trim(), details.trim(), telemetry)
                        isSubmitting = false
                        if (res.isSuccess) {
                            Toast.makeText(context, "Report sent! Ref: ${res.getOrNull()}", Toast.LENGTH_LONG).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                enabled = !isSubmitting && subject.isNotBlank() && details.isNotBlank(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("submit_feedback_sheet_btn")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Report", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
