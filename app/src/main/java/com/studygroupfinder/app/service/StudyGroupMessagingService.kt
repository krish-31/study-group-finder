package com.studygroupfinder.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.studygroup.finder.data.model.Notification
import com.studygroupfinder.app.MainActivity
import com.studygroupfinder.app.R

/**
 * Firebase Cloud Messaging service that handles incoming push notifications
 * and token refresh events for study group updates.
 *
 * On message received:
 * 1. Displays a system notification.
 * 2. Persists the notification to the Firestore `notifications` collection
 *    so it appears on the in-app Notifications screen.
 *
 * On token refresh:
 * - Updates the current user's `fcmToken` field in the `users` collection.
 */
class StudyGroupMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "study_group_notifications"
        private const val CHANNEL_NAME = "Study Group Updates"
    }

    // ── Token refresh ───────────────────────────────

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        updateFcmTokenInFirestore(token)
    }

    /**
     * Persist the FCM token to the current user's Firestore document
     * so the backend can target push notifications.
     */
    private fun updateFcmTokenInFirestore(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .update("fcmToken", token)
    }

    // ── Message received ────────────────────────────

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Study Group Finder"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: ""
        val type = message.data["type"] ?: Notification.TYPE_ANNOUNCEMENT

        // 1. Display a system notification
        showNotification(title = title, body = body)

        // 2. Save to Firestore so it appears on the in-app screen
        saveNotificationToFirestore(title = title, body = body, type = type)
    }

    /**
     * Write the notification payload into the Firestore `notifications`
     * collection for the currently signed-in user.
     */
    private fun saveNotificationToFirestore(
        title: String,
        body: String,
        type: String
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val notification = hashMapOf(
            "userId" to userId,
            "title" to title,
            "message" to body,
            "type" to type,
            "isRead" to false,
            "createdAt" to System.currentTimeMillis()
        )

        FirebaseFirestore.getInstance()
            .collection(Notification.COLLECTION)
            .add(notification)
    }

    // ── System notification display ─────────────────

    private fun showNotification(title: String, body: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for study group activity"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
