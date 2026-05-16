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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    val studentTab: StudentTab = StudentTab.MENU,
    val canteenTab: CanteenTab = CanteenTab.ORDERS,
    val loading: Boolean = false,
    val message: String? = null
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
                    state = state.copy(
                        menu = data.menu,
                        orders = data.orders,
                        loading = false
                    )
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

    fun authenticate(
        fullName: String,
        email: String,
        password: String,
        inviteCode: String
    ) {
        scope.launch {
            state = state.copy(loading = true, message = null)
            runCatching {
                if (state.authMode == AuthMode.LOGIN) {
                    repository.signIn(email, password, state.activeRole)
                } else {
                    repository.register(fullName, email, password, state.activeRole, inviteCode)
                }
            }.onSuccess { profile ->
                if (profile == null) {
                    state = state.copy(
                        loading = false,
                        message = "Account created. Check email verification, then login."
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
            state = state.copy(loading = true, message = null)
            runCatching { repository.signOut() }
                .onSuccess { state = AppUiState(activeRole = state.activeRole) }
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
                val profile = state.profile ?: error("Please login again.")
                loadDashboard(profile)
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
                val profile = state.profile ?: error("Please login again.")
                loadDashboard(profile)
            }.onSuccess { data ->
                state = state.copy(
                    orders = data.orders,
                    menu = data.menu,
                    loading = false
                )
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
                val profile = state.profile ?: error("Please login again.")
                loadDashboard(profile)
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
            .background(MaterialTheme.colorScheme.background)
    ) {
        val profile = state.profile
        if (profile == null) {
            AuthScreen(
                state = state,
                onRoleChange = { state = state.copy(activeRole = it) },
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
                    .align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun MissingConfigScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Supabase config missing",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
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
    onSubmit: (String, String, String, String) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "College Canteen",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(24.dp))

                RoleSelector(
                    selectedRole = state.activeRole,
                    onRoleChange = onRoleChange
                )
                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModeButton(
                        selected = state.authMode == AuthMode.LOGIN,
                        text = "Login",
                        onClick = { onModeChange(AuthMode.LOGIN) }
                    )
                    ModeButton(
                        selected = state.authMode == AuthMode.REGISTER,
                        text = "Register",
                        onClick = { onModeChange(AuthMode.REGISTER) }
                    )
                }
                Spacer(Modifier.height(16.dp))

                if (state.authMode == AuthMode.REGISTER) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.authMode == AuthMode.REGISTER && state.activeRole == UserRole.CANTEEN) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = inviteCode,
                        onValueChange = { inviteCode = it },
                        label = { Text("Canteen invite code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = { onSubmit(fullName, email, password, inviteCode) },
                    enabled = email.isNotBlank() && password.length >= 6 &&
                        (state.authMode == AuthMode.LOGIN || fullName.isNotBlank()) &&
                        (state.authMode == AuthMode.LOGIN ||
                            state.activeRole != UserRole.CANTEEN ||
                            inviteCode.isNotBlank()),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (state.authMode == AuthMode.LOGIN) Icons.Outlined.Login else Icons.Outlined.PersonAdd,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.authMode == AuthMode.LOGIN) "Login" else "Create account")
                }

                MessageBanner(state.message)
            }
        }
    }
}

@Composable
private fun RoleSelector(
    selectedRole: UserRole,
    onRoleChange: (UserRole) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        UserRole.entries.forEach { role ->
            val selected = selectedRole == role
            val modifier = Modifier.weight(1f)
            if (selected) {
                Button(
                    onClick = { onRoleChange(role) },
                    modifier = modifier
                ) {
                    Text(role.label)
                }
            } else {
                OutlinedButton(
                    onClick = { onRoleChange(role) },
                    modifier = modifier
                ) {
                    Text(role.label)
                }
            }
        }
    }
}

