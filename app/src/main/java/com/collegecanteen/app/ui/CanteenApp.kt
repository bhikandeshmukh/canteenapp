package com.collegecanteen.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.collegecanteen.app.data.CanteenRepository
import com.collegecanteen.app.data.CartLine
import com.collegecanteen.app.data.FoodItem
import com.collegecanteen.app.data.FoodItemWrite
import com.collegecanteen.app.data.OrderStatus
import com.collegecanteen.app.data.OrderWithItems
import com.collegecanteen.app.data.Profile
import com.collegecanteen.app.data.UserRole
import kotlinx.coroutines.launch
import java.util.Locale

private enum class AuthMode { LOGIN, REGISTER }
private enum class StudentTab { MENU, ORDERS, PROFILE }
private enum class CanteenTab { ORDERS, MENU }

private val Ink = Color(0xFF1C1008)
private val EspressoLight = Color(0xFF3D1E08)
private val Flame = Color(0xFFFF5722)
private val FlameDark = Color(0xFFBF360C)
private val FlameLight = Color(0xFFFFF3EE)
private val Saffron = Color(0xFFFF8F00)
private val Cream = Color(0xFFFFFBF5)
private val Mist = Color(0xFFF5EDE3)
private val Bark = Color(0xFFE8DDD0)
private val Steel = Color(0xFF78909C)
private val Leaf = Color(0xFF2E7D32)
private val Mint = Color(0xFFE8F5E9)
private val NonVeg = Color(0xFFC62828)
private val NonVegLight = Color(0xFFFFEBEE)
private val Honey = Saffron
private val Coral = Flame
private val Sky = Color(0xFF1E88E5)
private val Paper = Cream

private data class DashboardData(
    val menu: List<FoodItem>,
    val orders: List<OrderWithItems>
)

private data class AppUiState(
    val activeRole: UserRole = UserRole.STUDENT,
    val authMode: AuthMode = AuthMode.LOGIN,
    val profile: Profile? = null,
    val menu: List<FoodItem> = emptyList(),
    val orders: List<OrderWithItems> = emptyList(),
    val cart: Map<String, Int> = emptyMap(),
    val notes: String = "",
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val studentTab: StudentTab = StudentTab.MENU,
    val canteenTab: CanteenTab = CanteenTab.ORDERS,
    val showCart: Boolean = false,
    val loading: Boolean = false,
    val message: String? = null
)

private val EspressoBrush = Brush.linearGradient(
    colors = listOf(Ink, EspressoLight, Color(0xFF2A0F05))
)

private val FlameBrush = Brush.linearGradient(
    colors = listOf(Flame, Saffron)
)

