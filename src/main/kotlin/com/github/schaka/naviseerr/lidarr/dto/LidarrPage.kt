package com.github.schaka.naviseerr.lidarr.dto

import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class LidarrPage<T : Any>(
    page: Int,
    pageSize: Int,
    totalRecords: Long,
    records: List<T>,
) : PageImpl<T>(records, PageRequest.of(page-1, pageSize), totalRecords) {


}