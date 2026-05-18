package com.collegecanteen.app.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc

class CanteenRepository(
    private val client: SupabaseClient
) {
    private companion object {
        const val ADMIN_EMAIL = "admin@gmail.com"
    }

    suspend fun restoreSession(): Profile? {
        client.auth.awaitInitialization()
        val user = client.auth.currentUserOrNull() ?: return null
        return loadProfile(user.id)
    }

    suspend fun signIn(email: String, password: String, expectedRole: UserRole): Profile {
        val normalizedEmail = email.trim().lowercase()
        client.auth.signInWith(Email) {
            this.email = normalizedEmail
            this.password = password
        }

        val profile = loadCurrentProfile()
        if (normalizedEmail == ADMIN_EMAIL) {
            return profile.copy(
                fullName = profile.fullName?.takeIf { it.isNotBlank() } ?: "Admin",
                role = expectedRole.value
            )
        }
        if (profile.role != expectedRole.value) {
            client.auth.signOut()
            error("This account is registered as ${UserRole.fromValue(profile.role).label}.")
        }
        if (expectedRole == UserRole.STUDENT && !hasApprovedStudentRequest(profile.id)) {
            client.auth.signOut()
            error("Your account is waiting for admin approval.")
        }
        return profile
    }

    suspend fun register(
        fullName: String,
        email: String,
        password: String,
        role: UserRole,
        inviteCode: String
    ): Profile? {
        require(role == UserRole.STUDENT) { "Only student registration requests are supported." }
        val normalizedEmail = email.trim().lowercase()
        require(normalizedEmail != ADMIN_EMAIL) { "Admin account already exists. Please use login." }
        client.postgrest.rpc(
            "submit_student_access_request",
            AccessRequestInsert(
                fullName = fullName.trim(),
                email = normalizedEmail,
                password = password
            )
        )
        return null
    }

    suspend fun loadAccessRequests(): List<AccessRequest> {
        return client.postgrest["student_access_requests"].select {
            order("requested_at", Order.DESCENDING)
            limit(100L)
        }.decodeList()
    }

    suspend fun updateAccessRequestStatus(requestId: String, status: String) {
        client.postgrest["student_access_requests"].update(AccessRequestStatusUpdate(status)) {
            filter { eq("id", requestId) }
        }
    }

    suspend fun signOut() {
        client.auth.signOut()
    }

    suspend fun loadMenu(forCanteen: Boolean): List<FoodItem> {
        return client.postgrest["food_items"].select {
            if (!forCanteen) {
                filter { eq("is_available", true) }
            }
            order("name", Order.ASCENDING)
        }.decodeList()
    }

    suspend fun updateFoodAvailability(itemId: String, isAvailable: Boolean) {
        client.postgrest["food_items"].update(FoodAvailabilityUpdate(isAvailable)) {
            filter { eq("id", itemId) }
        }
    }

    suspend fun addFoodItem(item: FoodItemWrite) {
        client.postgrest["food_items"].insert(item)
    }

    suspend fun updateFoodItem(itemId: String, item: FoodItemWrite) {
        client.postgrest["food_items"].update(item) {
            filter { eq("id", itemId) }
        }
    }

    suspend fun placeOrder(cart: List<CartLine>, notes: String): FoodOrder {
        require(cart.isNotEmpty()) { "Cart is empty." }

        val studentId = currentUserId()
        val total = cart.sumOf { it.lineTotal }
        val order = client.postgrest["orders"].insert(
            OrderInsert(
                studentId = studentId,
                totalAmount = total,
                notes = notes.trim().ifBlank { null }
            )
        ) {
            select()
        }.decodeSingle<FoodOrder>()

        val orderItems = cart.map { line ->
            OrderItemInsert(
                orderId = order.id,
                foodItemId = line.item.id,
                itemName = line.item.name,
                itemPrice = line.item.price,
                quantity = line.quantity
            )
        }
        client.postgrest["order_items"].insert(orderItems)
        return order
    }

    suspend fun loadOrders(role: UserRole): List<OrderWithItems> {
        val userId = currentUserId()
        val orders = client.postgrest["orders"].select {
            if (role == UserRole.STUDENT) {
                filter { eq("student_id", userId) }
            }
            order("created_at", Order.DESCENDING)
            limit(75L)
        }.decodeList<FoodOrder>()

        if (orders.isEmpty()) return emptyList()

        val allVisibleItems = client.postgrest["order_items"].select {
            order("item_name", Order.ASCENDING)
        }.decodeList<OrderItem>()

        val studentsById = if (role == UserRole.CANTEEN) {
            client.postgrest["profiles"].select {
                order("full_name", Order.ASCENDING)
            }.decodeList<Profile>().associateBy { it.id }
        } else {
            emptyMap()
        }

        return orders.map { order ->
            OrderWithItems(
                order = order,
                items = allVisibleItems.filter { it.orderId == order.id },
                student = studentsById[order.studentId]
            )
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: OrderStatus) {
        client.postgrest["orders"].update(OrderStatusUpdate(status.value)) {
            filter { eq("id", orderId) }
        }
    }

    private suspend fun loadCurrentProfile(): Profile {
        val user = client.auth.currentUserOrNull() ?: error("Session not found.")
        return loadProfile(user.id)
    }

    private suspend fun loadProfile(userId: String): Profile {
        return client.postgrest["profiles"].select {
            filter { eq("id", userId) }
            limit(1L)
        }.decodeSingle()
    }

    private suspend fun hasApprovedStudentRequest(userId: String): Boolean {
        return client.postgrest["student_access_requests"].select {
            filter {
                eq("user_id", userId)
                eq("status", "approved")
            }
            limit(1L)
        }.decodeList<AccessRequest>().isNotEmpty()
    }

    private fun currentUserId(): String {
        return client.auth.currentUserOrNull()?.id ?: error("Please login again.")
    }
}