@Composable
fun CanteenApp(repository: CanteenRepository?) {
    if (repository == null) {
        MissingConfigScreen()
        return
    }

    var state by remember { mutableStateOf(AppUiState()) }
    val scope = rememberCoroutineScope()

    suspend fun loadDashboard(profile: Profile): DashboardData {
        val role = UserRole.fromValue(profile.role)
        return DashboardData(
            menu = repository.loadMenu(forCanteen = role == UserRole.CANTEEN),
            orders = repository.loadOrders(role)
        )
    }

    fun refresh(profile: Profile? = state.profile) {
        if (profile == null) return
        scope.launch {
            state = state.copy(loading = true, message = null)
            runCatching { loadDashboard(profile) }
                .onSuccess { data ->
                    state = state.copy(menu = data.menu, orders = data.orders, loading = false)
                }
                .onFailure { error ->
                    state = state.copy(loading = false, message = error.userMessage())
                }
        }
    }

    LaunchedEffect(Unit) {
        state = state.copy(loading = true)
        runCatching { repository.restoreSession() }
            .onSuccess { profile ->
                if (profile == null) {
                    state = state.copy(loading = false)
                } else {
                    val data = loadDashboard(profile)
                    state = state.copy(
                        profile = profile,
                        activeRole = UserRole.fromValue(profile.role),
                        menu = data.menu,
                        orders = data.orders,
                        loading = false
                    )
                }
            }
            .onFailure { error ->
                state = state.copy(loading = false, message = error.userMessage())
            }
    }

    fun authenticate(fullName: String, email: String, password: String) {
        scope.launch {
            state = state.copy(loading = true, message = null)
            runCatching {
                if (state.authMode == AuthMode.LOGIN) {
                    repository.signIn(email, password, state.activeRole)
                } else {
                    repository.register(fullName, email, password, UserRole.STUDENT, inviteCode = "")
                }
            }.onSuccess { profile ->
                if (profile == null) {
                    state = state.copy(
                        loading = false,
                        authMode = AuthMode.LOGIN,
                        message = "Account created. Email verify karke login karein."
                    )
                } else {
                    val data = loadDashboard(profile)
                    state = state.copy(
                        profile = profile,
                        activeRole = UserRole.fromValue(profile.role),
                        menu = data.menu,
                        orders = data.orders,
                        loading = false,
                        message = null
                    )
                }
            }.onFailure { error ->
                state = state.copy(loading = false, message = error.userMessage())
            }
        }
    }

    fun signOut() {
        scope.launch {
            val role = state.activeRole
            state = state.copy(loading = true, message = null)
            runCatching { repository.signOut() }
                .onSuccess { state = AppUiState(activeRole = role) }
                .onFailure { error -> state = state.copy(loading = false, message = error.userMessage()) }
        }
    }

    fun addToCart(item: FoodItem) {
        val current = state.cart[item.id] ?: 0
        state = state.copy(cart = state.cart + (item.id to current + 1))
    }

    fun removeFromCart(item: FoodItem) {
        val current = state.cart[item.id] ?: return
        val next = if (current <= 1) state.cart - item.id else state.cart + (item.id to current - 1)
        state = state.copy(cart = next)
    }

    fun placeOrder() {
        val cartLines = state.cartLines()
        scope.launch {
            state = state.copy(loading = true, message = null)
            runCatching {
                repository.placeOrder(cartLines, state.notes)
                loadDashboard(state.profile ?: error("Please login again."))
            }.onSuccess { data ->
                state = state.copy(
                    menu = data.menu,
                    orders = data.orders,
                    cart = emptyMap(),
                    notes = "",
                    studentTab = StudentTab.ORDERS,
                    showCart = false,
                    loading = false,
                    message = "Order placed."
                )
            }.onFailure { error ->
                state = state.copy(loading = false, message = error.userMessage())
            }
        }
    }

    fun updateOrder(orderId: String, status: OrderStatus) {
        scope.launch {
            state = state.copy(loading = true, message = null)
            runCatching {
                repository.updateOrderStatus(orderId, status)
                loadDashboard(state.profile ?: error("Please login again."))
            }.onSuccess { data ->
                state = state.copy(orders = data.orders, menu = data.menu, loading = false)
            }.onFailure { error ->
                state = state.copy(loading = false, message = error.userMessage())
            }
        }
    }

    fun toggleFood(item: FoodItem, available: Boolean) {
        scope.launch {
            state = state.copy(loading = true, message = null)
            runCatching {
                repository.updateFoodAvailability(item.id, available)
                loadDashboard(state.profile ?: error("Please login again."))
            }.onSuccess { data ->
                state = state.copy(menu = data.menu, orders = data.orders, loading = false)
            }.onFailure { error ->
                state = state.copy(loading = false, message = error.userMessage())
            }
        }
    }

    fun saveFoodItem(itemId: String?, item: FoodItemWrite) {
        scope.launch {
            state = state.copy(loading = true, message = null)
            runCatching {
                if (itemId == null) {
                    repository.addFoodItem(item)
                } else {
                    repository.updateFoodItem(itemId, item)
                }
                loadDashboard(state.profile ?: error("Please login again."))
            }.onSuccess { data ->
                state = state.copy(
                    menu = data.menu,
                    orders = data.orders,
                    loading = false,
                    message = if (itemId == null) "Menu item added." else "Menu item updated."
                )
            }.onFailure { error ->
                state = state.copy(loading = false, message = error.userMessage())
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
    ) {
        val profile = state.profile
        if (profile == null) {
            AuthScreen(
                state = state,
                onRoleChange = { role ->
                    state = state.copy(
                        activeRole = role,
                        authMode = if (role == UserRole.CANTEEN) AuthMode.LOGIN else state.authMode,
                        message = null
                    )
                },
                onModeChange = { state = state.copy(authMode = it, message = null) },
                onSubmit = ::authenticate
            )
        } else {
            when (UserRole.fromValue(profile.role)) {
                UserRole.STUDENT -> StudentHome(
                    state = state,
                    profile = profile,
                    onRefresh = { refresh() },
                    onSignOut = ::signOut,
                    onAdd = ::addToCart,
                    onRemove = ::removeFromCart,
                    onNotesChange = { state = state.copy(notes = it) },
                    onPlaceOrder = ::placeOrder,
                    onSearchChange = { state = state.copy(searchQuery = it) },
                    onCategoryChange = { state = state.copy(selectedCategory = it) },
                    onTabChange = { state = state.copy(studentTab = it, showCart = false) },
                    onCartToggle = {
                        state = state.copy(
                            showCart = state.cartLines().isNotEmpty() && !state.showCart
                        )
                    },
                    onCartClose = { state = state.copy(showCart = false) }
                )

                UserRole.CANTEEN -> CanteenHome(
                    state = state,
                    profile = profile,
                    onRefresh = { refresh() },
                    onSignOut = ::signOut,
                    onStatusChange = ::updateOrder,
                    onToggleFood = ::toggleFood,
                    onSaveFood = ::saveFoodItem,
                    onTabChange = { state = state.copy(canteenTab = it) }
                )
            }
        }

        if (state.loading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = Honey,
                trackColor = Mint
            )
        }
    }
}

@Composable
private fun MissingConfigScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = Paper) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
                BrandMark(size = 52.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Supabase config missing",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Create local.properties from local.properties.example and add SUPABASE_URL plus SUPABASE_PUBLISHABLE_KEY.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AuthScreen(
    state: AppUiState,
    onRoleChange: (UserRole) -> Unit,
    onModeChange: (AuthMode) -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val registerEnabled = state.activeRole == UserRole.STUDENT
    val isRegister = state.authMode == AuthMode.REGISTER && registerEnabled

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .verticalScroll(rememberScrollState())
    ) {
        AuthHero()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-18).dp)
                .padding(horizontal = 20.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(Modifier.padding(22.dp)) {
                    RoleSelector(
                        selectedRole = state.activeRole,
                        onRoleChange = onRoleChange
                    )
                    Spacer(Modifier.height(18.dp))

                    if (registerEnabled) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ModeButton(
                                selected = state.authMode == AuthMode.LOGIN,
                                text = "🔑 Login",
                                onClick = { onModeChange(AuthMode.LOGIN) },
                                modifier = Modifier.weight(1f)
                            )
                            ModeButton(
                                selected = state.authMode == AuthMode.REGISTER,
                                text = "✨ Register",
                                onClick = { onModeChange(AuthMode.REGISTER) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                    } else {
                        StaffLoginHeader()
                        Spacer(Modifier.height(18.dp))
                    }

                    if (isRegister) {
                        AuthInput(
                            value = fullName,
                            onValueChange = { fullName = it },
                            icon = "👤",
                            label = "Full Name",
                            placeholder = "Your full name"
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    AuthInput(
                        value = email,
                        onValueChange = { email = it },
                        icon = "📧",
                        label = "Email",
                        placeholder = "student@college.edu",
                        keyboardType = KeyboardType.Email
                    )
                    Spacer(Modifier.height(12.dp))
                    AuthInput(
                        value = password,
                        onValueChange = { password = it },
                        icon = "🔑",
                        label = "Password",
                        placeholder = "••••••••",
                        keyboardType = KeyboardType.Password,
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Spacer(Modifier.height(20.dp))
                    GradientButton(
                        text = if (isRegister) "✨ Create Account →" else "🚀 Login →",
                        enabled = email.isNotBlank() &&
                            password.length >= 6 &&
                            (!isRegister || fullName.isNotBlank()),
                        onClick = { onSubmit(fullName, email, password) }
                    )
                }
            }

            MessageBanner(state.message)
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun AuthHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(EspressoBrush)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .offset(x = 310.dp, y = (-40).dp)
                .clip(CircleShape)
                .background(Flame.copy(alpha = 0.12f))
        )
        Box(
            modifier = Modifier
                .size(80.dp)
                .offset(x = (-20).dp, y = 126.dp)
                .clip(CircleShape)
                .background(Saffron.copy(alpha = 0.08f))
        )
        Box(
            modifier = Modifier
                .size(50.dp)
                .offset(x = 292.dp, y = 20.dp)
                .clip(CircleShape)
                .background(Flame.copy(alpha = 0.08f))
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 44.dp, bottom = 40.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(size = 56.dp, radius = 16.dp, emojiSize = 26.sp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = "College Canteen",
                        fontSize = 24.sp,
                        lineHeight = 29.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Order • Track • Pickup",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.60f)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("🎓 Students", "👨‍🍳 Staff", "⚡ Live Tracking", "🎫 Token System")) { tag ->
                    HeroPill(tag, Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.80f))
                }
            }
        }
    }
}

