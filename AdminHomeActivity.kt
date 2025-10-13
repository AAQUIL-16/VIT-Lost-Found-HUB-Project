package com.example.myfirstapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.background
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

class AdminHomeActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentTheme by remember { mutableStateOf(AppTheme.AUTO) }
            val context = LocalContext.current
            val isSystemInDarkTheme = remember {
                val nightModeFlags = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }

            val useDarkTheme = when (currentTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.AUTO -> isSystemInDarkTheme
            }

            val colorScheme = if (useDarkTheme) {
                darkColorScheme(
                    primary = PremiumAqua,
                    secondary = PremiumLightAqua,
                    tertiary = PremiumLightAqua,
                    surface = Color(0xFF1E1E1E),
                    onSurface = Color.White,
                    background = Color(0xFF121212),
                    onBackground = Color.White
                )
            } else {
                lightColorScheme(
                    primary = PremiumAqua,
                    secondary = PremiumDarkAqua,
                    tertiary = PremiumDarkAqua,
                    surface = Color.White,
                    onSurface = Color.Black,
                    background = Color(0xFFF5F5F5),
                    onBackground = Color.Black
                )
            }

            MaterialTheme(colorScheme = colorScheme) {
                AdminLostFoundApp(
                    currentTheme = currentTheme,
                    onThemeChange = { currentTheme = it },
                    colorScheme = colorScheme
                )
            }
        }
    }

    @Composable
    fun AdminLostFoundApp(
        currentTheme: AppTheme,
        onThemeChange: (AppTheme) -> Unit,
        colorScheme: ColorScheme
    ) {
        var activeTab by remember { mutableStateOf(0) }
        var allItems by remember { mutableStateOf(listOf<LostFoundItem>()) }
        var claimRequests by remember { mutableStateOf(listOf<ClaimRequest>()) }
        var showSearch by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        var searchFilter by remember { mutableStateOf("All") }
        var menuExpanded by remember { mutableStateOf(false) }
        var notifications by remember { mutableStateOf(listOf<Notification>()) }
        var historyExpanded by remember { mutableStateOf(false) }
        var showApprovedHistory by remember { mutableStateOf(false) }
        var showGivenHistory by remember { mutableStateOf(false) }
        var showRejectedHistory by remember { mutableStateOf(false) }
        var approvedHistory by remember { mutableStateOf(listOf<HistoryItem>()) }
        var givenHistory by remember { mutableStateOf(listOf<HistoryItem>()) }
        var rejectedHistory by remember { mutableStateOf(listOf<HistoryItem>()) }
        var showFullImage by remember { mutableStateOf(false) }
        var selectedImageUrl by remember { mutableStateOf("") }
        var showNotifications by remember { mutableStateOf(false) }

        // Calculate unread count - only count claim request notifications
        val unreadCount = remember(notifications) {
            notifications.count { !it.read }
        }

        // Fetch ALL data including history
        LaunchedEffect(Unit) {
            // Fetch items
            db.collection("lost_and_found")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) return@addSnapshotListener
                    val itemsList = mutableListOf<LostFoundItem>()
                    snapshot?.documents?.forEach { doc ->
                        val item = LostFoundItem(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            location = doc.getString("location") ?: "",
                            status = doc.getString("status") ?: "",
                            imageUrl = doc.getString("imageUrl") ?: "",
                            uploaderEmail = doc.getString("uploaderEmail"),
                            uploaderName = doc.getString("uploaderName"),
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            isDeleted = doc.getBoolean("isDeleted") ?: false
                        )
                        itemsList.add(item)
                    }
                    allItems = itemsList.filter { !it.isDeleted }.sortedByDescending { it.timestamp }
                }

            // Fetch claims and update all history lists
            db.collection("claims")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) return@addSnapshotListener
                    val claimsList = mutableListOf<ClaimRequest>()
                    snapshot?.documents?.forEach { doc ->
                        val claim = ClaimRequest(
                            id = doc.id,
                            itemId = doc.getString("itemId") ?: "",
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            location = doc.getString("location") ?: "",
                            status = doc.getString("status") ?: "",
                            imageUrl = doc.getString("imageUrl") ?: "",
                            uploaderEmail = doc.getString("uploaderEmail") ?: "",
                            uploaderName = doc.getString("uploaderName") ?: "",
                            claimerEmail = doc.getString("claimerEmail") ?: "",
                            claimerName = doc.getString("claimerName") ?: "",
                            claimDescription = doc.getString("claimDescription") ?: "",
                            claimStatus = doc.getString("claimStatus") ?: "Pending",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            adminNotes = doc.getString("adminNotes") ?: ""
                        )
                        claimsList.add(claim)
                    }
                    claimRequests = claimsList.sortedByDescending { it.timestamp }

                    // Update all history lists from the same claims collection
                    approvedHistory = claimsList
                        .filter { it.claimStatus == "Approved" }
                        .map { claim ->
                            HistoryItem(
                                id = claim.id,
                                title = claim.title,
                                action = "Approved",
                                timestamp = claim.timestamp,
                                claimerEmail = claim.claimerEmail,
                                uploaderEmail = claim.uploaderEmail
                            )
                        }
                        .sortedByDescending { it.timestamp }

                    givenHistory = claimsList
                        .filter { it.claimStatus == "Given" }
                        .map { claim ->
                            HistoryItem(
                                id = claim.id,
                                title = claim.title,
                                action = "Given to Student",
                                timestamp = claim.timestamp,
                                claimerEmail = claim.claimerEmail,
                                uploaderEmail = claim.uploaderEmail
                            )
                        }
                        .sortedByDescending { it.timestamp }

                    // Add rejected history
                    rejectedHistory = claimsList
                        .filter { it.claimStatus == "Rejected" }
                        .map { claim ->
                            HistoryItem(
                                id = claim.id,
                                title = claim.title,
                                action = "Rejected",
                                timestamp = claim.timestamp,
                                claimerEmail = claim.claimerEmail,
                                uploaderEmail = claim.uploaderEmail
                            )
                        }
                        .sortedByDescending { it.timestamp }
                }

            // Fetch ONLY claim request notifications for admin
            db.collection("notifications")
                .whereEqualTo("type", "claim_request")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) return@addSnapshotListener
                    val notificationsList = mutableListOf<Notification>()
                    snapshot?.documents?.forEach { doc ->
                        val notification = Notification(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            title = doc.getString("title") ?: "",
                            message = doc.getString("message") ?: "",
                            type = doc.getString("type") ?: "",
                            read = doc.getBoolean("read") ?: false,
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                        notificationsList.add(notification)
                    }
                    notifications = notificationsList.sortedByDescending { it.timestamp }
                }
        }

        val filteredItems = remember(allItems, searchQuery, searchFilter) {
            if (searchQuery.isEmpty()) allItems else allItems.filter { item ->
                when (searchFilter) {
                    "Title" -> item.title.contains(searchQuery, ignoreCase = true)
                    "Category" -> item.status.contains(searchQuery, ignoreCase = true)
                    "Location" -> item.location.contains(searchQuery, ignoreCase = true)
                    "Email ID" -> (item.uploaderEmail?.contains(searchQuery, ignoreCase = true) == true)
                    "Date" -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(Date(item.timestamp)).contains(searchQuery, ignoreCase = true)
                    else -> item.title.contains(searchQuery, ignoreCase = true) ||
                            item.description.contains(searchQuery, ignoreCase = true) ||
                            item.location.contains(searchQuery, ignoreCase = true) ||
                            item.status.contains(searchQuery, ignoreCase = true) ||
                            (item.uploaderEmail?.contains(searchQuery, ignoreCase = true) == true)
                }
            }
        }

        val filteredClaims = remember(claimRequests, searchQuery, searchFilter) {
            if (searchQuery.isEmpty()) claimRequests else claimRequests.filter { claim ->
                when (searchFilter) {
                    "Title" -> claim.title.contains(searchQuery, ignoreCase = true)
                    "Category" -> claim.status.contains(searchQuery, ignoreCase = true)
                    "Location" -> claim.location.contains(searchQuery, ignoreCase = true)
                    "Email ID" -> claim.claimerEmail.contains(searchQuery, ignoreCase = true)
                    "Date" -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(Date(claim.timestamp)).contains(searchQuery, ignoreCase = true)
                    else -> claim.title.contains(searchQuery, ignoreCase = true) ||
                            claim.description.contains(searchQuery, ignoreCase = true) ||
                            claim.location.contains(searchQuery, ignoreCase = true) ||
                            claim.status.contains(searchQuery, ignoreCase = true) ||
                            claim.claimerEmail.contains(searchQuery, ignoreCase = true)
                }
            }
        }

        // Filter claims for display - only show Pending and Approved (not Given)
        val displayClaims = remember(claimRequests) {
            claimRequests.filter { it.claimStatus != "Given" }
        }

        val filteredDisplayClaims = remember(displayClaims, searchQuery, searchFilter) {
            if (searchQuery.isEmpty()) displayClaims else displayClaims.filter { claim ->
                when (searchFilter) {
                    "Title" -> claim.title.contains(searchQuery, ignoreCase = true)
                    "Category" -> claim.status.contains(searchQuery, ignoreCase = true)
                    "Location" -> claim.location.contains(searchQuery, ignoreCase = true)
                    "Email ID" -> claim.claimerEmail.contains(searchQuery, ignoreCase = true)
                    "Date" -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(Date(claim.timestamp)).contains(searchQuery, ignoreCase = true)
                    else -> claim.title.contains(searchQuery, ignoreCase = true) ||
                            claim.description.contains(searchQuery, ignoreCase = true) ||
                            claim.location.contains(searchQuery, ignoreCase = true) ||
                            claim.status.contains(searchQuery, ignoreCase = true) ||
                            claim.claimerEmail.contains(searchQuery, ignoreCase = true)
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(colorScheme.background)) {
            Scaffold(containerColor = Color.Transparent, contentColor = colorScheme.onBackground) { paddingValues ->
                Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
                    // Header
                    Box(modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(20.dp), clip = true)
                        .background(brush = Brush.verticalGradient(colors = listOf(colorScheme.primary.copy(alpha = 0.8f), colorScheme.secondary.copy(alpha = 0.6f))), shape = RoundedCornerShape(20.dp))
                        .border(1.dp, colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("VIT Lost & Found Hub", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Row {
                                // Notification with click to show all notifications
                                Box {
                                    IconButton(
                                        onClick = {
                                            showNotifications = true
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.Notifications, "Notifications", tint = Color.White)
                                    }
                                    if (unreadCount > 0) {
                                        Box(modifier = Modifier.size(16.dp).align(Alignment.TopEnd).clip(CircleShape).background(Color.Red).padding(2.dp), contentAlignment = Alignment.Center) {
                                            Text(if (unreadCount > 9) "9+" else unreadCount.toString(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                // Search
                                IconButton(onClick = { showSearch = !showSearch; if (!showSearch) searchQuery = "" }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.Search, "Search", tint = Color.White)
                                }
                                Spacer(Modifier.width(8.dp))
                                // Menu
                                Box {
                                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(40.dp)) {
                                        Icon(Icons.Default.MoreVert, "Menu", tint = Color.White)
                                    }
                                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }, modifier = Modifier.background(brush = Brush.verticalGradient(colors = listOf(colorScheme.surface, colorScheme.primary.copy(alpha = 0.1f)))).border(1.dp, colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))) {
                                        // Theme
                                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                                            Text("Theme", fontWeight = FontWeight.Bold, color = colorScheme.primary)
                                            Spacer(Modifier.height(8.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                listOf(Triple(AppTheme.LIGHT, Icons.Default.WbSunny, "Light"), Triple(AppTheme.DARK, Icons.Default.NightsStay, "Dark"), Triple(AppTheme.AUTO, Icons.Default.Settings, "Auto")).forEach { (theme, icon, label) ->
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onThemeChange(theme); menuExpanded = false }.padding(8.dp)) {
                                                        Box(modifier = Modifier.size(40.dp).background(if (currentTheme == theme) colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent, CircleShape).border(2.dp, if (currentTheme == theme) colorScheme.primary else Color.Transparent, CircleShape), contentAlignment = Alignment.Center) {
                                                            Icon(icon, label, tint = colorScheme.primary)
                                                        }
                                                        Text(label, fontSize = 10.sp, color = colorScheme.onSurface)
                                                    }
                                                }
                                            }
                                        }
                                        Divider()
                                        // Menu Items - Only History remains
                                        listOf(Triple(Icons.Default.History, "History") { historyExpanded = !historyExpanded }).forEach { (icon, text, action) ->
                                            DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = colorScheme.primary); Spacer(Modifier.width(12.dp)); Text(text, color = colorScheme.onSurface); if (text == "History") { Spacer(Modifier.weight(1f)); Icon(if (historyExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, "Expand", tint = colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(16.dp)) } } }, onClick = { action() }, modifier = Modifier.background(Color.Transparent))
                                        }
                                        // History Submenu
                                        if (historyExpanded) {
                                            Column {
                                                listOf(
                                                    Triple(Icons.Default.CheckCircle, "Approved History") { showApprovedHistory = true; menuExpanded = false; historyExpanded = false },
                                                    Triple(Icons.Default.DoneAll, "Given History") { showGivenHistory = true; menuExpanded = false; historyExpanded = false },
                                                    Triple(Icons.Default.Cancel, "Rejected History") { showRejectedHistory = true; menuExpanded = false; historyExpanded = false }
                                                ).forEach { (icon, text, action) ->
                                                    DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically) { Spacer(Modifier.width(24.dp)); Icon(icon, null, tint = colorScheme.primary, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(12.dp)); Text(text, color = colorScheme.onSurface, fontSize = 14.sp) } }, onClick = action)
                                                }
                                            }
                                        }
                                        Divider()
                                        DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.ExitToApp, null, tint = Color.Red); Spacer(Modifier.width(12.dp)); Text("Logout", color = Color.Red) } }, onClick = { auth.signOut(); startActivity(Intent(this@AdminHomeActivity, LoginActivity::class.java)); finish(); menuExpanded = false })
                                    }
                                }
                            }
                        }
                    }

                    // Search Section
                    if (showSearch) {
                        Spacer(Modifier.height(16.dp))
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Search...", color = colorScheme.onSurface.copy(alpha = 0.6f)) }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Search, "Search", tint = colorScheme.primary) }, colors = TextFieldDefaults.colors(focusedTextColor = colorScheme.onSurface, unfocusedTextColor = colorScheme.onSurface, focusedContainerColor = colorScheme.surface, unfocusedContainerColor = colorScheme.surface, focusedIndicatorColor = colorScheme.primary, unfocusedIndicatorColor = colorScheme.onSurface.copy(alpha = 0.3f)))
                                Spacer(Modifier.height(12.dp))
                                var filterExpanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(onClick = { filterExpanded = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.onSurface)) {
                                        Text("Filter: $searchFilter", modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                                        Icon(if (filterExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, "Filter", modifier = Modifier.size(20.dp))
                                    }
                                    DropdownMenu(expanded = filterExpanded, onDismissRequest = { filterExpanded = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                                        listOf("All", "Title", "Category", "Location", "Email ID", "Date").forEach { filter ->
                                            DropdownMenuItem(text = { Text(filter, fontWeight = if (filter == searchFilter) FontWeight.SemiBold else FontWeight.Normal, color = if (filter == searchFilter) colorScheme.primary else colorScheme.onSurface) }, onClick = { searchFilter = filter; filterExpanded = false })
                                        }
                                    }
                                }
                                if (searchQuery.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    val resultCount = if (activeTab == 0) filteredItems.size else filteredDisplayClaims.size
                                    Text("Found $resultCount items matching \"$searchQuery\" in $searchFilter", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    Spacer(Modifier.height(16.dp))

                    // Tabs
                    TabRow(selectedTabIndex = activeTab, containerColor = Color.Transparent, contentColor = colorScheme.primary, indicator = { tabPositions -> TabRowDefaults.Indicator(modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]).padding(horizontal = 16.dp).height(3.dp).background(brush = Brush.horizontalGradient(colors = listOf(colorScheme.primary, colorScheme.secondary)), shape = RoundedCornerShape(2.dp)), height = 3.dp) }, divider = {}) {
                        listOf("All Items", "Claim Requests").forEachIndexed { index, title ->
                            Tab(selected = activeTab == index, onClick = { activeTab = index }, text = { Text(title, fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Medium, color = if (activeTab == index) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.6f)) })
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Content
                    when (activeTab) {
                        0 -> {
                            val itemsToShow = if (searchQuery.isEmpty()) allItems else filteredItems
                            if (itemsToShow.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Inbox, "No items", modifier = Modifier.size(80.dp), tint = colorScheme.onSurface.copy(alpha = 0.3f))
                                        Spacer(Modifier.height(16.dp))
                                        Text(if (searchQuery.isEmpty()) "No items!" else "No items found matching \"$searchQuery\"", color = colorScheme.onSurface.copy(alpha = 0.6f), textAlign = TextAlign.Center)
                                    }
                                }
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize().weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(itemsToShow, key = { it.id }) { item ->
                                        PremiumAdminItemCard(
                                            item,
                                            colorScheme,
                                            onImageClick = { imageUrl ->
                                                selectedImageUrl = imageUrl
                                                showFullImage = true
                                            },
                                            onDelete = {
                                                deleteItem(item)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            val claimsToShow = if (searchQuery.isEmpty()) filteredDisplayClaims else filteredDisplayClaims
                            val pendingClaims = claimsToShow.filter { it.claimStatus == "Pending" }
                            val approvedClaims = claimsToShow.filter { it.claimStatus == "Approved" }

                            if (claimsToShow.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Assignment, "No items", modifier = Modifier.size(80.dp), tint = colorScheme.onSurface.copy(alpha = 0.3f))
                                        Spacer(Modifier.height(16.dp))
                                        Text("No items!", color = colorScheme.onSurface.copy(alpha = 0.6f), textAlign = TextAlign.Center)
                                    }
                                }
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize().weight(1f).padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (pendingClaims.isNotEmpty()) {
                                        item { Text("Pending Claims (${pendingClaims.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp)) }
                                        items(pendingClaims, key = { it.id }) { claim ->
                                            PremiumAdminClaimCard(
                                                claim,
                                                colorScheme,
                                                onApprove = { approveClaim(claim) },
                                                onReject = { rejectClaim(claim) },
                                                onImageClick = { imageUrl ->
                                                    selectedImageUrl = imageUrl
                                                    showFullImage = true
                                                }
                                            )
                                        }
                                    }
                                    if (approvedClaims.isNotEmpty()) {
                                        item { Text("Approved Claims - Ready to Give (${approvedClaims.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF4CAF50), modifier = Modifier.padding(vertical = 8.dp)) }
                                        items(approvedClaims, key = { it.id }) { claim ->
                                            PremiumAdminClaimCard(
                                                claim,
                                                colorScheme,
                                                onApprove = null,
                                                onReject = null,
                                                onMarkAsGiven = { markItemAsGiven(claim) },
                                                onImageClick = { imageUrl ->
                                                    selectedImageUrl = imageUrl
                                                    showFullImage = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // History Dialogs
        if (showApprovedHistory) {
            HistoryDialog(title = "Approved History", items = approvedHistory, colorScheme = colorScheme, onClose = { showApprovedHistory = false })
        }
        if (showGivenHistory) {
            HistoryDialog(title = "Given History", items = givenHistory, colorScheme = colorScheme, onClose = { showGivenHistory = false })
        }
        if (showRejectedHistory) {
            HistoryDialog(title = "Rejected History", items = rejectedHistory, colorScheme = colorScheme, onClose = { showRejectedHistory = false })
        }

        // Full Image Dialog - Updated with larger size
        if (showFullImage && selectedImageUrl.isNotEmpty()) {
            FullImageDialog(
                imageUrl = selectedImageUrl,
                colorScheme = colorScheme,
                onClose = {
                    showFullImage = false
                    selectedImageUrl = ""
                }
            )
        }

        // Notifications Dialog - Only shows claim request notifications
        if (showNotifications) {
            Dialog(onDismissRequest = { showNotifications = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.7f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.surface,
                        contentColor = colorScheme.onSurface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Claim Requests",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.primary
                            )

                            if (unreadCount > 0) {
                                TextButton(
                                    onClick = {
                                        // Mark all as read
                                        notifications.forEach { notification ->
                                            if (!notification.read) {
                                                db.collection("notifications")
                                                    .document(notification.id)
                                                    .update("read", true)
                                            }
                                        }
                                    }
                                ) {
                                    Text("Mark all read", color = colorScheme.primary)
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        if (notifications.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.NotificationsOff,
                                        contentDescription = "No notifications",
                                        modifier = Modifier.size(48.dp),
                                        tint = colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "No new claim requests",
                                        color = colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(notifications) { notification ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                // Mark as read when clicked
                                                if (!notification.read) {
                                                    db.collection("notifications")
                                                        .document(notification.id)
                                                        .update("read", true)
                                                }
                                            },
                                        elevation = CardDefaults.cardElevation(2.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (!notification.read)
                                                colorScheme.primary.copy(alpha = 0.1f)
                                            else
                                                colorScheme.surface
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    notification.title,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colorScheme.onSurface,
                                                    fontSize = 14.sp
                                                )
                                                if (!notification.read) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(colorScheme.primary)
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                notification.message,
                                                color = colorScheme.onSurface.copy(alpha = 0.8f),
                                                fontSize = 12.sp
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                SimpleDateFormat("MMM dd, HH:mm")
                                                    .format(Date(notification.timestamp)),
                                                fontSize = 10.sp,
                                                color = colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = { showNotifications = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun FullImageDialog(
        imageUrl: String,
        colorScheme: ColorScheme,
        onClose: () -> Unit
    ) {
        Dialog(onDismissRequest = onClose) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.9f), // Increased size for larger image display
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.9f),
                    contentColor = Color.White
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    // Close button
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            "Close",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Full screen image with larger display
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Full size image",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp), // Reduced padding for larger image
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }

    @Composable
    fun HistoryDialog(title: String, items: List<HistoryItem>, colorScheme: ColorScheme, onClose: () -> Unit) {
        Dialog(onDismissRequest = onClose) {
            Card(modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.65f), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = colorScheme.surface, contentColor = colorScheme.onSurface)) {
                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                    Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    if (items.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text("No history found", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = colorScheme.onSurface)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(items) { item ->
                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.title, fontWeight = FontWeight.Medium, color = colorScheme.onSurface, fontSize = 14.sp)
                                            Text(
                                                item.action,
                                                color = when (item.action) {
                                                    "Approved" -> Color(0xFF4CAF50)
                                                    "Given to Student" -> Color(0xFF2196F3)
                                                    "Rejected" -> Color(0xFFF44336)
                                                    else -> colorScheme.onSurface
                                                },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (item.claimerEmail.isNotEmpty()) Text("Claimed by: ${item.claimerEmail}", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                                            if (item.uploaderEmail.isNotEmpty()) Text("Uploaded by: ${item.uploaderEmail}", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                                        }
                                        Text(SimpleDateFormat("MMM dd, yyyy HH:mm").format(Date(item.timestamp)), fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onClose, modifier = Modifier.align(Alignment.End)) { Text("Close") }
                }
            }
        }
    }

    @Composable
    fun PremiumAdminItemCard(item: LostFoundItem, colorScheme: ColorScheme, onImageClick: (String) -> Unit, onDelete: () -> Unit) {
        Card(modifier = Modifier.fillMaxWidth().height(380.dp).padding(horizontal = 4.dp, vertical = 6.dp).shadow(16.dp, RoundedCornerShape(16.dp), clip = false, ambientColor = colorScheme.primary, spotColor = colorScheme.primary), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = colorScheme.surface, contentColor = colorScheme.onSurface)) {
            Box(modifier = Modifier.fillMaxSize().background(brush = Brush.verticalGradient(colors = listOf(colorScheme.surface, colorScheme.surface.copy(alpha = 0.8f))))) {
                // Delete button for all items
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(item.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary, modifier = Modifier.fillMaxWidth(), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    Text(item.description, color = colorScheme.onSurface.copy(alpha = 0.8f), lineHeight = 18.sp, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    Box(modifier = Modifier.wrapContentWidth().clip(RoundedCornerShape(10.dp)).background(when (item.status) {
                        "Lost" -> Color(0xFFF44336).copy(alpha = 0.2f)
                        "Found" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                        "Claimed" -> Color(0xFFFF9800).copy(alpha = 0.2f)
                        else -> colorScheme.primary.copy(alpha = 0.2f)
                    }).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(item.status, color = when (item.status) {
                            "Lost" -> Color(0xFFF44336)
                            "Found" -> Color(0xFF4CAF50)
                            "Claimed" -> Color(0xFFFF9800)
                            else -> colorScheme.primary
                        }, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, "Location", tint = colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(item.location, color = colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(6.dp))
                    // Updated to show uploader email prominently
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, "Uploader", tint = colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Uploader: ${item.uploaderEmail ?: "Unknown"}", fontSize = 14.sp, color = colorScheme.onSurface.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (item.imageUrl.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp)).shadow(4.dp, RoundedCornerShape(12.dp)).clickable { onImageClick(item.imageUrl) }) {
                            AsyncImage(model = item.imageUrl, "Item Image", modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(SimpleDateFormat("MMM dd, HH:mm").format(Date(item.timestamp)), fontSize = 12.sp, color = colorScheme.onSurface.copy(alpha = 0.5f))
                        if (item.status == "Claimed") Text("✓ Claimed", color = Color(0xFFFF9800), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    @Composable
    fun PremiumAdminClaimCard(claim: ClaimRequest, colorScheme: ColorScheme, onApprove: (() -> Unit)?, onReject: (() -> Unit)?, onMarkAsGiven: (() -> Unit)? = null, onImageClick: (String) -> Unit) {
        Card(modifier = Modifier.fillMaxWidth().padding(4.dp).shadow(8.dp, RoundedCornerShape(16.dp), clip = false), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = when (claim.claimStatus) {
            "Approved" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
            "Given" -> Color(0xFF2196F3).copy(alpha = 0.1f)
            "Rejected" -> Color(0xFFF44336).copy(alpha = 0.1f)
            else -> colorScheme.surface
        })) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(claim.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text(claim.description, color = colorScheme.onSurface.copy(alpha = 0.8f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                // Updated to show uploader email prominently
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Upload, null, tint = colorScheme.primary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Uploader: ${claim.uploaderName} (${claim.uploaderEmail})", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, tint = colorScheme.primary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Claimed by: ${claim.claimerName} (${claim.claimerEmail})", fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.8f))
                }
                Spacer(Modifier.height(8.dp))
                Text("Claim Reason: ${claim.claimDescription}", fontSize = 14.sp, color = colorScheme.onSurface.copy(alpha = 0.8f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = colorScheme.primary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(claim.location, color = colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 13.sp)
                    }
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(when (claim.claimStatus) {
                        "Pending" -> Color(0xFFFF9800).copy(alpha = 0.2f)
                        "Approved" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                        "Given" -> Color(0xFF2196F3).copy(alpha = 0.2f)
                        "Rejected" -> Color(0xFFF44336).copy(alpha = 0.2f)
                        else -> colorScheme.primary.copy(alpha = 0.2f)
                    }).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(
                            claim.claimStatus,
                            color = when (claim.claimStatus) {
                                "Pending" -> Color(0xFFFF9800)
                                "Approved" -> Color(0xFF4CAF50)
                                "Given" -> Color(0xFF2196F3)
                                "Rejected" -> Color(0xFFF44336)
                                else -> colorScheme.primary
                            },
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(SimpleDateFormat("MMM dd, yyyy HH:mm").format(Date(claim.timestamp)), fontSize = 12.sp, color = colorScheme.onSurface.copy(alpha = 0.5f))
                if (claim.imageUrl.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    AsyncImage(
                        model = claim.imageUrl,
                        "Item Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClick(claim.imageUrl) }
                    )
                }
                when (claim.claimStatus) {
                    "Pending" -> {
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Button(onClick = { onApprove?.invoke() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), modifier = Modifier.weight(1f).padding(end = 4.dp)) { Text("Approve") }
                            Button(onClick = { onReject?.invoke() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)), modifier = Modifier.weight(1f).padding(start = 4.dp)) { Text("Reject") }
                        }
                    }
                    "Approved" -> {
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { onMarkAsGiven?.invoke() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))) { Text("Mark as Given") }
                    }
                }
            }
        }
    }

    private fun deleteItem(item: LostFoundItem) {
        db.collection("lost_and_found").document(item.id).update("isDeleted", true)
            .addOnSuccessListener {
                Toast.makeText(this, "Item deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete item: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun approveClaim(claim: ClaimRequest) {
        // First update the claim status to Approved
        db.collection("claims").document(claim.id).update("claimStatus", "Approved")
            .addOnSuccessListener {
                // Update student's claim history
                db.collection("claim_history")
                    .whereEqualTo("claimerEmail", claim.claimerEmail)
                    .whereEqualTo("title", claim.title)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        querySnapshot.documents.forEach { doc ->
                            db.collection("claim_history").document(doc.id).update(
                                "action", "Claim Approved",
                                "claimStatus", "Approved"
                            )
                        }
                    }

                // Then mark the original item as Claimed in lost_and_found collection
                db.collection("lost_and_found").document(claim.itemId).update("status", "Claimed")
                    .addOnSuccessListener {
                        // Auto-reject all other pending claims for the same item
                        db.collection("claims")
                            .whereEqualTo("itemId", claim.itemId)
                            .whereEqualTo("claimStatus", "Pending")
                            .get()
                            .addOnSuccessListener { pendingClaimsSnapshot ->
                                val batch = db.batch()
                                pendingClaimsSnapshot.documents.forEach { pendingClaimDoc ->
                                    if (pendingClaimDoc.id != claim.id) {
                                        val pendingClaimRef = db.collection("claims").document(pendingClaimDoc.id)
                                        batch.update(pendingClaimRef, "claimStatus", "Rejected")

                                        // Send rejection notification to other claimers
                                        val otherClaimerEmail = pendingClaimDoc.getString("claimerEmail") ?: ""
                                        val otherClaimerName = pendingClaimDoc.getString("claimerName") ?: ""
                                        val rejectionNotification = mapOf(
                                            "userId" to otherClaimerEmail,
                                            "title" to "Claim Auto-Rejected ❌",
                                            "message" to "Your claim for '${claim.title}' was automatically rejected because another claim was approved for this item.",
                                            "type" to "claim_rejected",
                                            "read" to false,
                                            "timestamp" to System.currentTimeMillis(),
                                            "claimId" to pendingClaimDoc.id,
                                            "itemId" to claim.itemId
                                        )
                                        db.collection("notifications").add(rejectionNotification)
                                    }
                                }
                                batch.commit()
                            }

                        // Send notification to approved claimer
                        val approvedNotification = mapOf(
                            "userId" to claim.claimerEmail,
                            "title" to "Claim Approved! 🎉",
                            "message" to "Your claim for '${claim.title}' has been approved! Please contact the admin to collect your item.",
                            "type" to "claim_approved",
                            "read" to false,
                            "timestamp" to System.currentTimeMillis(),
                            "claimId" to claim.id,
                            "itemId" to claim.itemId
                        )
                        db.collection("notifications").add(approvedNotification)

                        // Also send notification to uploader
                        val uploaderNotification = mapOf(
                            "userId" to claim.uploaderEmail,
                            "title" to "Item Claim Approved ✅",
                            "message" to "Your item '${claim.title}' has been claimed by ${claim.claimerName}. The claim was approved.",
                            "type" to "item_claimed",
                            "read" to false,
                            "timestamp" to System.currentTimeMillis(),
                            "claimId" to claim.id,
                            "itemId" to claim.itemId
                        )
                        db.collection("notifications").add(uploaderNotification)

                        Toast.makeText(this, "Claim approved and other claims for this item auto-rejected", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to update item status: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to approve claim: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun rejectClaim(claim: ClaimRequest) {
        db.collection("claims").document(claim.id).update("claimStatus", "Rejected")
            .addOnSuccessListener {
                // Update student's claim history
                db.collection("claim_history")
                    .whereEqualTo("claimerEmail", claim.claimerEmail)
                    .whereEqualTo("title", claim.title)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        querySnapshot.documents.forEach { doc ->
                            db.collection("claim_history").document(doc.id).update(
                                "action", "Claim Rejected",
                                "claimStatus", "Rejected"
                            )
                        }
                    }

                val notification = mapOf(
                    "userId" to claim.claimerEmail,
                    "title" to "Claim Rejected ❌",
                    "message" to "Your claim for '${claim.title}' has been rejected.",
                    "type" to "claim_rejected",
                    "read" to false,
                    "timestamp" to System.currentTimeMillis(),
                    "claimId" to claim.id,
                    "itemId" to claim.itemId
                )
                db.collection("notifications").add(notification)
                Toast.makeText(this, "Claim rejected", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to reject claim: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun markItemAsGiven(claim: ClaimRequest) {
        db.collection("claims").document(claim.id).update("claimStatus", "Given")
            .addOnSuccessListener {
                // Update student's claim history
                db.collection("claim_history")
                    .whereEqualTo("claimerEmail", claim.claimerEmail)
                    .whereEqualTo("title", claim.title)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        querySnapshot.documents.forEach { doc ->
                            db.collection("claim_history").document(doc.id).update(
                                "action", "Item Given",
                                "claimStatus", "Given"
                            )
                        }
                    }

                val notification = mapOf(
                    "userId" to claim.claimerEmail,
                    "title" to "Item Given 🎁",
                    "message" to "Your item '${claim.title}' has been marked as given. Please collect it from the admin.",
                    "type" to "item_given",
                    "read" to false,
                    "timestamp" to System.currentTimeMillis(),
                    "claimId" to claim.id,
                    "itemId" to claim.itemId
                )
                db.collection("notifications").add(notification)

                // Also notify the uploader that their item has been given
                val uploaderNotification = mapOf(
                    "userId" to claim.uploaderEmail,
                    "title" to "Item Successfully Returned ✅",
                    "message" to "Your item '${claim.title}' has been successfully given to ${claim.claimerName}.",
                    "type" to "item_returned",
                    "read" to false,
                    "timestamp" to System.currentTimeMillis(),
                    "itemId" to claim.itemId
                )
                db.collection("notifications").add(uploaderNotification)

                Toast.makeText(this, "Item marked as given", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to mark as given: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
