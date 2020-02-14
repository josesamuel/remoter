package util.remoter.remoterclient

import android.app.Service
import android.content.Intent
import util.remoter.service.ISampleKotlinService_Stub

class SampleKotlinService : Service() {

    private val binder by lazy {
        ISampleKotlinService_Stub(KotlinServiceImpl())
    }

    override fun onBind(intent: Intent?) = binder
}