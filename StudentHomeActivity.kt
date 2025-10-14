package com.example.myfirstapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.text.style.TextOverflow

// -------------------- Data Models --------------------
data class LostFoundItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val status: String = "",
    val imageUrl: String = "",
    val uploaderEmail: String? = "",
    val uploaderName: String? = "",
    val timestamp: Long = 0,
    val isDeleted: Boolean = false
)

data class UserProfile(
    val email: String = "",
    val name: String = "",
    val username: String = ""
)

data class HistoryItem(
    val id: String = "",
    val title: String = "",
    val action: String = "", // "Claim Sent", "Claim Approved", "Claim Rejected"
    val timestamp: Long = 0,
    val uploaderEmail: String = "",
    val claimerEmail: String = "",
    val claimStatus: String = "", // To track status for color coding
    val adminNotes: String = ""
)

data class ClaimRequest(
    val id: String = "",
    val itemId: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val status: String = "",
    val imageUrl: String = "",
    val uploaderEmail: String = "",
    val uploaderName: String = "",
    val claimerEmail: String = "",
    val claimerName: String = "",
    val claimDescription: String = "",
    val claimStatus: String = "Pending", // Pending, Approved, Rejected
    val timestamp: Long = 0,
    val adminNotes: String = "",
    val adminActionTimestamp: Long = 0, // When admin approved/rejected
    val adminActionBy: String = "" // Admin email who processed
)

data class Notification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "",
    val read: Boolean = false,
    val timestamp: Long = 0,
    val claimId: String? = "",
    val itemId: String? = ""
)

// -------------------- Theme Configuration --------------------
enum class AppTheme {
    LIGHT, DARK, AUTO
}

// Custom color scheme for premium look
val PremiumAqua = Color(0xFF00BCD4)
val PremiumDarkAqua = Color(0xFF00838F)
val PremiumLightAqua = Color(0xFF80DEEA)