@Composable
private fun ModeButton(selected: Boolean, text: String, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(text) }
    } else {
        OutlinedButton(onClick = onClick) { Text(text) }
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
    onTabChange: (StudentTab) -> Unit
) {
    val cartLines = state.cartLines()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Student",
                subtitle = profile.fullName ?: "Student",
                onRefresh = onRefresh,
                onSignOut = onSignOut
            )
        },
        bottomBar = {
            if (cartLines.isNotEmpty() && state.studentTab == StudentTab.MENU) {
                CartBar(
                    cartLines = cartLines,
                    notes = state.notes,
                    onNotesChange = onNotesChange,
                    onPlaceOrder = onPlaceOrder
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = state.studentTab.ordinal) {
                Tab(
                    selected = state.studentTab == StudentTab.MENU,
                    onClick = { onTabChange(StudentTab.MENU) },
                    icon = { Icon(Icons.Outlined.Restaurant, contentDescription = null) },
                    text = { Text("Menu") }
                )
                Tab(
                    selected = state.studentTab == StudentTab.ORDERS,
                    onClick = { onTabChange(StudentTab.ORDERS) },
                    icon = { Icon(Icons.Outlined.Receipt, contentDescription = null) },
                    text = { Text("Orders") }
                )
            }

            MessageBanner(state.message)

            when (state.studentTab) {
                StudentTab.MENU -> MenuList(
                    menu = state.menu,
                    cart = state.cart,
                    canteenMode = false,
                    onAdd = onAdd,
                    onRemove = onRemove,
                    onToggleFood = null
                )

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
    Scaffold(
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
            TabRow(selectedTabIndex = state.canteenTab.ordinal) {
                Tab(
                    selected = state.canteenTab == CanteenTab.ORDERS,
                    onClick = { onTabChange(CanteenTab.ORDERS) },
                    icon = { Icon(Icons.Outlined.Receipt, contentDescription = null) },
                    text = { Text("Orders") }
                )
                Tab(
                    selected = state.canteenTab == CanteenTab.MENU,
                    onClick = { onTabChange(CanteenTab.MENU) },
                    icon = { Icon(Icons.Outlined.Restaurant, contentDescription = null) },
                    text = { Text("Menu") }
                )
            }

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
        title = {
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
            }
            IconButton(onClick = onSignOut) {
                Icon(Icons.Outlined.Logout, contentDescription = "Logout")
            }
        }
    )
}

@Composable
private fun MenuList(
    menu: List<FoodItem>,
    cart: Map<String, Int>,
    canteenMode: Boolean,
    onAdd: (FoodItem) -> Unit,
    onRemove: (FoodItem) -> Unit,
    onToggleFood: ((FoodItem, Boolean) -> Unit)?
) {
    if (menu.isEmpty()) {
        EmptyState(text = if (canteenMode) "No menu items." else "Menu is empty.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
        item { Spacer(Modifier.height(90.dp)) }
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!item.category.isNullOrBlank()) {
                        Text(
                            item.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Text(
                    currency(item.price),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!item.description.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    Switch(
                        checked = item.isAvailable,
                        onCheckedChange = onToggleAvailable
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (quantity > 0) {
                        IconButton(onClick = onRemove) {
                            Icon(Icons.Outlined.Remove, contentDescription = "Remove")
                        }
                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Button(onClick = onAdd) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add")
                    }
                }
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
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
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
                    Icon(Icons.Outlined.ShoppingCart, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${cartLines.sumOf { it.quantity }} items",
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    currency(cartLines.sumOf { it.lineTotal }),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("Notes") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onPlaceOrder,
                modifier = Modifier.fillMaxWidth()
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
    val nextStatuses = when (status) {
        OrderStatus.PENDING -> listOf(OrderStatus.PREPARING)
        OrderStatus.PREPARING -> listOf(OrderStatus.READY)
        OrderStatus.READY -> listOf(OrderStatus.COMPLETED)
        OrderStatus.COMPLETED,
        OrderStatus.CANCELLED -> emptyList()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Order #${order.id.take(8)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        shortDate(order.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusChip(status.label)
            }

            Spacer(Modifier.height(10.dp))
            orderWithItems.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${item.quantity} x ${item.itemName}")
                    Text(currency(item.itemPrice * item.quantity))
                }
            }

            if (!order.notes.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Note: ${order.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    currency(order.totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (canteenMode && nextStatuses.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        nextStatuses.forEach { next ->
                            Button(onClick = { onStatusChange?.invoke(order.id, next) }) {
                                Icon(Icons.Outlined.Check, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(next.label)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium
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
            Icon(
                imageVector = Icons.Outlined.Restaurant,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(34.dp)
            )
            Spacer(Modifier.height(8.dp))
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

private fun currency(value: Double): String =
    "Rs. %.2f".format(Locale.US, value)

private fun shortDate(value: String?): String =
    value?.replace("T", " ")?.take(16) ?: "-"

private fun Throwable.userMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: "Something went wrong."
