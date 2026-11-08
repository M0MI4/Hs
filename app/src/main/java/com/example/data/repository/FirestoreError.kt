package com.example.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException

enum class OperationType {
    CREATE,
    READ,
    UPDATE,
    DELETE,
    LIST
}

fun handleFirestoreError(exception: Exception, operation: OperationType, path: String) {
    val firestoreException = exception as? FirebaseFirestoreException
    val code = firestoreException?.code?.name ?: "UNKNOWN"
    val message = exception.message ?: "No error message provided"

    Log.e(
        "FirestoreError",
        """
        {
          "operation": "${operation.name}",
          "path": "$path",
          "code": "$code",
          "message": "$message"
        }
        """.trimIndent(),
        exception
    )
}
