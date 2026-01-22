package com.fcul.smartboy.repository

import com.fcul.smartboy.domain.cart.Cart
import com.fcul.smartboy.domain.cart.CartMetadata
import com.fcul.smartboy.domain.inventory.SellingItem
import com.fcul.smartboy.domain.inventory.SellingItemEntity
import com.fcul.smartboy.repository.base.CRUD
import com.fcul.smartboy.repository.base.CRUD.Companion.awaitTask
import com.fcul.smartboy.repository.base.Path
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import jakarta.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class CartRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : CRUD<Cart, Long> {

    private val user: FirebaseUser?
        get() = auth.currentUser

    private val col get() = firestore.collection(Path.USERS.path)

    fun observeCarts(): Flow<List<Cart>> = callbackFlow {
        val user = user ?: run {
            close()
            return@callbackFlow
        }

        val cartsRef = col.document(user.uid)
            .collection(Path.CART.path)

        val listener = cartsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val carts = snapshot?.documents?.mapNotNull { cartDoc ->
                val metadata = cartDoc.toObject(CartMetadata::class.java) ?: return@mapNotNull null

                cartDoc.reference
                    .collection(Path.CART_ITEMS.path)
                    .get()
                    .addOnSuccessListener { itemsSnapshot ->
                        val cartItems: List<SellingItem> =
                            itemsSnapshot.documents.mapNotNull { doc ->
                                doc.toObject(SellingItemEntity::class.java)?.toSellingItem()
                            }

                        trySend(
                            listOf(
                                Cart(
                                    userId = metadata.userId,
                                    userName = metadata.userName,
                                    sellerId = metadata.sellerId,
                                    sellerName = metadata.sellerName,
                                    totalPrice = metadata.totalPrice,
                                    items = cartItems
                                )
                            )
                        )
                    }

                null
            } ?: emptyList()

            if (carts.isEmpty()) {
                trySend(emptyList())
            }
        }

        awaitClose { listener.remove() }
    }

    override suspend fun create(document: Cart): Long {
        val user = user ?: return -1
        val userId = document.userId ?: return -1
        val id = userId.hashCode().toLong()

        val docRef = col.document(user.uid)
            .collection(Path.CART.path)
            .document(id.toString())

        val entity = CartMetadata(
            userId = document.userId,
            userName = document.userName,
            sellerId = document.sellerId,
            sellerName = document.sellerName,
            totalPrice = document.totalPrice
        )

        docRef.set(entity).awaitTask()

        val cartRef = docRef
            .collection(Path.CART_ITEMS.path)

        document.items.forEach { item ->
            val itemEntity = item.toEntity()
            cartRef.document(itemEntity.id.toString())
                .set(itemEntity).awaitTask()
        }

        return id
    }

    override suspend fun read(id: Long): Cart? {
        val user = user ?: return null

        val cartRef = col.document(user.uid)
            .collection(Path.CART.path)
            .document(id.toString())

        val metadata = cartRef.get().awaitTask()
            .toObject(CartMetadata::class.java) ?: return null

        val cartItemsSnapshot = cartRef
            .collection(Path.CART_ITEMS.path)
            .get().awaitTask()

        val cartItems: List<SellingItem> = cartItemsSnapshot.documents.mapNotNull { doc ->
            doc.toObject(SellingItemEntity::class.java)?.toSellingItem()
        }

        return Cart(
            userId = metadata.userId,
            userName = metadata.userName,
            sellerId = metadata.sellerId,
            sellerName = metadata.sellerName,
            totalPrice = metadata.totalPrice,
            items = cartItems
        )
    }

    override suspend fun update(id: Long, data: Any): Boolean {
        val user = user ?: return false

        val cart = data as Cart

        val cartRef = col.document(user.uid)
            .collection(Path.CART.path)
            .document(id.toString())

        val entity = CartMetadata(
            userId = cart.userId,
            userName = cart.userName,
            sellerId = cart.sellerId,
            sellerName = cart.sellerName,
            totalPrice = cart.items.fold(0) { acc, item -> acc + item.valuePerUnit * item.quantity }
        )

        cartRef.set(entity).awaitTask()

        val cartItemsRef = cartRef
            .collection(Path.CART_ITEMS.path)

        cartItemsRef.get().awaitTask().documents.forEach { doc ->
            doc.reference.delete().awaitTask()
        }

        cart.items.forEach { item ->
            val itemEntity = item.toEntity()
            cartItemsRef.document(itemEntity.id.toString())
                .set(itemEntity).awaitTask()
        }

        return true
    }

    override suspend fun delete(id: Long): Boolean {
        val user = user ?: return false

        val cartRef = col.document(user.uid)
            .collection(Path.CART.path)
            .document(id.toString())

        cartRef.delete().awaitTask()
        return true
    }
}