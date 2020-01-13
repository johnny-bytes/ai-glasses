package com.schloesser.masterthesis.data.response

import com.schloesser.masterthesis.entity.ClassSession

data class GetSessionsResponse(
    var results: List<ClassSession>
)