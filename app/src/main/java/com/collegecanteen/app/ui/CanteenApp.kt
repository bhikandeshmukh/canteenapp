package com.collegecanteen.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.collegecanteen.app.data.CanteenRepository
import com.collegecanteen.app.data.CartLine
import com.collegecanteen.app.data.FoodItem
import com.collegecanteen.app.data.OrderStatus
import com.collegecanteen.app.data.OrderWithItems
import com.collegecanteen.app.data.Profile
import com.collegecanteen.app.data.UserRole
import kotlinx.coroutines.launch
import java.util.Locale

private enum class AuthMode { LOGIN, REGISTER }
private enum class StudentTab { MENU, ORDERS }
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
                    onTabChange = { state = state.copy(studentTab = it) }
                )

                UserRole.CANTEEN -> CanteenHome(
                    state = state,
                    profile = profile,
                    onRefresh = { refresh() },
                    onSignOut = ::signOut,
                    onStatusChange = ::updateOrder,
                    onToggleFood = ::toggleFood,
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

    Surface(modifier = Modifier.fillMaxSize(), color = Paper) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            item {
                AuthHero()
                Spacer(Modifier.height(18.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        RoleSelector(
                            selectedRole = state.activeRole,
                            onRoleChange = onRoleChange
                        )
                        Spacer(Modifier.height(14.dp))

                        if (registerEnabled) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ModeButton(
                                    selected = state.authMode == AuthMode.LOGIN,
                                    text = "Login",
                                    onClick = { onModeChange(AuthMode.LOGIN) },
                                    modifier = Modifier.weight(1f)
                                )
                                ModeButton(
                                    selected = state.authMode == AuthMode.REGISTER,
                                    text = "Register",
                                    onClick = { onModeChange(AuthMode.REGISTER) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                        } else {
                            StaffLoginHeader()
                            Spacer(Modifier.height(14.dp))
                        }

                        if (isRegister) {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Full name") },
                                leadingIcon = { Icon(Icons.Outlined.PersonAdd, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { onSubmit(fullName, email, password) },
                            enabled = email.isNotBlank() &&
                                password.length >= 6 &&
                                (!isRegister || fullName.isNotBlank()),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Ink),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(
                                imageVector = if (isRegister) Icons.Outlined.PersonAdd else Icons.AutoMirrored.Outlined.Login,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (isRegister) "Create student account" else "Login")
                        }
                    }
                }

                MessageBanner(state.message)
            }
        }
    }
}

@Composable
private fun AuthHero() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(EspressoBrush)
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(size = 48.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "College Canteen",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Order - Track - Pickup",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroPill("Students", Color.White.copy(alpha = 0.10f), Color.White)
                HeroPill("Staff", Color.White.copy(alpha = 0.10f), Color.White)
                HeroPill("Live tracking", Color.White.copy(alpha = 0.10f), Color.White)
            }
        }
    }
}

@Composable
private fun StaffLoginHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = FlameLight
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = Flame)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Canteen staff login", fontWeight = FontWeight.SemiBold, color = Ink)
                Text(
                    "Register option disabled",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BrandMark(size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(FlameBrush),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Restaurant,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.58f)
        )
    }
}