// -------------------- Activity --------------------
class StudentHomeActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val imgbbApiKey = "4cb2a9e0aaeb007ebad2da149818b7f3"

    @OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentTheme by remember { mutableStateOf(AppTheme.AUTO) }

            // Get system dark mode preference
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

            MaterialTheme(
                colorScheme = colorScheme
            ) {
                PremiumLostFoundApp(
                    currentTheme = currentTheme,
                    onThemeChange = { currentTheme = it },
                    colorScheme = colorScheme,
                    useDarkTheme = useDarkTheme
                )
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
    @Composable
    fun PremiumLostFoundApp(
        currentTheme: AppTheme,
        onThemeChange: (AppTheme) -> Unit,
        colorScheme: ColorScheme,
        useDarkTheme: Boolean
    ) {
        var showForm by remember { mutableStateOf(false) }
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var location by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("") }
        var message by remember { mutableStateOf("") }
        var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
        var uploadedItems by remember { mutableStateOf(listOf<LostFoundItem>()) }
        var userItems by remember { mutableStateOf(listOf<LostFoundItem>()) }
        var fullImageUrl by remember { mutableStateOf<String?>(null) }
        var activeTab by remember { mutableStateOf(0) }
        var userProfile by remember { mutableStateOf<UserProfile?>(null) }
        var showProfile by remember { mutableStateOf(false) }
        var isUploading by remember { mutableStateOf(false) }
        var menuExpanded by remember { mutableStateOf(false) }
        var historyExpanded by remember { mutableStateOf(false) }

        // Search functionality
        var showSearch by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        var searchFilter by remember { mutableStateOf("All") }

        // History dialogs
        var showUploadHistory by remember { mutableStateOf(false) }
        var showDeleteHistory by remember { mutableStateOf(false) }
        var showClaimHistory by remember { mutableStateOf(false) }
        var deleteHistory by remember { mutableStateOf(listOf<HistoryItem>()) }
        var claimHistory by remember { mutableStateOf(listOf<HistoryItem>()) }
        var uploadHistory by remember { mutableStateOf(listOf<HistoryItem>()) }

        // Notification state
        var notifications by remember { mutableStateOf(listOf<Notification>()) }
        var unreadCount by remember { mutableStateOf(0) }
        var showNotifications by remember { mutableStateOf(false) }

        // Claim state
        var claimRequests by remember { mutableStateOf(listOf<ClaimRequest>()) }

        val configuration = LocalConfiguration.current

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri -> selectedImageUri = uri }

        // Fetch profile
        LaunchedEffect(Unit) {
            auth.currentUser?.email?.let { email ->
                db.collection("users").document(email).get()
                    .addOnSuccessListener { doc ->
                        userProfile = if (doc.exists()) {
                            doc.toObject(UserProfile::class.java) ?: UserProfile(email = email)
                        } else {
                            UserProfile(email = email)
                        }
                    }
                    .addOnFailureListener {
                        userProfile = UserProfile(email = auth.currentUser?.email ?: "")
                    }
            }
        }

        // Fetch notifications
        LaunchedEffect(Unit) {
            val currentUserEmail = auth.currentUser?.email
            if (currentUserEmail != null) {
                db.collection("notifications")
                    .whereEqualTo("userId", currentUserEmail)
                    .addSnapshotListener { snapshot, e ->
                        val notificationList = mutableListOf<Notification>()
                        snapshot?.documents?.forEach { doc ->
                            val notification = Notification(
                                id = doc.id,
                                userId = doc.getString("userId") ?: "",
                                title = doc.getString("title") ?: "",
                                message = doc.getString("message") ?: "",
                                type = doc.getString("type") ?: "",
                                read = doc.getBoolean("read") ?: false,
                                timestamp = doc.getLong("timestamp") ?: 0,
                                claimId = doc.getString("claimId") ?: "",
                                itemId = doc.getString("itemId") ?: ""
                            )
                            notificationList.add(notification)
                        }
                        notifications = notificationList.sortedByDescending { it.timestamp }
                        unreadCount = notificationList.count { !it.read }
                    }
            }
        }

        // Fetch claim requests
        LaunchedEffect(Unit) {
            db.collection("claims")
                .addSnapshotListener { snapshot, e ->
                    val claimList = mutableListOf<ClaimRequest>()
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
                            timestamp = doc.getLong("timestamp") ?: 0,
                            adminNotes = doc.getString("adminNotes") ?: ""
                        )
                        claimList.add(claim)
                    }
                    claimRequests = claimList
                }
        }

        // Fetch all items (including deleted for history)
        LaunchedEffect(Unit) {
            db.collection("lost_and_found")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Toast.makeText(this@StudentHomeActivity, "Error loading items: ${e.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

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

                    val nonDeletedItems = itemsList.filter { !it.isDeleted && it.status != "Claimed" }.sortedByDescending { it.timestamp }
                    uploadedItems = nonDeletedItems

                    val email = auth.currentUser?.email
                    userItems = nonDeletedItems.filter { it.uploaderEmail == email }.sortedByDescending { it.timestamp }

                    // Update upload history with ALL items by current user (including deleted)
                    val currentUserEmail = auth.currentUser?.email
                    if (currentUserEmail != null) {
                        val userUploadHistory = itemsList
                            .filter { it.uploaderEmail == currentUserEmail }
                            .map {
                                HistoryItem(
                                    title = it.title,
                                    action = "Uploaded",
                                    timestamp = it.timestamp,
                                    uploaderEmail = it.uploaderEmail ?: ""
                                )
                            }
                            .sortedByDescending { it.timestamp }
                        uploadHistory = userUploadHistory
                    }
                }
        }

        // Fetch delete history
        val currentUserEmail = auth.currentUser?.email
        if (currentUserEmail != null) {
            db.collection("delete_history")
                .whereEqualTo("uploaderEmail", currentUserEmail)
                .addSnapshotListener { snapshot, e ->
                    val historyList = mutableListOf<HistoryItem>()
                    snapshot?.documents?.forEach { doc ->
                        val historyItem = HistoryItem(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            action = doc.getString("action") ?: "Deleted",
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                            uploaderEmail = doc.getString("uploaderEmail") ?: currentUserEmail
                        )
                        historyList.add(historyItem)
                    }
                    deleteHistory = historyList.sortedByDescending { it.timestamp }
                }

            // Fetch claim history
            db.collection("claim_history")
                .whereEqualTo("claimerEmail", currentUserEmail)
                .addSnapshotListener { snapshot, e ->
                    val historyList = mutableListOf<HistoryItem>()
                    snapshot?.documents?.forEach { doc ->
                        val historyItem = HistoryItem(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            action = doc.getString("action") ?: "Claimed",
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                            claimerEmail = doc.getString("claimerEmail") ?: currentUserEmail,
                            uploaderEmail = doc.getString("uploaderEmail") ?: "",
                            claimStatus = doc.getString("claimStatus") ?: "",
                            adminNotes = doc.getString("adminNotes") ?: ""
                        )
                        historyList.add(historyItem)
                    }
                    claimHistory = historyList.sortedByDescending { it.timestamp }
                }
        }

        // Filter items based on search
        val filteredItems = remember(uploadedItems, userItems, searchQuery, searchFilter, activeTab) {
            val itemsToFilter = if (activeTab == 0) uploadedItems else userItems
            if (searchQuery.isEmpty()) itemsToFilter else itemsToFilter.filter { item ->
                when (searchFilter) {
                    "Title" -> item.title.contains(searchQuery, ignoreCase = true)
                    "Category" -> item.status.contains(searchQuery, ignoreCase = true)
                    "Location" -> item.location.contains(searchQuery, ignoreCase = true)
                    "Email ID" -> (item.uploaderEmail?.contains(searchQuery, ignoreCase = true) == true) ||
                            (item.uploaderName?.contains(searchQuery, ignoreCase = true) == true)
                    "Date" -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(Date(item.timestamp)).contains(searchQuery, ignoreCase = true)
                    else -> item.title.contains(searchQuery, ignoreCase = true) ||
                            item.description.contains(searchQuery, ignoreCase = true) ||
                            item.location.contains(searchQuery, ignoreCase = true) ||
                            item.status.contains(searchQuery, ignoreCase = true) ||
                            (item.uploaderEmail?.contains(searchQuery, ignoreCase = true) == true) ||
                            (item.uploaderName?.contains(searchQuery, ignoreCase = true) == true)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                contentColor = colorScheme.onBackground
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    // Premium Header with Glass Morphism Effect - FIXED HEADER
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(20.dp),
                                clip = false
                            )
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        colorScheme.primary.copy(alpha = 0.8f),
                                        colorScheme.secondary.copy(alpha = 0.6f)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = colorScheme.primary.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(12.dp) // Reduced padding
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Title with reduced size and weight
                            Text(
                                "VIT Lost & Found HUB",
                                fontSize = 16.sp, // Reduced from 22.sp
                                fontWeight = FontWeight.SemiBold, // Reduced from Bold
                                color = Color.White,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Premium Action Row - FIXED ALIGNMENT
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Notification Bell with Badge
                                Box {
                                    IconButton(
                                        onClick = { showNotifications = true },
                                        modifier = Modifier.size(36.dp) // Reduced size
                                    ) {
                                        Icon(
                                            Icons.Default.Notifications,
                                            contentDescription = "Notifications",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp) // Reduced icon size
                                        )
                                    }
                                    if (unreadCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(14.dp) // Reduced size
                                                .clip(CircleShape)
                                                .background(Color.Red)
                                                .padding(2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                                color = Color.White,
                                                fontSize = 7.sp, // Reduced font size
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Search Icon with Animation
                                IconButton(
                                    onClick = {
                                        showSearch = !showSearch
                                        if (!showSearch) searchQuery = ""
                                    },
                                    modifier = Modifier.size(36.dp) // Reduced size
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp) // Reduced icon size
                                    )
                                }

                                // Premium Menu with Theme Options - FIXED SIZE
                                Box {
                                    IconButton(
                                        onClick = { menuExpanded = true },
                                        modifier = Modifier.size(36.dp) // Reduced size
                                    ) {
                                        Icon(
                                            Icons.Default.AccountCircle,
                                            contentDescription = "Menu",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp) // Reduced icon size
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                        modifier = Modifier
                                            .background(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        colorScheme.surface,
                                                        colorScheme.primary.copy(alpha = 0.1f)
                                                    )
                                                )
                                            )
                                            .border(1.dp, colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    ) {
                                        // Theme Selection Section
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Text("Theme", fontWeight = FontWeight.Bold, color = colorScheme.primary)
                                            Spacer(Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                listOf(
                                                    Triple(AppTheme.LIGHT, Icons.Default.WbSunny, "Light"),
                                                    Triple(AppTheme.DARK, Icons.Default.NightsStay, "Dark"),
                                                    Triple(AppTheme.AUTO, Icons.Default.Settings, "Auto")
                                                ).forEach { (theme, icon, label) ->
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        modifier = Modifier
                                                            .clickable {
                                                                onThemeChange(theme)
                                                                menuExpanded = false
                                                            }
                                                            .padding(8.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(40.dp)
                                                                .background(
                                                                    if (currentTheme == theme) colorScheme.primary.copy(alpha = 0.2f)
                                                                    else Color.Transparent,
                                                                    CircleShape
                                                                )
                                                                .border(
                                                                    2.dp,
                                                                    if (currentTheme == theme) colorScheme.primary
                                                                    else Color.Transparent,
                                                                    CircleShape
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(icon, label, tint = colorScheme.primary)
                                                        }
                                                        Text(label, fontSize = 10.sp, color = colorScheme.onSurface)
                                                    }
                                                }
                                            }
                                        }
                                        Divider()

                                        // Rest of menu items with premium styling
                                        listOf(
                                            Triple(Icons.Default.FileUpload, "Upload Item") { showForm = true },
                                            Triple(Icons.Default.Person, "User Profile") { showProfile = true },
                                            Triple(Icons.Default.History, "History") { historyExpanded = !historyExpanded }
                                        ).forEach { (icon, text, action) ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(icon, null, tint = colorScheme.primary)
                                                        Spacer(Modifier.width(12.dp))
                                                        Text(text, color = colorScheme.onSurface)

                                                        if (text == "History") {
                                                            Spacer(Modifier.weight(1f))
                                                            Icon(
                                                                if (historyExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                                contentDescription = "Expand History",
                                                                tint = colorScheme.onSurface.copy(alpha = 0.6f),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    action()
                                                    if (text != "History") menuExpanded = false
                                                },
                                                modifier = Modifier.background(Color.Transparent)
                                            )

                                            Divider()
                                        }

                                        // Nested history items
                                        if (historyExpanded) {
                                            Column {
                                                listOf(
                                                    Triple(Icons.Default.CloudUpload, "Upload History") { showUploadHistory = true },
                                                    Triple(Icons.Default.Delete, "Delete History") { showDeleteHistory = true },
                                                    Triple(Icons.Default.CheckCircle, "Claim History") { showClaimHistory = true }
                                                ).forEach { (icon, text, action) ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Spacer(Modifier.width(24.dp))
                                                                Icon(icon, null, tint = colorScheme.primary, modifier = Modifier.size(18.dp))
                                                                Spacer(Modifier.width(12.dp))
                                                                Text(text, color = colorScheme.onSurface, fontSize = 14.sp)
                                                            }
                                                        },
                                                        onClick = {
                                                            action()
                                                            menuExpanded = false
                                                            historyExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.ExitToApp, null, tint = Color.Red)
                                                    Spacer(Modifier.width(12.dp))
                                                    Text("Logout", color = Color.Red)
                                                }
                                            },
                                            onClick = {
                                                auth.signOut()
                                                startActivity(Intent(this@StudentHomeActivity, LoginActivity::class.java))
                                                finish()
                                                menuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Search Section as New Tab
                    if (showSearch) {
                        Spacer(Modifier.height(16.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Search Input
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search items...", color = colorScheme.onSurface.copy(alpha = 0.6f)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = {
                                        Icon(Icons.Default.Search, contentDescription = "Search", tint = colorScheme.primary)
                                    },
                                    colors = TextFieldDefaults.colors(
                                        focusedTextColor = colorScheme.onSurface,
                                        unfocusedTextColor = colorScheme.onSurface,
                                        focusedContainerColor = colorScheme.surface,
                                        unfocusedContainerColor = colorScheme.surface,
                                        focusedIndicatorColor = colorScheme.primary,
                                        unfocusedIndicatorColor = colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                )

                                Spacer(Modifier.height(12.dp))

                                // Search Filter Dropdown
                                var filterExpanded by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedButton(
                                        onClick = { filterExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = colorScheme.onSurface
                                        )
                                    ) {
                                        Text("Filter: $searchFilter", modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                                        Icon(
                                            if (filterExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                            contentDescription = "Filter",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = filterExpanded,
                                        onDismissRequest = { filterExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        listOf("All", "Title", "Category", "Location", "Email ID", "Date").forEach { filter ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        filter,
                                                        fontWeight = if (filter == searchFilter) FontWeight.SemiBold else FontWeight.Normal,
                                                        color = if (filter == searchFilter) colorScheme.primary else colorScheme.onSurface
                                                    )
                                                },
                                                onClick = {
                                                    searchFilter = filter
                                                    filterExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Search Results Info
                                if (searchQuery.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Found ${filteredItems.size} items matching \"$searchQuery\" in $searchFilter",
                                        fontSize = 12.sp,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    Spacer(Modifier.height(16.dp))

                    // Premium Tabs with Animation
                    TabRow(
                        selectedTabIndex = activeTab,
                        containerColor = Color.Transparent,
                        contentColor = colorScheme.primary,
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                modifier = Modifier
                                    .tabIndicatorOffset(tabPositions[activeTab])
                                    .padding(horizontal = 16.dp)
                                    .height(3.dp)
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(colorScheme.primary, colorScheme.secondary)
                                        ),
                                        shape = RoundedCornerShape(2.dp)
                                    ),
                                height = 3.dp
                            )
                        },
                        divider = {}
                    ) {
                        listOf("All Items", "My Items").forEachIndexed { index, title ->
                            Tab(
                                selected = activeTab == index,
                                onClick = { activeTab = index },
                                text = {
                                    Text(
                                        title,
                                        fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Medium,
                                        color = if (activeTab == index) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Premium Items List
                    val itemsToShow = filteredItems

                    if (itemsToShow.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Inbox,
                                    contentDescription = "No items",
                                    modifier = Modifier.size(80.dp),
                                    tint = colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    if (searchQuery.isEmpty()) {
                                        if (activeTab == 0) "No items found" else "You haven't uploaded any items yet"
                                    } else {
                                        "No items found matching \"$searchQuery\""
                                    },
                                    color = colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(itemsToShow, key = { it.id }) { item ->
                                PremiumItemCard(
                                    item = item,
                                    colorScheme = colorScheme,
                                    onImageClick = { fullImageUrl = item.imageUrl },
                                    onDelete = {
                                        db.collection("lost_and_found")
                                            .document(item.id)
                                            .update("isDeleted", true)
                                            .addOnSuccessListener {
                                                val deleteRecord = hashMapOf(
                                                    "title" to item.title,
                                                    "action" to "Deleted",
                                                    "timestamp" to System.currentTimeMillis(),
                                                    "uploaderEmail" to auth.currentUser?.email
                                                )
                                                db.collection("delete_history").add(deleteRecord)
                                                Toast.makeText(this@StudentHomeActivity, "Item deleted successfully", Toast.LENGTH_SHORT).show()
                                            }
                                    },
                                    onClaim = { description ->
                                        val currentUser = auth.currentUser
                                        if (currentUser != null) {
                                            val claimRecord = hashMapOf(
                                                "itemId" to item.id,
                                                "title" to item.title,
                                                "description" to item.description,
                                                "location" to item.location,
                                                "status" to item.status,
                                                "imageUrl" to item.imageUrl,
                                                "uploaderEmail" to item.uploaderEmail,
                                                "uploaderName" to item.uploaderName,
                                                "claimerEmail" to currentUser.email,
                                                "claimerName" to (userProfile?.name ?: userProfile?.username ?: currentUser.email ?: "Unknown"),
                                                "claimDescription" to description,
                                                "claimStatus" to "Pending",
                                                "timestamp" to System.currentTimeMillis(),
                                                "adminNotes" to "",
                                                "adminActionTimestamp" to 0L,
                                                "adminActionBy" to ""
                                            )

                                            db.collection("claims").add(claimRecord)
                                                .addOnSuccessListener { docRef ->
                                                    // Add to claim history with "Claim Sent" status
                                                    val historyRecord = hashMapOf(
                                                        "title" to item.title,
                                                        "action" to "Claim Sent",
                                                        "claimStatus" to "Pending",
                                                        "timestamp" to System.currentTimeMillis(),
                                                        "claimerEmail" to currentUser.email,
                                                        "uploaderEmail" to item.uploaderEmail,
                                                        "adminNotes" to ""
                                                    )
                                                    db.collection("claim_history").add(historyRecord)

                                                    // Send notification to uploader
                                                    val notificationData = hashMapOf(
                                                        "userId" to item.uploaderEmail,
                                                        "title" to "New Claim Request",
                                                        "message" to "The item '${item.title}' has been claimed by ${currentUser.email}",
                                                        "type" to "claim_request",
                                                        "read" to false,
                                                        "timestamp" to System.currentTimeMillis(),
                                                        "claimId" to docRef.id,
                                                        "itemId" to item.id
                                                    )
                                                    db.collection("notifications").add(notificationData)

                                                    Toast.makeText(this@StudentHomeActivity, "Claim request sent to admin!", Toast.LENGTH_LONG).show()
                                                }
                                                .addOnFailureListener { e ->
                                                    Toast.makeText(this@StudentHomeActivity, "Claim failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                    },
                                    auth = auth,
                                    db = db,
                                    userProfile = userProfile,
                                    claimRequests = claimRequests
                                )
                            }
                        }
                    }
                }
            }

            // Premium Upload Form Dialog
            if (showForm) {
                Dialog(
                    onDismissRequest = {
                        showForm = false
                        title = ""
                        description = ""
                        location = ""
                        status = ""
                        message = ""
                        selectedImageUri = null
                        isUploading = false
                    },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .fillMaxHeight(0.95f)
                            .shadow(24.dp, RoundedCornerShape(28.dp)),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = colorScheme.surface,
                            contentColor = colorScheme.onSurface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        ) {
                            Text(
                                "Upload Lost/Found Item",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.primary
                            )

                            Spacer(Modifier.height(16.dp))

                            if (isUploading) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Spacer(Modifier.height(8.dp))
                            }

                            // SCROLLABLE CONTENT
                            // SCROLLABLE CONTENT
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    label = { Text("Title *", color = colorScheme.onSurface) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isUploading, // Add this line
                                    colors = TextFieldDefaults.colors(
                                        focusedTextColor = colorScheme.onSurface,
                                        unfocusedTextColor = colorScheme.onSurface,
                                        focusedContainerColor = colorScheme.surface,
                                        unfocusedContainerColor = colorScheme.surface
                                    )
                                )
                                Spacer(Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { description = it },
                                    label = { Text("Description *", color = colorScheme.onSurface) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isUploading, // Add this line
                                    colors = TextFieldDefaults.colors(
                                        focusedTextColor = colorScheme.onSurface,
                                        unfocusedTextColor = colorScheme.onSurface,
                                        focusedContainerColor = colorScheme.surface,
                                        unfocusedContainerColor = colorScheme.surface
                                    )
                                )
                                Spacer(Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = location,
                                    onValueChange = { location = it },
                                    label = { Text("Location *", color = colorScheme.onSurface) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isUploading, // Add this line
                                    colors = TextFieldDefaults.colors(
                                        focusedTextColor = colorScheme.onSurface,
                                        unfocusedTextColor = colorScheme.onSurface,
                                        focusedContainerColor = colorScheme.surface,
                                        unfocusedContainerColor = colorScheme.surface
                                    )
                                )
                                Spacer(Modifier.height(12.dp))

                                Text("Status *", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colorScheme.onSurface)
                                Spacer(Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    // Found Option
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 4.dp)
                                            .clickable(
                                                enabled = !isUploading, // Add this line
                                                onClick = { if (!isUploading) status = "Found" } // Add condition
                                            ),
                                        elevation = CardDefaults.cardElevation(if (status == "Found") 8.dp else 2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            RadioButton(
                                                selected = status == "Found",
                                                onClick = { if (!isUploading) status = "Found" }, // Add condition
                                                enabled = !isUploading, // Add this line
                                                colors = RadioButtonDefaults.colors(
                                                    selectedColor = Color(0xFF4CAF50)
                                                )
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "Found",
                                                fontWeight = if (status == "Found") FontWeight.Bold else FontWeight.Normal,
                                                color = if (status == "Found") Color(0xFF4CAF50) else colorScheme.onSurface
                                            )
                                        }
                                    }

                                    // Lost Option
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 4.dp)
                                            .clickable(
                                                enabled = !isUploading, // Add this line
                                                onClick = { if (!isUploading) status = "Lost" } // Add condition
                                            ),
                                        elevation = CardDefaults.cardElevation(if (status == "Lost") 8.dp else 2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            RadioButton(
                                                selected = status == "Lost",
                                                onClick = { if (!isUploading) status = "Lost" }, // Add condition
                                                enabled = !isUploading, // Add this line
                                                colors = RadioButtonDefaults.colors(
                                                    selectedColor = Color(0xFFF44336)
                                                )
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "Lost",
                                                fontWeight = if (status == "Lost") FontWeight.Bold else FontWeight.Normal,
                                                color = if (status == "Lost") Color(0xFFF44336) else colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        if (!isUploading) { // Add condition
                                            launcher.launch("image/*")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isUploading // Add this line
                                ) {
                                    Text(if (selectedImageUri != null) "Change Image" else "Select Image *")
                                }

                                selectedImageUri?.let { uri ->
                                    Spacer(Modifier.height(8.dp))
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Selected Image",
                                        modifier = Modifier
                                            .size(120.dp)
                                            .align(Alignment.CenterHorizontally)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                }

                                if (message.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        message,
                                        color = if (message.contains("success")) colorScheme.primary
                                        else colorScheme.error,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))

                            // SUBMIT BUTTON - ALWAYS VISIBLE AT BOTTOM
                            // SUBMIT BUTTON - ALWAYS VISIBLE AT BOTTOM
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(
                                    onClick = {
                                        if (!isUploading) { // Add condition
                                            showForm = false
                                            title = ""
                                            description = ""
                                            location = ""
                                            status = ""
                                            message = ""
                                            selectedImageUri = null
                                            isUploading = false
                                        }
                                    },
                                    enabled = !isUploading // Add this line
                                ) {
                                    Text("Cancel", color = colorScheme.onSurface)
                                }

                                Button(
                                    onClick = {
                                        if (title.isEmpty() || description.isEmpty() || location.isEmpty() || status.isEmpty() || selectedImageUri == null) {
                                            message = "Please fill all fields and select image"
                                            return@Button
                                        }

                                        isUploading = true
                                        message = "Uploading..."

                                        uploadToImgBB(selectedImageUri!!) { url ->
                                            val data = hashMapOf(
                                                "title" to title,
                                                "description" to description,
                                                "location" to location,
                                                "status" to status,
                                                "imageUrl" to url,
                                                "uploaderEmail" to auth.currentUser?.email,
                                                "uploaderName" to auth.currentUser?.email,
                                                "timestamp" to System.currentTimeMillis(),
                                                "isDeleted" to false
                                            )

                                            db.collection("lost_and_found").add(data)
                                                .addOnSuccessListener {
                                                    message = "Item uploaded successfully!"
                                                    Toast.makeText(
                                                        this@StudentHomeActivity,
                                                        "Item uploaded successfully!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    showForm = false
                                                    title = ""
                                                    description = ""
                                                    location = ""
                                                    status = ""
                                                    selectedImageUri = null
                                                    isUploading = false
                                                }
                                                .addOnFailureListener { e ->
                                                    message = "Upload failed: ${e.message}"
                                                    isUploading = false
                                                }
                                        }
                                    },
                                    enabled = !isUploading
                                ) {
                                    if (isUploading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(if (isUploading) "Uploading..." else "Submit")
                                }
                            }
                        }
                    }
                }
            }

            // Full Image Dialog
            if (fullImageUrl != null) {
                Dialog(onDismissRequest = { fullImageUrl = null }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .fillMaxHeight(0.85f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = fullImageUrl,
                                contentDescription = "Full Image",
                                modifier = Modifier.fillMaxSize()
                            )

                            // Upload status info
                            val currentItem = uploadedItems.find { it.imageUrl == fullImageUrl }
                            currentItem?.let { item ->
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(16.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "Uploaded by: ${getUploaderDisplayName(item, userProfile)}",
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            IconButton(
                                onClick = { fullImageUrl = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }

            // User Profile Dialog
            // User Profile Dialog - ENHANCED VERSION
            if (showProfile) {
                Dialog(onDismissRequest = { showProfile = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .fillMaxHeight(0.85f), // Increased height for better spacing
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = colorScheme.surface,
                            contentColor = colorScheme.onSurface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        ) {
                            // Header with Close Button at Top
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "User Profile",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.primary
                                )

                                IconButton(
                                    onClick = { showProfile = false },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Scrollable Content
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                // Email Section
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = colorScheme.surface,
                                        contentColor = colorScheme.onSurface
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            "Account Information",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.primary
                                        )
                                        Spacer(Modifier.height(12.dp))

                                        // Email
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Email,
                                                contentDescription = "Email",
                                                tint = colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text("Email", fontWeight = FontWeight.Medium, color = colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 12.sp)
                                                Text(
                                                    auth.currentUser?.email ?: "Not available",
                                                    fontWeight = FontWeight.Normal,
                                                    color = colorScheme.onSurface,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(12.dp))

                                        // User Type
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = "Role",
                                                tint = colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text("Role", fontWeight = FontWeight.Medium, color = colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 12.sp)
                                                Text(
                                                    "Student",
                                                    fontWeight = FontWeight.Normal,
                                                    color = colorScheme.onSurface,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                // Upload History Summary
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = colorScheme.surface,
                                        contentColor = colorScheme.onSurface
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            "Upload History",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.primary
                                        )
                                        Spacer(Modifier.height(12.dp))

                                        // Stats Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    uploadHistory.size.toString(),
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colorScheme.primary
                                                )
                                                Text(
                                                    "Total Uploads",
                                                    fontSize = 7.sp,
                                                    color = colorScheme.onSurface.copy(alpha = 0.7f)
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    userItems.size.toString(),
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF4CAF50)
                                                )
                                                Text(
                                                    "Active Items",
                                                    fontSize = 7.sp,
                                                    color = colorScheme.onSurface.copy(alpha = 0.7f)
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    deleteHistory.size.toString(),
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFF44336)
                                                )
                                                Text(
                                                    "Deleted Items",
                                                    fontSize = 7.sp,
                                                    color = colorScheme.onSurface.copy(alpha = 0.7f)
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    claimHistory.size.toString(),
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFFD700)
                                                )
                                                Text(
                                                    "Claims Made",
                                                    fontSize = 7.sp,
                                                    color = colorScheme.onSurface.copy(alpha = 0.7f)
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(16.dp))

                                        if (uploadHistory.isNotEmpty()) {
                                            Text(
                                                "Recent Uploads",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colorScheme.onSurface
                                            )
                                            Spacer(Modifier.height(8.dp))

                                            LazyColumn(
                                                modifier = Modifier.heightIn(max = 200.dp)
                                            ) {
                                                items(uploadHistory.take(5)) { item ->
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp),
                                                        elevation = CardDefaults.cardElevation(2.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = colorScheme.surface.copy(alpha = 0.5f)
                                                        )
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(12.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    item.title,
                                                                    fontWeight = FontWeight.Medium,
                                                                    color = colorScheme.onSurface,
                                                                    fontSize = 14.sp,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                Text(
                                                                    item.action,
                                                                    color = colorScheme.primary,
                                                                    fontSize = 12.sp
                                                                )
                                                            }
                                                            Text(
                                                                SimpleDateFormat("MMM dd")
                                                                    .format(Date(item.timestamp)),
                                                                fontSize = 12.sp,
                                                                color = colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(80.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(
                                                        Icons.Default.Inbox,
                                                        contentDescription = "No uploads",
                                                        modifier = Modifier.size(32.dp),
                                                        tint = colorScheme.onSurface.copy(alpha = 0.3f)
                                                    )
                                                    Spacer(Modifier.height(8.dp))
                                                    Text(
                                                        "No items uploaded yet",
                                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                        color = colorScheme.onSurface.copy(alpha = 0.6f),
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Close Button at Bottom - FIXED POSITION
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { showProfile = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorScheme.primary
                                )
                            ) {
                                Text("Close Profile", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // History Dialogs
            if (showUploadHistory) {
                HistoryDialog("Upload History", uploadHistory, colorScheme) { showUploadHistory = false }
            }
            if (showDeleteHistory) {
                HistoryDialog("Delete History", deleteHistory, colorScheme) { showDeleteHistory = false }
            }
            if (showClaimHistory) {
                HistoryDialog("Claim History", claimHistory, colorScheme) { showClaimHistory = false }
            }

            // Notifications Dialog
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
                                    "Notifications",
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
                                            "No notifications yet",
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
    }

    @Composable
    fun PremiumItemCard(
        item: LostFoundItem,
        colorScheme: ColorScheme,
        onImageClick: () -> Unit,
        onDelete: () -> Unit,
        onClaim: (String) -> Unit,
        auth: FirebaseAuth,
        db: FirebaseFirestore,
        userProfile: UserProfile?,
        claimRequests: List<ClaimRequest>
    ) {
        var claimStatus by remember { mutableStateOf("") }
        var showClaimDialog by remember { mutableStateOf(false) }
        var claimDescription by remember { mutableStateOf("") }
        val context = LocalContext.current

        // Check claim status - FIXED LOGIC
        LaunchedEffect(item.id, claimRequests) {
            val currentUserEmail = auth.currentUser?.email
            if (currentUserEmail != null) {
                val userClaim = claimRequests.find {
                    it.itemId == item.id && it.claimerEmail == currentUserEmail
                }
                claimStatus = userClaim?.claimStatus ?: ""
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .padding(horizontal = 4.dp, vertical = 6.dp)
                .shadow(16.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface, contentColor = colorScheme.onSurface),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                colorScheme.surface,
                                colorScheme.surface.copy(alpha = 0.8f)
                            )
                        )
                    )
            ) {
                // Delete button for owner only
                if (item.uploaderEmail == auth.currentUser?.email) {
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
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header with title and status
                    Text(
                        item.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(8.dp))

                    // Description
                    Text(
                        item.description,
                        color = colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = 18.sp,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(8.dp))

                    // Status badge
                    Box(
                        modifier = Modifier
                            .wrapContentWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when (item.status) {
                                    "Lost" -> Color(0xFFF44336).copy(alpha = 0.2f)
                                    "Found" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                    else -> colorScheme.primary.copy(alpha = 0.2f)
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            item.status,
                            color = when (item.status) {
                                "Lost" -> Color(0xFFF44336)
                                "Found" -> Color(0xFF4CAF50)
                                else -> colorScheme.primary
                            },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Location
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            item.location,
                            color = colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    // Uploader info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Uploader",
                            tint = colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "By: ${getUploaderDisplayName(item, userProfile)}",
                            fontSize = 12.sp,
                            color = colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Image - Larger size
                    if (item.imageUrl.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onImageClick)
                        ) {
                            AsyncImage(
                                model = item.imageUrl,
                                contentDescription = "Item Image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Footer with timestamp and claim button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            SimpleDateFormat("MMM dd, HH:mm")
                                .format(Date(item.timestamp)),
                            fontSize = 12.sp,
                            color = colorScheme.onSurface.copy(alpha = 0.5f)
                        )

                        // CLAIM BUTTON - FIXED SIZE ISSUE
                        if (item.status == "Found" && item.uploaderEmail != auth.currentUser?.email) {
                            when (claimStatus) {
                                "Pending" -> {
                                    Box(
                                        modifier = Modifier
                                            .wrapContentWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFFFD700).copy(alpha = 0.2f))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            "⌛ Claim Sent",
                                            color = Color(0xFFFFD700),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                "Approved" -> {
                                    Box(
                                        modifier = Modifier
                                            .wrapContentWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF4CAF50).copy(alpha = 0.2f))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            "✓ Claim Approved",
                                            color = Color(0xFF4CAF50),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                "Rejected" -> {
                                    Box(
                                        modifier = Modifier
                                            .wrapContentWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFF44336).copy(alpha = 0.2f))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            "✗ Claim Rejected",
                                            color = Color(0xFFF44336),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                else -> {
                                    Button(
                                        onClick = {
                                            showClaimDialog = true
                                            claimDescription = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4CAF50)
                                        ),
                                        modifier = Modifier
                                            .height(40.dp)
                                            .width(120.dp)
                                    ) {
                                        Text(
                                            "Claim Item",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Claim Description Dialog
        if (showClaimDialog) {
            Dialog(onDismissRequest = { showClaimDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.5f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.surface,
                        contentColor = colorScheme.onSurface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Text(
                            "Claim Item",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(
                            "Why do you want to claim this item?",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSurface
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = claimDescription,
                            onValueChange = { claimDescription = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            placeholder = { Text("Enter your claim reason...") },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = colorScheme.onSurface,
                                unfocusedTextColor = colorScheme.onSurface,
                                focusedContainerColor = colorScheme.surface,
                                unfocusedContainerColor = colorScheme.surface
                            )
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = { showClaimDialog = false }
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    if (claimDescription.isNotEmpty()) {
                                        // Call the onClaim function with the description
                                        onClaim(claimDescription)
                                        showClaimDialog = false

                                        // Show success message
                                        Toast.makeText(
                                            context,
                                            "Claim request sent to admin!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Please enter a claim reason",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                enabled = claimDescription.isNotEmpty()
                            ) {
                                Text("Submit Claim")
                            }
                        }
                    }
                }
            }
        }
    }

    // Helper function
    private fun getUploaderDisplayName(item: LostFoundItem, userProfile: UserProfile?): String {
        return when {
            item.uploaderEmail == FirebaseAuth.getInstance().currentUser?.email -> {
                FirebaseAuth.getInstance().currentUser?.email ?: "Unknown User"
            }
            !item.uploaderName.isNullOrEmpty() -> item.uploaderName
            !item.uploaderEmail.isNullOrEmpty() -> item.uploaderEmail
            else -> "Unknown User"
        }
    }

    // -------------------- Helper Composable --------------------
    @Composable
    fun HistoryDialog(title: String, items: List<HistoryItem>, colorScheme: ColorScheme, onClose: () -> Unit) {
        Dialog(onDismissRequest = onClose) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.65f),
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
                    Text(
                        title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )

                    Spacer(Modifier.height(12.dp))

                    if (items.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No history found", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = colorScheme.onSurface)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(items) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    elevation = CardDefaults.cardElevation(2.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = when (item.claimStatus) {
                                            "Approved" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                                            "Rejected" -> Color(0xFFF44336).copy(alpha = 0.1f)
                                            "Pending" -> Color(0xFFFFD700).copy(alpha = 0.1f)
                                            else -> colorScheme.surface
                                        }
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
                                            Text(item.title, fontWeight = FontWeight.Medium, color = colorScheme.onSurface, fontSize = 14.sp)
                                            Text(
                                                SimpleDateFormat("MMM dd, HH:mm")
                                                    .format(Date(item.timestamp)),
                                                fontSize = 12.sp,
                                                color = colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            item.action,
                                            color = when {
                                                item.action.contains("Approved") -> Color(0xFF4CAF50)
                                                item.action.contains("Rejected") -> Color(0xFFF44336)
                                                item.action.contains("Sent") -> Color(0xFFFFD700)
                                                else -> colorScheme.onSurface
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (item.adminNotes.isNotEmpty()) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                "Admin Notes: ${item.adminNotes}",
                                                fontSize = 11.sp,
                                                color = colorScheme.onSurface.copy(alpha = 0.7f),
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = onClose,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }

    // -------------------- Image Upload Function --------------------
    private fun uploadToImgBB(uri: Uri, callback: (String) -> Unit) {
        try {
            val bytes = contentResolver.openInputStream(uri)?.readBytes() ?: run {
                runOnUiThread {
                    Toast.makeText(this, "Failed to read image", Toast.LENGTH_SHORT).show()
                }
                return
            }

            val client = OkHttpClient()
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image",
                    "upload_${System.currentTimeMillis()}.jpg",
                    bytes.toRequestBody("image/*".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("https://api.imgbb.com/1/upload?key=$imgbbApiKey")
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@StudentHomeActivity, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.body?.string()?.let { responseBody ->
                        val url = Regex("\"url\":\"(.*?)\"").find(responseBody)?.groups?.get(1)?.value
                        if (url != null) {
                            runOnUiThread { callback(url) }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@StudentHomeActivity, "Failed to get image URL", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            })
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, "Upload error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

