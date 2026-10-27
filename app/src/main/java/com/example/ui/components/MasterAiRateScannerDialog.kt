package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ScannedRateRow
import com.example.ui.BossAppState
import com.example.ui.BossAppViewModel
import com.example.util.RateListDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val CarbonDarkBg = Color(0xFF0C0F14)
private val CarbonPanelBg = Color(0xFF14181F)
private val CarbonCardBg = Color(0xFF1C222B)
private val CarbonBorder = Color(0xFF2B3441)
private val BossGold = Color(0xFFF5B738)
private val SkyAccent = Color(0xFF38BDF8)
private val EmeraldAccent = Color(0xFF10B981)
private val RoseAccent = Color(0xFFF43F5E)

private const val TEST_SAMPLE_SHEET = """SLIDING WINDOWS SECTIONS
DC30C 1.6mm | DULL 815 | H23 / PC-RAL 855 | SAHARA / BROWN 875
DC26C 1.6mm | DULL 955 | BLACK / MULTI 1065
M23 Normal | DULL 352 | WOOD COAT 440
M24 (1.6mm) | DULL 495 | SAHARA / BROWN 530
KT.1 | DULL 420 | H23 / PC-RAL 445
CW-10 1.2mm mh | DULL 780 | ANODIZED 825"""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterAiRateScannerDialog(
    viewModel: BossAppViewModel,
    state: BossAppState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State for multi-image / multi-file batch scanning
    var selectedPhotoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var thumbnailBitmaps by remember { mutableStateOf<Map<Uri, Bitmap>>(emptyMap()) }
    var rawRateText by remember { mutableStateOf("") }
    var isScanningRates by remember { mutableStateOf(false) }
    var scannedRows by remember { mutableStateOf<List<ScannedRateRow>>(emptyList()) }
    var fiveColumnOutput by remember { mutableStateOf("") }
    var scanError by remember { mutableStateOf<String?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showSavedRatesModal by remember { mutableStateOf(false) }

    // Multi-photo picker (PickMultipleVisualMedia supports selecting 3, 4+ photos at once)
    val multiPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            val combined = (selectedPhotoUris + uris).distinct()
            selectedPhotoUris = combined
            // Load thumbnails in background
            scope.launch {
                val newThumbnails = thumbnailBitmaps.toMutableMap()
                for (u in uris) {
                    if (!newThumbnails.containsKey(u)) {
                        val bmp = RateListDetector.loadBitmapFromUri(context, u, 250)
                        if (bmp != null) newThumbnails[u] = bmp
                    }
                }
                thumbnailBitmaps = newThumbnails
            }
            Toast.makeText(context, "${uris.size} photo(s) added for batch rate scanning!", Toast.LENGTH_SHORT).show()
        }
    }

    // Document / PDF file picker
    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            val combined = (selectedPhotoUris + uris).distinct()
            selectedPhotoUris = combined
            Toast.makeText(context, "${uris.size} document file(s) added!", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 28.dp, bottom = 12.dp, start = 8.dp, end = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = CarbonDarkBg,
            border = BorderStroke(1.dp, CarbonBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = BossGold.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = BossGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Industrial AI Rate Scanner",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Multi-Photo (3, 4+ at once) • Standardized 5-Column Dataset",
                                color = BossGold,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { showSavedRatesModal = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BossGold.copy(alpha = 0.15f),
                                contentColor = BossGold
                            ),
                            border = BorderStroke(1.dp, BossGold.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Saved Rates", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Explanatory note
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CarbonPanelBg,
                        border = BorderStroke(1.dp, CarbonBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = SkyAccent, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Aap aik sath 3, 4 ya zyada Rate List Photos / PDFs akathay select kar ke scan kar saktay hain. System tamam pages ko aik sath read kar ke 5-column dataset banaye ga!",
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    // Database Rate Control Bar (Clear All & Restore Defaults)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = RoseAccent.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, RoseAccent.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Database Rate Control:",
                                    color = Color(0xFFFECDD3),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { showClearConfirm = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = RoseAccent,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Clear All Data", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.restoreDefaultRates()
                                            Toast.makeText(context, "Factory default rates restored!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
                                        border = BorderStroke(1.dp, CarbonBorder),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Restore Defaults", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            if (showClearConfirm) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF4C0519),
                                    border = BorderStroke(1.dp, RoseAccent),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "Confirm: Kya aap waqai Rate List ka sara data Clear (0) karna chahtay hain?",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    viewModel.clearAllRateListData()
                                                    scannedRows = emptyList()
                                                    fiveColumnOutput = ""
                                                    showClearConfirm = false
                                                    Toast.makeText(context, "All rate list data cleared to 0!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = RoseAccent, contentColor = Color.White),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Text("Yes, Clear Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            TextButton(
                                                onClick = { showClearConfirm = false },
                                                colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Text("Cancel", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section 1: Select Multiple Photos / PDFs
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CarbonPanelBg,
                        border = BorderStroke(1.dp, CarbonBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "1. Select Multiple Photos (3, 4+ Ek Sath)",
                                    color = BossGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (selectedPhotoUris.isNotEmpty()) {
                                    TextButton(
                                        onClick = {
                                            selectedPhotoUris = emptyList()
                                            thumbnailBitmaps = emptyMap()
                                        },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Clear Selected (${selectedPhotoUris.size})", color = RoseAccent, fontSize = 11.sp)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        multiPhotoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BossGold,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pick 3, 4+ Photos", fontSize = 11.5.sp, fontWeight = FontWeight.Black)
                                }

                                OutlinedButton(
                                    onClick = { documentPicker.launch("*/*") },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = BorderStroke(1.dp, CarbonBorder),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(0.7f).height(38.dp)
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add PDF", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Thumbnails Horizontal Scroll Row
                            if (selectedPhotoUris.isNotEmpty()) {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    itemsIndexed(selectedPhotoUris) { idx, uri ->
                                        val bmp = thumbnailBitmaps[uri]
                                        Box(
                                            modifier = Modifier
                                                .width(96.dp)
                                                .height(84.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(CarbonCardBg)
                                                .border(1.dp, CarbonBorder, RoundedCornerShape(8.dp))
                                        ) {
                                            if (bmp != null) {
                                                Image(
                                                    bitmap = bmp.asImageBitmap(),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Image, contentDescription = null, tint = BossGold, modifier = Modifier.size(24.dp))
                                                }
                                            }
                                            // Badge Index
                                            Surface(
                                                shape = RoundedCornerShape(bottomEnd = 6.dp),
                                                color = Color.Black.copy(alpha = 0.75f),
                                                modifier = Modifier.align(Alignment.TopStart)
                                            ) {
                                                Text(
                                                    text = "#${idx + 1}",
                                                    color = BossGold,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                            // Delete Button
                                            IconButton(
                                                onClick = {
                                                    selectedPhotoUris = selectedPhotoUris.filter { it != uri }
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(22.dp)
                                                    .background(RoseAccent, CircleShape)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section 2: Paste Raw Rate List Text / Test Sample Sheet
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CarbonPanelBg,
                        border = BorderStroke(1.dp, CarbonBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "2. Or Paste Raw Rate List Text / Sample Sheet",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(
                                    onClick = { rawRateText = TEST_SAMPLE_SHEET },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Load Test Sample Sheet", color = BossGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedTextField(
                                value = rawRateText,
                                onValueChange = { rawRateText = it },
                                placeholder = {
                                    Text(
                                        text = "Paste raw OCR text, WhatsApp rate list, or 5-column lines here...",
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                },
                                minLines = 4,
                                maxLines = 6,
                                textStyle = LocalTextStyle.current.copy(
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = CarbonDarkBg,
                                    unfocusedContainerColor = CarbonDarkBg,
                                    focusedBorderColor = BossGold,
                                    unfocusedBorderColor = CarbonBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Primary Scan & Extract Button
                    Button(
                        onClick = {
                            if (selectedPhotoUris.isEmpty() && rawRateText.isBlank()) {
                                Toast.makeText(context, "Please select at least 1 photo or enter raw rate text!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            scope.launch {
                                isScanningRates = true
                                scanError = null
                                try {
                                    val rows = RateListDetector.detectScannedRowsFromMultipleUris(
                                        context = context,
                                        uris = selectedPhotoUris,
                                        rawTextInput = rawRateText
                                    )
                                    scannedRows = rows
                                    fiveColumnOutput = RateListDetector.formatScannedRowsToFiveColumnText(rows)
                                    if (rows.isNotEmpty()) {
                                        Toast.makeText(
                                            context,
                                            "Extracted ${rows.size} standardized 5-column rate entries across ${selectedPhotoUris.size.coerceAtLeast(1)} sheet(s)!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        scanError = "No rate sections could be extracted. Please check image clarity or load the sample sheet."
                                    }
                                } catch (e: Throwable) {
                                    scanError = e.message ?: "Failed to process rate lists."
                                } finally {
                                    isScanningRates = false
                                }
                            }
                        },
                        enabled = !isScanningRates,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BossGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        if (isScanningRates) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.5.dp, color = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scanning & Mapping to Master Lists...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan & Extract 5-Column Rate Dataset", fontSize = 12.5.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    if (scanError != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF4C0519),
                            border = BorderStroke(1.dp, RoseAccent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = scanError ?: "",
                                color = Color(0xFFFECDD3),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Extracted 5-Column Dataset Preview Table
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CarbonPanelBg,
                        border = BorderStroke(1.dp, CarbonBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Extracted 5-Column Dataset (${scannedRows.size} Records)",
                                        color = BossGold,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Format: [Title] | [Code] | [Gauge] | [Color] | [Rate]",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 9.5.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                if (scannedRows.isNotEmpty()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("5-Column Rates", fiveColumnOutput))
                                                Toast.makeText(context, "Copied pure 5-column dataset to clipboard!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier
                                                .size(30.dp)
                                                .background(CarbonCardBg, RoundedCornerShape(6.dp))
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = BossGold, modifier = Modifier.size(14.dp))
                                        }

                                        Button(
                                            onClick = {
                                                val res = viewModel.upsertScannedRates(scannedRows)
                                                val updated = res.first
                                                val inserted = res.second
                                                Toast.makeText(
                                                    context,
                                                    "Intelligent Upsert Complete: $updated updated, $inserted inserted (0 deleted)!",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = EmeraldAccent,
                                                contentColor = Color.Black
                                            ),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Upsert to Rates", fontSize = 10.5.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }

                            if (scannedRows.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CarbonDarkBg)
                                        .border(BorderStroke(1.dp, CarbonBorder), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Upload 3, 4+ rate list photos or click 'Load Test Sample Sheet' → 'Scan & Extract' to preview the 5-column dataset.",
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(16.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            } else {
                                // 5-Column Table
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CarbonDarkBg,
                                    border = BorderStroke(1.dp, CarbonBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        // Sticky Table Header
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(CarbonCardBg)
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Category", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f))
                                            Text("Code", color = BossGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
                                            Text("Gauge", color = SkyAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f))
                                            Text("Color", color = Color(0xFFE2E8F0), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                            Text("Rate (Rs)", color = EmeraldAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
                                            Text("Action", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f))
                                        }

                                        Divider(color = CarbonBorder)

                                        // Table Rows (Editable Rate)
                                        scannedRows.forEachIndexed { idx, row ->
                                            val cleanG = row.gauge.replace("mm", "", ignoreCase = true).trim().ifBlank { "1.2" }
                                            val exists = state.rates[cleanG]?.get(row.color)?.containsKey(row.sectionCode) == true

                                            var editingRate by remember(row.rate) { mutableStateOf(row.rate.toLong().toString()) }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = row.sectionTitle,
                                                    color = Color(0xFFCBD5E1),
                                                    fontSize = 10.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1.3f)
                                                )
                                                Text(
                                                    text = row.sectionCode,
                                                    color = BossGold,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.weight(0.9f)
                                                )
                                                Text(
                                                    text = "${row.gauge}mm",
                                                    color = SkyAccent,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.weight(0.7f)
                                                )
                                                Text(
                                                    text = row.color,
                                                    color = Color.White,
                                                    fontSize = 9.5.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                // Editable Rate Box
                                                OutlinedTextField(
                                                    value = editingRate,
                                                    onValueChange = { newVal ->
                                                        editingRate = newVal
                                                        val num = newVal.toDoubleOrNull()
                                                        if (num != null && num > 0.0) {
                                                            row.rate = num
                                                            fiveColumnOutput = RateListDetector.formatScannedRowsToFiveColumnText(scannedRows)
                                                        }
                                                    },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    textStyle = LocalTextStyle.current.copy(
                                                        color = EmeraldAccent,
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace
                                                    ),
                                                    shape = RoundedCornerShape(4.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedContainerColor = CarbonCardBg,
                                                        unfocusedContainerColor = CarbonCardBg,
                                                        focusedBorderColor = EmeraldAccent,
                                                        unfocusedBorderColor = CarbonBorder
                                                    ),
                                                    modifier = Modifier.weight(0.9f).height(32.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                // Status Badge
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (exists) EmeraldAccent.copy(alpha = 0.15f) else SkyAccent.copy(alpha = 0.15f),
                                                    border = BorderStroke(1.dp, if (exists) EmeraldAccent.copy(alpha = 0.6f) else SkyAccent.copy(alpha = 0.6f)),
                                                    modifier = Modifier.weight(0.8f)
                                                ) {
                                                    Text(
                                                        text = if (exists) "UPDATE" else "INSERT",
                                                        color = if (exists) EmeraldAccent else SkyAccent,
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Black,
                                                        modifier = Modifier.padding(vertical = 3.dp),
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                    )
                                                }
                                            }
                                            if (idx < scannedRows.size - 1) {
                                                Divider(color = CarbonBorder.copy(alpha = 0.5f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Sub-modal: Saved Rates Window (Browse current saved database rates by Gauge & Color)
    if (showSavedRatesModal) {
        SavedRatesViewerDialog(
            state = state,
            onDismiss = { showSavedRatesModal = false }
        )
    }
}

@Composable
fun SavedRatesViewerDialog(
    state: BossAppState,
    onDismiss: () -> Unit
) {
    var selectedGauge by remember { mutableStateOf("1.6") }
    var selectedColor by remember { mutableStateOf("DULL") }
    var searchSection by remember { mutableStateOf("") }

    val cleanG = selectedGauge.replace("mm", "", ignoreCase = true).trim()
    val currentSecRates: Map<String, Double> = remember(state.rates, cleanG, selectedColor) {
        state.rates[cleanG]?.get(selectedColor) ?: emptyMap()
    }
    val filteredEntries: List<Pair<String, Double>> = remember(currentSecRates, searchSection) {
        currentSecRates.filter { (sec, _) ->
            searchSection.isBlank() || sec.contains(searchSection.trim().uppercase())
        }.toList().sortedBy { it.first }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(14.dp),
            color = CarbonDarkBg,
            border = BorderStroke(1.dp, CarbonBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("📋 Saved Master Rates Window", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Browse currently saved rates in database", color = BossGold, fontSize = 10.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Gauge chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("1.2", "1.4", "1.6", "2.0", "2.5")) { g ->
                        val isSel = cleanG == g
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) BossGold else CarbonCardBg,
                            border = BorderStroke(1.dp, if (isSel) BossGold else CarbonBorder),
                            modifier = Modifier.clickable { selectedGauge = g }
                        ) {
                            Text(
                                text = "${g}mm",
                                color = if (isSel) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Color chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val colors = listOf("DULL", "SAHARA/BROWN", "H23/PC-RAL", "BLACK/MULTI", "WOOD COAT")
                    items(colors) { c ->
                        val isSel = selectedColor == c
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) SkyAccent else CarbonCardBg,
                            border = BorderStroke(1.dp, if (isSel) SkyAccent else CarbonBorder),
                            modifier = Modifier.clickable { selectedColor = c }
                        ) {
                            Text(
                                text = c,
                                color = if (isSel) Color.Black else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Search field
                OutlinedTextField(
                    value = searchSection,
                    onValueChange = { searchSection = it },
                    placeholder = { Text("Search section code (e.g. DC30C, M24...)", color = Color(0xFF64748B), fontSize = 11.sp) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 11.sp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CarbonPanelBg,
                        unfocusedContainerColor = CarbonPanelBg,
                        focusedBorderColor = BossGold,
                        unfocusedBorderColor = CarbonBorder
                    ),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                )

                // Results Table
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CarbonPanelBg,
                    border = BorderStroke(1.dp, CarbonBorder),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    if (filteredEntries.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No section rates found for $cleanG mm | $selectedColor", color = Color(0xFF64748B), fontSize = 11.sp)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(CarbonCardBg).padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Section Code", color = BossGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text("Rate (Rs)", color = EmeraldAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Divider(color = CarbonBorder)
                            }
                            items(filteredEntries) { entry ->
                                val sec = entry.first
                                val rate = entry.second
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(sec, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Text("Rs ${rate.toLong()}", color = EmeraldAccent, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                }
                                Divider(color = CarbonBorder.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }
        }
    }
}
