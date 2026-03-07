package com.github.schaka.naviseerr.download.processing

import org.springframework.stereotype.Service

@Service
class DummyAcoustIdService : AcoustIdService {

    override fun recognize(filePath: String): AcoustIdResult = AcoustIdResult.RECOGNIZED
}
