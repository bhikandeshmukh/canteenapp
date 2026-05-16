package com.collegecanteen.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class UserRole(val value: String, val label: String) {
    STUDENT("student", "Student"),
    CANTEEN("canteen", "Canteen");

    companion object {
        fun fromValue(value: String?): UserRole =
            entries.firstOrNull { it.value == value } ?: STUDENT
    }
}

enum class OrderStatus(val value: String, val label: String) {
    PENDING("pending", "Received"),
    PREPARING("preparing", "Preparing"),
    READY("ready", "Packed"),
    COMPLETED("completed", "Handed over"),
    CANCELLED("cancelled", "Cancelled");

    companion object {
        fun fromValue(value: String?): OrderStatus =
            entries.firstOrNull { it.value == value } ?: PENDING
    }
}

@Serializable
data class Profile(
    val id: String,
    @SerialName("full_name") val fullName: String? = null,
    val role: String = UserRole.STUDENT.value,
    val phone: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class FoodItem(
    val id: String,
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val price: Double,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("is_available") val isAvailable: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class FoodOrder(
    val id: String,
    @SerialName("student_id") val studentId: String,
    val status: String,
    @SerialName("total_amount") val totalAmount: Double,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class OrderItem(
    val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("food_item_id") val foodItemId: String,
    @SerialName("item_name") val itemName: String,
    @SerialName("item_price") val itemPrice: Double,
    val quantity: Int
)

@Serializable
data class OrderInsert(
    @SerialName("student_id") val studentId: String,
    @SerialName("total_amount") val totalAmount: Double,
    val notes: String? = null
)

@Serializable
data class OrderItemInsert(
    @SerialName("order_id") val orderId: String,
    @SerialName("food_item_id") val foodItemId: String,
    @SerialName("item_name") val itemName: String,
    @SerialName("item_price") val itemPrice: Double,
    val quantity: Int
)

@Serializable
data class OrderStatusUpdate(
    val status: String
)

@Serializable
data class FoodAvailabilityUpdate(
    @SerialName("is_available") val isAvailable: Boolean
)

data class CartLine(
    val item: FoodItem,
    val quantity: Int
) {
    val lineTotal: Double = item.price * quantity
}

data class OrderWithItems(
    val order: FoodOrder,
    val items: List<OrderItem>
)
