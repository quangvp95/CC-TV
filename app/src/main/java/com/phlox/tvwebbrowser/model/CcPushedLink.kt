package com.phlox.tvwebbrowser.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class CcPushedLink(
    var sender: String? = null,
    var receiver: String? = null,
    var timestamp: String? = null,
    var url: String? = null
)