@Composable
private fun StaffLoginHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = FlameLight
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔒", fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Staff Login Only", fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 13.sp)
                Text(
                    "Accounts created by canteen admin",
                    fontSize = 12.sp,
                    color = Steel
                )
            }
        }
    }
}

@Composable
private fun BrandMark(
    size: androidx.compose.ui.unit.Dp,
    radius: androidx.compose.ui.unit.Dp = 10.dp,
    emojiSize: androidx.compose.ui.unit.TextUnit = 20.sp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(FlameBrush),
        contentAlignment = Alignment.Center
    ) {
        Text("🍽️", fontSize = emojiSize)
    }
}

@Composable
private fun HeroPill(text: String, background: Color, content: Color) {
    Surface(shape = RoundedCornerShape(20.dp), color = background) {
        Text(
            text = text,
            color = content,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun RoleSelector(
    selectedRole: UserRole,
    onRoleChange: (UserRole) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Bark)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        UserRole.entries.forEach { role ->
            val selected = selectedRole == role
            val color = if (selected) {
                if (role == UserRole.STUDENT) Ink else Flame
            } else {
                Color.Transparent
            }
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = color
            ) {
                Button(
                    onClick = { onRoleChange(role) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    elevation = null,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 11.dp)
                ) {
                    Text(
                        text = if (role == UserRole.STUDENT) "🎓 Student" else "👨‍🍳 Canteen",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeButton(
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .border(
                width = 2.dp,
                color = if (selected) Flame else Bark,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) FlameLight else Color.Transparent
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = if (selected) Flame else Steel,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun AuthInput(
    value: String,
    onValueChange: (String) -> Unit,
    icon: String,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Column {
        Text(
            text = label.uppercase(Locale.US),
            color = Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Mist)
                .border(1.5.dp, Bark, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 17.sp)
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                visualTransformation = visualTransformation,
                cursorBrush = SolidColor(Flame),
                textStyle = TextStyle(color = Ink, fontSize = 14.sp),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (value.isBlank()) {
                        Text(placeholder, color = Steel.copy(alpha = 0.75f), fontSize = 14.sp)
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
private fun GradientButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.48f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .alpha(alpha)
            .clip(RoundedCornerShape(14.dp))
            .background(EspressoBrush)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentHome(
    state: AppUiState,
    profile: Profile,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
    onAdd: (FoodItem) -> Unit,
    onRemove: (FoodItem) -> Unit,
    onNotesChange: (String) -> Unit,
    onPlaceOrder: () -> Unit,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onTabChange: (StudentTab) -> Unit,
    onCartToggle: () -> Unit,
    onCartClose: () -> Unit
) {
    val cartLines = state.cartLines()
    val categories = remember(state.menu) {
        previewCategories(state.menu)
    }
    val headerTitle = when (state.studentTab) {
        StudentTab.MENU -> "Today's Menu"
        StudentTab.ORDERS -> "My Orders"
        StudentTab.PROFILE -> "My Profile"
    }
    val headerSubtitle = when (state.studentTab) {
        StudentTab.MENU -> "Choose items for pickup order"
        StudentTab.ORDERS -> "Track your pickup status in real time"
        StudentTab.PROFILE -> "Account and role details"
    }
    val filteredMenu = state.menu.filter { item ->
        val categoryMatch = state.selectedCategory == "All" || item.category == state.selectedCategory
        val searchMatch = state.searchQuery.isBlank() ||
            item.name.contains(state.searchQuery, ignoreCase = true) ||
            item.description.orEmpty().contains(state.searchQuery, ignoreCase = true)
        categoryMatch && searchMatch
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            FlexibleAppHeader(
                title = headerTitle,
                subtitle = headerSubtitle,
                profile = profile,
                roleLabel = "Student",
                onSignOut = onSignOut
            )
            MessageBanner(state.message)
            Box(modifier = Modifier.weight(1f)) {
                when (state.studentTab) {
                    StudentTab.MENU -> StudentMenuScreen(
                        menu = filteredMenu,
                        cart = state.cart,
                        onAdd = onAdd,
                        onRemove = onRemove,
                        searchQuery = state.searchQuery,
                        onSearchChange = onSearchChange,
                        categories = categories,
                        selectedCategory = state.selectedCategory,
                        onCategoryChange = onCategoryChange
                    )

                    StudentTab.ORDERS -> StudentOrdersScreen(orders = state.orders)
                    StudentTab.PROFILE -> ProfileScreen(profile = profile)
                }

                if (state.showCart && cartLines.isNotEmpty()) {
                    CartSheet(
                        cartLines = cartLines,
                        onClose = onCartClose,
                        onPlaceOrder = onPlaceOrder,
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(5f)
                    )
                }
            }
            StudentBottomNav(
                selectedTab = state.studentTab,
                cartCount = cartLines.sumOf { it.quantity },
                onMenuClick = { onTabChange(StudentTab.MENU) },
                onCartClick = onCartToggle,
                onOrdersClick = { onTabChange(StudentTab.ORDERS) },
                onProfileClick = { onTabChange(StudentTab.PROFILE) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CanteenHome(
    state: AppUiState,
    profile: Profile,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
    onStatusChange: (String, OrderStatus) -> Unit,
    onToggleFood: (FoodItem, Boolean) -> Unit,
    onSaveFood: (String?, FoodItemWrite) -> Unit,
    onTabChange: (CanteenTab) -> Unit
) {
    val pending = state.orders.count { OrderStatus.fromValue(it.order.status) == OrderStatus.PENDING }
    val preparing = state.orders.count { OrderStatus.fromValue(it.order.status) == OrderStatus.PREPARING }
    val ready = state.orders.count { OrderStatus.fromValue(it.order.status) == OrderStatus.READY }
    val completedRevenue = state.orders
        .filter { OrderStatus.fromValue(it.order.status) == OrderStatus.COMPLETED }
        .sumOf { it.order.totalAmount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
    ) {
        CanteenHeader(
            profile = profile,
            pending = pending,
            preparing = preparing,
            ready = ready,
            revenue = completedRevenue,
            selectedTab = state.canteenTab,
            onTabChange = onTabChange,
            onSignOut = onSignOut
        )
        MessageBanner(state.message)

        when (state.canteenTab) {
            CanteenTab.ORDERS -> CanteenOrdersList(
                orders = state.orders,
                onStatusChange = onStatusChange
            )

            CanteenTab.MENU -> CanteenMenuList(
                menu = state.menu,
                onToggleFood = onToggleFood,
                onSaveFood = onSaveFood
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    title: String,
    subtitle: String,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Paper,
            titleContentColor = Ink
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(size = 38.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
            }
            IconButton(onClick = onSignOut) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Logout")
            }
        }
    )
}

@Composable
private fun FlexibleAppHeader(
    title: String,
    subtitle: String,
    profile: Profile,
    roleLabel: String,
    onSignOut: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EspressoBrush)
            .padding(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandMark(size = 38.dp, radius = 10.dp, emojiSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${displayName(profile)} • $roleLabel",
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Surface(
            modifier = Modifier.clickable(onClick = onSignOut),
            shape = RoundedCornerShape(10.dp),
            color = Color.White.copy(alpha = 0.12f)
        ) {
            Text(
                "Logout",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun StudentTopBar(
    profile: Profile,
    onSignOut: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Cream)
            .border(1.dp, Bark.copy(alpha = 0.75f))
            .padding(start = 18.dp, top = 14.dp, end = 18.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark(size = 36.dp, radius = 10.dp, emojiSize = 18.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "College Canteen",
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
                Text(
                    text = "${profile.fullName ?: "Student"} • Student",
                    fontSize = 11.sp,
                    color = Steel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Mist,
            modifier = Modifier.clickable(onClick = onSignOut)
        ) {
            Text(
                text = "Logout",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Steel
            )
        }
    }
}

@Composable
private fun StudentMenuScreen(
    menu: List<FoodItem>,
    cart: Map<String, Int>,
    onAdd: (FoodItem) -> Unit,
    onRemove: (FoodItem) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SearchField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp)
        )

        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                CategoryChip(
                    category = category,
                    selected = selectedCategory == category,
                    onClick = { onCategoryChange(category) }
                )
            }
        }

        if (menu.isEmpty()) {
            EmptyState(text = "No items found")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(menu, key = { it.id }) { item ->
                    FoodCard(
                        item = item,
                        quantity = cart[item.id] ?: 0,
                        onAdd = { onAdd(item) },
                        onRemove = { onRemove(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuStatPill(value: String, label: String, dotColor: Color) {
    Surface(shape = RoundedCornerShape(10.dp), color = Color.White.copy(alpha = 0.10f)) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = value,
                    fontSize = 16.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.60f)
                )
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.5.dp, Bark, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🔍", fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            cursorBrush = SolidColor(Flame),
            textStyle = TextStyle(color = Ink, fontSize = 14.sp),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                if (value.isBlank()) {
                    Text("Search food items...", color = Steel.copy(alpha = 0.78f), fontSize = 14.sp)
                }
                innerTextField()
            }
        )
        if (value.isNotBlank()) {
            Text(
                text = "×",
                color = Steel,
                fontSize = 18.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onValueChange("") }
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .border(
                width = 1.5.dp,
                color = if (selected) Ink else Bark,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Ink else Color.White
    ) {
        Text(
            text = "${categoryEmoji(category)} $category",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = if (selected) Color.White else Steel,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun FoodCard(
    item: FoodItem,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    val veg = isVegFood(item)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Bark),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier
                    .size(58.dp)
                    .alpha(if (item.isAvailable) 1f else 0.45f),
                shape = RoundedCornerShape(14.dp),
                color = if (veg) Mint else NonVegLight
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(foodEmoji(item.category), fontSize = 26.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            VegIndicator(isVeg = veg, size = 14)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                item.name,
                                color = if (item.isAvailable) Ink else Steel,
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = item.category.orEmpty(),
                            fontSize = 11.sp,
                            color = Steel,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        if (!item.description.isNullOrBlank()) {
                            Text(
                                text = item.description,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = Steel,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        if (!item.isAvailable) {
                            Text(
                                text = "❌ Unavailable",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NonVeg,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = currency(item.price),
                        fontSize = 17.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = Flame
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (item.isAvailable) {
                        PreviewQuantityControls(quantity, onAdd, onRemove)
                    } else {
                        Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFECEFF1)) {
                            Text(
                                "Not available",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 11.sp,
                                color = Steel,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewQuantityControls(
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    if (quantity > 0) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Mist)
                .border(1.5.dp, Bark, RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            QuantityButton(text = "−", background = Color.White, content = Ink, onClick = onRemove)
            Text(
                text = quantity.toString(),
                color = Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.width(20.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            QuantityButton(text = "+", background = Ink, content = Color.White, onClick = onAdd)
        }
    } else {
        Surface(
            modifier = Modifier.clickable(onClick = onAdd),
            shape = RoundedCornerShape(12.dp),
            color = Ink,
            shadowElevation = 2.dp
        ) {
            Text(
                "+ Add",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun QuantityButton(
    text: String,
    background: Color,
    content: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(30.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = background,
        shadowElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = content, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StudentOrdersScreen(orders: List<OrderWithItems>) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (orders.isEmpty()) {
            EmptyState(text = "No orders yet.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(orders, key = { it.order.id }) { order ->
                    StudentOrderCard(order)
                }
            }
        }
    }
}

@Composable
private fun StudentOrderCard(orderWithItems: OrderWithItems) {
    val order = orderWithItems.order
    val status = OrderStatus.fromValue(order.status)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Bark),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Bark.copy(alpha = 0.55f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TokenBlock(token = orderToken(order.id), compact = false)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(shortDate(order.createdAt), color = Steel, fontSize = 11.sp)
                        if (status == OrderStatus.PREPARING) {
                            Text("⏱ ~8 min wait", color = Saffron, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                StatusChip(status = status)
            }
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                orderWithItems.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${item.quantity}× ${item.itemName}",
                            color = Ink,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(currency(item.itemPrice * item.quantity), color = Steel, fontSize = 13.sp)
                    }
                }
                HorizontalDivider(color = Bark, modifier = Modifier.padding(top = 6.dp, bottom = 10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        currency(order.totalAmount),
                        color = Flame,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                    if (status == OrderStatus.READY) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Mint,
                            border = BorderStroke(1.5.dp, Leaf)
                        ) {
                            Text(
                                "🔔 Ready for Pickup!",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                color = Leaf,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(profile: Profile) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Bark),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(14.dp), color = FlameLight) {
                            Text(
                                text = displayName(profile).take(1).uppercase(Locale.US),
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                                color = Flame,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                displayName(profile),
                                color = Ink,
                                fontSize = 18.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                UserRole.fromValue(profile.role).label,
                                color = Steel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    ProfileRow(label = "Role", value = UserRole.fromValue(profile.role).label)
                    ProfileRow(label = "Phone", value = profile.phone ?: "Not added")
                    ProfileRow(label = "Member Since", value = shortDate(profile.createdAt))
                    ProfileRow(label = "User ID", value = profile.id.take(8).uppercase(Locale.US))
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Steel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(
            value,
            color = Ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CartSheet(
    cartLines: List<CartLine>,
    onClose: () -> Unit,
    onPlaceOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Ink.copy(alpha = 0.60f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Color.White)
                .clickable(enabled = false) {}
                .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "🛒 Your Cart",
                    color = Ink,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "×",
                    color = Steel,
                    fontSize = 24.sp,
                    modifier = Modifier.clickable(onClick = onClose)
                )
            }
            Spacer(Modifier.height(16.dp))
            cartLines.forEach { line ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Bark.copy(alpha = 0.55f))
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(foodEmoji(line.item.category), fontSize = 20.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(line.item.name, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${line.quantity} × ${currency(line.item.price)}",
                                color = Steel,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Text(
                        currency(line.lineTotal),
                        color = Flame,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Ink.copy(alpha = 0.7f))
                    .padding(top = 14.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total", color = Ink, fontSize = 16.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
                Text(
                    currency(cartLines.sumOf { it.lineTotal }),
                    color = Flame,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )
            }
            GradientButton(text = "✓ Place Order", onClick = onPlaceOrder)
        }
    }
}

@Composable
private fun CanteenHeader(
    profile: Profile,
    pending: Int,
    preparing: Int,
    ready: Int,
    revenue: Double,
    selectedTab: CanteenTab,
    onTabChange: (CanteenTab) -> Unit,
    onSignOut: () -> Unit
) {
    val title = if (selectedTab == CanteenTab.ORDERS) "Order Counter" else "Menu Manager"
    val subtitle = if (selectedTab == CanteenTab.ORDERS) {
        "Prepare packs and handover quickly"
    } else {
        "Add items, edit prices and availability"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(EspressoBrush)
            .padding(start = 18.dp, top = 16.dp, end = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(size = 38.dp, radius = 10.dp, emojiSize = 20.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        subtitle,
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${displayName(profile)} • Admin",
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Surface(
                modifier = Modifier.clickable(onClick = onSignOut),
                shape = RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = 0.12f)
            ) {
                Text(
                    "Logout",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CanteenStatCard("📦", "New Orders", pending.toString(), Modifier.weight(1f))
            CanteenStatCard("👨‍🍳", "Cooking", preparing.toString(), Modifier.weight(1f))
            CanteenStatCard("✅", "Ready", ready.toString(), Modifier.weight(1f))
            CanteenStatCard("💰", "Today's Rev", currency(revenue), Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            CanteenTabButton(
                text = "📦 Orders",
                selected = selectedTab == CanteenTab.ORDERS,
                onClick = { onTabChange(CanteenTab.ORDERS) },
                modifier = Modifier.weight(1f)
            )
            CanteenTabButton(
                text = "📋 Menu",
                selected = selectedTab == CanteenTab.MENU,
                onClick = { onTabChange(CanteenTab.MENU) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CanteenStatCard(icon: String, label: String, value: String, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.10f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 16.sp)
            Spacer(Modifier.height(3.dp))
            Text(
                value,
                color = Color.White,
                fontSize = 13.sp,
                lineHeight = 13.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                label,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CanteenTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(top = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.50f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(9.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(if (selected) Flame else Color.Transparent)
        )
    }
}

@Composable
private fun CanteenOrdersList(
    orders: List<OrderWithItems>,
    onStatusChange: (String, OrderStatus) -> Unit
) {
    if (orders.isEmpty()) {
        EmptyState(text = "No orders received.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(orders, key = { it.order.id }) { order ->
            CanteenOrderCard(orderWithItems = order, onStatusChange = onStatusChange)
        }
    }
}

@Composable
private fun CanteenOrderCard(
    orderWithItems: OrderWithItems,
    onStatusChange: (String, OrderStatus) -> Unit
) {
    val order = orderWithItems.order
    val status = OrderStatus.fromValue(order.status)
    val nextStatus = status.nextStatus()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Bark),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Bark.copy(alpha = 0.55f))
                    .padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    TokenBlock(token = orderToken(order.id), compact = true)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            studentDisplayName(orderWithItems),
                            color = Ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!order.notes.isNullOrBlank()) {
                            Text(
                                "📝 \"${order.notes}\"",
                                color = Saffron,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                StatusChip(status = status)
            }
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                orderWithItems.items.forEach { item ->
                    Text(
                        "${item.quantity}× ${item.itemName}",
                        color = Ink,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 3.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                HorizontalDivider(color = Bark, modifier = Modifier.padding(top = 7.dp, bottom = 10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        currency(order.totalAmount),
                        color = Flame,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                    if (nextStatus != null) {
                        Surface(
                            modifier = Modifier.clickable { onStatusChange(order.id, nextStatus) },
                            shape = RoundedCornerShape(12.dp),
                            color = Ink,
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                "${nextStatus.actionLabel()} →",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CanteenMenuList(
    menu: List<FoodItem>,
    onToggleFood: (FoodItem, Boolean) -> Unit,
    onSaveFood: (String?, FoodItemWrite) -> Unit
) {
    var adding by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }

    if (menu.isEmpty()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AddMenuItemButton(onClick = {
                    adding = true
                    editingId = null
                })
            }
            if (adding) {
                item {
                    MenuEditorCard(
                        item = null,
                        onCancel = { adding = false },
                        onSave = { write ->
                            adding = false
                            onSaveFood(null, write)
                        }
                    )
                }
            }
            item { EmptyState(text = "No menu items.") }
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AddMenuItemButton(onClick = {
                adding = true
                editingId = null
            })
        }
        if (adding) {
            item {
                MenuEditorCard(
                    item = null,
                    onCancel = { adding = false },
                    onSave = { write ->
                        adding = false
                        onSaveFood(null, write)
                    }
                )
            }
        }
        items(menu, key = { it.id }) { item ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CanteenMenuCard(
                    item = item,
                    onToggle = { onToggleFood(item, !item.isAvailable) },
                    onEdit = {
                        adding = false
                        editingId = if (editingId == item.id) null else item.id
                    }
                )
                if (editingId == item.id) {
                    MenuEditorCard(
                        item = item,
                        onCancel = { editingId = null },
                        onSave = { write ->
                            editingId = null
                            onSaveFood(item.id, write)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CanteenMenuCard(item: FoodItem, onToggle: () -> Unit, onEdit: () -> Unit) {
    val veg = isVegFood(item)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Bark, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(46.dp)
                .alpha(if (item.isAvailable) 1f else 0.5f),
            shape = RoundedCornerShape(12.dp),
            color = if (item.isAvailable) Mist else Color(0xFFECEFF1)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(foodEmoji(item.category), fontSize = 22.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VegIndicator(isVeg = veg, size = 12)
                Spacer(Modifier.width(6.dp))
                Text(
                    item.name,
                    color = if (item.isAvailable) Ink else Steel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                "${item.category.orEmpty()} • ${currency(item.price)}",
                color = Steel,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Surface(
                modifier = Modifier.clickable(onClick = onEdit),
                shape = RoundedCornerShape(9.dp),
                color = FlameLight,
                border = BorderStroke(1.dp, Flame.copy(alpha = 0.65f))
            ) {
                Text(
                    "Edit",
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                    color = Flame,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            AvailabilitySwitch(checked = item.isAvailable, onClick = onToggle)
            Text(
                if (item.isAvailable) "On" else "Off",
                color = if (item.isAvailable) Leaf else Steel,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AddMenuItemButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Ink,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("+ Add New Item", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MenuEditorCard(
    item: FoodItem?,
    onCancel: () -> Unit,
    onSave: (FoodItemWrite) -> Unit
) {
    var name by remember(item?.id) { mutableStateOf(item?.name.orEmpty()) }
    var category by remember(item?.id) { mutableStateOf(item?.category.orEmpty()) }
    var price by remember(item?.id) { mutableStateOf(item?.price?.let { "%.0f".format(Locale.US, it) }.orEmpty()) }
    var description by remember(item?.id) { mutableStateOf(item?.description.orEmpty()) }
    var available by remember(item?.id) { mutableStateOf(item?.isAvailable ?: true) }
    var error by remember(item?.id) { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Flame.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (item == null) "Add Menu Item" else "Edit Menu Item",
                color = Ink,
                fontSize = 16.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
            MenuEditField("Name", name, { name = it }, "Masala Dosa")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MenuEditField(
                    label = "Category",
                    value = category,
                    onValueChange = { category = it },
                    placeholder = "Snacks",
                    modifier = Modifier.weight(1f)
                )
                MenuEditField(
                    label = "Price",
                    value = price,
                    onValueChange = { price = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    placeholder = "45",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(0.75f)
                )
            }
            MenuEditField("Description", description, { description = it }, "Short item details")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Available", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                AvailabilitySwitch(checked = available, onClick = { available = !available })
            }
            if (error != null) {
                Text(error.orEmpty(), color = NonVeg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onCancel),
                    shape = RoundedCornerShape(12.dp),
                    color = Mist,
                    border = BorderStroke(1.dp, Bark)
                ) {
                    Box(Modifier.padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
                        Text("Cancel", color = Steel, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val parsedPrice = price.toDoubleOrNull()
                            if (name.isBlank()) {
                                error = "Name required."
                            } else if (parsedPrice == null || parsedPrice < 0.0) {
                                error = "Valid price required."
                            } else {
                                error = null
                                onSave(
                                    FoodItemWrite(
                                        name = name.trim(),
                                        description = description.trim().ifBlank { null },
                                        category = category.trim().ifBlank { null },
                                        price = parsedPrice,
                                        isAvailable = available
                                    )
                                )
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = Ink
                ) {
                    Box(Modifier.padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
                        Text("Save", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuEditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label.uppercase(Locale.US), color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(Flame),
            textStyle = TextStyle(color = Ink, fontSize = 14.sp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Mist)
                .border(1.dp, Bark, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 11.dp),
            decorationBox = { innerTextField ->
                if (value.isBlank()) {
                    Text(placeholder, color = Steel.copy(alpha = 0.72f), fontSize = 14.sp)
                }
                innerTextField()
            }
        )
    }
}

@Composable
private fun AvailabilitySwitch(checked: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) Leaf else Bark)
            .clickable(onClick = onClick)
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
private fun TokenBlock(token: String, compact: Boolean) {
    Surface(shape = RoundedCornerShape(10.dp), color = Ink) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (compact) 11.dp else 12.dp,
                vertical = if (compact) 5.dp else 6.dp
            )
        ) {
            Text("TOKEN", color = Color.White.copy(alpha = 0.55f), fontSize = if (compact) 8.sp else 9.sp)
            Text(
                "# $token",
                color = Color(0xFFF9A825),
                fontSize = if (compact) 15.sp else 16.sp,
                lineHeight = if (compact) 15.sp else 16.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun VegIndicator(isVeg: Boolean, size: Int) {
    val color = if (isVeg) Leaf else NonVeg
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(3.dp))
            .border(1.5.dp, color, RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size((size / 2).dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
private fun StatusChip(status: OrderStatus) {
    val colors = statusColors(status)
    Surface(shape = RoundedCornerShape(20.dp), color = colors.first) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(colors.third)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = status.previewLabel(),
                color = colors.second,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

private data class StatItem(val label: String, val value: String, val color: Color)

@Composable
private fun DashboardHeader(
    title: String,
    subtitle: String,
    stats: List<StatItem>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(EspressoBrush)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.72f)
            )
            Spacer(Modifier.height(14.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(stats) { stat ->
                    StatCard(stat)
                }
            }
        }
    }
}

@Composable
private fun MenuControls(
    query: String,
    categories: List<String>,
    selectedCategory: String,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onSearchChange,
            label = { Text("Search food items") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Steel) },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { category ->
                val selected = category == selectedCategory
                Button(
                    onClick = { onCategoryChange(category) },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) Ink else Color.White,
                        contentColor = if (selected) Color.White else Steel
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (selected) 0.dp else 1.dp)
                ) {
                    Text(category, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun StudentBottomNav(
    selectedTab: StudentTab,
    cartCount: Int,
    onMenuClick: () -> Unit,
    onCartClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Bark.copy(alpha = 0.7f))
                .padding(top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                selected = selectedTab == StudentTab.MENU,
                label = "Menu",
                iconText = "🍛",
                onClick = onMenuClick
            )
            BottomNavItem(
                selected = false,
                label = "Cart",
                badge = cartCount.takeIf { it > 0 },
                iconText = "🛒",
                onClick = onCartClick
            )
            BottomNavItem(
                selected = selectedTab == StudentTab.ORDERS,
                label = "Orders",
                iconText = "📋",
                onClick = onOrdersClick
            )
            BottomNavItem(
                selected = selectedTab == StudentTab.PROFILE,
                label = "Profile",
                iconText = "👤",
                onClick = onProfileClick
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    selected: Boolean,
    label: String,
    badge: Int? = null,
    iconText: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box {
            Text(
                text = iconText,
                fontSize = 22.sp,
                modifier = Modifier.alpha(if (selected) 1f else 0.5f)
            )
            if (badge != null) {
                Surface(
                    shape = CircleShape,
                    color = Flame,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-4).dp)
                        .size(16.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = badge.toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Flame else Steel
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .size(width = 24.dp, height = 3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Flame)
            )
        } else {
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatCard(stat: StatItem) {
    Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.10f)) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(stat.color)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    stat.value,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    stat.label,
                    color = Color.White.copy(alpha = 0.68f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun StudentTabs(selectedTab: StudentTab, onTabChange: (StudentTab) -> Unit) {
    PrimaryTabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = Paper,
        contentColor = Leaf
    ) {
        Tab(
            selected = selectedTab == StudentTab.MENU,
            onClick = { onTabChange(StudentTab.MENU) },
            icon = { Icon(Icons.Outlined.Restaurant, contentDescription = null) },
            text = { Text("Menu") }
        )
        Tab(
            selected = selectedTab == StudentTab.ORDERS,
            onClick = { onTabChange(StudentTab.ORDERS) },
            icon = { Icon(Icons.Outlined.Receipt, contentDescription = null) },
            text = { Text("Orders") }
        )
    }
}

@Composable
private fun CanteenTabs(selectedTab: CanteenTab, onTabChange: (CanteenTab) -> Unit) {
    PrimaryTabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = Paper,
        contentColor = Coral
    ) {
        Tab(
            selected = selectedTab == CanteenTab.ORDERS,
            onClick = { onTabChange(CanteenTab.ORDERS) },
            icon = { Icon(Icons.Outlined.Receipt, contentDescription = null) },
            text = { Text("Orders") }
        )
        Tab(
            selected = selectedTab == CanteenTab.MENU,
            onClick = { onTabChange(CanteenTab.MENU) },
            icon = { Icon(Icons.Outlined.Restaurant, contentDescription = null) },
            text = { Text("Menu") }
        )
    }
}

@Composable
private fun MenuList(
    menu: List<FoodItem>,
    cart: Map<String, Int>,
    canteenMode: Boolean,
    emptyText: String = if (canteenMode) "No menu items." else "Menu is empty.",
    onAdd: (FoodItem) -> Unit,
    onRemove: (FoodItem) -> Unit,
    onToggleFood: ((FoodItem, Boolean) -> Unit)?
) {
    if (menu.isEmpty()) {
        EmptyState(text = emptyText)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(menu, key = { it.id }) { item ->
            MenuItemCard(
                item = item,
                quantity = cart[item.id] ?: 0,
                canteenMode = canteenMode,
                onAdd = { onAdd(item) },
                onRemove = { onRemove(item) },
                onToggleAvailable = { available -> onToggleFood?.invoke(item, available) }
            )
        }
        item { Spacer(Modifier.height(92.dp)) }
    }
}

@Composable
private fun MenuItemCard(
    item: FoodItem,
    quantity: Int,
    canteenMode: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onToggleAvailable: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FoodAvatar(item.name, item.category)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!item.category.isNullOrBlank()) {
                            Text(
                                item.category,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        currency(item.price),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Flame
                    )
                }

                if (!item.description.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(12.dp))
                if (canteenMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatusChip(text = if (item.isAvailable) "Available" else "Hidden")
                        Switch(checked = item.isAvailable, onCheckedChange = onToggleAvailable)
                    }
                } else {
                    QuantityControls(
                        quantity = quantity,
                        onAdd = onAdd,
                        onRemove = onRemove
                    )
                }
            }
        }
    }
}

@Composable
private fun FoodAvatar(name: String, category: String?) {
    val bg = when (category?.lowercase(Locale.US)) {
        "beverages" -> Color(0xFFE7F0FA)
        "snacks" -> FlameLight
        "breakfast" -> Color(0xFFFFF3CF)
        "main course" -> Color(0xFFFFF8E1)
        "south indian" -> Color(0xFFFFF8E1)
        else -> Mint
    }
    val fg = when (category?.lowercase(Locale.US)) {
        "beverages" -> Sky
        "snacks" -> Flame
        "breakfast" -> Color(0xFF9A6B00)
        "main course" -> Saffron
        "south indian" -> Saffron
        else -> Leaf
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        modifier = Modifier.size(54.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "F",
                color = fg,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun QuantityControls(
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        if (quantity > 0) {
            Surface(shape = RoundedCornerShape(8.dp), color = Mist) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Outlined.Remove, contentDescription = "Remove", tint = Ink)
                    }
                    Text(
                        text = quantity.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                    IconButton(onClick = onAdd, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add", tint = Ink)
                    }
                }
            }
        } else {
            Button(
                onClick = onAdd,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add")
            }
        }
    }
}

@Composable
private fun CartBar(
    cartLines: List<CartLine>,
    notes: String,
    onNotesChange: (String) -> Unit,
    onPlaceOrder: () -> Unit
) {
    Surface(color = Color.White, tonalElevation = 4.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Mint) {
                        Icon(
                            Icons.Outlined.ShoppingCart,
                            contentDescription = null,
                            tint = Leaf,
                            modifier = Modifier.padding(9.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "${cartLines.sumOf { it.quantity }} items",
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )
                        Text(
                            "Pickup order",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    currency(cartLines.sumOf { it.lineTotal }),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Leaf
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("Notes") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onPlaceOrder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink)
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Place order")
            }
        }
    }
}

@Composable
private fun OrdersList(
    orders: List<OrderWithItems>,
    canteenMode: Boolean,
    onStatusChange: ((String, OrderStatus) -> Unit)?
) {
    if (orders.isEmpty()) {
        EmptyState(text = if (canteenMode) "No orders received." else "No orders yet.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(orders, key = { it.order.id }) { order ->
            OrderCard(
                orderWithItems = order,
                canteenMode = canteenMode,
                onStatusChange = onStatusChange
            )
        }
    }
}

@Composable
private fun OrderCard(
    orderWithItems: OrderWithItems,
    canteenMode: Boolean,
    onStatusChange: ((String, OrderStatus) -> Unit)?
) {
    val order = orderWithItems.order
    val status = OrderStatus.fromValue(order.status)
    val nextStatus = when (status) {
        OrderStatus.PENDING -> OrderStatus.PREPARING
        OrderStatus.PREPARING -> OrderStatus.READY
        OrderStatus.READY -> OrderStatus.COMPLETED
        OrderStatus.COMPLETED,
        OrderStatus.CANCELLED -> null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Order #${order.id.take(8).uppercase(Locale.US)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                    Text(
                        shortDate(order.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusChip(status.label)
            }

            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                orderWithItems.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${item.quantity} x ${item.itemName}",
                            color = Ink,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            currency(item.itemPrice * item.quantity),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!order.notes.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        "Note: ${order.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    currency(order.totalAmount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Leaf
                )
                if (canteenMode && nextStatus != null) {
                    Button(
                        onClick = { onStatusChange?.invoke(order.id, nextStatus) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = status.actionColor())
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(nextStatus.label)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(text: String) {
    val bg = when (text) {
        OrderStatus.PENDING.label -> Color(0xFFFFE8DD)
        OrderStatus.PREPARING.label -> Color(0xFFFFF3CF)
        OrderStatus.READY.label -> Mint
        OrderStatus.COMPLETED.label -> Color(0xFFE7F0FA)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = when (text) {
        OrderStatus.PENDING.label -> Coral
        OrderStatus.PREPARING.label -> Color(0xFF8C6200)
        OrderStatus.READY.label -> Leaf
        OrderStatus.COMPLETED.label -> Sky
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}

@Composable
private fun MessageBanner(message: String?) {
    if (message.isNullOrBlank()) return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFFFF3CF)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF664600)
        )
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(8.dp), color = Mint) {
                Icon(
                    imageVector = Icons.Outlined.Restaurant,
                    contentDescription = null,
                    tint = Leaf,
                    modifier = Modifier
                        .size(54.dp)
                        .padding(12.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun AppUiState.cartLines(): List<CartLine> {
    return cart.mapNotNull { (itemId, quantity) ->
        menu.firstOrNull { it.id == itemId }?.let { CartLine(it, quantity) }
    }
}

private fun displayName(profile: Profile): String {
    val role = UserRole.fromValue(profile.role)
    return profile.fullName?.takeIf { it.isNotBlank() }
        ?: if (role == UserRole.CANTEEN) "Canteen Admin" else "Student"
}

private fun studentDisplayName(order: OrderWithItems): String =
    order.student?.fullName?.takeIf { it.isNotBlank() } ?: "Student #${orderToken(order.order.id)}"

private fun previewCategories(menu: List<FoodItem>): List<String> {
    val base = listOf("All", "Breakfast", "Main Course", "Snacks", "Beverages")
    val extras = menu
        .mapNotNull { it.category?.takeIf(String::isNotBlank) }
        .distinct()
        .filterNot { it in base }
    return base + extras
}

private fun categoryEmoji(category: String): String =
    when (category.lowercase(Locale.US)) {
        "all" -> "🍽️"
        "breakfast" -> "🌅"
        "main course" -> "🍛"
        "snacks" -> "🥨"
        "beverages" -> "☕"
        "south indian" -> "🥞"
        else -> "🍽️"
    }

private fun foodEmoji(category: String?): String =
    when (category?.lowercase(Locale.US)) {
        "breakfast", "south indian" -> "🥞"
        "main course" -> "🍛"
        "snacks" -> "🥪"
        "beverages" -> "🧋"
        else -> "🍽️"
    }

private fun isVegFood(item: FoodItem): Boolean {
    val text = listOf(item.name, item.category.orEmpty(), item.description.orEmpty())
        .joinToString(" ")
        .lowercase(Locale.US)
    return listOf("chicken", "egg", "fish", "mutton", "meat", "non veg", "non-veg")
        .none { it in text }
}

private fun orderToken(id: String): String {
    val digits = id.filter(Char::isDigit)
    return if (digits.isNotBlank()) {
        digits.takeLast(4).padStart(4, '0')
    } else {
        id.take(4).uppercase(Locale.US).padStart(4, '0')
    }
}

private fun OrderStatus.previewLabel(): String =
    when (this) {
        OrderStatus.PENDING -> "Received"
        OrderStatus.PREPARING -> "Preparing"
        OrderStatus.READY -> "Packed ✓"
        OrderStatus.COMPLETED -> "Done"
        OrderStatus.CANCELLED -> "Cancelled"
    }

private fun OrderStatus.nextStatus(): OrderStatus? =
    when (this) {
        OrderStatus.PENDING -> OrderStatus.PREPARING
        OrderStatus.PREPARING -> OrderStatus.READY
        OrderStatus.READY -> OrderStatus.COMPLETED
        OrderStatus.COMPLETED,
        OrderStatus.CANCELLED -> null
    }

private fun OrderStatus.actionLabel(): String =
    when (this) {
        OrderStatus.PREPARING -> "Start Cooking"
        OrderStatus.READY -> "Mark Packed"
        OrderStatus.COMPLETED -> "Hand Over"
        else -> previewLabel()
    }

private fun statusColors(status: OrderStatus): Triple<Color, Color, Color> =
    when (status) {
        OrderStatus.PENDING -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), Color(0xFFFF6D00))
        OrderStatus.PREPARING -> Triple(Color(0xFFFFF8E1), Color(0xFFF57F17), Color(0xFFFFB300))
        OrderStatus.READY -> Triple(Mint, Color(0xFF1B5E20), Color(0xFF43A047))
        OrderStatus.COMPLETED -> Triple(Color(0xFFE3F2FD), Color(0xFF0D47A1), Color(0xFF1E88E5))
        OrderStatus.CANCELLED -> Triple(Color(0xFFFAFAFA), Color(0xFF616161), Color(0xFF9E9E9E))
    }

private fun OrderStatus.actionColor(): Color =
    when (this) {
        OrderStatus.PENDING -> Coral
        OrderStatus.PREPARING -> Color(0xFFB47B00)
        OrderStatus.READY -> Leaf
        OrderStatus.COMPLETED -> Sky
        OrderStatus.CANCELLED -> Color(0xFF707871)
    }

private fun currency(value: Double): String =
    "₹%.0f".format(Locale.US, value)

private fun shortDate(value: String?): String =
    value?.replace("T", " ")?.take(16) ?: "-"

private fun Throwable.userMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: "Something went wrong."
