package com.oblab.toiletmap.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.oblab.toiletmap.ToiletMapApp

/** 등록 때 못 채운 주소를 네트워크가 연결되면 채운다. */
class AddressWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val done = (applicationContext as ToiletMapApp).repository.fillMissingAddresses()
        // 바다 위처럼 주소가 아예 없는 좌표도 있어서 무한 재시도는 하지 않는다
        return if (done || runAttemptCount >= MAX_ATTEMPTS) Result.success() else Result.retry()
    }

    companion object {
        private const val MAX_ATTEMPTS = 5

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<AddressWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            // 이미 예약돼 있으면 그대로 둔다. 실행될 때 주소 없는 장소를 모두 다시 조회하므로 충분하다
            WorkManager.getInstance(context).enqueueUniqueWork("fill-addresses", ExistingWorkPolicy.KEEP, request)
        }
    }
}