@Composable
private fun HeroPill(text: String, background: Color, content: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = background) {
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
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
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
                shape = RoundedCornerShape(8.dp),
                color = color
            ) {
                Button(
                    onClick = { onRoleChange(role) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    elevation = null,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text(role.label, fontWeight = FontWeight.SemiBold)
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
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(44.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Leaf)
        ) {
            Text(text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(44.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(text)
        }
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
    onTabChange: (StudentTab) -> Unit
) {
    val cartLines = state.cartLines()
    val activeOrders = state.orders.count { OrderStatus.fromValue(it.order.status) != OrderStatus.COMPLETED }
    val categories = remember(state.menu) {
        listOf("All") + state.menu.mapNotNull { it.category?.takeIf(String::isNotBlank) }.distinct()
    }
    val filteredMenu = state.menu.filter { item ->
        val categoryMatch = state.selectedCategory == "All" || item.category == state.selectedCategory
        val searchMatch = state.searchQuery.isBlank() ||
            item.name.contains(state.searchQuery, ignoreCase = true) ||
            item.description.orEmpty().contains(state.searchQuery, ignoreCase = true)
        categoryMatch && searchMatch
    }

    Scaffold(
        containerColor = Paper,
        topBar = {
            AppTopBar(
                title = "Student",
                subtitle = profile.fullName ?: "Student",
                onRefresh = onRefresh,
                onSignOut = onSignOut
            )
        },
        bottomBar = {
            Column {
                if (cartLines.isNotEmpty() && state.studentTab == StudentTab.MENU) {
                    CartBar(
                        cartLines = cartLines,
                        notes = state.notes,
                        onNotesChange = onNotesChange,
                        onPlaceOrder = onPlaceOrder
                    )
                }
                StudentBottomNav(
                    selectedTab = state.studentTab,
                    cartCount = cartLines.sumOf { it.quantity },
                    onTabChange = onTabChange
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            DashboardHeader(
                title = "Today's menu",
                subtitle = "Choose items and place pickup order",
                stats = listOf(
                    StatItem("Menu", state.menu.size.toString(), Leaf),
                    StatItem("Cart", cartLines.sumOf { it.quantity }.toString(), Coral),
                    StatItem("Active", activeOrders.toString(), Sky)
                )
            )
            MessageBanner(state.message)

            when (state.studentTab) {
                StudentTab.MENU -> {
                    MenuControls(
                        query = state.searchQuery,
                        categories = categories,
                        selectedCategory = state.selectedCategory,
                        onSearchChange = onSearchChange,
                        onCategoryChange = onCategoryChange
                    )
                    MenuList(
                        menu = filteredMenu,
                        cart = state.cart,
                        canteenMode = false,
                        emptyText = "No items found.",
                        onAdd = onAdd,
                        onRemove = onRemove,
                        onToggleFood = null
                    )
                }

                StudentTab.ORDERS -> OrdersList(
                    orders = state.orders,
                    canteenMode = false,
                    onStatusChange = null
                )
            }
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
    onTabChange: (CanteenTab) -> Unit
) {
    val pending = state.orders.count { OrderStatus.fromValue(it.order.status) == OrderStatus.PENDING }
    val ready = state.orders.count { OrderStatus.fromValue(it.order.status) == OrderStatus.READY }

    Scaffold(
        containerColor = Paper,
        topBar = {
            AppTopBar(
                title = "Canteen",
                subtitle = profile.fullName ?: "Staff",
                onRefresh = onRefresh,
                onSignOut = onSignOut
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            DashboardHeader(
                title = "Order counter",
                subtitle = "Prepare packs and handover quickly",
                stats = listOf(
                    StatItem("Received", pending.toString(), Coral),
                    StatItem("Packed", ready.toString(), Leaf),
                    StatItem("Menu", state.menu.size.toString(), Sky)
                )
            )
            CanteenTabs(state.canteenTab, onTabChange)
            MessageBanner(state.message)

            when (state.canteenTab) {
                CanteenTab.ORDERS -> OrdersList(
                    orders = state.orders,
                    canteenMode = true,
                    onStatusChange = onStatusChange
                )

                CanteenTab.MENU -> MenuList(
                    menu = state.menu,
                    cart = emptyMap(),
                    canteenMode = true,
                    emptyText = "No menu items.",
                    onAdd = { _ -> },
                    onRemove = { _ -> },
                    onToggleFood = onToggleFood
                )
            }
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
    onTabChange: (StudentTab) -> Unit
) {
    Surface(
        color = Color.White,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                selected = selectedTab == StudentTab.MENU,
                label = "Menu",
                icon = { Icon(Icons.Outlined.Restaurant, contentDescription = null) },
                onClick = { onTabChange(StudentTab.MENU) }
            )
            BottomNavItem(
                selected = selectedTab == StudentTab.MENU && cartCount > 0,
                label = "Cart",
                badge = cartCount.takeIf { it > 0 },
                icon = { Icon(Icons.Outlined.ShoppingCart, contentDescription = null) },
                onClick = { onTabChange(StudentTab.MENU) }
            )
            BottomNavItem(
                selected = selectedTab == StudentTab.ORDERS,
                label = "Orders",
                icon = { Icon(Icons.Outlined.Receipt, contentDescription = null) },
                onClick = { onTabChange(StudentTab.ORDERS) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    selected: Boolean,
    label: String,
    badge: Int? = null,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = if (selected) Flame else Steel
        ),
        elevation = null,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                icon()
                if (badge != null) {
                    Surface(
                        shape = CircleShape,
                        color = Flame,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(17.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = badge.toString(),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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

private fun OrderStatus.actionColor(): Color =
    when (this) {
        OrderStatus.PENDING -> Coral
        OrderStatus.PREPARING -> Color(0xFFB47B00)
        OrderStatus.READY -> Leaf
        OrderStatus.COMPLETED -> Sky
        OrderStatus.CANCELLED -> Color(0xFF707871)
    }

private fun currency(value: Double): String =
    "Rs. %.2f".format(Locale.US, value)

private fun shortDate(value: String?): String =
    value?.replace("T", " ")?.take(16) ?: "-"

private fun Throwable.userMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: "Something went wrong."
