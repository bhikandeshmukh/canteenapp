package com.collegecanteen.app.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class CanteenRepository(
    private val client: SupabaseClient
) {
    suspend fun restoreSession(): Profile? {
        client.auth.awaitInitialization()
        val user = client.auth.currentUserOrNull() ?: return null
        return loadProfile(user.id)
    }

    suspend fun signIn(email: String, password: String, expectedRole: UserRole): Profile {
        client.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }

        val profile = loadCurrentProfile()
        if (profile.role != expectedRole.value) {
            client.auth.signOut()
            error("This account is registered as ${UserRole.fromValue(profile.role).label}.")
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
        client.auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
            data = buildJsonObject {
                put("full_name", fullName.trim())
                put("role", role.value)
                if (role == UserRole.CANTEEN && inviteCode.isNotBlank()) {
                    put("invite_code", inviteCode.trim())
                }
            }
        }

        val user = client.auth.currentUserOrNull() ?: return null
        val profile = loadProfile(user.id)
        if (profile.role != role.value) {
            client.auth.signOut()
            error("Invalid canteen invite code.")
        }
        return profile
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

    private fun currentUserId(): String {
        return client.auth.currentUserOrNull()?.id ?: error("Please login again.")
    }
}
